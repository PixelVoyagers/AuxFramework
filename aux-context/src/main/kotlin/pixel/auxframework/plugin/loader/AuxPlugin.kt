package pixel.auxframework.plugin.loader

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.collect.Range
import pixel.auxframework.core.registry.Identifier
import pixel.auxframework.core.registry.Identifiers
import pixel.auxframework.plugin.util.VersionRange
import java.io.File
import java.util.zip.ZipFile

class AuxPluginMetadata(private val file: File) {

    data class Metadata(private val name: String = "", private val version: String = "", private val dependencies: List<String> = mutableListOf(), private val languageAdapter: String? = null) {
        fun getName() = name
        fun getVersion() = version
        fun getDependencies(): List<Pair<String, VersionRange>> = dependencies.map {
            val split = it.split(":")
            split.first() to VersionRange(split.subList(1, split.size).joinToString(":"))
        }.toMutableList().apply {
            add(getLanguageAdapter().getNamespace() to VersionRange(Range.all()))
        }
        fun getLanguageAdapter() = languageAdapter?.let {
            Identifiers.parseOrThrow(it, defaultNamespace = "auxframework")
        } ?: Identifier("auxframework", "default")
    }

    private val zipFile = ZipFile(file)

    private val metadata = YAMLMapper().readValue<Metadata>(zipFile.getInputStream(zipFile.getEntry("plugin.yml")))

    fun getPluginFile() = file
    fun getZipFile() = zipFile
    fun getData() = metadata

}

class AuxPlugin(private val metadata: AuxPluginMetadata.Metadata, internal var initialized: Boolean = false, internal var languageAdapter: AuxLanguageAdapter? = null) {

    fun isInitialized() = initialized
    internal var file: File? = null
    fun getPluginFile() = file
    fun getPluginMetadata() = metadata
    internal var classLoader: AuxPluginClassLoader? = null
    fun getPluginClassLoader() = classLoader!!
    fun getPluginLanguageAdapter() = languageAdapter!!
    override fun toString() = "AuxPlugin[${getPluginMetadata().getName()}:${getPluginMetadata().getVersion()}]"

}