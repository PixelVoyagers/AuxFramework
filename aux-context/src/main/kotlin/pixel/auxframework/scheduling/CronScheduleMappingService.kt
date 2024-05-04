package pixel.auxframework.scheduling

import kotlinx.coroutines.runBlocking
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import pixel.auxframework.annotation.Service
import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.DisposableComponent
import pixel.auxframework.context.builtin.AfterContextRefreshed
import pixel.auxframework.scheduling.annotation.CronSchedule
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend

@Service
class CronScheduleMappingService : ScheduleMapping<CronSchedule>, DisposableComponent, AfterContextRefreshed {

    val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler()

    @Suppress("UNCHECKED_CAST")
    override fun mappingSchedule(
        annotation: CronSchedule,
        memberFunction: KFunction<*>,
        componentDefinition: ComponentDefinition
    ): CompiledSchedule {
        val component = componentDefinition.cast<Any>()
        val compiledSchedule = object : CompiledSchedule(), InvocationHandler {
            override fun run() = runBlocking { memberFunction.callSuspend(component) }.let {}
            override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {
                return if (method.name == "execute") run()
                else method.invoke(proxy, *args)
            }
        }
        val jobClass = ByteBuddy()
            .subclass(Any::class.java)
            .implement(Job::class.java)
            .method(ElementMatchers.named("execute"))
            .intercept(InvocationHandlerAdapter.of(compiledSchedule))
            .make()
            .load(component::class.java.classLoader)
            .loaded
        val job = JobBuilder.newJob(jobClass as Class<out Job>?)
            .withIdentity(memberFunction.name, componentDefinition.name)
            .build()
        val trigger = TriggerBuilder.newTrigger()
            .withSchedule(CronScheduleBuilder.cronSchedule(annotation.cronExpression))
            .forJob(job)
            .startNow()
            .build()
        scheduler.scheduleJob(job, trigger)
        return compiledSchedule
    }

    override fun dispose() {
        if (scheduler.isStarted) scheduler.shutdown()
    }

    override fun afterContextRefreshed() {
        if (scheduler.isStarted) scheduler.shutdown()
        scheduler.start()
    }

}
