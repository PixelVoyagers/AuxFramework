package pixel.auxframework.context

import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.ComponentFactory
import pixel.auxframework.component.factory.ComponentProcessor

open class DefaultComponentFactory(private val context: AuxContext) : ComponentFactory() {

    internal val container = mutableSetOf<ComponentDefinition>()

    override fun getAllComponents() = container.toSet()

    override fun registerComponentDefinition(componentDefinition: ComponentDefinition) {
        container.add(componentDefinition)
    }

    override fun dispose() {
        container.clear()
    }

}

@Suppress("LeakingThis")
open class DefaultAuxContext : AuxContext() {

    init {
        classLoaders += this::class.java.classLoader
    }

    override val components = DefaultComponentFactory(this)

    override val componentProcessor = ComponentProcessor(this)

}