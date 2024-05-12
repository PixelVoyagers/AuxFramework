package pixel.auxframework.web.annotation

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class QueryVariable(val name: String = "<default>")
