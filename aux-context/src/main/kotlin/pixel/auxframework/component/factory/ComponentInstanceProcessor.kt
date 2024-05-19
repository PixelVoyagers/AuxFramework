package pixel.auxframework.component.factory


/**
 * 处理组件实例
 */
interface ComponentInstanceProcessor {

    fun processComponentInstance(componentDefinition: ComponentDefinition, instance: Any?) = instance

}