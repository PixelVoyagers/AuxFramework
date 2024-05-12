package pixel.auxframework.application

import pixel.auxframework.core.AuxVersion
import pixel.auxframework.logging.common.AnsiColor
import pixel.auxframework.logging.common.AnsiFormat
import pixel.auxframework.logging.common.AnsiStyle
import java.io.PrintStream

interface Banner {

    fun printBanner(context: ApplicationContext, stream: PrintStream)

    /**
     * 默认 [Banner]
     */
    companion object : Banner {

        val BANNER_COLOR_FORMAT = AnsiFormat
            .Builder()
            .foregroundColor(AnsiColor.BLUE)
            .style(AnsiStyle.BOLD)
            .build()

        val VERSION_COLOR_FORMAT = AnsiFormat
            .Builder()
            .foregroundColor(AnsiColor.CYAN)
            .style(AnsiStyle.ITALIC, AnsiStyle.BOLD)
            .build()

        val BANNER = """
            __   _  _  _  _  ____  ____   __   _  _  ____  _  _   __  ____  __ _ 
           / _\ / )( \( \/ )(  __)(  _ \ / _\ ( \/ )(  __)/ )( \ /  \(  _ \(  / )
          /    \) \/ ( )  (  ) _)  )   //    \/ \/ \ ) _) \ /\ /(  O ))   / )  ( 
          \_/\_/\____/(_/\_)(__)  (__\_)\_/\_/\_)(_/(____)(_/\_) \__/(__\_)(__\_)
           """.trimIndent().lines()

        override fun printBanner(context: ApplicationContext, stream: PrintStream) {
            if (!context.log.isInfoEnabled) return
            stream.println(
                BANNER.joinToString("\n") {
                    BANNER_COLOR_FORMAT.format(it)
                }
            )
            val versions = listOf(
                "AuxFramework" to AuxVersion.current().toString(),
                "Kotlin" to KotlinVersion.CURRENT,
                "JVM" to System.getProperty("java.vm.version", "<null>")
            )
            stream.println(
                versions.filter { it.second != "<null>" }.joinToString(separator = " ") {
                    "${VERSION_COLOR_FORMAT.format(it.first)}(${it.second})"
                }
            )
        }
    }

}