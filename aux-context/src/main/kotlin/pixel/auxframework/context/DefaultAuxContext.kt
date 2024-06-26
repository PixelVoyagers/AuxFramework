package pixel.auxframework.context

import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.ComponentFactory
import pixel.auxframework.component.factory.ComponentProcessor

open class DefaultComponentFactory(private val context: AuxContext) : ComponentFactory() {

    internal val container = mutableListOf<ComponentDefinition>()

    override fun getAllComponents() = container.toSet()

    override fun defineComponent(componentDefinition: ComponentDefinition) {
        container += componentDefinition
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

    override val componentFactory = DefaultComponentFactory(this)

    override val componentProcessor = ComponentProcessor(this)

}