package pixel.auxframework.core.runtime.status

interface StatusSupplier <T : BaseStatus> {
    fun getStatus(): T
}