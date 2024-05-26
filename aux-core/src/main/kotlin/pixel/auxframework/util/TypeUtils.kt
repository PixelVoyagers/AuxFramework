package pixel.auxframework.util

import com.fasterxml.jackson.core.type.TypeReference
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

/**
 * 类类型引用
 */
class ClassTypeReference(private val clazz: Class<*>) : TypeReference<Any?>() {

    override fun getType() = clazz

}

fun Type.toParameterized(): ParameterizedType = this as ParameterizedType
fun Type.toJavaClass(): Class<*> = this as Class<*>
fun Type.toClass(): KClass<*> = toJavaClass().kotlin

fun KType.toParameterized(): ParameterizedType = this.javaType.toParameterized()
fun KType.toJavaClass(): Class<*> = this.javaType.toJavaClass()
fun KType.toClass(): KClass<*> = this.javaType.toClass()
