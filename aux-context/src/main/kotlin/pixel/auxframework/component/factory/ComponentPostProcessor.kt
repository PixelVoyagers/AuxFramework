package pixel.auxframework.component.factory

/**
 * 在组件基础初始化后执行
 */
interface ComponentPostProcessor {

    fun processComponent(componentDefinition: ComponentDefinition)

}