package pixel.auxframework.component.annotation

/**
 * 根据名称限定组件
 * @see Autowired
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class Qualifier(val name: String)
