package pixel.auxframework.component.factory

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * 组件定义
 * @see ComponentFactory
 * @see ComponentProcessor
 */
open class ComponentDefinition(var name: String, val type: KClass<*>) {

    constructor(
        instance: Any,
        name: String = instance::class.java.name,
        type: KClass<*> = instance::class,
        loaded: Boolean = true
    ) : this(name, type) {
        this.instance = instance
        this.loaded = loaded
    }

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

    @Suppress("UNCHECKED_CAST")
    open fun <T> castOrNull(): T? = try {
        instance as? T?
    } catch (_: Throwable) {
        null
    }

    open fun isInstance(type: KClass<*>) =
        (isInitialized() && type.isInstance(cast())) || type == this.type || this.type.isSubclassOf(type)

    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?) = other != null && other is ComponentDefinition && other.name == name

    override fun toString() = "ComponentDefinition[${this.name}]"

}

inline fun <reified T> ComponentDefinition.isInstance() = isInstance(T::class)
