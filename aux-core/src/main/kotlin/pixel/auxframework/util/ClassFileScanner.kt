package pixel.auxframework.util

import org.apache.commons.io.FilenameUtils
import org.reflections.Store
import org.reflections.scanners.Scanner
import org.reflections.util.NameHelper
import org.reflections.util.QueryBuilder
import org.reflections.util.QueryFunction
import org.reflections.vfs.Vfs

object ClassFileScanner : Scanner, QueryBuilder, NameHelper {


    override fun acceptsInput(file: String): Boolean {
        val name = FilenameUtils.getBaseName(file)
        if (name == "module-info" || name == "package-info") return false
        return file.endsWith(".class")
    }

    override fun scan(file: javassist.bytecode.ClassFile): List<MutableMap.MutableEntry<String, String>> {
        throw UnsupportedOperationException()
    }

    override fun scan(file: Vfs.File): List<Map.Entry<String, String>> {
        return listOf(entry(file.relativePath, file.name))
    }

    override fun index() = "auxframework:classes"

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