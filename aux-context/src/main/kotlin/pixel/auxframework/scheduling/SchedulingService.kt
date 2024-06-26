package pixel.auxframework.scheduling

import pixel.auxframework.component.annotation.*
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

@Preload
@Component
data class SchedulingConfig(@Autowired(false) var enabled: Boolean = true)

@Repository
interface CompiledSchedulesRepository : SimpleListRepository<CompiledSchedule>

interface ScheduleMapping<T : Annotation> {
    fun mappingSchedule(
        annotation: T,
        memberFunction: KFunction<*>,
        componentDefinition: ComponentDefinition
    ): CompiledSchedule
}

@Service
class SchedulingService(
    val repository: CompiledSchedulesRepository,
    val componentFactory: ComponentFactory,
    val config: SchedulingConfig
) : ComponentPostProcessor {

    override fun processComponent(componentDefinition: ComponentDefinition, instance: Any?) = instance.also {
        if (!config.enabled) return@also
        val component = (componentDefinition.castOrNull<Any>() ?: return@also)
        val componentClass = component::class
        componentClass.findAnnotation<Scheduled>() ?: return@also
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
                mappers[annotation.annotationClass]?.forEach {
                    it.mappingSchedule(
                        annotation,
                        member,
                        componentDefinition
                    )
                }
            }
        }
    }

}
