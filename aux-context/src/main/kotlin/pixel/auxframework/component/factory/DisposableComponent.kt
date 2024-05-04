package pixel.auxframework.component.factory

/**
 * 可销毁的组件
 */
interface DisposableComponent {

    /**
     * 在组件销毁后执行
     */
    fun dispose()

}