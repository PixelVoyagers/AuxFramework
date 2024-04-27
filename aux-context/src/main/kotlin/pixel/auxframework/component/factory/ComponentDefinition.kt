package pixel.auxframework.component.factory

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

open class ComponentDefinition(var name: String, val type: KClass<*>) {

    constructor(type: KClass<*>) : this(type.java.name, type)

    private var instance: Any? = null
    var loaded = false

    open fun isLoaded(): Boolean = isInitialized() && loaded
    open fun isInitialized(): Boolean = instance != null

    fun setInstance(instance: Any?) {
        if (this.instance != null) throw IllegalStateException("Instance already set")
        this.instance = instance
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T> cast(): T = instance!! as T

    open fun isInstance(type: KClass<*>) = (type.isInstance(isLoaded() && cast())) || type == this.type || this.type.isSubclassOf(type)

}

inline fun <reified T> ComponentDefinition.isInstance() = isInstance(T::class)
