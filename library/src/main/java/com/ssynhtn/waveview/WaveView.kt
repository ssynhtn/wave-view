package com.ssynhtn.waveview

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import java.util.Collections
import kotlin.math.max
import kotlin.math.sin

@Suppress("unused")
class WaveView : View {
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var animatorSet: AnimatorSet? = null
    private var animationPaused = false


    class WaveData(
        waveLength: Float,
        waveHeight: Float,
        fixedHeight: Float,
        offset: Float,
        startColor: Int,
        endColor: Int,
        alpha: Float,
        duration: Long,
        right: Boolean
    ) {
        /**
         * waveLength 波长
         * waveHeight 波高度
         * fixedHeight   波谷距离View的底部的固定高度, 相当于这个波的marginBottom
         * offset    波的初始位置
         * startColor    渐变色左边的颜色
         * endColor      渐变色右边的颜色
         * alpha     透明度
         * duration  波动周期, 走一个波长所需要的时间
         * right 是否向右运动
         * sampleSize 用path绘制正弦曲线, 一个周期取的样点
         * gradientOrientation 渐变色方向
         */
        var waveLength = 200f
            get() = field * lengthScale
        var waveHeight = 50f
            get() = field * heightScale
        var fixedHeight: Float = 0f
            get() = field * fixedHeightScale
        var offset: Float = 0f
        var duration: Long = 2000
            get() = (field * durationScale).toLong()
        var startColor: Int = Color.WHITE
        var endColor: Int = Color.WHITE
        var alpha: Float = 1f
        var right: Boolean = true
        var sampleSize: Int = DEFAULT_SAMPLE_SIZE
        var gradientOrientation: Int = GRADIENT_ORIENTATION_HORIZONTAL

        var shader: Shader? = null
        var valueAnimator: ValueAnimator? = null
        val path: Path = Path()

        var lengthScale: Float = 1f
        var heightScale: Float = 1f
        var durationScale: Float = 1f
        var fixedHeightScale: Float = 1f

        init {
            this.waveLength = waveLength
            this.waveHeight = waveHeight
            this.fixedHeight = fixedHeight
            this.offset = offset
            this.startColor = startColor
            this.endColor = endColor
            this.alpha = alpha
            this.duration = duration
            this.right = right
        }
        
        companion object {
            const val GRADIENT_ORIENTATION_VERTICAL: Int = 1
            const val GRADIENT_ORIENTATION_HORIZONTAL: Int = 2
            private const val DEFAULT_SAMPLE_SIZE = 16
        }
    }


    private val waveDataList: MutableList<WaveData> = ArrayList()

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun startAnimation() {
        if (USE_ANIMATION) {
            if (animatorSet != null) return

            animatorSet = AnimatorSet()

            val animators: MutableList<Animator> = ArrayList()
            var first = true
            for (waveData in waveDataList) {
                val animator = ValueAnimator.ofFloat(0f, 1f)
                animator.setDuration(waveData.duration)
                animator.interpolator = LinearInterpolator()
                animator.repeatMode = ValueAnimator.RESTART
                animator.repeatCount = ValueAnimator.INFINITE
                if (first) {
                    animator.addUpdateListener { //                            Log.d(TAG, "onAnimationUpdate");
                        postInvalidateOnAnimation()
                    }
                }
                first = false
                waveData.valueAnimator = animator
                animators.add(animator)
            }

            animatorSet?.playTogether(animators)
            animatorSet?.start()
        }
    }

    private val isAnimationStarted: Boolean
        get() = animatorSet?.isStarted == true

    fun isAnimationPaused(): Boolean {
        return isAnimationStarted && animationPaused
    }

    fun pauseAnimation() {
        val animatorSet = this.animatorSet ?: return
        if (!animatorSet.isStarted) return

        if (animationPaused) return

        animationPaused = true
        animatorSet.pause()
    }

