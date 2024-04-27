package pixel.auxframework.context

import pixel.auxframework.component.factory.ComponentDefinition
import pixel.auxframework.component.factory.ComponentFactory

open class DefaultComponentFactory(private val context: AuxContext) : ComponentFactory() {

    internal val pool = mutableSetOf<ComponentDefinition>()

    override fun getAllComponents() = pool.toSet()

    override fun registerComponentDefinition(componentDefinition: ComponentDefinition) {
        pool.add(componentDefinition)
    }

    override fun dispose() {
        pool.clear()
    }

}

@Suppress("LeakingThis")
open class DefaultAuxContext : AuxContext() {

    init {
        classLoaders += this::class.java.classLoader
    }

    override val components = DefaultComponentFactory(this)

    override val componentsService = ComponentsService(this)

}