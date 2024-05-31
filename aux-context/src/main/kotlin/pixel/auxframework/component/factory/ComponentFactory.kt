package pixel.auxframework.component.factory

import pixel.auxframework.component.annotation.Qualifier
import pixel.auxframework.util.toClass
import kotlin.reflect.KClass
import kotlin.reflect.KType

class ComponentNotFoundException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * 组件工厂
 * @see [pixel.auxframework.context.DefaultComponentFactory]
 */
abstract class ComponentFactory {

    open fun dispose() {}

    /**
     * 定义组件
     */
    abstract fun defineComponent(componentDefinition: ComponentDefinition)

    /**
     * 获取全部组件
     */
    abstract fun getAllComponents(): Set<ComponentDefinition>

    /**
     * 根据名称获取组件定义
     */
    open fun getComponentDefinition(name: String): ComponentDefinition =
        getAllComponents().firstOrNull { it.name == name } ?: throw ComponentNotFoundException(name)

    /**
     * 根据类型获取组件定义
     */
    open fun getComponentDefinition(type: Class<*>): ComponentDefinition = getComponentDefinition(type.kotlin)

    /**
     * 根据类型获取组件定义
     */
    open fun getComponentDefinition(type: KClass<*>): ComponentDefinition =
        getComponentDefinitions(type).firstOrNull() ?: throw ComponentNotFoundException(type.toString())

    /**
     * 根据类型获取组件定义
     */
    open fun getComponentDefinition(
        type: KType,
        annotations: List<Annotation> = type.annotations
    ): ComponentDefinition {
        val qualifier = annotations.filterIsInstance<Qualifier>().firstOrNull()
        return if (qualifier != null) getComponent(qualifier.name)
        else getComponentDefinition(type.toClass())
    }

    /**
     * 根据类型获取全部组件定义
     */
    open fun getComponentDefinitions(type: Class<*>) = getAllComponents().filter { it.isInstance(type.kotlin) }

    /**
     * 根据类型获取全部组件定义
     */
    open fun getComponentDefinitions(type: KClass<*>) = getComponentDefinitions(type.java)

    /**
     * 根据名称获取组件
     */
    open fun <T> getComponent(name: String): T = getComponentDefinition(name).cast()

    /**
     * 根据类型获取组件
     */
    open fun <T : Any> getComponent(type: KClass<T>): T =
        getComponents(type.java).firstOrNull() ?: throw ComponentNotFoundException(type.toString())

    /**
     * 根据类型获取组件
     */
    open fun <T : Any> getComponent(type: Class<T>): T = getComponent(type.kotlin)

    /**
     * 根据类型获取全部组件
     */
    open fun <T> getComponents(type: Class<T>) = getComponentDefinitions(type).map { it.cast<T>() }

    /**
     * 根据类型获取全部组件
     */
    open fun <T : Any> getComponents(type: KClass<T>) =
        getComponentDefinitions(type.java).filter(ComponentDefinition::isInitialized).map { it.cast<T>() }

}

/**
 * 根据类型获取组件
 */
inline fun <reified T : Any> ComponentFactory.getComponent() = getComponent(T::class)

/**
 * 根据类型获取全部组件
 */
inline fun <reified T : Any> ComponentFactory.getComponents() = getComponents(T::class)

/**
 * 判断是否存在某个类型的组件
 */
inline fun <reified T : Any> ComponentFactory.hasComponent() = getComponents<T>().isNotEmpty()
