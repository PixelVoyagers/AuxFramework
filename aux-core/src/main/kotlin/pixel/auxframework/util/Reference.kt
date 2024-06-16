package pixel.auxframework.util

interface Reference <T> {

    fun get(): T
    fun set(value: T)

}

class MutableReference<T>(private var value: T) : Reference<T> {

    override fun get() = value
    override fun set(value: T) {
        this.value = value
    }

    override fun hashCode() = get().hashCode()
    override fun equals(other: Any?) = other === this || (other != null && other is MutableReference<*> && other.value == value)

}
