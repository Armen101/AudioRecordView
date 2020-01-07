package com.visualizer.amplitude

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.*


class AudioRecordView : View {

    enum class AlignTo(var value: Int){
        CENTER(1),
        BOTTOM(2)
    }

    private val maxReportableAmp = 22760f //effective size,  max fft = 32760
    private val uninitialized = 0f
    var chunkAlignTo = AlignTo.CENTER

    private val chunkPaint = Paint()

    private var usageWidth = 0f
    private var chunkHeights = ArrayList<Float>()
    private var chunkWidths = ArrayList<Float>()
    private var topBottomPadding = 6.dp()

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
            }
            field = value
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    fun recreate() {
        usageWidth = 0f
        chunkWidths.clear()
        chunkHeights.clear()
        invalidate()
    }

    fun update(fft: Int) {
        handleNewFFT(fft)
        invalidate() // call to the onDraw function
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
                chunkMaxHeight = getDimension(R.styleable.AudioRecordView_chunkMaxHeight, chunkMaxHeight)
                chunkMinHeight = getDimension(R.styleable.AudioRecordView_chunkMinHeight, chunkMinHeight)
                chunkRoundedCorners = getBoolean(R.styleable.AudioRecordView_chunkRoundedCorners, chunkRoundedCorners)
                chunkWidth = getDimension(R.styleable.AudioRecordView_chunkWidth, chunkWidth)
                chunkColor = getColor(R.styleable.AudioRecordView_chunkColor, chunkColor)
                chunkAlignTo = when (getInt(R.styleable.AudioRecordView_chunkAlignTo, chunkAlignTo.ordinal)) {
                        AlignTo.BOTTOM.value -> AlignTo.BOTTOM
                        else -> AlignTo.CENTER
                    }

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

        if (chunkHeights.size >= maxChunkCount) {
            chunkHeights.removeAt(0)
        } else {
            usageWidth += chunkHorizontalScale
            chunkWidths.add(chunkWidths.size, usageWidth)
        }

        if (chunkMaxHeight == uninitialized) {
            chunkMaxHeight = height - topBottomPadding * 2
        } else if (chunkMaxHeight > height - topBottomPadding * 2) {
            chunkMaxHeight = height - topBottomPadding * 2
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

        fftPoint += chunkMinHeight

        if (fftPoint > chunkMaxHeight) {
            fftPoint = chunkMaxHeight
        } else if (fftPoint < chunkMinHeight) {
            fftPoint = chunkMinHeight
        }

        chunkHeights.add(chunkHeights.size, fftPoint)
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
            val chunkX = chunkWidths[i]
            val startY = verticalCenter - chunkHeights[i] / 2
            val stopY = verticalCenter + chunkHeights[i] / 2

            canvas.drawLine(chunkX, startY, chunkX, stopY, chunkPaint)
        }
    }

    private fun drawAlignBottom(canvas: Canvas) {
        for (i in 0 until chunkHeights.size - 1) {
            val chunkX = chunkWidths[i]
            val startY = height.toFloat() //bottom
            val stopY = height.toFloat() - chunkHeights[i]

            canvas.drawLine(chunkX, startY, chunkX, stopY, chunkPaint)
        }
    }
}