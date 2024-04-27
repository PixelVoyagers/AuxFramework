package pixel.auxframework

import org.junit.jupiter.api.Test
import pixel.auxframework.annotation.Autowired
import pixel.auxframework.annotation.Component
import pixel.auxframework.component.factory.*
import pixel.auxframework.context.DefaultAuxContext
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuxFrameworkTests {

    @Component
    class ComponentA : AfterComponentAutowired, PostConstruct {

        @Autowired
        private var lazyDependency: ComponentB? = null

        override fun afterComponentAutowired() {
            assertEquals(this, lazyDependency?.dependency)
        }

        override fun postConstruct() {
            assertNull(lazyDependency)
        }

    }

    @Component
    class ComponentB(val dependency: ComponentA)

    @Component
    class TestService : AuxService

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
