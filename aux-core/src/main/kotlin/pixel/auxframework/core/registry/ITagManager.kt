package pixel.auxframework.core.registry

import pixel.auxframework.util.toImmutableSet
import java.util.*

class TagKey<T>(private val registry: ResourceKey<IRegistry<T>>, private val location: Identifier) {

    fun getRegistry() = registry
    fun getLocation() = location

    fun <R> cast(registry: ResourceKey<IRegistry<R>>) = TagKey(registry, getLocation())

    override fun hashCode() = Objects.hash(registry, location)

    override fun equals(other: Any?) = other != null && (other === this || (other is TagKey<*> && other.hashCode() == hashCode()))

    fun splitPath() = getLocation().getPath().split("/").filter { it.isNotEmpty() }.toTypedArray()

    fun isChildOf(key: TagKey<T>): Boolean {
        if (key.getRegistry() != getRegistry()) throw IllegalArgumentException("Unequal registries: ${getRegistry()} and ${key.getRegistry()}")
        val thisSplitPath = splitPath()
        val keySplitPath = key.splitPath()
        if (thisSplitPath.size <= keySplitPath.size) return false
        keySplitPath.forEachIndexed { index, value ->
            if (thisSplitPath[index] != value) return false
        }
        return true
    }


    fun isParentOf(key: TagKey<T>) = key.isChildOf(this)

}

interface ITagManager <T> : Iterable<Pair<TagKey<*>, Set<T>>> {

    fun getRegistry(): IRegistry<T>

    fun getTags(): Set<TagKey<T>>

    fun createTagKey(location: Identifier) = TagKey(getRegistry().getRegistryKey(), location)
    fun addValues(key: TagKey<T>, vararg values: T)

    fun getSelfValues(key: TagKey<T>): Set<T>
    fun getChildValues(key: TagKey<T>): Set<T>
    fun getValues(key: TagKey<T>) = mutableSetOf<T>().apply {
        this += getSelfValues(key)
        this += getChildValues(key)
    }.toImmutableSet()

    fun removeTags(vararg tagKey: TagKey<T>)
    fun removeTagValues(tagKey: TagKey<T>, vararg values: T)
    fun clearTags(vararg tagKey: TagKey<T>)

    fun contains(key: TagKey<T>, value: T): Boolean = getValues(key).contains(value)
    fun contains(key: TagKey<T>): Boolean

}

open class TagManager<T>(private val registry: IRegistry<T>) : ITagManager<T> {

    protected val entries = mutableMapOf<TagKey<T>, MutableSet<T>>()

    override fun iterator() = iterator {
        for (item in entries) {
            yield(item.key to item.value.toImmutableSet())
        }
    }

    override fun getChildValues(key: TagKey<T>): Set<T> {
        val result = hashSetOf<T>()
        for (other in this)
            if (other.first.isChildOf(key)) result.addAll(other.second)
        return result.toImmutableSet()
    }

    override fun getSelfValues(key: TagKey<T>) = entries.getOrElse(key, ::emptySet).toImmutableSet()

    override fun addValues(key: TagKey<T>, vararg values: T) {
        entries.getOrPut(key, ::mutableSetOf).addAll(values)
    }

    override fun clearTags(vararg tagKey: TagKey<T>) {
        for (key in tagKey) {
            entries[key]?.clear()
        }
    }

    override fun removeTags(vararg tagKey: TagKey<T>) = tagKey.forEach(entries::remove)

    override fun removeTagValues(tagKey: TagKey<T>, vararg values: T) {
        entries[tagKey]?.removeAll(values.toSet())
    }

    override fun getTags() = entries.keys.toImmutableSet()

    override fun getRegistry() = registry

    override fun contains(key: TagKey<T>) = key in entries

}
