package pixel.auxframework.component.factory

import arrow.core.Some
import arrow.core.firstOrNone
import arrow.core.getOrElse
import arrow.core.toOption
import kotlinx.coroutines.runBlocking
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.implementation.InvocationHandlerAdapter
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import pixel.auxframework.component.annotation.*
import pixel.auxframework.context.AuxContext
import pixel.auxframework.context.builtin.AbstractComponentMethodInvocationHandler
import pixel.auxframework.util.toClass
import pixel.auxframework.util.toParameterized
import java.lang.reflect.Array
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

/**
 * 在组件处理时发送错误
 */
class ComponentProcessingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class ComponentProcessor(private val context: AuxContext) {

    open fun scanComponents(classLoaders: Set<ClassLoader>, defineComponents: Boolean = true, block: ConfigurationBuilder.() -> Unit = {}): Set<KClass<*>> {
        val classes = mutableSetOf<KClass<*>>()
        for (classLoader in classLoaders) {
            val reflections = Reflections(
                ConfigurationBuilder()
                    .forPackages(*classLoader.definedPackages.map { it.name }.toTypedArray())
                    .addClassLoaders(classLoader)
                    .setScanners(Scanners.TypesAnnotated)
                    .also(block)
            )
            val types = reflections.getTypesAnnotatedWith(Component::class.java).filter {
                !it.isAnnotation
            }.filter {
                it.kotlin.findAnnotation<OnlyIn>().toOption().map { onlyIn ->
                    var accept = true
                    if (onlyIn.contextName != "<null>") accept = accept && context.name == onlyIn.contextName
                    accept = accept && onlyIn.contextType.all { type -> type.isInstance(context) }
                    accept
                }.getOrElse { true }
            }
            classes += types.map { it.kotlin }
            if (defineComponents) types.forEach { type -> context.componentFactory().defineComponent(ComponentDefinition(type.kotlin)) }
        }
        return classes
    }

    open fun initializeComponent(
        component: ComponentDefinition,
        dependencyStack: MutableList<ComponentDefinition> = mutableListOf()
    ) = component.also {
        if (component.isInitialized()) return@also
        try {
            component.setInstance(
                createComponentInstance(component, dependencyStack)
            )
        } catch (cause: Throwable) {
            throw ComponentProcessingException(
                "An error occurred while creating the component '${component.name}'",
                cause
            )
        }
    }

    open fun createAbstractComponentInstance(
        component: ComponentDefinition,
        dependencyStack: MutableList<ComponentDefinition> = mutableListOf()
    ): Any? {
        if (component.type.findAnnotation<Service>() != null) {
            val loaded = ServiceLoader.load(component.type.java).firstOrNull()
            if (loaded != null) return loaded
        }
        val handler = InvocationHandler { proxy, method, args ->
            val arguments = args ?: emptyArray()
            val abstractComponentMethodInvocationHandlers =
                context.componentFactory().getComponentDefinitions(AbstractComponentMethodInvocationHandler::class.java)
                    .map { initializeComponent(it).cast<AbstractComponentMethodInvocationHandler<Any?>>() }
            val handlers = abstractComponentMethodInvocationHandlers.filter {
                it::class.java.genericInterfaces.first { type ->
                    type.toParameterized().rawType.toClass()
                        .isSubclassOf(AbstractComponentMethodInvocationHandler::class)
                }.toParameterized().actualTypeArguments.first().toClass().isInstance(proxy)
            }
            for (handler in handlers) {
                val result = handler.handleAbstractComponentMethodInvocation(proxy, method, arguments)
                if (result is Some) return@InvocationHandler result.value
            }
            if (method.isDefault) method.invoke(proxy, *arguments)
            else throw UnsupportedOperationException()
        }
        val clazz = if (component.type.java.isInterface) ByteBuddy()
            .subclass(Any::class.java)
            .annotateType(*component.type.annotations.toTypedArray())
            .implement(component.type.java, DynamicComponent::class.java)
            .intercept(InvocationHandlerAdapter.of(handler))
            .make()
            .load(component.type.java.classLoader)
            .loaded
        else ByteBuddy()
            .subclass(component.type.java)
            .annotateType(*component.type.annotations.toTypedArray())
            .implement(DynamicComponent::class.java)
            .method(MethodDescription::isAbstract)
            .intercept(InvocationHandlerAdapter.of(handler))
            .make()
            .load(component.type.java.classLoader)
            .loaded
        val definition = ComponentDefinition(clazz.kotlin)
        context.componentFactory().defineComponent(definition)
        return createComponentInstance(definition, dependencyStack)
    }

