package ctrip.android.ai.transform

/**
 * Transform接口
 */
interface Transform {
    fun rebuildSql()
}

/**
 * CustomView示例
 */
class CustomView {
    private val customData = listOf<String>()
    
    init {
        // 注册上下文提供者
        TransformRegistry.registerContextProvider(
            CustomView::class,
            object : ContextProvider<CustomView> {
                override fun getContext(): CustomView = this@CustomView
            }
        )
        
        // 注册Transform工厂
        TransformRegistry.registerTransform("custom_view_transform") {
            CustomViewTransform()
        }
    }
    
    /**
     * CustomView的Transform实现
     */
    inner class CustomViewTransform : Transform {
        override fun rebuildSql() {
            // 通过注册表获取上下文
            val context = TransformRegistry.getContext(CustomView::class)
            context?.let { view ->
                // 现在可以访问CustomView的成员变量
                view.customData.resolve()
            }
        }
    }
    
    /**
     * 模拟数据解析方法
     */
    private fun List<String>.resolve() {
        println("Resolving data: $this")
    }
}

/**
 * 另一个示例：VideoGoodsView的Transform实现
 */
class VideoGoodsView {
    private val videoData = mutableListOf<String>()
    private var currentPosition = 0
    
    init {
        // 注册上下文提供者
        TransformRegistry.registerContextProvider(
            VideoGoodsView::class,
            object : ContextProvider<VideoGoodsView> {
                override fun getContext(): VideoGoodsView = this@VideoGoodsView
            }
        )
        
        // 注册Transform工厂
        TransformRegistry.registerTransform("video_goods_transform") {
            VideoGoodsTransform()
        }
    }
    
    /**
     * VideoGoodsView的Transform实现
     */
    inner class VideoGoodsTransform : Transform {
        override fun rebuildSql() {
            val context = TransformRegistry.getContext(VideoGoodsView::class)
            context?.let { view ->
                // 访问VideoGoodsView的成员变量
                val sql = "SELECT * FROM videos WHERE id IN (${view.videoData.joinToString(",")}) AND position > ${view.currentPosition}"
                println("Generated SQL: $sql")
            }
        }
    }
    
    fun addVideoData(data: String) {
        videoData.add(data)
    }
    
    fun setPosition(position: Int) {
        currentPosition = position
    }
}
