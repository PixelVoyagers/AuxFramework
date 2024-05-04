package pixel.auxframework.context.builtin

import arrow.core.Option
import java.lang.reflect.Method

/**
 * 抽象组件方法调用处理程序
 */
interface AbstractComponentMethodInvocationHandler<T> {

    fun handleAbstractComponentMethodInvocation(proxy: T, method: Method, arguments: Array<out Any?>): Option<Any?>

}