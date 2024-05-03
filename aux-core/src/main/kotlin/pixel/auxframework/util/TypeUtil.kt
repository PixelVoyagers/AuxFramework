package pixel.auxframework.util

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

fun Type.toParameterized(): ParameterizedType = this as ParameterizedType
fun Type.toJavaClass(): Class<*> = this as Class<*>
fun Type.toClass(): KClass<*> = toJavaClass().kotlin

fun KType.toParameterized(): ParameterizedType = this.javaType.toParameterized()
fun KType.toJavaClass(): Class<*> = this.javaType.toJavaClass()
fun KType.toClass(): KClass<*> = this.javaType.toClass()
