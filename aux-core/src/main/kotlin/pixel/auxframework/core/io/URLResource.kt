package pixel.auxframework.core.io

import org.apache.commons.io.FilenameUtils
import java.io.InputStream
import java.net.URL

class URLResource(private val url: URL) : Resource {

    override fun getPath(): String = url.toString()
    override fun getName(): String = FilenameUtils.getName(url.file)
    override fun getURL() = url
    override fun isOpen() = true
    override fun canRead() = true
    override fun inputStream(): InputStream = url.openStream()

}