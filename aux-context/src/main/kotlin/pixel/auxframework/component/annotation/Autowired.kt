package pixel.auxframework.component.annotation

/**
 * 当挂载到 [AnnotationTarget.CONSTRUCTOR] 时，将该constructor设置为默认构造器
 *
 * 当挂载到 [AnnotationTarget.PROPERTY] 时，自动装填
 *
 * @see Qualifier
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class Autowired
