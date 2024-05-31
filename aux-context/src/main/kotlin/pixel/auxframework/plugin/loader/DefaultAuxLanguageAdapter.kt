package pixel.auxframework.plugin.loader

import pixel.auxframework.component.annotation.Component
import pixel.auxframework.core.registry.Identifier

@Component
class DefaultAuxLanguageAdapter : AuxLanguageAdapter {

    override fun getIdentifier() = Identifier("auxframework", "default")
    override fun createClassLoader(plugin: AuxPlugin) =
        AuxPluginClassLoader(
            plugin,
            *plugin.getPluginFile()?.toURI()?.toURL()?.let { arrayOf(it) } ?: emptyArray(),
            parent = this::class.java.classLoader)

    override suspend fun disposePlugin(plugin: AuxPlugin) {}
    override suspend fun initializePlugin(plugin: AuxPlugin) {}

}