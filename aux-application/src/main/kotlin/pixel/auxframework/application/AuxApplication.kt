package pixel.auxframework.application

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.getComponents
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.measureTime

interface ApplicationListener {
    fun onClose() {}
    fun onStart(timeUsed: Duration) {}
}

open class DefaultApplicationListener(val application: AuxApplication) : ApplicationListener {

    override fun onStart(timeUsed: Duration) {
        application.log.info("Startup completed, taking $timeUsed.")
    }

    override fun onClose() {
        application.log.info("Disposed!")
    }

}

open class AuxApplication(private val builder: AuxApplicationBuilder) {

    fun getAuxApplicationBuilder() = builder

    val log: Logger = LoggerFactory.getLogger(builder.target!!.java)
    val context = builder.context!!

    protected open fun appendComponents(list: MutableList<ComponentDefinition>) {
        list.add(ComponentDefinition(builder, loaded = true))
        list.add(ComponentDefinition(builder.target!!))
        list.add(ComponentDefinition(this, loaded = true))
        list.add(ComponentDefinition(DefaultApplicationListener(this), loaded = true))
    }

    open fun run(vararg args: String) {
        val timeUsed = measureTime {
            mutableListOf<ComponentDefinition>()
                .also(::appendComponents)
                .forEach(context.componentFactory()::defineComponent)
            builder.banner.printBanner(context, System.out)
            context.run(*args)
        }
        context.componentFactory().getComponents<ApplicationListener>().forEach { it.onStart(timeUsed) }
    }

    open fun close() {
        context.componentFactory().getComponents<ApplicationListener>().forEach(ApplicationListener::onClose)
        context.close()
    }

}

data class AuxApplicationBuilder(
    val name: String? = null,
    val target: KClass<*>? = Any::class,
    val context: ApplicationContext? = null,
    val banner: Banner = Banner
) {

    inline fun <reified T> target() = target(T::class)
    fun target(target: KClass<*>) = copy(target = target)
    fun name(name: String) = copy(name = name)
    fun context(context: ApplicationContext) = copy(context = context)

    fun complete() = copy(
        target = target!!,
        context = context ?: ApplicationContext(),
        name = name ?: "AuxApplication@${context?.name}"
    )

    fun build(): AuxApplication = AuxApplication(complete())

}
