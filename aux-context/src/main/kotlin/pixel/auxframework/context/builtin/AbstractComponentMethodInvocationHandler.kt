package pixel.auxframework.context.builtin

import arrow.core.Option
import java.lang.reflect.Method

interface AbstractComponentMethodInvocationHandler <T> {

    fun handleAbstractComponentMethodInvocation(proxy: T, method: Method, arguments: Array<out Any?>): Option<Any?>

}