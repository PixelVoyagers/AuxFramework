package pixel.auxframework.context

import pixel.auxframework.annotations.Autowired
import pixel.auxframework.component.factory.*
import java.lang.reflect.Array
import java.lang.reflect.InvocationTargetException
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
                throw ComponentInitializingException("An error occurred while creating the component '${component.name}'", cause)
            }
        }
    }

    open fun construct(component: ComponentDefinition) : Any? {
        try {
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
                return constructor.callBy(arguments).also {
                    if (it is PostConstruct) it.postConstruct()
                }
            } catch (cause: InvocationTargetException) {
                throw ComponentInitializingException("An error occurred while constructing component '${component.name}' ($constructor)", cause)
            }
        } catch (cause: StackOverflowError) {
            throw ComponentInitializingException("Circular dependency error occurred while creating component '${component.name}'", cause)
        }
    }

    open fun autowireComponents(components: Set<ComponentDefinition>) {
        for (component in components) {
            val instance = component.cast<Any>()
            for (field in instance::class.memberProperties) {
                if (field.findAnnotation<Autowired>() == null) continue
                if (field is KMutableProperty<*>) {
                    val autowired = context.components().getComponentDefinitionByType(field.returnType, field.annotations)
                    field.setter.isAccessible = true
                    field.setter.call(instance, autowired.cast())
                }
            }
            context.components().getAllComponents()
            if (instance is AfterComponentAutowired) instance.afterComponentAutowired()
        }

    }

}