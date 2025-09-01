/**
 * AI 任务数据管理器
 * 用于管理 AI 任务相关的数据，实现与 RecyclerView 的解耦
 */
class AITaskDataManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: AITaskDataManager? = null
        
        fun getInstance(): AITaskDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AITaskDataManager().also { INSTANCE = it }
            }
        }
        
        /**
         * 静态方法：直接通过参数获取未显示内容ID列表
         * @param data 视频商品数据列表
         * @param currentPosition 当前显示位置
         * @return SQL IN子句格式的字符串
         */
        fun getUnShownContentIdsForSQL(data: VideoGoodsViewListData?, currentPosition: Int): String {
            if (data?.videoGoodsViewDataList?.isEmpty() == true) {
                return ""
            }
            
            val unshownContentIds = mutableListOf<String>()
            val startIndex = currentPosition + 1
            val dataList = data?.videoGoodsViewDataList ?: return ""
            
            for (i in startIndex until dataList.size) {
                val videoGoodsViewData = dataList[i]
                if (!videoGoodsViewData.hasShow && 
                    !videoGoodsViewData.contentId.isNullOrEmpty() && 
                    !isLiveContent(videoGoodsViewData)) {
                    unshownContentIds.add(videoGoodsViewData.contentId)
                }
            }
            
            if (unshownContentIds.isEmpty()) {
                return ""
            }
            
            // 转换成SQL IN子句格式: "'id1','id2','id3'"
            return unshownContentIds.joinToString(
                prefix = "'",
                separator = "','",
                postfix = "'"
            )
        }
        
        /**
         * 静态方法：判断是否为直播内容
         * @param videoGoodsViewData 视频商品数据
         * @return 是否为直播内容
         */
        private fun isLiveContent(videoGoodsViewData: VideoGoodsViewData): Boolean {
            return "LIVE".equals(videoGoodsViewData.mediaType, ignoreCase = true)
        }
        
        /**
         * 静态方法：获取未显示的内容ID列表（不包含SQL格式）
         * @param data 视频商品数据列表
         * @param currentPosition 当前显示位置
         * @return 内容ID列表
         */
        fun getUnShownContentIds(data: VideoGoodsViewListData?, currentPosition: Int): List<String> {
            if (data?.videoGoodsViewDataList?.isEmpty() == true) {
                return emptyList()
            }
            
            val unshownContentIds = mutableListOf<String>()
            val startIndex = currentPosition + 1
            val dataList = data?.videoGoodsViewDataList ?: return emptyList()
            
            for (i in startIndex until dataList.size) {
                val videoGoodsViewData = dataList[i]
                if (!videoGoodsViewData.hasShow && 
                    !videoGoodsViewData.contentId.isNullOrEmpty() && 
                    !isLiveContent(videoGoodsViewData)) {
                    unshownContentIds.add(videoGoodsViewData.contentId)
                }
            }
            
            return unshownContentIds
        }
        
        /**
         * 静态方法：将内容ID列表转换为SQL IN子句格式
         * @param contentIds 内容ID列表
         * @return SQL IN子句格式的字符串
         */
        fun convertToSQLInClause(contentIds: List<String>): String {
            if (contentIds.isEmpty()) {
                return ""
            }
            
            return contentIds.joinToString(
                prefix = "'",
                separator = "','",
                postfix = "'"
            )
        }
    }
    
    // 存储每个 taskKey 对应的数据
    private val taskDataMap = mutableMapOf<String, VideoGoodsViewListData>()
    
    // 存储每个 taskKey 对应的当前显示位置
    private val taskPositionMap = mutableMapOf<String, Int>()
    
    /**
     * 注册任务数据
     * @param taskKey 任务标识
     * @param data 视频商品数据
     * @param currentPosition 当前显示位置
     */
    fun registerTaskData(taskKey: String, data: VideoGoodsViewListData, currentPosition: Int) {
        taskDataMap[taskKey] = data
        taskPositionMap[taskKey] = currentPosition
    }
    
    /**
     * 更新任务位置
     * @param taskKey 任务标识
     * @param currentPosition 当前显示位置
     */
    fun updateTaskPosition(taskKey: String, currentPosition: Int) {
        taskPositionMap[taskKey] = currentPosition
    }
    
    /**
     * 获取任务的未显示内容ID列表（用于SQL查询）
     * @param taskKey 任务标识
     * @return SQL IN子句格式的字符串，如 "'id1','id2','id3'"
     */
    fun getUnShownContentIdsForSQL(taskKey: String): String {
        val data = taskDataMap[taskKey] ?: return ""
        val currentPosition = taskPositionMap[taskKey] ?: 0
        
        return getUnShownContentIdsForSQL(data, currentPosition)
    }
    
    /**
     * 获取任务的未显示内容ID列表（不包含SQL格式）
     * @param taskKey 任务标识
     * @return 内容ID列表
     */
    fun getUnShownContentIds(taskKey: String): List<String> {
        val data = taskDataMap[taskKey] ?: return emptyList()
        val currentPosition = taskPositionMap[taskKey] ?: 0
        
        return getUnShownContentIds(data, currentPosition)
    }
    
    /**
     * 清理任务数据
     * @param taskKey 任务标识
     */
    fun clearTaskData(taskKey: String) {
        taskDataMap.remove(taskKey)
        taskPositionMap.remove(taskKey)
    }
    
    /**
     * 清理所有任务数据
     */
    fun clearAllTaskData() {
        taskDataMap.clear()
        taskPositionMap.clear()
    }
    
    /**
     * 获取所有注册的任务Key
     * @return 任务Key列表
     */
    fun getAllTaskKeys(): List<String> {
        return taskDataMap.keys.toList()
    }
    
    /**
     * 检查任务是否存在
     * @param taskKey 任务标识
     * @return 是否存在
     */
    fun hasTask(taskKey: String): Boolean {
        return taskDataMap.containsKey(taskKey)
    }
}
