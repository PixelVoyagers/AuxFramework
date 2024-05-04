package pixel.auxframework.scheduling.annotation

@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class CronSchedule(val cronExpression: String)
