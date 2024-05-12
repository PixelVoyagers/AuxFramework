package pixel.auxframework.core.registry

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import java.util.*

object Identifiers {

    /**
     * 解析标识符，如果失败则返回 [Optional.empty]
     */
    fun parse(identifier: String, defaultNamespace: String? = null): Option<Identifier> {
        if (identifier.trim().isEmpty()) return None
        val split = identifier.split(":")
        if (split.isEmpty()) return None
        if (split.size == 1) return Some(Identifier(defaultNamespace ?: return None, split.first()))
        return Some(
            Identifier(
                split.first(),
                split.subList(1, split.size).joinToString(separator = ":")
            )
        )
    }

    /**
     * 解析标识符
     */
    fun parseOrThrow(identifier: String, defaultNamespace: String? = null): Identifier =
        parse(identifier, defaultNamespace = defaultNamespace).getOrNull()!!

    /**
     * 判断是否为合法命名空间
     */
    fun isNamespace(namespace: String) = namespace.trim().isNotEmpty() && namespace.chars().allMatch {
        val character = it.toChar()
        character == '<' || character == '>' || character == '_' || character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' || character == '-'
    }

    /**
     * 判断是否为合法路径
     */
    fun isPath(path: String) = path.trim().isNotEmpty() && path.chars().allMatch {
        val character = it.toChar()
        character == ':' || character == '.' || character == '_' || character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' || character == '/' || character == '-'
    }

    operator fun Identifier.div(path: String) = toList().toMutableList().apply { add(path) }.toIdentifier()
    operator fun String.div(path: String) = identifierOf("$this:$path")
    fun List<String>.toIdentifier() = Identifier(first(), subList(1, size).joinToString("/"))

}

class Identifier(private val namespace: String, path: String) : Iterable<String> {

    private val path = path.split("/").filterNot(String::isEmpty).joinToString("/")

    override fun iterator() = mutableListOf(namespace).apply {
        addAll(path.split("/"))
    }.iterator()

    fun toPathList() = toMutableList().also(MutableList<*>::removeFirst)

    /**
     * 获取命名空间
     */
    fun getNamespace() = namespace

    /**
     * 获取路径
     */
    fun getPath() = path

    init {
        assert(Identifiers.isNamespace(namespace)) { "Invalid namespace: $namespace" }
        assert(Identifiers.isPath(path)) { "Invalid path: $path" }
    }

    override fun hashCode() = toString().hashCode()
    override fun toString() = "${getNamespace()}:${getPath()}"
    override fun equals(other: Any?) = other === this || (other != null && other.toString() == toString())

    /**
     * 格式化
     */
    fun format(formatter: (namespace: String, path: String) -> String) = formatter(getNamespace(), getPath())

}

fun identifierOf(identifier: String, defaultNamespace: String? = null) = Identifiers.parseOrThrow(identifier, defaultNamespace)

