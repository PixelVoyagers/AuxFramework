package pixel.auxframework.context

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pixel.auxframework.component.annotation.Preload
import pixel.auxframework.component.factory.*
import pixel.auxframework.context.builtin.AfterContextRefreshed
import pixel.auxframework.context.builtin.ArgumentsProperty
import pixel.auxframework.context.builtin.VersionProperty
import pixel.auxframework.core.AuxVersion
import kotlin.reflect.full.findAnnotation

/**
 * 上下文
 */
abstract class AuxContext {

    /**
     * 上下文名称
     */
    var name: String = this.toString()

    /**
     * 日志
     */
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 上下文类加载器
     * @see scan
     */
    val classLoaders = mutableSetOf<ClassLoader>()

    protected abstract val componentFactory: ComponentFactory
    protected abstract val componentProcessor: ComponentProcessor

    init {
        classLoaders += this::class.java.classLoader
    }

    /**
     * 获取组件工厂
     */
    fun componentFactory() = componentFactory

    /**
     * 添加内置组件
     */
    protected open fun appendComponents(list: MutableList<ComponentDefinition>) {
        list.addAll(
            listOf(
                ComponentDefinition(this, loaded = true),
                ComponentDefinition(componentFactory(), loaded = true),
                ComponentDefinition(componentProcessor, loaded = true)
            )
        )
    }

    /**
     * 刷新
     */
    open fun refresh() {
        componentFactory().getAllComponents()
            .forEach {
                if (it.type.findAnnotation<Preload>()?.enabled == true)
                    componentProcessor.initializeComponent(it)
            }
        componentFactory().getAllComponents()
            .forEach {
                if (it.type.findAnnotation<Preload>()?.enabled != true)
                    componentProcessor.initializeComponent(it)
            }
        componentFactory().getComponents<BeforeContextRefresh>().forEach(BeforeContextRefresh::beforeContextRefresh)
        componentFactory().getComponents<PostContextRefresh>().forEach(PostContextRefresh::postContextRefresh)
        componentFactory.getAllComponents().map(componentProcessor::autowireComponent)
            .forEach { component ->
                componentFactory()
                    .getComponents<ComponentPostProcessor>()
                    .forEach { component.setInstance(it.processComponent(component, component.cast())) }
            }
        componentFactory().getComponents<AfterContextRefreshed>().forEach(AfterContextRefreshed::afterContextRefreshed)
    }

    /**
     * 扫描组件类
     */
    protected open fun scan() {
        componentProcessor.scanComponents(classLoaders)
    }

    /**
     * 启动上下文
     * @see run
     */
    open fun launch(vararg args: String) {
        mutableListOf<ComponentDefinition>().also(::appendComponents)
            .forEach(componentFactory()::defineComponent)
        scan()
        refresh()
    }

    /**
     * 运行上下文
     */
    open fun run(vararg args: String) {
        if (!componentFactory().hasComponent<ArgumentsProperty>())
            componentFactory().defineComponent(ComponentDefinition(ArgumentsProperty(*args), loaded = true))
        if (!componentFactory().hasComponent<VersionProperty>())
            componentFactory().defineComponent(
                ComponentDefinition(
                    VersionProperty(AuxVersion.current()),
                    loaded = true
                )
            )
        launch(*args)
    }

    /**
     * 在上下文挂后执行
     * @see close
     */
    fun dispose() {
        componentFactory().dispose()
    }

    /**
     * 关闭上下文
     */
    fun close() {
        componentFactory().getComponents<DisposableComponent>().forEach(DisposableComponent::dispose)
        dispose()
    }

}
