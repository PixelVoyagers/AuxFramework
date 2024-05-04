package pixel.auxframework.scheduling

import pixel.auxframework.annotation.Repository
import pixel.auxframework.annotation.Service
import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.ComponentFactory
import pixel.auxframework.component.factory.ComponentPostProcessor
import pixel.auxframework.component.factory.getComponents
import pixel.auxframework.context.builtin.SimpleListRepository
import pixel.auxframework.scheduling.annotation.Scheduled
import pixel.auxframework.util.toClass
import pixel.auxframework.util.toParameterized
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions

@Repository
interface CompiledSchedulesRepository : SimpleListRepository<CompiledSchedule>

interface ScheduleMapping <T : Annotation> {
    fun mappingSchedule(annotation: T, memberFunction: KFunction<*>, componentDefinition: ComponentDefinition) : CompiledSchedule
}

@Service
class SchedulingService(val repository: CompiledSchedulesRepository, val componentFactory: ComponentFactory) : ComponentPostProcessor {
    override fun processComponent(componentDefinition: ComponentDefinition) {
        val component = (componentDefinition.castOrNull<Any>() ?: return)
        val componentClass = component::class
        componentClass.findAnnotation<Scheduled>() ?: return
        val mappers = mutableMapOf<KClass<*>, MutableSet<ScheduleMapping<Annotation>>>()
        for (mapper in componentFactory.getComponents<ScheduleMapping<Annotation>>()) {
            val type = mapper::class
                .java.genericInterfaces
                .firstOrNull { type -> type.toParameterized().rawType.toClass().isSubclassOf(ScheduleMapping::class) }
                ?.toParameterized()?.actualTypeArguments?.firstOrNull()?.toClass()
                ?: continue
            mappers.getOrPut(type) { mutableSetOf() }.add(mapper)
        }
        for (member in componentClass.memberFunctions) {
            member.findAnnotation<Scheduled>() ?: continue
            for (annotation in member.annotations) {
                mappers[annotation.annotationClass]?.forEach { it.mappingSchedule(annotation, member, componentDefinition) }
            }
        }
    }

}