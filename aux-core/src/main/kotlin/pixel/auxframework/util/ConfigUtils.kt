package pixel.auxframework.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

object ConfigUtils {


    enum class ConfigTypes(private val mapper: () -> ObjectMapper) : ConfigType {

        JSON({ JsonMapper() }), YAML({ YAMLMapper() }), XML({ XmlMapper() }), CSV({ CsvMapper() }), TOML({ TomlMapper() });

        override fun <T> write(value: T): ByteArray = mapper().writeValueAsBytes(value)

        override fun <T : Any> readAs(bytes: ByteArray, typeReference: TypeReference<T>): T =
            mapper().readValue(bytes, typeReference)

    }

    interface ConfigType {

        /**
         * 读取
         */
        fun <T : Any> readAs(bytes: ByteArray, typeReference: TypeReference<T>): T

        /**
         * 读取
         */
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> readAs(bytes: ByteArray, clazz: Class<T>) =
            (readAs(bytes, ClassTypeReference(clazz) as TypeReference<T>) as? T)!!

        /**
         * 默认
         */
        fun <T> default(clazz: Class<T>): ByteArray {
            val value = clazz.getConstructor().newInstance()
            return write(value)
        }

        /**
         * 写入
         */
        fun <T> write(value: T): ByteArray

    }

    /**
     * 读取
     */
    inline fun <reified T : Any> ConfigType.read(bytes: ByteArray, typeReference: TypeReference<T> = jacksonTypeRef()) =
        readAs(bytes, typeReference)


}

inline fun <reified T : Any> Path.useAuxConfig(
    name: String,
    type: ConfigUtils.ConfigType = ConfigUtils.ConfigTypes.YAML
): T {
    val file = Path.of(this.pathString, name).toFile()
    file.parentFile.mkdirs()
    if (!file.exists()) {
        file.createNewFile()
        file.writeBytes(type.default(T::class.java))
    }
    return type.readAs(file.readBytes(), jacksonTypeRef<T>())
}

inline fun <reified T : Any> File.useAuxConfig(
    name: String,
    type: ConfigUtils.ConfigType = ConfigUtils.ConfigTypes.YAML
) = toPath().useAuxConfig<T>(name, type)
