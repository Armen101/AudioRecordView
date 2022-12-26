package com.visualizer.amplitude

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import java.util.*
import kotlin.math.floor


@MainThread
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

    var chunkAlignTo = AlignTo.CENTER
    var direction = Direction.LeftToRight
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
    var chunkMaxHeight = UNINITIALISED
        get() {
            val possibleMaxHeight = height - (topBottomPadding * 2)
            if (field == UNINITIALISED || field > possibleMaxHeight) {
                field = possibleMaxHeight
            }
            return field
        }
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
    private val chunks = Collections.synchronizedList<Chunk>(mutableListOf())
    private val chunkPaint = Paint()
    private var lastUpdateTime = 0L
    private var topBottomPadding = 6.dp()

    init {
        attrs?.let { init(it) } ?: run { init() }
    }

    fun recreate() {
        chunks.clear()
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
            Log.w(AudioRecordView::class.simpleName, e.message ?: e.javaClass.simpleName)
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
        val maxChunkCount = floor((width / chunkHorizontalScale).toDouble())
        if (chunks.isNotEmpty() && chunks.size == maxChunkCount.toInt()) {
            shiftX()
            chunks.removeFirst()
        }

        val chunkX = getChunkX(chunkHorizontalScale)

        val verticalDrawScale = chunkMaxHeight - chunkMinHeight
        if (verticalDrawScale == 0f) {
            return
        }

        val point = MAX_REPORTABLE_AMP / verticalDrawScale
        if (point == 0f) {
            return
        }

        var fftPoint = fft / point

        if (chunkSoftTransition && chunks.isNotEmpty()) {
            val updateTimeInterval = System.currentTimeMillis() - lastUpdateTime
            val scaleFactor = calculateScaleFactor(updateTimeInterval)
            val prevFftWithoutAdditionalSize = chunks.last().height - chunkMinHeight
            fftPoint = fftPoint.softTransition(prevFftWithoutAdditionalSize, 2.2f, scaleFactor)
        }

        fftPoint += chunkMinHeight

        if (fftPoint > chunkMaxHeight) {
            fftPoint = chunkMaxHeight
        } else if (fftPoint < chunkMinHeight) {
            fftPoint = chunkMinHeight
        }
        chunks.add(Chunk(chunkX, fftPoint))
    }

    private fun getChunkX(chunkHorizontalScale: Float): Float {
        val theLastChunkPosition = if (chunks.isNotEmpty()) {
            chunks.last().x
        } else {
            0f
        }
        return theLastChunkPosition + chunkHorizontalScale
    }

    private fun shiftX() {
        if (chunks.isEmpty()) return
        if (chunks.size <= 1) return

        var currentX = chunks[0].x
        for (i in 1 until chunks.size) {
            val nextX = chunks[i].x
            chunks[i].x = currentX
            currentX = nextX
        }
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
        drawLine(
            canvas = canvas,
            calculateX = { x ->
                if (direction == Direction.RightToLeft) {
                    width - chunkWidth
                } else {
                    x
                }
            },
            calculateY = { chunkHeight ->
                if (chunkAlignTo == AlignTo.BOTTOM) {
                    val startY = height.toFloat() - topBottomPadding
                    val stopY = startY - chunkHeight
                    Position(start = startY, end = stopY)
                } else {
                    val verticalCenter = height / 2
                    val startY = verticalCenter - (chunkHeight / 2)
                    val stopY = verticalCenter + (chunkHeight / 2)
                    Position(start = startY, end = stopY)
                }
            }
        )
    }

    private fun drawLine(
        canvas: Canvas,
        calculateX: (Float) -> Float,
        calculateY: (Float) -> Position
    ) {
        chunks.forEach { chunk ->
            val chunkX = calculateX(chunk.x)
            val yPosition = calculateY(chunk.height)
            canvas.drawLine(chunkX, yPosition.start, chunkX, yPosition.end, chunkPaint)
        }
    }

    companion object {
        private val LOG_TAG = AudioRecordView::class.java.simpleName
        private const val UNINITIALISED = 0f
        private const val MAX_REPORTABLE_AMP = 22760f //effective size,  max fft = 32760
    }
}