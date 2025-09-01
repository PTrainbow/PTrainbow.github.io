/**
 * 使用示例
 * 展示如何在 RecyclerView 中注册数据，以及如何直接通过 taskKey 调用 AI 任务
 */
class UsageExample {
    
    // 在 RecyclerView 的 Adapter 或相关类中
    class VideoRecyclerViewAdapter {
        
        private var videoGoodsViewListData: VideoGoodsViewListData? = null
        private var currentPosition: Int = 0
        private val taskKey: String = "video_task_001" // 你的实际 taskKey
        
        /**
         * 设置数据时注册到数据管理器
         */
        fun setData(data: VideoGoodsViewListData) {
            this.videoGoodsViewListData = data
            // 注册数据到管理器
            AITaskDataManager.getInstance().registerTaskData(
                taskKey = taskKey,
                data = data,
                currentPosition = currentPosition
            )
        }
        
        /**
         * 位置变化时更新数据管理器
         */
        fun onPositionChanged(newPosition: Int) {
            currentPosition = newPosition
            // 更新位置信息
            AITaskDataManager.getInstance().updateTaskPosition(taskKey, newPosition)
        }
        
        /**
         * 清理数据
         */
        fun clearData() {
            AITaskDataManager.getInstance().clearTaskData(taskKey)
        }
    }
    
    // 在需要调用 AI 任务的地方（不依赖 RecyclerView）
    class AITaskCaller {
        
        /**
         * 直接通过 taskKey 调用 AI 任务
         */
        suspend fun callAITaskByTaskKey(taskKey: String): List<String> {
            // 创建任务处理器，传入 taskKey 获取器
            val taskProcedure = AITaskProcedure(object : DataReorderListener {
                override fun getAITaskId(): String = taskKey
            })
            
            // 启动任务
            return taskProcedure.startTask()
        }
        
        /**
         * 使用示例：在任意地方调用 AI 任务
         */
        suspend fun exampleUsage() {
            val taskKey = "video_task_001"
            
            // 直接调用，不需要依赖 RecyclerView
            val result = callAITaskByTaskKey(taskKey)
            
            // 处理结果
            println("AI 任务结果: $result")
        }
    }
    
    // 在 ViewModel 或其他业务逻辑类中使用
    class VideoViewModel {
        
        private val taskKey = "video_task_001"
        private val aiTaskCaller = AITaskCaller()
        
        /**
         * 在 ViewModel 中调用 AI 任务
         */
        suspend fun performAITask(): List<String> {
            return aiTaskCaller.callAITaskByTaskKey(taskKey)
        }
        
        /**
         * 更新数据（从网络或其他来源）
         */
        fun updateVideoData(data: VideoGoodsViewListData, currentPosition: Int) {
            // 注册数据到管理器
            AITaskDataManager.getInstance().registerTaskData(
                taskKey = taskKey,
                data = data,
                currentPosition = currentPosition
            )
        }
    }
}

/**
 * 数据重排序监听器接口
 */
interface DataReorderListener {
    fun getAITaskId(): String
}
