package pixel.auxframework.core.io

import java.io.InputStream
import java.net.URL

interface Resource {
    fun canRead(): Boolean
    fun isOpen(): Boolean
    fun getURL(): URL
    fun getName(): String
    fun getPath(): String
    fun inputStream(): InputStream

}