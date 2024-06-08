package pixel.auxframework.core.io.resource

import pixel.auxframework.core.io.FileSystemResource
import pixel.auxframework.core.io.Resource
import java.io.File
import java.net.URI
import java.nio.file.Path

class PathResourceScanner(
    val resourceLoader: ResourceLoader,
    private val pathMatcher: PathMatcher = DefaultPathMatcher()
) : ResourceLoader {

    fun getMatcher() = pathMatcher

    companion object {
        const val PREFIX_CLASSPATH = "classpath"
        const val PREFIX_FILE = "file"
    }

    override fun getResource(name: String): Resource? {
        return if (name.startsWith("$PREFIX_FILE:")) FileSystemResource(File(name.removePrefix("$PREFIX_FILE:")))
        else if (name.startsWith("$PREFIX_CLASSPATH:")) resourceLoader.getResource(name)
        else null
    }

    override fun getResources(pattern: String): List<Resource> {
        return if (pattern.startsWith("$PREFIX_FILE:")) getFileResources(pattern)
        else if (pattern.startsWith("$PREFIX_CLASSPATH:")) resourceLoader.getResources(pattern)
        else emptyList()
    }

    fun getFileResources(pattern: String): List<Resource> {
        val pathURI = URI.create(pattern)
        val path = pathURI.path.split("/", "\\").filter(String::isNotEmpty)

        val realPath = path.subList(0, path.indexOfFirst { "*" in it }.let { if (it == -1) path.size else it })
        val file = Path.of("", *realPath.toTypedArray()).toFile()

        return file.walkTopDown().filter {
            pathMatcher.matches(it.path, pattern)
        }.map { FileSystemResource(it) }.toList()
    }

    override fun getClassLoader() = resourceLoader.getClassLoader()

}