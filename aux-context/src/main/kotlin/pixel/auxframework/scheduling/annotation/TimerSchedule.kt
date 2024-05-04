package pixel.auxframework.scheduling.annotation

import java.util.concurrent.TimeUnit

@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class TimerSchedule(
    val interval: Long,
    val timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    val fixed: Boolean = false
)

