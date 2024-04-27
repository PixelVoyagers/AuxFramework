package pixel.auxframework.context.builtin

import pixel.auxframework.annotation.Component
import pixel.auxframework.annotation.Repository
import pixel.auxframework.component.factory.AuxService
import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.ComponentPostProcessor
import pixel.auxframework.component.factory.isInstance

@Repository
interface ServiceRepository : SimpleListRepository<AuxService>

@Component
class ServiceComponentProcessor(private val repository: ServiceRepository) : ComponentPostProcessor {

    override fun processComponent(componentDefinition: ComponentDefinition) {
        if (componentDefinition.isInstance<AuxService>()) {
            repository.add(componentDefinition.cast())
        }
    }

}