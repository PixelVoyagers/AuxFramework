package pixel.auxframework.component.factory

import arrow.core.Some
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import pixel.auxframework.annotation.Autowired
import pixel.auxframework.context.AuxContext
import pixel.auxframework.context.builtin.AbstractComponentMethodInvocationHandler
import pixel.auxframework.util.toClass
import pixel.auxframework.util.toParameterized
import java.lang.reflect.Array
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

open class ComponentProcessor(private val context: AuxContext) {

    open fun initializeComponent(component: ComponentDefinition) = component.also {
        if (component.isInitialized()) return@also
        try {
            component.setInstance(construct(component))
        } catch (cause: Throwable) {
            throw ComponentInitializingException(
                "An error occurred while creating the component '${component.name}'",
                cause
            )
        }
    }

    open fun constructInterface(component: ComponentDefinition): Any? {
        val handler = InvocationHandler { proxy, method, args ->
            val arguments = args ?: emptyArray()
            val abstractComponentMethodInvocationHandlers = context.components().getComponents<AbstractComponentMethodInvocationHandler<Any?>>()
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

    open fun construct(component: ComponentDefinition): Any? {
        try {
            val instance = when {
                component.type.java.isInterface -> constructInterface(component)
                else -> {
                    val constructor = component.type.constructors.first()
                    val arguments = mutableMapOf<KParameter, Any?>()
                    for (parameter in constructor.parameters) arguments[parameter] = autowire(parameter.type)
                    try {
                        constructor.callBy(arguments)
                    } catch (cause: InvocationTargetException) {
                        throw ComponentInitializingException(
                            "An error occurred while constructing component '${component.name}' ($constructor)",
                            cause
                        )
                    }
                }
            }
            return instance.also {
                if (it is PostConstruct) it.postConstruct()
            }
        } catch (cause: StackOverflowError) {
            throw ComponentInitializingException(
                "Circular dependency error occurred while creating component '${component.name}'",
                cause
            )
        }
    }

    open fun autowire(type: KType, annotations: List<Annotation> = type.annotations): Any? {
        val classifier = type.toClass()
        if (classifier.java.isArray) {
            val arrayComponentType = classifier.java.arrayType().componentType
            val components = context.components()
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
            val componentDefinition = context.components().getComponentDefinitionByType(type, annotations)
            initializeComponent(componentDefinition)
            return componentDefinition.cast()
        }
    }

    open fun autowireComponent(component: ComponentDefinition) = initializeComponent(component).also {
        if (component.isLoaded()) return@also
        val instance = component.cast<Any>()
        for (field in instance::class.memberProperties) {
            if (field.findAnnotation<Autowired>() == null) continue
            if (field is KMutableProperty<*>) {
                val autowired =
                    context.components().getComponentDefinitionByType(field.returnType, field.annotations)
                field.setter.isAccessible = true
                field.setter.call(instance, autowired.cast())
            }
        }
        context.components().getAllComponents()
        if (instance is AfterComponentAutowired) instance.afterComponentAutowired()
    }

}
