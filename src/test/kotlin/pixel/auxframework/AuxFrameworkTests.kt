package pixel.auxframework

import org.junit.jupiter.api.Test
import pixel.auxframework.annotations.Autowired
import pixel.auxframework.annotations.Component
import pixel.auxframework.component.factory.*
import pixel.auxframework.context.DefaultAuxContext
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Component
class ComponentA : AfterComponentAutowired, PostConstruct {

    @Autowired private var componentB: ComponentB? = null
    override fun afterComponentAutowired() {
        assertEquals(componentB?.dependency, this)
    }

    override fun postConstruct() {
        assertNull(componentB)
    }

}
@Component
class ComponentB(val dependency: ComponentA)

class AuxFrameworkTests {

    @Test
    fun `Context Tests`() {
        val context = DefaultAuxContext()
        context.launch()
        val dependency = context.components()
            .getAllComponents()
            .filter(ComponentDefinition::isInitialized)
            .map { it.cast<Any>() }
            .filterIsInstance<ComponentB>()
            .firstOrNull()
        assertNotNull(dependency)
        assertEquals(dependency.dependency, context.components().getComponent())
        context.dispose()
    }

}