package pixel.auxframework.application

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import pixel.auxframework.component.annotation.Autowired
import pixel.auxframework.component.annotation.Component
import pixel.auxframework.component.annotation.OnlyIn
import pixel.auxframework.component.factory.*
import pixel.auxframework.context.AuxContext
import pixel.auxframework.scheduling.annotation.Scheduled
import pixel.auxframework.scheduling.annotation.TimerSchedule
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuxApplicationTests {

    class AuxFrameworkTestsContext : ApplicationContext()

    @OnlyIn(contextType = [AuxFrameworkTestsContext::class])
    @Component
    class ComponentA : AfterComponentAutowired, PostConstruct {

        @Autowired
        private lateinit var context: AuxContext

        @Autowired
        private lateinit var components: Array<*>

        @Autowired
        private lateinit var componentFactory: ComponentFactory

        @Component
        fun subComponent(): Any {
            return object : ComponentDefinitionAware {
                override fun setComponentDefinition(componentDefinition: ComponentDefinition) {
                    assertEquals(this, componentDefinition.cast())
                }
            }
        }

        @Autowired
        var lazyDependency: ComponentB? = null

        override fun afterComponentAutowired() {
            val componentFactoryComponents = componentFactory.getComponents<Any>()
            assertAll(
                *components
                    .map {
                        { assert(it in componentFactoryComponents) }
                    }.toTypedArray()
            )
            assertEquals(this, lazyDependency?.dependency)
        }

        override fun postConstruct() {
            assertNull(lazyDependency)
        }

    }

    @OnlyIn(contextType = [AuxFrameworkTestsContext::class])
    @Component
    @Scheduled
    class ComponentB(val dependency: ComponentA) {

        @Scheduled
        @TimerSchedule(0, TimeUnit.MILLISECONDS)
        fun run() = assert(dependency.lazyDependency == this)

    }

    @OnlyIn(contextType = [AuxFrameworkTestsContext::class])
    @Component
    class TestModule : AuxModule, AuxModuleStatusSupplier {
        override fun getStatus() = enumValues<ModuleStatus>().random()
    }

    @Test
    fun `Application Tests`() {
        val context = AuxFrameworkTestsContext()
        context.name = this::class.jvmName
        val application = AuxApplicationBuilder()
            .target<AuxApplicationTests>()
            .context(context)
            .build()
        application.run()
        val dependency = context.componentFactory()
            .getAllComponents()
            .filter(ComponentDefinition::isInitialized)
            .map { it.cast<Any>() }
            .filterIsInstance<ComponentB>()
            .firstOrNull()
        assertNotNull(dependency)
        assertEquals(dependency.dependency, context.componentFactory().getComponent())
        application.close()
    }

}
