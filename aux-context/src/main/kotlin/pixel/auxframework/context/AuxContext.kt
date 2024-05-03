package pixel.auxframework.context

import arrow.core.getOrElse
import arrow.core.toOption
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pixel.auxframework.annotation.Component
import pixel.auxframework.annotation.OnlyIn
import pixel.auxframework.component.factory.*
import kotlin.reflect.full.findAnnotation

abstract class AuxContext : DisposableComponent {

    var name: String = this.toString()
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    val classLoaders = mutableSetOf<ClassLoader>()
    protected abstract val components: ComponentFactory
    protected abstract val componentsService: ComponentsService

    init {
        classLoaders += this::class.java.classLoader
    }

    fun components() = components

    protected fun appendComponents(list: MutableList<Any>) {
        list.addAll(
            listOf(
                this, components, componentsService
            )
        )
    }

    open fun refresh() {
        mutableListOf<Any>().also(::appendComponents).forEach { components.registerComponentDefinition(
            ComponentDefinition(it)
        ) }
        components.getAllComponents().filterNot(ComponentDefinition::isInitialized).toSet().apply {
            componentsService.initializeComponents(this)
            for (component in this) {
                val componentPostProcessors = components().getComponents<ComponentPostProcessor>()
                componentPostProcessors.forEach { it.processComponent(component) }
            }
        }
        componentsService.autowireComponents(
            components.getAllComponents().filter(ComponentDefinition::isInitialized).filterNot(
                ComponentDefinition::isLoaded
            ).toSet()
        )
    }

    protected fun scan() {
        for (classLoader in classLoaders) {
            val reflections = Reflections(
                ConfigurationBuilder()
                    .forPackages(*classLoader.definedPackages.map { it.name }.toTypedArray())
                    .addClassLoaders(classLoader)
                    .setScanners(Scanners.TypesAnnotated)
            )
            val types = reflections.getTypesAnnotatedWith(Component::class.java).filter {
                !(it.isAnnotation || (!it.isInterface && it.kotlin.isAbstract))
            }.filter {
                it.kotlin.findAnnotation<OnlyIn>().toOption().map { onlyIn ->
                    var accept = true
                    if (onlyIn.contextName != "<null>") accept = accept && this@AuxContext.name == onlyIn.contextName
                    accept = accept && onlyIn.contextType.all { type -> type.isInstance(this@AuxContext) }
                    accept
                }.getOrElse { true }
            }
            types.forEach { type -> components().registerComponentDefinition(ComponentDefinition(type.kotlin)) }
        }
    }

    open fun launch() {
        scan()
        refresh()
    }

    open fun run(vararg args: String) {
        log.info("Starting...")
        launch()
    }

    override fun dispose() {
        log.info("Disposed!")
    }

    fun close() {
        components.getComponents<DisposableComponent>().forEach(DisposableComponent::dispose)
    }

}
