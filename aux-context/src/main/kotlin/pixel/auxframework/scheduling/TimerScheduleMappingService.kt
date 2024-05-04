package pixel.auxframework.scheduling

import kotlinx.coroutines.runBlocking
import pixel.auxframework.component.annotation.Service
import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.DisposableComponent
import pixel.auxframework.context.builtin.AfterContextRefreshed
import pixel.auxframework.scheduling.annotation.TimerSchedule
import java.util.*
import kotlin.math.max
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend

@Service
class TimerScheduleMappingService : ScheduleMapping<TimerSchedule>, DisposableComponent, AfterContextRefreshed {

    val timer = Timer()
    var isRunning = true

    override fun mappingSchedule(
        annotation: TimerSchedule,
        memberFunction: KFunction<*>,
        componentDefinition: ComponentDefinition
    ): CompiledSchedule {
        val component = componentDefinition.cast<Any>()
        val compiledSchedule = object : CompiledSchedule() {
            override fun run() = runBlocking { memberFunction.callSuspend(component) }.let {}
        }
        val task = object : TimerTask() {
            override fun run() {
                if (isRunning) compiledSchedule.run()
            }
        }
        val interval = annotation.timeUnit.toMillis(annotation.interval)
        if (annotation.fixed) timer.scheduleAtFixedRate(task, 0, max(interval, 1))
        else timer.schedule(task, 0, max(interval, 1))
        return compiledSchedule
    }

    override fun dispose() {
        isRunning = false
    }

    override fun afterContextRefreshed() {
        isRunning = true
    }

}
