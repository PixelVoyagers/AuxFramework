package pixel.auxframework.annotation

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class OnlyIn(val contextName: String = "<null>", val contextType: Array<KClass<*>> = [])
