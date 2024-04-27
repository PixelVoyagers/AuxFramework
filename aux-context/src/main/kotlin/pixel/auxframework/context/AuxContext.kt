package pixel.auxframework.context

import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import pixel.auxframework.annotations.Component
import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.ComponentFactory
import pixel.auxframework.component.factory.ComponentPostProcessor
import pixel.auxframework.component.factory.getComponents

abstract class AuxContext {

    val classLoaders = mutableSetOf<ClassLoader>()
    protected abstract val components: ComponentFactory
    protected abstract val componentsService: ComponentsService

    init {
        classLoaders += this::class.java.classLoader
    }

    fun components() = components

    open fun refresh() {
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
            val types = reflections.getTypesAnnotatedWith(Component::class.java)
            types.forEach { type -> components().registerComponentDefinition(ComponentDefinition(type.kotlin)) }
        }
    }

    open fun launch() {
        scan()
        refresh()
    }

    open fun dispose() {
        components().dispose()
    }

}
