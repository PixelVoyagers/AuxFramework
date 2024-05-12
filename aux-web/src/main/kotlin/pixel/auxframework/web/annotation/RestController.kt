package pixel.auxframework.web.annotation

import pixel.auxframework.component.annotation.Controller

/**
 * 控制器
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Controller
@MustBeDocumented
annotation class RestController(vararg val roots: Path)
