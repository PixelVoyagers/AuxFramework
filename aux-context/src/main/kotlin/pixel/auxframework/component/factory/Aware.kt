package pixel.auxframework.component.factory

import pixel.auxframework.component.annotation.Service
import pixel.auxframework.context.builtin.AfterContextRefreshed

interface Aware

interface ComponentDefinitionAware : Aware {
    fun setComponentDefinition(componentDefinition: ComponentDefinition)
}

@Service
class ComponentDefinitionAwareService(private val componentFactory: ComponentFactory) : AfterContextRefreshed {

    override fun afterContextRefreshed() {
        componentFactory.getAllComponents()
            .filter { it.isInstance<ComponentDefinitionAware>() }
            .forEach { it.cast<ComponentDefinitionAware>().setComponentDefinition(it) }
    }

}
