package pixel.auxframework.component.factory

import arrow.core.Some
import arrow.core.firstOrNone
import arrow.core.getOrElse
import kotlinx.coroutines.runBlocking
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import pixel.auxframework.component.annotation.Autowired
import pixel.auxframework.component.annotation.Component
import pixel.auxframework.component.annotation.Qualifier
import pixel.auxframework.context.AuxContext
import pixel.auxframework.context.builtin.AbstractComponentMethodInvocationHandler
import pixel.auxframework.util.toClass
import pixel.auxframework.util.toParameterized
import java.lang.reflect.Array
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

/**
 * 在组件处理时发送错误
 */
class ComponentProcessingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class ComponentProcessor(private val context: AuxContext) {

    open fun initializeComponent(
        component: ComponentDefinition,
        stack: MutableList<ComponentDefinition> = mutableListOf()
    ) = component.also {
        if (component.isInitialized()) return@also
        try {
            component.setInstance(createComponentInstance(component, stack))
        } catch (cause: Throwable) {
            throw ComponentProcessingException(
                "An error occurred while creating the component '${component.name}'",
                cause
            )
        }
    }

    open fun createInterfaceComponentInstance(
        component: ComponentDefinition,
        dependencyStack: MutableList<ComponentDefinition> = mutableListOf()
    ): Any? {
        val handler = InvocationHandler { proxy, method, args ->
            val arguments = args ?: emptyArray()
            val abstractComponentMethodInvocationHandlers =
                context.componentFactory().getComponents<AbstractComponentMethodInvocationHandler<Any?>>()
            val results = abstractComponentMethodInvocationHandlers.filter {
                it::class.java.genericInterfaces.first { type ->
                    type.toParameterized().rawType.toClass().isSubclassOf(AbstractComponentMethodInvocationHandler::class)
                }.toParameterized().actualTypeArguments.first().toClass().isInstance(proxy)
            }.map {
                it.handleAbstractComponentMethodInvocation(proxy, method, arguments)
            }
            val result = results.filterIsInstance<Some<*>>().firstOrNull()
            if (result == null || result.isNone()) run {
                if (method.isDefault) method.invoke(proxy, *arguments)
                else throw UnsupportedOperationException()
            } else result.getOrNull()
        }
        return ByteBuddy()
            .subclass(Any::class.java)
            .annotateType(*component.type.annotations.toTypedArray())
            .implement(component.type.java, DynamicComponent::class.java)
            .intercept(InvocationHandlerAdapter.of(handler))
            .make()
            .load(component.type.java.classLoader)
            .loaded
            .getConstructor().newInstance()
    }

    open fun createComponentInstance(
        componentDefinition: ComponentDefinition,
        dependencyStack: MutableList<ComponentDefinition> = mutableListOf()
    ): Any? {
        try {
            if (componentDefinition in dependencyStack) throw StackOverflowError(dependencyStack.joinToString { it.name })
            dependencyStack.add(componentDefinition)
            val instance = when {
                componentDefinition.type.java.isInterface -> createInterfaceComponentInstance(
                    componentDefinition,
                    dependencyStack
                )

                else -> {
                    val constructor = componentDefinition.type.constructors
                        .firstOrNone { it.findAnnotation<Autowired>() != null }
                        .getOrElse { componentDefinition.type.constructors.first() }
                    val actualArguments =
                        constructor.parameters.associateWith { autowire(it.type, it.annotations, dependencyStack) }
                    try {
                        constructor.callBy(actualArguments)
                    } catch (cause: InvocationTargetException) {
                        throw ComponentProcessingException(
                            "An error occurred while constructing component '${componentDefinition.name}' ($constructor)",
                            cause
                        )
                    }
                }
            }
            return instance.also {
                if (it is PostConstruct) it.postConstruct()
            }
        } catch (cause: StackOverflowError) {
            throw ComponentProcessingException(
                "Circular dependency error occurred while creating component '${componentDefinition.name}'",
                cause
            )
        }
    }

    open fun autowire(
        type: KType,
        annotations: List<Annotation> = type.annotations,
        dependencyStack: MutableList<ComponentDefinition> = mutableListOf()
    ): Any? {
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
                    if (it.name == null) component else autowire(
                        it.type,
                        it.annotations
                    )
                }
                val invocation = if (member.isSuspend) runBlocking {
                    member.callSuspendBy(actualArguments)
                } else member.callBy(actualArguments)
                if (invocation != null)
                    context.componentFactory()
                        .registerComponentDefinition(
                            ComponentDefinition(
                                invocation,
                                name = "${componentDefinition.name}::${member.name}"
                            )
                        )
            }
            if (instance is AfterComponentAutowired) instance.afterComponentAutowired()
        }

}
