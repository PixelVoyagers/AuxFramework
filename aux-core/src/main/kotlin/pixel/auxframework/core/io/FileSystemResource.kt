package pixel.auxframework.core.io

import java.io.File
import java.net.URL

class FileSystemResource(private val file: File) : Resource {

    fun getFile() = file
    override fun getName(): String = file.name
    override fun getPath(): String = file.path
    override fun getURL(): URL = file.toURI().toURL()
    override fun inputStream() = file.inputStream()
    override fun isOpen() = true
    override fun canRead() = file.canRead()

}