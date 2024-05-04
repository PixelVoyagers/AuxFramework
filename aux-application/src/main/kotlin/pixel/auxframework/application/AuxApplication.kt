package pixel.auxframework.application

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.getComponents
import kotlin.reflect.KClass

interface ApplicationListener {
    fun onClose()
    fun onStart()
}

open class DefaultApplicationListener(val application: AuxApplication) : ApplicationListener {

    override fun onStart() {
        application.log.info("Started!")
    }

    override fun onClose() {
        application.log.info("Disposed!")
    }

}

open class AuxApplication(val builder: AuxApplicationBuilder) {

    val log: Logger = LoggerFactory.getLogger(this::class.java)

    val context = builder.context!!

    fun run(vararg args: String) {
        context.componentFactory().registerComponentDefinition(ComponentDefinition(builder.target!!))
        context.componentFactory().registerComponentDefinition(ComponentDefinition(this, loaded = true))
        context.componentFactory().registerComponentDefinition(
            ComponentDefinition(
                DefaultApplicationListener(this@AuxApplication), loaded = true
            )
        )
        context.run(*args)
        context.componentFactory().getComponents<ApplicationListener>().forEach(ApplicationListener::onStart)
    }

    fun close() {
        context.componentFactory().getComponents<ApplicationListener>().forEach(ApplicationListener::onClose)
        context.close()
    }

}

data class AuxApplicationBuilder(val target: KClass<*>? = null, val context: ApplicationContext? = null) {

    inline fun <reified T> target() = target(T::class)
    fun target(target: KClass<*>) = copy(target = target)

    fun context(context: ApplicationContext) = copy(context = context)

    fun complete() = copy(target = target!!, context = context ?: ApplicationContext())
    fun build(): AuxApplication = AuxApplication(complete())

}
