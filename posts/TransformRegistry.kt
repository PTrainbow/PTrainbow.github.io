package ctrip.android.ai.transform

import kotlin.reflect.KClass

/**
 * Transform注册表
 * 用于管理所有的Transform实现，支持依赖注入和上下文传递
 */
object TransformRegistry {
    
    // 存储Transform工厂的映射
    private val transformFactories = mutableMapOf<String, TransformFactory<*>>()
    
    // 存储上下文提供者的映射
    private val contextProviders = mutableMapOf<KClass<*>, ContextProvider<*>>()
    
    /**
     * 注册Transform工厂
     */
    fun <T : Transform> registerTransform(key: String, factory: TransformFactory<T>) {
        transformFactories[key] = factory
    }
    
    /**
     * 注册上下文提供者
     */
    fun <T : Any> registerContextProvider(contextClass: KClass<T>, provider: ContextProvider<T>) {
        contextProviders[contextClass] = provider
    }
    
    /**
     * 获取Transform实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Transform> getTransform(key: String): T? {
        val factory = transformFactories[key] as? TransformFactory<T>
        return factory?.create()
    }
    
    /**
     * 获取上下文
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getContext(contextClass: KClass<T>): T? {
        val provider = contextProviders[contextClass] as? ContextProvider<T>
        return provider?.getContext()
    }
    
    /**
     * 清除所有注册
     */
    fun clear() {
        transformFactories.clear()
        contextProviders.clear()
    }
}

/**
 * Transform工厂接口
 */
interface TransformFactory<T : Transform> {
    fun create(): T
}

/**
 * 上下文提供者接口
 */
interface ContextProvider<T : Any> {
    fun getContext(): T
}

/**
 * 简化的taskProduceDo方法
 */
fun taskProduceDo(key: String) {
    val transform = TransformRegistry.getTransform<Transform>(key)
    transform?.rebuildSql()
}
