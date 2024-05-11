package pixel.auxframework.context

import arrow.core.getOrElse
import arrow.core.toOption
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pixel.auxframework.component.annotation.Component
import pixel.auxframework.component.annotation.OnlyIn
import pixel.auxframework.component.factory.*
import pixel.auxframework.context.builtin.AfterContextRefreshed
import pixel.auxframework.context.builtin.ArgumentsProperty
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
    protected open fun appendComponents(list: MutableList<Any>) {
        list.addAll(
            listOf(
                this, componentFactory(), componentProcessor
            )
        )
    }

    /**
     * 刷新
     */
    open fun refresh() {
        componentFactory().getAllComponents()
            .map {
                componentProcessor.initializeComponent(it)
            }
        componentFactory.getAllComponents().map(componentProcessor::autowireComponent)
            .forEach { component ->
                componentFactory()
                    .getComponents<ComponentPostProcessor>()
                    .forEach { it.processComponent(component) }
            }
        componentFactory().getComponents<AfterContextRefreshed>().forEach(AfterContextRefreshed::afterContextRefreshed)
    }

    /**
     * 扫描组件类
     */
    protected open fun scan() {
        for (classLoader in classLoaders) {
            val reflections = Reflections(
                ConfigurationBuilder()
                    .forPackages(*classLoader.definedPackages.map { it.name }.toTypedArray())
                    .addClassLoaders(classLoader)
                    .setScanners(Scanners.TypesAnnotated)
            )
            val types = reflections.getTypesAnnotatedWith(Component::class.java).filter {
                !(it.isAnnotation || (!it.isInterface && it.kotlin.isAbstract))
            }.filter {
                it.kotlin.findAnnotation<OnlyIn>().toOption().map { onlyIn ->
                    var accept = true
                    if (onlyIn.contextName != "<null>") accept = accept && this@AuxContext.name == onlyIn.contextName
                    accept = accept && onlyIn.contextType.all { type -> type.isInstance(this@AuxContext) }
                    accept
                }.getOrElse { true }
            }
            types.forEach { type -> componentFactory().registerComponentDefinition(ComponentDefinition(type.kotlin)) }
        }
    }

    /**
     * 启动上下文
     * @see run
     */
    open fun launch() {
        mutableListOf<Any>().also(::appendComponents)
            .map { ComponentDefinition(it, loaded = true) }
            .forEach(componentFactory()::registerComponentDefinition)
        scan()
        refresh()
    }

    /**
     * 运行上下文
     */
    open fun run(vararg args: String) {
        componentFactory().registerComponentDefinition(ComponentDefinition(ArgumentsProperty(*args), loaded = true))
        launch()
    }

    /**
     * 在上下文挂后执行
     * @see close
     */
    fun dispose() {}

    /**
     * 关闭上下文
     */
    fun close() {
        componentFactory().getComponents<DisposableComponent>().forEach(DisposableComponent::dispose)
        dispose()
    }

}
