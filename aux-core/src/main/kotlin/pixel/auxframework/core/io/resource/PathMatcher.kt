package pixel.auxframework.core.io.resource

import org.apache.commons.io.FilenameUtils
import java.net.URI

interface PathMatcher {
    fun matches(path: String, pattern: String): Boolean
}

class DefaultPathMatcher : PathMatcher {

    override fun matches(path: String, pattern: String): Boolean {
        val resolvedPattern = FilenameUtils.separatorsToUnix(URI.create(pattern).path).trimStart('/')
        return FilenameUtils.wildcardMatch(FilenameUtils.separatorsToUnix(path).trimStart('/'), resolvedPattern)
    }

}
