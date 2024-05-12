package pixel.auxframework.util

import com.google.common.base.Objects

object FunctionUtils {

    private val memory: MutableMap<String, Any?> = mutableMapOf()
    @Suppress("UNCHECKED_CAST")
    fun <T> memorize(vararg seeds: Any?, block: () -> T): T {
        val stack = Throwable().stackTrace
        val caller = stack.getOrNull(1) ?: return block()
        val key = "seed=${Objects.hashCode(*seeds)},${seeds.size},${seeds::class.java.arrayType().name};" +
                "module=${caller.moduleName}@${caller.moduleVersion};" +
                "classLoader=${caller.classLoaderName};" +
                "method=${caller.className}::${caller.methodName};" +
                "native=${caller.isNativeMethod};" +
                "file=${caller.fileName}@${caller.lineNumber}"
        return (memory.getOrPut(key, block) as? T)!!
    }

}