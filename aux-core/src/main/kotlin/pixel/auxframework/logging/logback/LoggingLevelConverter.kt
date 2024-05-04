package pixel.auxframework.logging.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import pixel.auxframework.logging.common.AnsiColor
import pixel.auxframework.logging.common.AnsiFormat
import pixel.auxframework.logging.common.AnsiStyle

class LoggingLevelConverter : ClassicConverter() {

    override fun convert(event: ILoggingEvent) = getColorFormat(event.level).format(event.level.toString())

    fun getColorFormat(level: Level): AnsiFormat {
        return when (level.toInt()) {
            Level.ERROR_INT -> AnsiFormat.Builder().foregroundColor(AnsiColor.Red).style(AnsiStyle.Bold).build()
            Level.WARN_INT -> AnsiFormat.Builder().foregroundColor(AnsiColor.Yellow).style(AnsiStyle.Bold).build()
            Level.INFO_INT -> AnsiFormat.Builder().foregroundColor(AnsiColor.Cyan).build()
            Level.DEBUG_INT -> AnsiFormat.Builder().foregroundColor(AnsiColor.Blue).build()
            else -> AnsiFormat.Builder().build()
        }
    }

}
