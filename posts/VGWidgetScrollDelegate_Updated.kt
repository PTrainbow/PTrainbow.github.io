package ctrip.android.publiccontent.widget.videogoods.delegate

import ai.onnxruntime.OrtSession
import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import ctrip.android.ai.datacollect.AIDBManager
import ctrip.android.ai.engine.AIEnum
import ctrip.android.ai.engine.InferenceCallback
import ctrip.android.ai.feature.CommonTransform
import ctrip.android.ai.feature.FeatureProvider
import ctrip.android.ai.model.AITaskInfo
import ctrip.android.ai.model.ModelInfo
import ctrip.android.ai.model.ModelResult
import ctrip.android.ai.task.TaskProcedure
import ctrip.android.publiccontent.widget.videogoods.adapter.NoMoreDataViewHolder
import ctrip.android.publiccontent.widget.videogoods.bean.VideoGoodsViewData
import ctrip.android.publiccontent.widget.videogoods.bean.VideoGoodsViewListData
import ctrip.android.publiccontent.widget.videogoods.holder.VGBaseItemLifecycle
import ctrip.android.publiccontent.widget.videogoods.manager.CTVideoGoodsMobileConfigManager.getOpenAITask
import ctrip.android.publiccontent.widget.videogoods.widget.CTVideoGoodsWidget
import ctrip.business.live.CTLiveRoomChild
import kotlin.math.max
import ctrip.foundation.util.UBTLogUtil
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class VGWidgetScrollDelegate(val snapHelper: PagerSnapHelper?,
                             private val scrollDirection: String,
                             private val mVideoGoodsViewListData: VideoGoodsViewListData?,
                             private val mLiveItemLifecycleDelegate: VGLiveItemLifecycleDelegate,
                             val positionChangeListener: CTVideoGoodsWidget.IPositionChangeListener?) {

    interface IDataReorderListener {
        fun getAITaskId(): String?
        fun onDataReordered(startPosition: Int, itemCount: Int)
    }

    object TransformHolder {
        val instance
    }

    private var dataReorderListener: IDataReorderListener? = null

    fun setDataReorderListener(listener: IDataReorderListener?) {
        dataReorderListener = listener
    }

    private var mTotalShowCurrentPosition = 0
    private var mCurrentPosition = 0
    private var mIsManualScrolling = false
    private var mItemStartTime = 0L

    private var maxScrollPosition = 0
    private var maxSuccessScrollPosition = 0

    init {
        mItemStartTime = System.currentTimeMillis()
    }

    // ... 其他方法保持不变 ...

    /**
     * 修改后的方法：使用静态调用
     * 现在可以直接静态调用，不需要依赖实例
     */
    fun getUnShownContentIdsForSQL(): String {
        // 使用静态方法，直接传入参数
        return AITaskDataManager.getUnShownContentIdsForSQL(mVideoGoodsViewListData, mTotalShowCurrentPosition)
    }

    /**
     * 新增方法：获取未显示的内容ID列表（不包含SQL格式）
     */
    fun getUnShownContentIds(): List<String> {
        return AITaskDataManager.getUnShownContentIds(mVideoGoodsViewListData, mTotalShowCurrentPosition)
    }

    /**
     * 新增方法：获取指定位置的未显示内容ID列表
     */
    fun getUnShownContentIdsForPosition(position: Int): String {
        return AITaskDataManager.getUnShownContentIdsForSQL(mVideoGoodsViewListData, position)
    }

    /**
     * 新增方法：获取指定位置的未显示内容ID列表（不包含SQL格式）
     */
    fun getUnShownContentIdsForPositionAsList(position: Int): List<String> {
        return AITaskDataManager.getUnShownContentIds(mVideoGoodsViewListData, position)
    }

    // ... 其他方法保持不变 ...

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
                    // 使用静态方法，直接传入当前数据
                    val replacement = AITaskDataManager.getUnShownContentIdsForSQL(mVideoGoodsViewListData, mTotalShowCurrentPosition)
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

    // ... 其他方法保持不变 ...
}

/**
 * 使用示例：
 * 
 * // 原来的调用方式（需要实例）
 * val delegate = VGWidgetScrollDelegate(...)
 * val sqlResult = delegate.getUnShownContentIdsForSQL()
 * 
 * // 新的静态调用方式（不需要实例）
 * val sqlResult = AITaskDataManager.getUnShownContentIdsForSQL(data, position)
 * 
 * // 在任意地方都可以调用
 * class SomeOtherClass {
 *     fun someMethod() {
 *         val data = getVideoData()
 *         val position = getCurrentPosition()
 *         val sqlResult = AITaskDataManager.getUnShownContentIdsForSQL(data, position)
 *     }
 * }
 */
