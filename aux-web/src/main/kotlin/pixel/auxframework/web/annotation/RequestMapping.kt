package pixel.auxframework.web.annotation

annotation class Path(val path: String, val isRegex: Boolean = false)

/**
 * 请求处理器
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class RequestMapping(vararg val paths: Path, val methods: Array<String> = ["GET"])