    open fun createConcreteComponentInstance(
        componentDefinition: ComponentDefinition,
        dependencyStack: MutableList<ComponentDefinition>
    ): Any? {
        val constructor = componentDefinition.type.constructors
            .firstOrNone { it.findAnnotation<Autowired>() != null }
            .getOrElse { componentDefinition.type.constructors.first() }
        val actualArguments = constructor.parameters
            .associateWith { param -> autowire(param.type, param.annotations, dependencyStack) }
            .filter { !(it.key.isOptional && it.value == null) }
        return try {
            constructor.callBy(actualArguments)
        } catch (cause: InvocationTargetException) {
            throw ComponentProcessingException(
                "An error occurred while constructing component '${componentDefinition.name}' ($constructor)",
                cause
            )
        } catch (_: Throwable) {
            null
        }
    }

    open fun createComponentInstance(
        componentDefinition: ComponentDefinition,
        dependencyStack: MutableList<ComponentDefinition> = mutableListOf()
    ): Any? {
        if (componentDefinition.isLoaded() || componentDefinition.isInitialized()) return componentDefinition.cast()
        if (componentDefinition.type.findAnnotation<DoNotConstruct>() != null) return null
        if (componentDefinition in dependencyStack) throw StackOverflowError(dependencyStack.joinToString { it.name })
        dependencyStack.add(componentDefinition)
        val instance = when {
            componentDefinition.type.java.isInterface || componentDefinition.type.isAbstract -> createAbstractComponentInstance(
                componentDefinition,
                dependencyStack
            )

            else -> createConcreteComponentInstance(componentDefinition, dependencyStack)
        }
        return instance.also {
            if (it is PostConstruct) it.postConstruct()
        }
    }

    open fun autowire(
        type: KType,
        annotations: List<Annotation> = type.annotations,
        dependencyStack: MutableList<ComponentDefinition> = mutableListOf()
    ): Any? {
        for (annotation in annotations) {
            if (annotation is Autowired && !annotation.enable) return null
        }
        val classifier = type.toClass()
        if (classifier.java.isArray) {
            val arrayComponentType = classifier.java.componentType
            val qualifier = annotations.firstOrNull { it is Qualifier } as? Qualifier
            val components = context.componentFactory()
                .getComponentDefinitions(arrayComponentType)
                .filter {
                    qualifier == null || qualifier.name == it.name
                }
                .map {
                    initializeComponent(it, dependencyStack)
                    it.cast<Any>()
                }
            val array = Array.newInstance(arrayComponentType, components.size)
            components.forEachIndexed { index, element ->
                Array.set(array, index, element)
            }
            return array
        } else {
            val componentDefinition = context.componentFactory().getComponentDefinition(type, annotations)
            initializeComponent(componentDefinition, dependencyStack)
            return componentDefinition.cast()
        }
    }

    open fun autowireComponent(componentDefinition: ComponentDefinition) =
        initializeComponent(componentDefinition).also {
            if (componentDefinition.isLoaded()) return@also
            val instance = componentDefinition.cast<Any>()
            for (field in instance::class.memberProperties) {
                if (field.findAnnotation<Autowired>() == null) continue
                if (field is KMutableProperty<*>) {
                    val autowired = autowire(field.returnType, field.annotations)
                    field.setter.isAccessible = true
                    field.setter.call(instance, autowired)
                }
            }
            val component = componentDefinition.cast<Any>()
            for (member in component::class.memberFunctions) {
                member.findAnnotation<Component>() ?: continue
                val actualArguments = member.parameters.associateWith {
                    if (it.name == null) component else autowire(it.type, it.annotations)
                }
                val invocation = if (member.isSuspend) runBlocking {
                    member.callSuspendBy(actualArguments)
                } else member.callBy(actualArguments)
                if (invocation != null)
                    context.componentFactory()
                        .defineComponent(
                            ComponentDefinition(
                                invocation,
                                name = "${componentDefinition.name}::${member.name}"
                            )
                        )
            }
            if (instance is AfterComponentAutowired) instance.afterComponentAutowired()
        }

}
