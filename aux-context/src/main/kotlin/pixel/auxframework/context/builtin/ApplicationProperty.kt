package pixel.auxframework.context.builtin

import pixel.auxframework.core.AuxVersion
import kotlin.reflect.KClass

interface ApplicationProperty<T> {
    fun get(): T
}

interface MutableApplicationProperty<T> : ApplicationProperty<T> {
    fun set(value: T)
}

open class DefaultMutableApplicationProperty<T : Any>(
    initialize: T,
    private val type: KClass<out T> = initialize::class
) : MutableApplicationProperty<T> {
    var value = initialize
    override fun get() = value
    override fun set(value: T) {
        this.value = value
    }
}

class ArgumentsProperty(vararg arguments: String) : DefaultMutableApplicationProperty<Array<out String>>(arguments)
class VersionProperty(version: AuxVersion) : DefaultMutableApplicationProperty<AuxVersion>(version)

