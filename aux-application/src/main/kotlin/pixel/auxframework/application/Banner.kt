package pixel.auxframework.application

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

        val COLOR_FORMAT = AnsiFormat
            .Builder()
            .foregroundColor(AnsiColor.Blue)
            .style(AnsiStyle.Bold)
            .build()

        val BANNER = """
            __   _  _  _  _  ____  ____   __   _  _  ____  _  _   __  ____  __ _ 
           / _\ / )( \( \/ )(  __)(  _ \ / _\ ( \/ )(  __)/ )( \ /  \(  _ \(  / )
          /    \) \/ ( )  (  ) _)  )   //    \/ \/ \ ) _) \ /\ /(  O ))   / )  ( 
          \_/\_/\____/(_/\_)(__)  (__\_)\_/\_/\_)(_/(____)(_/\_) \__/(__\_)(__\_)
           """.trimIndent().lines()

        override fun printBanner(context: ApplicationContext, stream: PrintStream) {
            stream.println(
                BANNER.joinToString("\n") {
                    COLOR_FORMAT.format(it)
                }
            )
        }
    }

}