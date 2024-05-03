package pixel.auxframework.context

import arrow.core.Some
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import pixel.auxframework.annotation.Autowired
import pixel.auxframework.component.factory.*
import pixel.auxframework.context.builtin.AbstractComponentMethodInvocationHandler
import java.lang.reflect.Array
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

open class ComponentsService(private val context: AuxContext) {

    open fun initializeComponents(components: Set<ComponentDefinition>) {
        for (component in components) {
            if (component.isInitialized()) continue
            try {
                component.setInstance(construct(component))
            } catch (cause: Throwable) {
                throw ComponentInitializingException(
                    "An error occurred while creating the component '${component.name}'",
                    cause
                )
            }
        }
    }

    fun constructAbstract(component: ComponentDefinition): Any? {
        val handler = InvocationHandler { proxy, method, args ->
            val arguments = args ?: emptyArray()
            val components = context.components().getComponents<AbstractComponentMethodInvocationHandler<Any?>>()
            val results = components.filter {
                val type = ((it::class.java.genericInterfaces.first() as ParameterizedType).actualTypeArguments.first())
                (type as Class<*>).kotlin.isInstance(proxy)
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
            val instance: Any?
            if (component.type.isAbstract) instance = constructAbstract(component)
            else {
                val constructor = component.type.constructors.first()
                val arguments = mutableMapOf<KParameter, Any?>()
                for (parameter in constructor.parameters) {
                    if ((parameter.type.classifier as KClass<*>).java.isArray) {
                        val type = parameter.type.classifier as KClass<*>
                        val arrayComponentType = type.java.arrayType().componentType
                        val components = context.components().getComponentDefinitions(arrayComponentType).map {
                            if (!it.isInitialized())
                                it.setInstance(construct(it))
                            it.cast<Any>()
                        }
                        val array = Array.newInstance(arrayComponentType) as kotlin.Array<*>
                        for (index in components.indices) {
                            Array.set(array, index, components[index])
                        }
                        arguments[parameter] = array
                    } else {
                        val componentDefinition = context.components().getComponentDefinitionByType(parameter.type)
                        if (!componentDefinition.isInitialized())
                            componentDefinition.setInstance(construct(componentDefinition))
                        arguments[parameter] = componentDefinition.cast()
                    }
                }
                try {
                    instance = constructor.callBy(arguments)
                } catch (cause: InvocationTargetException) {
                    throw ComponentInitializingException(
                        "An error occurred while constructing component '${component.name}' ($constructor)",
                        cause
                    )
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

    open fun autowireComponents(components: Set<ComponentDefinition>) {
        for (component in components) {
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

}
