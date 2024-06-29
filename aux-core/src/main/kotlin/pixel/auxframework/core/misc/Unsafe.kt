package pixel.auxframework.core.misc

object Unsafe

context(Unsafe)
@Suppress("UNCHECKED_CAST")
fun <T> Any?.cast(): T = this as T

context(Unsafe)
@Suppress("UNCHECKED_CAST")
fun <T> Any?.castOrNull(): T? = runCatching {
    this@castOrNull as? T
}.getOrNull()
