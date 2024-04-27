package pixel.auxframework.component.factory

import pixel.auxframework.annotations.Qualifier
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

abstract class ComponentFactory {

    abstract fun registerComponentDefinition(componentDefinition: ComponentDefinition)
    abstract fun getAllComponents(): Set<ComponentDefinition>

    open fun getComponentDefinition(name: String): ComponentDefinition = getAllComponents().first { it.name == name }
    open fun getComponentDefinition(type: Class<*>): ComponentDefinition = getComponentDefinitions(type).first()
    open fun getComponentDefinition(type: KClass<*>): ComponentDefinition = getComponentDefinitions(type).first()
    open fun <T> getComponents(type: Class<T>) = getComponentDefinitions(type).map { it.cast<T>() }
    open fun <T : Any> getComponents(type: KClass<T>) = getComponentDefinitions(type.java).map { it.cast<T>() }
    open fun getComponentDefinitions(type: Class<*>) = getAllComponents().filter {
        it.type == type.kotlin || it.type.isSubclassOf(type.kotlin) || (it.isInitialized() && type.isInstance(it.cast()))
    }

    open fun getComponentDefinitions(type: KClass<*>) = getComponentDefinitions(type.java)
    open fun <T> getComponent(name: String): T = getComponentDefinition(name).cast()
    open fun <T : Any> getComponent(type: KClass<T>): T = getComponents(type.java).first()
    open fun <T> getComponent(type: Class<T>): T = getComponents(type).first()

    abstract fun dispose()

}

inline fun <reified T : Any> ComponentFactory.getComponent() = getComponent(T::class)
inline fun <reified T : Any> ComponentFactory.getComponents() = getComponents(T::class)

fun ComponentFactory.getComponentDefinitionByType(type: KType, annotations: List<Annotation>): ComponentDefinition {
    val qualifier = annotations.filterIsInstance<Qualifier>().firstOrNull()
    return if (qualifier != null) getComponent(qualifier.name)
    else getComponentDefinition(type.classifier!! as KClass<*>)
}

fun ComponentFactory.getComponentDefinitionByType(type: KType) = getComponentDefinitionByType(type, type.annotations)
