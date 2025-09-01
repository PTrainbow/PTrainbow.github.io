/**
 * AI 任务处理类
 * 使用 AITaskDataManager 实现与 RecyclerView 的解耦
 */
class AITaskProcedure(
    private val dataReorderListener: DataReorderListener? = null
) {
    
    private suspend fun startAITaskProcedure(): List<String> {
        return suspendCancellableCoroutine {
            val inferenceCallback = object : InferenceCallback<OrtSession.Result>() {
                override fun onSuccessResult(modelInfo: ModelInfo?, featureKey: String?, result: OrtSession.Result) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        try {
                            val outputTensor = result["output"]?.get()?.value
                            if (outputTensor != null && outputTensor is Array<*>) {
                                val size = outputTensor.size
                                if (outputTensor.isNotEmpty()) {
                                    val rawContentIds = FloatArray(size)
                                    for (i in 0..<size) {
                                        val array = outputTensor[i] as FloatArray
                                        rawContentIds[i] = array[0]
                                    }
                                    val reorderedContentIds = rawContentIds.map {
                                        AIDBManager.instance.resumeBigInt(it)?.toLong().toString()
                                    }
                                    UBTLogUtil.logTrace("o_platform_video_ai_procedure_success", mapOf("success" to true))
                                    UBTLogUtil.logDevTrace("vg_ai_log_dev_trace", mapOf("success" to true, "result" to reorderedContentIds))
                                    it.resumeWithSafely(Result.success(reorderedContentIds))
                                }
                            }
                        } catch (e: Exception) {
                        } finally {
                        }
                    }
                }

                override fun onError(inferenceError: AIEnum.Error, aiTaskInfo: AITaskInfo): Boolean {
                    it.resumeWithSafely(Result.success(arrayListOf()))
                    UBTLogUtil.logDevTrace("vg_ai_log_dev_trace", mapOf("success" to false, "reason" to inferenceError.toString()))
                    return true
                }

                override fun createHolder(): ModelResult<OrtSession.Result> {
                    return object : ModelResult<OrtSession.Result>() {
                        override fun getType(): Class<OrtSession.Result> {
                            return OrtSession.Result::class.java
                        }
                    }
                }
            }

            val featureProvider = object : FeatureProvider<FloatArray> {
                override fun defaultItemValue(): FloatArray {
                    return floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
                }

                override fun reBuildSQL(sql: String, placeHolder: List<String>): String {
                    // 使用 taskKey 从数据管理器获取数据
                    val taskKey = dataReorderListener?.getAITaskId() ?: ""
                    val replacement = AITaskDataManager.getInstance().getUnShownContentIdsForSQL(taskKey)
                    UBTLogUtil.logDevTrace("vg_ai_log_dev_trace", mapOf("reBuildSQL" to replacement))
                    return sql.replace(placeHolder[0], replacement)
                }

                override fun getTensorClass(): Class<Array<FloatArray>> {
                    return Array<FloatArray>::class.java
                }
            }

            val immersiveTaskKey = dataReorderListener?.getAITaskId() ?: ""
            val transform = object : CommonTransform<FloatArray>() {
                override fun providerCallback(): InferenceCallback<out Any> {
                    return inferenceCallback
                }

                override fun providerFeatureProvider(): FeatureProvider<FloatArray> {
                    return featureProvider
                }
            }

            val taskProcedure = TaskProcedure(
                immersiveTaskKey, transform
            )
            taskProcedure.procedure()
        }
    }
    
    /**
     * 启动 AI 任务
     */
    suspend fun startTask(): List<String> {
        return startAITaskProcedure()
    }
}



