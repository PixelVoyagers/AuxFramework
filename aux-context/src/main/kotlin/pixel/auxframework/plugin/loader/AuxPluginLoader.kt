package pixel.auxframework.plugin.loader

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import pixel.auxframework.component.annotation.*
import pixel.auxframework.component.factory.*
import pixel.auxframework.context.builtin.SimpleListRepository
import pixel.auxframework.core.AuxVersion
import pixel.auxframework.core.registry.Identifier
import pixel.auxframework.util.ClassFileScanner
import java.io.File
import kotlin.reflect.full.findAnnotation

open class PluginInitializingException(message: String? = null, cause: Throwable? = null) :
    RuntimeException(message, cause)

@Component
data class AuxPluginLoaderConfig(@Autowired(false) var directories: MutableList<File> = mutableListOf())

@Repository
abstract class AuxPluginContainer(private val defaultAuxLanguageAdapter: DefaultAuxLanguageAdapter) :
    SimpleListRepository<AuxPlugin>, PostConstruct, DisposableComponent {

    override fun postConstruct() {
        add(
            AuxPlugin(
                AuxPluginMetadata.Metadata(name = "auxframework", version = AuxVersion.current().version),
                initialized = true,
                languageAdapter = defaultAuxLanguageAdapter,
            )
        )
    }

    fun getPluginByName(pluginName: String) = getAll().firstOrNull { it.getPluginMetadata().getName() == pluginName }
    fun isPluginInitialized(pluginName: String) = getPluginByName(pluginName)?.isInitialized() ?: false
    fun isPluginExists(pluginName: String) = getPluginByName(pluginName) != null

    override fun dispose(): Unit = runBlocking {
        getAll().map {
            async {
                it.getPluginLanguageAdapter().disposePlugin(it)
            }
        }.awaitAll()
    }

}

@Service
open class AuxPluginLoader(
    private val defaultAuxLanguageAdapter: DefaultAuxLanguageAdapter,
    private val componentProcessor: ComponentProcessor,
    private val componentFactory: ComponentFactory,
    private val container: AuxPluginContainer
) : PostContextRefresh {

    override fun postContextRefresh() = runBlocking {
        initializePlugins(scanPlugins())
        val definitions = mutableSetOf<ComponentDefinition>()
        for (plugin in container.getAll()) runCatching {
            plugin.componentClasses.forEach {
                val definition = ComponentDefinition(type = it)
                componentFactory.defineComponent(definition)
                definitions += definition
            }
        }
        definitions.forEach {
            if (it.type.findAnnotation<Preload>()?.enabled == true) componentProcessor.initializeComponent(it)
        }
        definitions.forEach {
            if (it.type.findAnnotation<Preload>()?.enabled != true) componentProcessor.initializeComponent(it)
        }
    }

    fun getConfig() = componentFactory.getComponent<AuxPluginLoaderConfig>()

    fun scanPlugins() = mutableSetOf<AuxPluginMetadata>().apply {
        for (directory in getConfig().directories) {
            for (file in directory.listFiles()!!) {
                if (!file.isFile) continue
                val metadata = AuxPluginMetadata(file)
                this += metadata
            }
        }
    }

    suspend fun initializePlugins(plugins: Set<AuxPluginMetadata>) {
        for (pluginMetadata in plugins) {
            val plugin = AuxPlugin(pluginMetadata.getData())
            plugin.file = pluginMetadata.getPluginFile()
            if (!container.isPluginExists(pluginMetadata.getData().getName()))
                container.add(plugin)
            else throw PluginInitializingException("Duplicate plugin: '${pluginMetadata.getData().getName()}'")
        }
        for (plugin in container.getAll()) {
            initializePlugin(plugin, getLanguageAdapter(plugin.getPluginMetadata().getLanguageAdapter()))
        }
    }

    suspend fun getLanguageAdapter(
        identifier: Identifier,
        dependencyStack: MutableList<AuxPlugin> = mutableListOf()
    ): AuxLanguageAdapter {
        if (identifier == defaultAuxLanguageAdapter.getIdentifier()) return defaultAuxLanguageAdapter
        val adapters = componentFactory.getComponents<AuxLanguageAdapter>()
        val found = adapters.firstOrNull { it.getIdentifier() == identifier }
        if (found != null) return found
        val plugin = container.getPluginByName(identifier.getNamespace())
            ?: throw PluginInitializingException("Plugin not found: '${identifier.getNamespace()}'")
        if (!plugin.isInitialized()) {
            dependencyStack += plugin
            initializePlugin(
                plugin,
                getLanguageAdapter(plugin.getPluginMetadata().getLanguageAdapter(), dependencyStack)
            )
        }
        return componentFactory.getComponents<AuxLanguageAdapter>().firstOrNull { it.getIdentifier() == identifier }
            ?: throw PluginInitializingException("Undefined language adapter: '${identifier.getNamespace()}'")
    }

    suspend fun initializePlugin(
        plugin: AuxPlugin,
        adapter: AuxLanguageAdapter,
        dependencyStack: MutableList<AuxPlugin> = mutableListOf()
    ) {
        plugin.languageAdapter = adapter
        if (dependencyStack.contains(plugin)) {
            val stackOverFlowError =
                StackOverflowError(dependencyStack.joinToString { it.getPluginMetadata().getName() })
            throw PluginInitializingException(
                "Circular dependency error occurred while initializing plugin '${
                    plugin.getPluginMetadata().getName()
                }'",
                stackOverFlowError
            )
        }
        dependencyStack += plugin
        if (plugin.isInitialized()) return
        for (dependency in plugin.getPluginMetadata().getDependencies()) {
            val dependencyPlugin = container.getPluginByName(dependency.first)
                ?: throw PluginInitializingException("Plugin not found: '${dependency.first}'")
            if (!dependencyPlugin.isInitialized()) {
                initializePlugin(
                    dependencyPlugin,
                    getLanguageAdapter(
                        dependencyPlugin.getPluginMetadata().getLanguageAdapter(),
                        dependencyStack = dependencyStack
                    ),
                    dependencyStack
                )
            }
        }
        plugin.classLoader = adapter.createClassLoader(plugin)
        adapter.initializePlugin(plugin)
        plugin.classes = Reflections(
            ConfigurationBuilder()
                .addScanners(ClassFileScanner)
                .addClassLoaders(plugin.classLoader)
                .addUrls(*plugin.classLoader!!.urLs)
                .forPackages(*plugin.classLoader!!.definedPackages.map { it.name }.toTypedArray())
        ).getAll(ClassFileScanner).mapNotNull {
            runCatching { plugin.classLoader?.loadClass(it.split("/").filter(String::isNotEmpty).joinToString(".")) }.getOrNull()?.kotlin
        }.toSet()
        plugin.componentClasses = componentProcessor.scanComponents(setOf(plugin.getPluginClassLoader()), false) {
            addUrls(*plugin.classLoader!!.urLs)
        }
    }

}

interface AuxLanguageAdapter {

    fun getIdentifier(): Identifier
    fun createClassLoader(plugin: AuxPlugin): AuxPluginClassLoader

    suspend fun initializePlugin(plugin: AuxPlugin)
    suspend fun disposePlugin(plugin: AuxPlugin)

}
