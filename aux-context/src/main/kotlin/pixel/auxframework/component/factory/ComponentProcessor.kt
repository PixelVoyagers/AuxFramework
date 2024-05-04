package pixel.auxframework.component.factory

import arrow.core.Some
import arrow.core.firstOrNone
import arrow.core.getOrElse
import kotlinx.coroutines.runBlocking
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import pixel.auxframework.annotation.Autowired
import pixel.auxframework.annotation.Component
import pixel.auxframework.context.AuxContext
import pixel.auxframework.context.builtin.AbstractComponentMethodInvocationHandler
import pixel.auxframework.util.toClass
import pixel.auxframework.util.toParameterized
import java.lang.reflect.Array
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KType
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * 在组件处理时发送错误
 */
class ComponentProcessingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class ComponentProcessor(private val context: AuxContext) {

    open fun initializeComponent(component: ComponentDefinition) = component.also {
        if (component.isInitialized()) return@also
        try {
            component.setInstance(construct(component))
        } catch (cause: Throwable) {
            throw ComponentProcessingException(
                "An error occurred while creating the component '${component.name}'",
                cause
            )
        }
    }

    open fun constructInterface(component: ComponentDefinition): Any? {
        val handler = InvocationHandler { proxy, method, args ->
            val arguments = args ?: emptyArray()
            val abstractComponentMethodInvocationHandlers = context.componentFactory().getComponents<AbstractComponentMethodInvocationHandler<Any?>>()
            val results = abstractComponentMethodInvocationHandlers.filter {
                it::class.java.genericInterfaces.first()
                    .toParameterized()
                    .actualTypeArguments.first()
                    .toClass()
                    .isInstance(proxy)
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

    open fun construct(componentDefinition: ComponentDefinition): Any? {
        try {
            val instance = when {
                componentDefinition.type.java.isInterface -> constructInterface(componentDefinition)
                else -> {
                    val constructor = componentDefinition.type.constructors
                        .firstOrNone { it.findAnnotation<Autowired>() != null }
                        .getOrElse { componentDefinition.type.constructors.first() }
                    val actualArguments = constructor.parameters.associateWith { autowire(it.type, it.annotations) }
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

    open fun autowire(type: KType, annotations: List<Annotation> = type.annotations): Any? {
        val classifier = type.toClass()
        if (classifier.java.isArray) {
            val arrayComponentType = classifier.java.arrayType().componentType
            val components = context.componentFactory()
                .getComponentDefinitions(arrayComponentType)
                .map {
                    initializeComponent(it)
                    it.cast<Any>()
                }
            val array = Array.newInstance(arrayComponentType, components.size)
            components.forEachIndexed { index, element ->
                Array.set(array, index, element)
            }
            return array
        } else {
            val componentDefinition = context.componentFactory().getComponentDefinition(type, annotations)
            initializeComponent(componentDefinition)
            return componentDefinition.cast()
        }
    }

    open fun autowireComponent(componentDefinition: ComponentDefinition) = initializeComponent(componentDefinition).also {
        if (componentDefinition.isLoaded()) return@also
        val instance = componentDefinition.cast<Any>()
        for (field in instance::class.memberProperties) {
            if (field.findAnnotation<Autowired>() == null) continue
            if (field is KMutableProperty<*>) {
                val autowired =
                    context.componentFactory().getComponentDefinition(field.returnType, field.annotations)
                field.setter.isAccessible = true
                field.setter.call(instance, autowired.cast())
            }
        }
        val component = componentDefinition.cast<Any>()
        for (member in component::class.memberFunctions) {
            member.findAnnotation<Component>() ?: continue
            val actualArguments = member.parameters.associateWith { if (it.name == null) component else autowire(it.type, it.annotations) }
            val invoke = if (member.isSuspend) runBlocking {
                member.callSuspendBy(actualArguments)
            } else member.callBy(actualArguments)
            if (invoke != null)
                context.componentFactory().registerComponentDefinition(ComponentDefinition(invoke, name = "${componentDefinition.name}::${member.name}"))
        }
        if (instance is AfterComponentAutowired) instance.afterComponentAutowired()
    }

}