    fun resumeAnimation() {
        val animatorSet = this.animatorSet ?: return
        if (!animatorSet.isStarted) return

        if (animationPaused) {
            animationPaused = false
            animatorSet.resume()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        animatorSet?.let {
            it.end()
            animatorSet = null
            for (waveData in waveDataList) {
                waveData.valueAnimator = null
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxHeight = 0f
        for (waveData in waveDataList) {
            maxHeight = max(
                maxHeight,
                waveData.fixedHeight + waveData.waveHeight
            )
        }

        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            resolveSize(maxHeight.toInt(), heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        for (waveData in waveDataList) {
            resetPath(waveData, w, h)
        }
    }

    private fun resetPath(waveData: WaveData, w: Int, h: Int) {
        if (waveData.gradientOrientation == WaveData.GRADIENT_ORIENTATION_HORIZONTAL) {
            waveData.shader = LinearGradient(
                0f,
                0f,
                w.toFloat(),
                0f,
                waveData.startColor,
                waveData.endColor,
                Shader.TileMode.REPEAT
            )
        } else if (waveData.gradientOrientation == WaveData.GRADIENT_ORIENTATION_VERTICAL) {
            waveData.shader = LinearGradient(
                0f,
                h - waveData.fixedHeight - waveData.waveHeight,
                0f,
                h - waveData.fixedHeight,
                waveData.startColor,
                waveData.endColor,
                Shader.TileMode.REPEAT
            )
        }

        val cy = h - waveData.fixedHeight - waveData.waveHeight / 2
        setSineWave(w, h, cy, waveData, waveData.sampleSize)
    }

    private fun setSineWave(w: Int, h: Int, cy: Float, waveData: WaveData, sampleCount: Int) {
        val path = waveData.path
        path.reset()

        val waveLength = waveData.waveLength
        val waveHeight = waveData.waveHeight

        val offMax = waveData.offset + waveLength
        val offMin = waveData.offset - waveLength
        val left = -offMax
        val right = w - offMin
        var x = left
        val delta = waveData.waveLength / sampleCount

        var prePreviousPointX = x - delta * 2
        var prePreviousPointY =
            computeY(prePreviousPointX, left, waveLength, waveHeight, cy, waveData)
        var previousPointX = x - delta
        var previousPointY = computeY(previousPointX, left, waveLength, waveHeight, cy, waveData)
        var currentPointX = x
        var currentPointY = computeY(currentPointX, left, waveLength, waveHeight, cy, waveData)
        var nextPointX = x + delta
        var nextPointY = computeY(nextPointX, left, waveLength, waveHeight, cy, waveData)

        var first = true
        while (x - delta < right) {
            if (first) {
                // Move to start point.
                path.moveTo(currentPointX, currentPointY)
                first = false
            } else {
                // Calculate control points.
                val firstDiffX = (currentPointX - prePreviousPointX)
                val firstDiffY = (currentPointY - prePreviousPointY)
                val secondDiffX = (nextPointX - previousPointX)
                val secondDiffY = (nextPointY - previousPointY)
                val firstControlPointX = previousPointX + (LINE_SMOOTHNESS * firstDiffX)
                val firstControlPointY = previousPointY + (LINE_SMOOTHNESS * firstDiffY)
                val secondControlPointX = currentPointX - (LINE_SMOOTHNESS * secondDiffX)
                val secondControlPointY = currentPointY - (LINE_SMOOTHNESS * secondDiffY)
                path.cubicTo(
                    firstControlPointX,
                    firstControlPointY,
                    secondControlPointX,
                    secondControlPointY,
                    currentPointX,
                    currentPointY
                )
                //                Log.d(TAG, "cubic to " + currentPointX + ", " + currentPointY);
            }

            // Shift values by one back to prevent recalculation of values that have
            // been already calculated.
            prePreviousPointX = previousPointX
            prePreviousPointY = previousPointY
            previousPointX = currentPointX
            previousPointY = currentPointY
            currentPointX = nextPointX
            currentPointY = nextPointY
            nextPointX += delta
            nextPointY = computeY(nextPointX, left, waveLength, waveHeight, cy, waveData)

            x += delta
        }

        path.lineTo(x, h.toFloat())
        path.lineTo(left, h.toFloat())

        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (waveData in waveDataList) {
            drawWave(canvas, waveData)
        }
    }

    private val matrix = Matrix()

    private fun drawWave(canvas: Canvas, waveData: WaveData) {
        val translation =
            if (USE_ANIMATION) (waveData.valueAnimator?.animatedFraction ?: 0f) * waveData.waveLength * (if (waveData.right) 1 else -1) + waveData.offset else waveData.offset

        canvas.save()
        canvas.translate(translation, 0f)
        matrix.setTranslate(-translation, 0f)
        waveData.shader?.setLocalMatrix(matrix)

        paint.alpha = (waveData.alpha * 255).toInt()
        paint.setShader(waveData.shader)

        canvas.drawPath(waveData.path, paint)
        canvas.restore()
    }


    private fun computeY(
        x: Float,
        startX: Float,
        waveLength: Float,
        waveHeight: Float,
        cy: Float,
        waveData: WaveData
    ): Float {
        val cycle = (x - waveData.offset - startX) / waveLength
        return (cy - waveHeight / 2 * sineWaveShapeFunction((cycle * Math.PI * 2).toFloat()))
    }

    private fun sineWaveShapeFunction(x: Float): Float {
        return sin(x.toDouble()).toFloat()
    }


    private fun addWaveData(vararg waveData: WaveData) {
        Collections.addAll(waveDataList, *waveData)

        requestLayout()
    }

    fun updateWaveLength(i: Int, lengthScale: Float) {
        if (i >= 0 && i < waveDataList.size) {
            val waveData = waveDataList[i]
            waveData.lengthScale = lengthScale

            val width = width
            val height = height

            if (width > 0 && height > 0) {
                resetPath(waveData, width, height)
            }
        }
    }

    fun updateWaveHeight(i: Int, heightScale: Float) {
        if (i >= 0 && i < waveDataList.size) {
            val waveData = waveDataList[i]
            waveData.heightScale = heightScale

            requestLayout()
            post {
                val width = width
                val height = height
                if (width > 0 && height > 0) {
                    resetPath(waveData, width, height)
                }
            }
        }
    }

    fun updateWaveDuration(i: Int, durationScale: Float) {
        if (i >= 0 && i < waveDataList.size) {
            val waveData = waveDataList[i]
            waveData.durationScale = durationScale
            waveData.valueAnimator?.setDuration(waveData.duration)
        }
    }

    fun updateWaveFixedHeight(i: Int, scale: Float) {
        if (i >= 0 && i < waveDataList.size) {
            val waveData = waveDataList[i]
            waveData.fixedHeightScale = scale

            requestLayout()
            post {
                val width = width
                val height = height
                if (width > 0 && height > 0) {
                    resetPath(waveData, width, height)
                }
            }
        }
    }

    fun addDefaultWaves(right: Int, left: Int) {
        for (i in 0 until right) {
            addWaveData(
                WaveData(
                    (800 + Math.random() * 100).toFloat(),
                    (100 + Math.random() * 20).toFloat(),
                    (200 + Math.random() * 20).toFloat(),
                    (Math.random() * 50).toFloat(),
                    Color.RED,
                    Color.BLUE,
                    0.3f,
                    (2000 + Math.random() * 1000).toLong(),
                    true
                )
            )
        }

        for (i in 0 until left) {
            addWaveData(
                WaveData(
                    (800 + Math.random() * 100).toFloat(),
                    (100 + Math.random() * 20).toFloat(),
                    (200 + Math.random() * 20).toFloat(),
                    (Math.random() * 50).toFloat(),
                    Color.RED,
                    Color.BLUE,
                    0.3f,
                    (2000 + Math.random() * 1000).toLong(),
                    false
                )
            )
        }
    }

    companion object {
        private const val LINE_SMOOTHNESS = 0.16f
        private const val USE_ANIMATION = true
    }
}
