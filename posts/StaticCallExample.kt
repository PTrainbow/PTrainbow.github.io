/**
 * 静态调用示例
 * 展示如何脱离实例直接静态执行 getUnShownContentIdsForSQL 方法
 */
class StaticCallExample {
    
    /**
     * 示例1：直接静态调用（推荐方式）
     * 不需要注册到管理器，直接传入参数即可
     */
    fun example1_DirectStaticCall() {
        // 假设你有这些数据
        val videoData = VideoGoodsViewListData()
        val currentPosition = 5
        
        // 直接静态调用，不需要实例
        val sqlResult = AITaskDataManager.getUnShownContentIdsForSQL(videoData, currentPosition)
        println("SQL结果: $sqlResult")
        
        // 也可以获取原始列表
        val contentIds = AITaskDataManager.getUnShownContentIds(videoData, currentPosition)
        println("内容ID列表: $contentIds")
    }
    
    /**
     * 示例2：在任意地方调用，不依赖RecyclerView
     */
    fun example2_AnywhereCall() {
        // 在任意业务逻辑中
        val data = getVideoDataFromSomewhere()
        val position = getCurrentPositionFromSomewhere()
        
        // 直接调用，完全解耦
        val sqlClause = AITaskDataManager.getUnShownContentIdsForSQL(data, position)
        
        // 使用SQL结果
        val sql = "SELECT * FROM content WHERE id IN ($sqlClause)"
        println("构建的SQL: $sql")
    }
    
    /**
     * 示例3：在AI任务中直接使用
     */
    fun example3_InAITask() {
        val featureProvider = object : FeatureProvider<FloatArray> {
            override fun reBuildSQL(sql: String, placeHolder: List<String>): String {
                // 直接静态调用，不需要依赖任何实例
                val data = getCurrentVideoData()
                val position = getCurrentPosition()
                val replacement = AITaskDataManager.getUnShownContentIdsForSQL(data, position)
                
                return sql.replace(placeHolder[0], replacement)
            }
            
            // ... 其他方法
        }
    }
    
    /**
     * 示例4：在ViewModel中使用
     */
    fun example4_InViewModel() {
        class VideoViewModel {
            private var videoData: VideoGoodsViewListData? = null
            private var currentPosition: Int = 0
            
            fun updatePosition(newPosition: Int) {
                currentPosition = newPosition
            }
            
            fun getUnShownContentIds(): String {
                // 直接静态调用，不需要任何实例
                return AITaskDataManager.getUnShownContentIdsForSQL(videoData, currentPosition)
            }
            
            fun performAITask() {
                val contentIds = getUnShownContentIds()
                // 使用contentIds进行AI任务
            }
        }
    }
    
    /**
     * 示例5：在工具类中使用
     */
    object VideoUtils {
        fun buildContentQuery(data: VideoGoodsViewListData?, position: Int): String {
            val contentIds = AITaskDataManager.getUnShownContentIdsForSQL(data, position)
            return "SELECT * FROM video_content WHERE content_id IN ($contentIds)"
        }
        
        fun getUnShownCount(data: VideoGoodsViewListData?, position: Int): Int {
            val contentIds = AITaskDataManager.getUnShownContentIds(data, position)
            return contentIds.size
        }
    }
    
    /**
     * 示例6：在协程中使用
     */
    suspend fun example6_InCoroutine() {
        val data = getVideoDataAsync()
        val position = getPositionAsync()
        
        // 在协程中直接调用
        val contentIds = AITaskDataManager.getUnShownContentIds(data, position)
        
        // 处理结果
        contentIds.forEach { contentId ->
            // 处理每个未显示的内容
        }
    }
    
    /**
     * 示例7：在测试中使用
     */
    fun example7_InTest() {
        // 测试数据
        val testData = createTestVideoData()
        val testPosition = 3
        
        // 直接测试静态方法
        val result = AITaskDataManager.getUnShownContentIdsForSQL(testData, testPosition)
        
        // 验证结果
        assert(result.isNotEmpty()) { "结果不应该为空" }
        assert(result.startsWith("'")) { "结果应该以单引号开始" }
        assert(result.endsWith("'")) { "结果应该以单引号结束" }
    }
    
    // 辅助方法（模拟）
    private fun getVideoDataFromSomewhere(): VideoGoodsViewListData? = null
    private fun getCurrentPositionFromSomewhere(): Int = 0
    private fun getCurrentVideoData(): VideoGoodsViewListData? = null
    private fun getCurrentPosition(): Int = 0
    private suspend fun getVideoDataAsync(): VideoGoodsViewListData? = null
    private suspend fun getPositionAsync(): Int = 0
    private fun createTestVideoData(): VideoGoodsViewListData = VideoGoodsViewListData()
}

/**
 * 使用总结：
 * 
 * 1. 直接静态调用（推荐）：
 *    AITaskDataManager.getUnShownContentIdsForSQL(data, position)
 * 
 * 2. 获取原始列表：
 *    AITaskDataManager.getUnShownContentIds(data, position)
 * 
 * 3. 转换为SQL格式：
 *    AITaskDataManager.convertToSQLInClause(contentIds)
 * 
 * 优势：
 * - 完全解耦，不依赖任何实例
 * - 可以在任意地方调用
 * - 线程安全
 * - 易于测试
 * - 代码简洁
 */
