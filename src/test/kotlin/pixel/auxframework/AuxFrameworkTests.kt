package pixel.auxframework

import org.junit.jupiter.api.Test
import pixel.auxframework.annotation.Autowired
import pixel.auxframework.annotation.Component
import pixel.auxframework.annotation.OnlyIn
import pixel.auxframework.component.factory.*
import pixel.auxframework.context.AuxContext
import pixel.auxframework.context.DefaultAuxContext
import kotlin.concurrent.thread
import kotlin.reflect.jvm.jvmName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuxFrameworkTests {

    class AuxFrameworkTestsContext : DefaultAuxContext()

    @OnlyIn(contextType = [AuxFrameworkTestsContext::class])
    @Component
    class ComponentA : AfterComponentAutowired, PostConstruct {

        @Autowired private lateinit var context: AuxContext

        @Component
        fun subComponent(): Any {
            return object : ComponentDefinitionAware {
                override fun setComponentDefinition(componentDefinition: ComponentDefinition) {
                    assertEquals(this, componentDefinition.cast())
                }
            }
        }

        @Autowired
        private var lazyDependency: ComponentB? = null

        override fun afterComponentAutowired() {
            assertEquals(this, lazyDependency?.dependency)
        }

        override fun postConstruct() {
            assertNull(lazyDependency)
        }

    }

    @OnlyIn(contextType = [AuxFrameworkTestsContext::class])
    @Component
    class ComponentB(val dependency: ComponentA)

    @OnlyIn(contextType = [AuxFrameworkTestsContext::class])
    @Component
    class TestModule : AuxModule, AuxModuleStatusSupplier {
        override fun getStatus() = enumValues<ModuleStatus>().random()
    }

    @Test
    fun `Context Tests`() {
        val context = AuxFrameworkTestsContext()
        context.name = this::class.jvmName
        context.run()
        val dependency = context.componentFactory()
            .getAllComponents()
            .filter(ComponentDefinition::isInitialized)
            .map { it.cast<Any>() }
            .filterIsInstance<ComponentB>()
            .firstOrNull()
        assertNotNull(dependency)
        assertEquals(dependency.dependency, context.componentFactory().getComponent())
        Runtime.getRuntime().addShutdownHook(
            thread(start = false) {
                context.close()
            }
        )
    }

}
