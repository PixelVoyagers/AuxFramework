package pixel.auxframework.util

import java.util.*

/**
 * 转为不可变列表
 */
fun <T> List<T>.toImmutableList(): List<T> = Collections.unmodifiableList(toMutableList())

/**
 * 不可变列表
 */
@SafeVarargs
fun <T> immutableListOf(vararg elements: T): List<T> = Collections.unmodifiableList(elements.toList())

/**
 * 转为不可变列表
 */
fun <T> Set<T>.toImmutableSet(): Set<T> = Collections.unmodifiableSet(toMutableSet())

/**
 * 不可变列表
 */
@SafeVarargs
fun <T> immutableSetOf(vararg elements: T): Set<T> = Collections.unmodifiableSet(elements.toSet())

/**
 * 转为不可变表
 */
fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> = Collections.unmodifiableMap(toMap())

/**
 * 不可变表
 */
@SafeVarargs
fun <K, V> immutableMapOf(vararg elements: Pair<K, V>): Map<K, V> = Collections.unmodifiableMap(elements.toMap())

