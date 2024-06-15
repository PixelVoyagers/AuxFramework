package pixel.auxframework.core.registry

interface RegistryAware <T : IRegistry<*>> {

    fun setRegistry(registry: T)

}