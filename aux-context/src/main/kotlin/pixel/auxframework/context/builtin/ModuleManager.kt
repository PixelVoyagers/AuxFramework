package pixel.auxframework.context.builtin

import pixel.auxframework.component.annotation.Component
import pixel.auxframework.component.annotation.Repository
import pixel.auxframework.component.factory.AuxModule
import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.ComponentPostProcessor
import pixel.auxframework.component.factory.isInstance

@Repository
interface ModuleRepository : SimpleListRepository<AuxModule>

@Component
class ModuleComponentProcessor(private val repository: ModuleRepository) : ComponentPostProcessor {

    override fun processComponent(componentDefinition: ComponentDefinition) {
        if (componentDefinition.isInstance<AuxModule>()) {
            repository.add(componentDefinition.cast())
        }
    }

}