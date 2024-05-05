package pixel.auxframework.logging.common

class AnsiFormat(private val builder: Builder) {

    companion object {
        var enabled = true
    }

    data class Builder(
        val background: Int = AnsiColor.DEFAULT.offset,
        val foreground: Int = AnsiColor.DEFAULT.offset,
        val styles: Set<Int> = mutableSetOf()
    ) {
        fun backgroundColor(color: AnsiColor) = copy(background = color.offset)
        fun foregroundColor(color: AnsiColor) = copy(foreground = color.offset)
        fun backgroundColor(color: Int) = copy(background = color)
        fun foregroundColor(color: Int) = copy(foreground = color)
        fun style(vararg style: AnsiStyle) =
            copy(styles = mutableSetOf(*this.styles.toTypedArray(), *style.map(AnsiStyle::code).toTypedArray()))

        fun style(vararg style: Int) = copy(styles = mutableSetOf(*this.styles.toTypedArray(), *style.toTypedArray()))
        fun build() = AnsiFormat(this)
    }

    fun format(text: String): String {
        if (!enabled) return text
        var flags = "${builder.foreground + 30};${builder.background + 40}"
        if (builder.styles.isNotEmpty()) flags += builder.styles.joinToString(
            separator = ";",
            prefix = ";",
            transform = Int::toString
        )
        return "\u001B[${flags}m$text\u001B[m"
    }

}