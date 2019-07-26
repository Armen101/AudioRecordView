package com.dba_droid.visualizer
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.*


class AudioRecordView : View {

    private val DENSITY = Resources.getSystem().displayMetrics.density
    private val MAX_REPORTABLE_AMP = 22760f //effective size,  max fft = 32760
    private val UNINITIALIZED = 0f

    private val chunkPaint = Paint()

    private var lastFFT = 0.toFloat()
    private var usageWidth = 0.toDouble()
    private var chunkHeights = ArrayList<Float>()
    private var chunkWidths = ArrayList<Double>()
    private var topBottomPadding = 10 * DENSITY

    var chunkColor = Color.RED
    var chunkWidth = 2 * DENSITY
    var chunkSpace = 1 * DENSITY
    var chunkMaxHeight = UNINITIALIZED
    var chunkMinHeight = 3 * DENSITY  // don't recommendation size <= 10 dp

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
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

                chunkWidth = getDimension(R.styleable.AudioRecordView_chunkWidth, chunkWidth)
                chunkPaint.strokeWidth = chunkWidth

                chunkColor = getColor(R.styleable.AudioRecordView_chunkColor, chunkColor)
                chunkPaint.color = chunkColor

                setWillNotDraw(false)
                chunkPaint.isAntiAlias = true
            } finally {
                recycle()
            }
        }
    }

    fun recreate() {
        lastFFT = 0f
        usageWidth = 0.0
        chunkWidths = ArrayList()
        chunkHeights = ArrayList()
        invalidate()
    }

    fun update(fft: Int) {
        this.lastFFT = fft.toFloat()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val chunkHorizontalScale = (chunkWidth + chunkSpace).toDouble()
        val maxLineCount = width / chunkHorizontalScale
        val centerView = (height / 2).toFloat()

        if (chunkWidths.size >= maxLineCount) {
            chunkHeights.removeAt(0)
        } else {
            usageWidth += chunkHorizontalScale
            chunkWidths.add(chunkWidths.size, usageWidth)
        }

        if (chunkMaxHeight == UNINITIALIZED) {
            chunkMaxHeight = getHeight() - topBottomPadding * 2
        } else if (chunkMaxHeight > getHeight() - topBottomPadding * 2) {
            chunkMaxHeight = getHeight() - topBottomPadding * 2
        }

        val verticalDrawScale = chunkMaxHeight - chunkMinHeight
        if (verticalDrawScale == 0f) {
            return
        }

        val point = MAX_REPORTABLE_AMP / verticalDrawScale
        if (point == 0f) {
            return
        }

        if (lastFFT == 0f) {
            return
        }

        var fftPoint = lastFFT / point

        fftPoint += chunkMinHeight

        if (fftPoint > chunkMaxHeight) {
            fftPoint = chunkMaxHeight
        } else if (fftPoint < chunkMinHeight) {
            fftPoint = chunkMinHeight
        }

        chunkHeights.add(chunkHeights.size, fftPoint)

        for (i in 0 until chunkHeights.size - 1) {
            val startX = chunkWidths[i].toFloat()
            val stopX = chunkWidths[i].toFloat()
            val startY = centerView - chunkHeights[i] / 2
            val stopY = centerView + chunkHeights[i] / 2

            canvas.drawLine(startX, startY, stopX, stopY, chunkPaint)
        }
    }
}