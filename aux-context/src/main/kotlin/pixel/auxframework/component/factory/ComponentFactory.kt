package pixel.auxframework.component.factory

import pixel.auxframework.annotation.Qualifier
import pixel.auxframework.util.toClass
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * 组件工厂
 * @see [pixel.auxframework.context.DefaultComponentFactory]
 */
abstract class ComponentFactory : DisposableComponent {

    /**
     * 注册组件
     */
    abstract fun registerComponentDefinition(componentDefinition: ComponentDefinition)

    /**
     * 获取全部组件
     */
    abstract fun getAllComponents(): Set<ComponentDefinition>

    /**
     * 根据名称获取组件定义
     */
    open fun getComponentDefinition(name: String): ComponentDefinition = getAllComponents().first { it.name == name }

    /**
     * 根据类型获取组件定义
     */
    open fun getComponentDefinition(type: Class<*>): ComponentDefinition = getComponentDefinitions(type).first()

    /**
     * 根据类型获取组件定义
     */
    open fun getComponentDefinition(type: KClass<*>): ComponentDefinition = getComponentDefinitions(type).first()

    /**
     * 根据类型获取组件定义
     */
    open fun getComponentDefinition(type: KType, annotations: List<Annotation> = type.annotations): ComponentDefinition {
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
    open fun <T : Any> getComponent(type: KClass<T>): T = getComponents(type.java).first()

    /**
     * 根据类型获取组件
     */
    open fun <T> getComponent(type: Class<T>): T = getComponents(type).first()

    /**
     * 根据类型获取全部组件
     */
    open fun <T> getComponents(type: Class<T>) = getComponentDefinitions(type).map { it.cast<T>() }

    /**
     * 根据类型获取全部组件
     */
    open fun <T : Any> getComponents(type: KClass<T>) = getComponentDefinitions(type.java).map { it.cast<T>() }

}

/**
 * 根据类型获取组件
 */
inline fun <reified T : Any> ComponentFactory.getComponent() = getComponent(T::class)

/**
 * 根据类型获取全部组件
 */
inline fun <reified T : Any> ComponentFactory.getComponents() = getComponents(T::class)
