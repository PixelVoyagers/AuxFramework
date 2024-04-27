package pixel.auxframework.core.io.resource

import javassist.bytecode.ClassFile
import org.apache.commons.io.FilenameUtils
import org.reflections.Reflections
import org.reflections.Store
import org.reflections.scanners.Scanner
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.NameHelper
import org.reflections.util.QueryBuilder
import org.reflections.util.QueryFunction
import org.reflections.vfs.Vfs
import pixel.auxframework.core.io.Resource
import pixel.auxframework.core.io.URLResource
import java.util.regex.Pattern

object ClassFileScanner : Scanner, QueryBuilder, NameHelper {

    override fun scan(classFile: ClassFile?): MutableList<MutableMap.MutableEntry<String, String>> {
        throw UnsupportedOperationException()
    }

    override fun acceptsInput(file: String): Boolean {
        val name = FilenameUtils.getBaseName(file)
        if (name == "module-info" || name == "package-info") return false
        return file.endsWith(".class")
    }

    override fun scan(file: Vfs.File): List<Map.Entry<String, String>> {
        return listOf(entry(file.name, file.relativePath))
    }

    override fun index() = "auxframework.classes"

    override fun with(pattern: String): QueryFunction<Store, String> {
        return QueryFunction { store ->
            store.getOrDefault(index(), emptyMap())
                .entries.stream()
                .filter { (key) -> key.matches(pattern.toRegex()) }
                .flatMap { (_, value): Map.Entry<String, Set<String>> -> value.stream() }
                .toList().toSet()
        }
    }

}

class DefaultResourceLoader(private val classLoader: ClassLoader, private val matcher: PathMatcher = DefaultPathMatcher()) : ResourceLoader {

    fun getMatcher() = matcher

    override fun getClassLoader() = classLoader

    override fun getResources(pattern: String): List<Resource> {
        if (!pattern.startsWith("${PathResourceScanner.prefixClasspath}:")) return emptyList()
        val reflections = Reflections(
            ConfigurationBuilder()
                .forPackages(*classLoader.definedPackages.map { it.name }.toTypedArray())
                .addClassLoaders(classLoader)
                .setScanners(Scanners.Resources, ClassFileScanner)
        )
        val resourceNames = reflections.getResources(Pattern.compile(".*")).apply {
            this += reflections.getAll(ClassFileScanner)
        }
        return resourceNames.filter { matcher.matches(it, pattern) }.mapNotNull {
            classLoader.getResource(it)?.let { resource -> URLResource(resource) }
        }
    }

}