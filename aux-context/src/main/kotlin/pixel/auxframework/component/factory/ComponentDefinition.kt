package pixel.auxframework.component.factory

import kotlin.reflect.KClass

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
    fun <T> cast(): T = instance!! as T

}