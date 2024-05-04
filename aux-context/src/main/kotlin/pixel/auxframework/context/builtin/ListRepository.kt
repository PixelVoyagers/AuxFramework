package pixel.auxframework.context.builtin

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import pixel.auxframework.component.annotation.Repository
import pixel.auxframework.component.annotation.Service
import java.lang.reflect.Method

/**
 * 列表仓库
 *
 * ```kotlin
 * @Repository
 * interface MyDataRepository : ListRepository {
 *     fun addData(data: Data)
 *     fun getAllData(): List<Data>
 *     fun removeData(data: Data)
 *     fun removeDataElement(data: Data)
 *     fun removeDataAt(index: Int)
 * }
 * ```
 * @see SimpleListRepository
 */
interface ListRepository

/**
 * 简单的列表仓库
 */
interface SimpleListRepository<T> : ListRepository {
    fun add(element: T)
    fun getAll(): List<T>
    fun remove(element: T)
    fun removeAt(index: Int)
    fun removeElement(element: T)
}

@Service
class ListRepositoryMethodInvocationMappingService : AbstractComponentMethodInvocationHandler<ListRepository> {

    private val container = mutableMapOf<ListRepository, MutableMap<String, MutableList<Any?>>>()

    override fun handleAbstractComponentMethodInvocation(
        proxy: ListRepository,
        method: Method,
        arguments: Array<out Any?>
    ): Option<Any?> {
        if (!proxy::class.java.isAnnotationPresent(Repository::class.java)) return None
        if (method.name.startsWith("add")) {
            val name = method.name.removePrefix("add")
            container.getOrPut(proxy) { mutableMapOf() }
                .getOrPut(name) { mutableListOf() }
                .add(arguments[0])
            return Some(null)
        } else if (method.name.startsWith("getAll")) {
            val name = method.name.removePrefix("getAll")
            val all = container.getOrPut(proxy) { mutableMapOf() }.getOrPut(name) { mutableListOf() }
            return Some(all)
        } else if (method.name.startsWith("remove")) {
            val name = method.name.removePrefix("remove")
            if (name.endsWith("At")) {
                container.getOrPut(proxy) { mutableMapOf() }
                    .getOrPut(name.removeSuffix("At")) { mutableListOf() }
                    .removeAt(arguments[0] as Int)
            } else if (name.endsWith("Element")) {
                container.getOrPut(proxy) { mutableMapOf() }
                    .getOrPut(name.removeSuffix("Element")) { mutableListOf() }
                    .remove(arguments[0])
            } else {
                container.getOrPut(proxy) { mutableMapOf() }
                    .getOrPut(name) { mutableListOf() }
                    .remove(arguments[0])
            }
            return Some(null)
        }
        return None
    }

}
