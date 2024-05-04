package pixel.auxframework.scheduling.annotation

@Retention(AnnotationRetention.RUNTIME)
annotation class CronSchedule(val cronExpression: String)
