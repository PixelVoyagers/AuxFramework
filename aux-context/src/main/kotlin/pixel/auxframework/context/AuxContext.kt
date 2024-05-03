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
    protected abstract val componentProcessor: ComponentProcessor

    init {
        classLoaders += this::class.java.classLoader
    }

    fun components() = components

    protected fun appendComponents(list: MutableList<Any>) {
        list.addAll(
            listOf(
                this, components, componentProcessor
            )
        )
    }

    open fun refresh() {
        components.getAllComponents()
            .map(componentProcessor::initializeComponent)
            .forEach { component ->
                components()
                    .getComponents<ComponentPostProcessor>()
                    .forEach { it.processComponent(component) }
            }
        components.getAllComponents().map(componentProcessor::autowireComponent)
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
        mutableListOf<Any>().also(::appendComponents)
            .map { ComponentDefinition(it) }
            .map { it.also { it.loaded = true } }
            .forEach(components::registerComponentDefinition)
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
