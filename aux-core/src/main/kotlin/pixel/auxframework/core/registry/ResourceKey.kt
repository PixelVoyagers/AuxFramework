package pixel.auxframework.core.registry

import com.google.common.base.Objects

class ResourceKey<T>(private val registryName: Identifier, private val location: Identifier) {

    fun getRegistryName() = registryName
    fun getLocation() = location

    @Suppress("UNCHECKED_CAST")
    fun <R> cast() = this as ResourceKey<R>

    fun isFor(resourceKey: ResourceKey<IRegistry<*>>) = resourceKey.location == registryName

    override fun hashCode() = Objects.hashCode(registryName, location)

    override fun equals(other: Any?) =
        other != null && (other === this || (other is ResourceKey<*> && other.registryName == this.registryName && other.location == this.location)) || (other is ResourceKey<*> && other.hashCode() == hashCode())

}