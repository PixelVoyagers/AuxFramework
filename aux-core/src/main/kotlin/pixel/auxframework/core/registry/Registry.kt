package pixel.auxframework.core.registry

import com.google.common.collect.HashBiMap

interface IRegistry <T> : Iterable<Pair<Identifier, T>> {

    fun freeze()
    fun unfreeze()
    fun isFrozen(): Boolean

    fun getRegistryKey(): ResourceKey<IRegistry<T>>

    fun tags(): ITagManager<T>

    fun register(name: Identifier, value: () -> T): T

    fun contains(key: Identifier): Boolean
    fun contains(value: T): Boolean

    fun get(key: Identifier): T?
    fun get(value: T): Identifier?

}

open class Registry<T>(private val registryKey: ResourceKey<IRegistry<T>>, private val onRegistry: ((Identifier, T) -> Unit)? = null) : IRegistry<T> {

    protected open val tagManager: ITagManager<T> by lazy {
        TagManager(this)
    }

    protected val entries: HashBiMap<Identifier, T> = HashBiMap.create<Identifier, T>()
    private var isFrozen: Boolean = true

    override fun freeze() {
        isFrozen = true
    }

    override fun unfreeze() {
        isFrozen = false
    }

    override fun isFrozen() = isFrozen

    override fun tags() = tagManager

    override fun getRegistryKey() = registryKey

    override operator fun contains(key: Identifier) = key in entries
    override operator fun contains(value: T) = value in entries.inverse()

    @Suppress("UNCHECKED_CAST")
    override fun register(name: Identifier, value: () -> T) = value().also {
        if (isFrozen) throw IllegalStateException("Registry is already frozen")
        if (name in this || it in this) throw IllegalStateException("Duplicate registry entry: $name")
        if (it is RegistryAware<*>) (it as RegistryAware<IRegistry<T>>).setRegistry(this)
        entries[name] = it
        onRegistry?.invoke(name, it)
    }

    override fun iterator() = iterator {
        for (entry in entries) {
            yield(entry.key to entry.value)
        }
    }

    override fun get(key: Identifier) = entries[key]
    override fun get(value: T) = entries.inverse()[value]

}

