package pixel.auxframework.core.io.resource

import pixel.auxframework.core.io.Resource

interface ResourceLoader {

    fun getResource(name: String): Resource? = getResources(name).firstOrNull()
    fun getResources(pattern: String): List<Resource>
    fun getClassLoader(): ClassLoader? = null

}