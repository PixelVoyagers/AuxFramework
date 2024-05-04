package pixel.auxframework.annotation

import kotlin.reflect.KClass

/**
 * 限定上下文
 * @see Component
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class OnlyIn(val contextName: String = "<null>", val contextType: Array<KClass<*>> = [])
