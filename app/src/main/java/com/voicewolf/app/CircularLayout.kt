package com.voicewolf.app

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom ViewGroup that arranges child views in a circular pattern
 */
class CircularLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private val childSize: Int
        get() = if (childCount > 0 && childCount <= 13) {
            when (childCount) {
                in 1..4 -> 80
                in 5..8 -> 70
                else -> 60
            }
        } else 60

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = minOf(width, height)

        setMeasuredDimension(size, size)

        val childMeasureSpec = MeasureSpec.makeMeasureSpec(childSize, MeasureSpec.EXACTLY)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(childMeasureSpec, childMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val centerX = width / 2
        val centerY = height / 2
        val radius = (minOf(width, height) / 2) - childSize / 2 - 20

        val childCount = this.childCount
        val angleStep = 360.0 / maxOf(childCount, 1)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            // Start from top (-90 degrees) and go clockwise
            val angle = Math.toRadians(i * angleStep - 90)
            
            val childLeft = (centerX + radius * cos(angle) - childSize / 2).toInt()
            val childTop = (centerY + radius * sin(angle) - childSize / 2).toInt()
            val childRight = childLeft + childSize
            val childBottom = childTop + childSize

            child.layout(childLeft, childTop, childRight, childBottom)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }


    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(childSize, childSize)
    }


    class LayoutParams : MarginLayoutParams {
        constructor(width: Int, height: Int) : super(width, height)
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
        constructor(source: LayoutParams?) : super(source)
    }
}
