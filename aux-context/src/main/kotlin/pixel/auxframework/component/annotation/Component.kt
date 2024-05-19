package pixel.auxframework.component.annotation

/**
 * 组件
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Component

/**
 * 提前加载
 * @see Component
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Preload(val enabled: Boolean = true)

