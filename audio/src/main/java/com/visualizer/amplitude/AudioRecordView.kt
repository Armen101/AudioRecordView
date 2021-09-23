package com.visualizer.amplitude

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.lang.Exception
import java.util.*


class AudioRecordView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class AlignTo(var value: Int) {
        CENTER(1),
        BOTTOM(2)
    }

    enum class Direction(var value: Int) {
        RightToLeft(1),
        LeftToRight(2)
    }

    private val maxReportableAmp = 22760f //effective size,  max fft = 32760
    private val uninitialized = 0f
    var chunkAlignTo = AlignTo.CENTER
    var direction = Direction.LeftToRight

    private val chunkPaint = Paint()
    private var lastUpdateTime = 0L

    private var usageWidth = 0f
    private var chunkHeights = ArrayList<Float>()
    private var chunkWidths = ArrayList<Float>()
    private var topBottomPadding = 6.dp()

    var chunkSoftTransition = false
    var chunkColor = Color.RED
        set(value) {
            chunkPaint.color = value
            field = value
        }
    var chunkWidth = 2.dp()
        set(value) {
            chunkPaint.strokeWidth = value
            field = value
        }
    var chunkSpace = 1.dp()
    var chunkMaxHeight = uninitialized
    var chunkMinHeight = 3.dp()  // recommended size > 10 dp
    var chunkRoundedCorners = false
        set(value) {
            if (value) {
                chunkPaint.strokeCap = Paint.Cap.ROUND
            } else {
                chunkPaint.strokeCap = Paint.Cap.BUTT
            }
            field = value
        }

    init {
        attrs?.let { init(it) } ?: run { init() }
    }

    fun recreate() {
        usageWidth = 0f
        chunkWidths.clear()
        chunkHeights.clear()
        invalidate()
    }

    /**
     * Call this function when you need to add a new chunk
     * @param fft Used to draw the height of each chunk.
     */
    fun update(fft: Int) {
        if (height == 0) {
            Log.w(LOG_TAG, "You must call the update fun when the view is displayed")
            return
        }
        try {
            handleNewFFT(fft)
            invalidate() // call to the onDraw function
            lastUpdateTime = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(AudioRecordView::class.simpleName, e.message ?: e.javaClass.simpleName)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawChunks(canvas)
    }

    private fun init() {
        chunkPaint.strokeWidth = chunkWidth
        chunkPaint.color = chunkColor
    }

    private fun init(attrs: AttributeSet) {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.AudioRecordView,
            0, 0
        ).apply {
            try {
                chunkSpace = getDimension(R.styleable.AudioRecordView_chunkSpace, chunkSpace)
                chunkMaxHeight =
                    getDimension(R.styleable.AudioRecordView_chunkMaxHeight, chunkMaxHeight)
                chunkMinHeight =
                    getDimension(R.styleable.AudioRecordView_chunkMinHeight, chunkMinHeight)
                chunkRoundedCorners =
                    getBoolean(R.styleable.AudioRecordView_chunkRoundedCorners, chunkRoundedCorners)
                chunkWidth = getDimension(R.styleable.AudioRecordView_chunkWidth, chunkWidth)
                chunkColor = getColor(R.styleable.AudioRecordView_chunkColor, chunkColor)
                chunkAlignTo =
                    when (getInt(R.styleable.AudioRecordView_chunkAlignTo, chunkAlignTo.value)) {
                        AlignTo.BOTTOM.value -> AlignTo.BOTTOM
                        else -> AlignTo.CENTER
                    }
                direction =
                    when (getInt(R.styleable.AudioRecordView_direction, direction.value)) {
                        Direction.RightToLeft.value -> Direction.RightToLeft
                        else -> Direction.LeftToRight
                    }

                chunkSoftTransition =
                    getBoolean(R.styleable.AudioRecordView_chunkSoftTransition, chunkSoftTransition)

                setWillNotDraw(false)
                chunkPaint.isAntiAlias = true
            } finally {
                recycle()
            }
        }
    }

    private fun handleNewFFT(fft: Int) {
        if (fft == 0) {
            return
        }

        val chunkHorizontalScale = chunkWidth + chunkSpace
        val maxChunkCount = width / chunkHorizontalScale
        if (chunkHeights.isNotEmpty() && chunkHeights.size >= maxChunkCount) {
            chunkHeights.removeAt(0)
        } else {
            usageWidth += chunkHorizontalScale
            chunkWidths.add(usageWidth)
        }

        if (chunkMaxHeight == uninitialized) {
            chunkMaxHeight = height - (topBottomPadding * 2)
        } else if (chunkMaxHeight > height - (topBottomPadding * 2)) {
            chunkMaxHeight = height - (topBottomPadding * 2)
        }

        val verticalDrawScale = chunkMaxHeight - chunkMinHeight
        if (verticalDrawScale == 0f) {
            return
        }

        val point = maxReportableAmp / verticalDrawScale
        if (point == 0f) {
            return
        }

        var fftPoint = fft / point

        if (chunkSoftTransition && chunkHeights.isNotEmpty()) {
            val updateTimeInterval = System.currentTimeMillis() - lastUpdateTime
            val scaleFactor = calculateScaleFactor(updateTimeInterval)
            val prevFftWithoutAdditionalSize = chunkHeights.last() - chunkMinHeight
            fftPoint = fftPoint.softTransition(prevFftWithoutAdditionalSize, 2.2f, scaleFactor)
        }

        fftPoint += chunkMinHeight

        if (fftPoint > chunkMaxHeight) {
            fftPoint = chunkMaxHeight
        } else if (fftPoint < chunkMinHeight) {
            fftPoint = chunkMinHeight
        }

        chunkHeights.add(chunkHeights.size, fftPoint)
    }

    private fun calculateScaleFactor(updateTimeInterval: Long): Float {
        return when (updateTimeInterval) {
            in 0..50 -> 1.6f
            in 50..100 -> 2.2f
            in 100..150 -> 2.8f
            in 150..200 -> 4.2f
            in 200..500 -> 4.8f
            else -> 5.4f
        }
    }

    private fun drawChunks(canvas: Canvas) {
        when (chunkAlignTo) {
            AlignTo.BOTTOM -> drawAlignBottom(canvas)
            else -> drawAlignCenter(canvas)
        }
    }

    private fun drawAlignCenter(canvas: Canvas) {
        val verticalCenter = height / 2
        for (i in 0 until chunkHeights.size - 1) {
            val chunkX = getChunkX(i)
            val startY = verticalCenter - chunkHeights[i] / 2
            val stopY = verticalCenter + chunkHeights[i] / 2

            canvas.drawLine(chunkX, startY, chunkX, stopY, chunkPaint)
        }
    }

    private fun drawAlignBottom(canvas: Canvas) {
        for (i in 0 until chunkHeights.size - 1) {
            val chunkX = getChunkX(i)
            val startY = height.toFloat() - topBottomPadding
            val stopY = startY - chunkHeights[i]

            canvas.drawLine(chunkX, startY, chunkX, stopY, chunkPaint)
        }
    }

    private fun getChunkX(index: Int) = if (direction == Direction.RightToLeft) {
        width - chunkWidths[index]
    } else {
        chunkWidths[index]
    }

    companion object {

        private val LOG_TAG = AudioRecordView::class.java.simpleName

    }
}