package pixel.auxframework.plugin.loader

import java.net.URL
import java.net.URLClassLoader

open class AuxPluginClassLoader(val plugin: AuxPlugin, vararg urls: URL, parent: ClassLoader? = null) :
    URLClassLoader(urls, parent) {

    companion object {

        fun getPluginByClass(clazz: Class<*>): AuxPlugin? {
            val classLoader = clazz.classLoader
            return if (classLoader is AuxPluginClassLoader) classLoader.plugin
            else null
        }

        fun getPluginByObject(obj: Any) = getPluginByClass(obj::class.java)

    }


    override fun loadClass(name: String?): Class<*> {
        return runCatching { super.loadClass(name) }.getOrNull() ?: runCatching { parent.loadClass(name) }.getOrNull() ?: AuxPluginLoader::class.java.classLoader.loadClass(name)
    }

}