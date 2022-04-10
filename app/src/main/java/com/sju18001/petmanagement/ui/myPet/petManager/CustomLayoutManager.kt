package com.sju18001.petmanagement.ui.myPet.petManager

import android.content.Context
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import java.lang.Float.min
import kotlin.math.max

class CustomLayoutManager constructor(
    context: Context,
    snapHelper: SnapHelper,
    mode: Mode,
    minScale: Float,
    yOffset: Float
): LinearLayoutManager(context) {
    private val snapHelper = snapHelper
    
    /** 애니메이션의 모드 */
    private val mode = mode

    /** 아이템이 작아지는 한도(예를 들어, 0.5f이면 최대 절반만큼 작아집니다.) */
    private val minScale = minScale

    /** y값이 변하는 정도 */
    private val yOffset = yOffset

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)

        if(state != null){
            layoutItems(state)

            if(mode == Mode.SCALE_MODE){
                for(i in 0 until itemCount) findViewByPosition(i)?.pivotY = (height/2).toFloat()
            }
        }
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        if(state != null){
            layoutItems(state)
        }

        return super.scrollHorizontallyBy(dx, recycler, state)
    }

    private fun layoutItems(state: RecyclerView.State) {
        if(state.isPreLayout) return

        val centerPos = getCenterPosition() ?: return
        for(pos in centerPos-1..centerPos+1){
            val item = findViewByPosition(pos) ?: continue
            if(item.tag == "DRAGGED") continue

            layoutItem(item)
        }
    }

    private fun getCenterPosition(): Int? {
        val centerView = snapHelper.findSnapView(this)
        return centerView?.let { getPosition(it) }
    }

    private fun layoutItem(item: View) {
        val itemCenterX = item.x + item.width/2

        when(mode){
            Mode.Y_MODE -> {
                item.y = computeY(itemCenterX)
            }
            Mode.SCALE_MODE -> {
                val scale = computeScale(itemCenterX)
                item.scaleX = scale
                item.scaleY = scale
                item.pivotX = computePivot(itemCenterX, item.width)
            }
        }
    }

    private fun computeY(itemCenterX: Float): Float {
        return if(itemCenterX < width/2) {
            min(1f, itemCenterX / (width/2)) * yOffset - yOffset
        }else{
            min(1f, (width - itemCenterX) / (width/2)) * yOffset - yOffset
        }
    }

    private fun computeScale(itemCenterX: Float): Float {
        return if(itemCenterX < width/2) {
            min(1f, itemCenterX / (width/2)) * (1 - minScale) + minScale
        }else{
            min(1f, (width - itemCenterX) / (width/2)) * (1 - minScale) + minScale
        }
    }

    private fun computePivot(itemCenterX: Float, itemWidth: Int): Float {
        return if(itemCenterX < width/2) itemWidth.toFloat() else 0f
    }


    enum class Mode {
        SCALE_MODE, Y_MODE
    }

    class Builder constructor(context: Context, snapHelper: SnapHelper){
        private val context = context
        private val snapHelper = snapHelper
        private var mode = Mode.Y_MODE
        private var minScale = 0.85f
        private var yOffset = -75f

        fun setMode(mode: Mode): Builder {
            this.mode = mode
            return this
        }

        fun setMinScale(minScale: Float): Builder {
            this.minScale = minScale
            return this
        }

        fun setYOffset(yOffset: Float): Builder {
            this.yOffset = yOffset
            return this
        }

        fun build(): CustomLayoutManager {
            return CustomLayoutManager(context, snapHelper, mode, minScale, yOffset)
        }
    }
}