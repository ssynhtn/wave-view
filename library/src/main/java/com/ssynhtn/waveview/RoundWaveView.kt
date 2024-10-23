package com.ssynhtn.waveview

import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import com.ssynhtn.library.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Created by huangtongnao on 2017/10/25.
 * @author ssynh
 */
class RoundWaveView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private val path: Path
    private val paint: Paint

    private var pointCount: Int
    private var innerSizeRatio: Float = 0.75f
    private var firstRadius: FloatArray
    private var secondRadius: FloatArray //
    private var animationOffset: FloatArray //
    private var xs: FloatArray //
    private var ys: FloatArray

    private var fractions: FloatArray

    private var animators: Array<ValueAnimator?>

    private var angleOffset: Float = 0f

    private var currentCx: Float = 0f
    private var currentCy: Float = 0f
    private var translationRadius: Float = 0f
    private var translationRadiusStep: Float = 0f

    private var useAnimation: Boolean

    private var lastTranslationAngle = 0f
    private fun randomTranslate() {
        val r = translationRadiusStep
        val outR = translationRadius

        val cx = (width / 2).toFloat()
        val cy = (height / 2).toFloat()
        val vx = currentCx - cx
        val vy = currentCy - cy
        val ratio = 1 - r / outR
        val wx = vx * ratio
        val wy = vy * ratio
        lastTranslationAngle =
            ((Math.random() - 0.5) * Math.PI / 4 + lastTranslationAngle).toFloat()
        val distRatio = Math.random().toFloat()

        currentCx = (cx + wx + r * distRatio * cos(lastTranslationAngle.toDouble())).toFloat()
        currentCy = (cy + wy + r * distRatio * sin(lastTranslationAngle.toDouble())).toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val radius = (min(w.toDouble(), h.toDouble()) / 2).toFloat()
        val innerRadius = radius * innerSizeRatio
        val ringWidth = radius - innerRadius

        for (i in 0 until pointCount) {
            firstRadius[i] = (innerRadius + ringWidth * Math.random()).toFloat()
            secondRadius[i] = (innerRadius + ringWidth * Math.random()).toFloat()
            animationOffset[i] = Math.random().toFloat()
        }

        paint.setShader(
            LinearGradient(
                0f,
                0f,
                0f,
                h.toFloat(),
                Color.RED,
                Color.BLUE,
                Shader.TileMode.MIRROR
            )
        )
        paint.alpha = (0.2f * 255).toInt()

        currentCx = (w / 2).toFloat()
        currentCy = (h / 2).toFloat()
        translationRadius = radius / 6
        translationRadiusStep = radius / 4000
    }

    private var temp: FloatArray = FloatArray(2)

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundWaveView)
        pointCount = typedArray.getInt(R.styleable.RoundWaveView_pointCount, 6)
        useAnimation = typedArray.getBoolean(R.styleable.RoundWaveView_useAnimation, true)
        typedArray.recycle()

        firstRadius = FloatArray(pointCount)
        secondRadius = FloatArray(pointCount)
        animationOffset = FloatArray(pointCount)
        xs = FloatArray(pointCount)
        ys = FloatArray(pointCount)

        fractions = FloatArray(pointCount + 1)

        animators = arrayOfNulls(pointCount)

        angleOffset = (Math.PI * 2 / pointCount * Math.random()).toFloat()

        path = Path()
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        //        paint.setColor(Color.MAGENTA);
        paint.style = Paint.Style.FILL

        val delta = 1.0f / pointCount
        for (i in fractions.indices) {
            fractions[i] = delta * i
        }

        for (i in 0 until pointCount) {
            var pos = (Math.random() * pointCount).toInt()
            val dest = FloatArray(pointCount * 2 + 1)
            var inc = 1
            for (j in dest.indices) {
                dest[j] = fractions[pos]
                pos += inc
                if (pos < 0 || pos >= fractions.size) {
                    inc = -inc
                    pos += inc * 2
                }
            }

            if (i == 0) {
                val list: MutableList<Float> = ArrayList()
                for (f in dest) {
                    list.add(f)
                }
                Log.d(ContentValues.TAG, "DEST $list")
            }

            if (useAnimation) {
                val animator =
                    ValueAnimator.ofFloat(*dest).setDuration((4000 + Math.random() * 2000).toLong())
                animator.interpolator = LinearInterpolator()
                animator.repeatMode = ValueAnimator.RESTART
                animator.repeatCount = ValueAnimator.INFINITE
                animator.start()
                if (i == 0) {
                    animator.addUpdateListener {
                        randomTranslate()
                        invalidate()
                    }
                }
                animators[i] = animator
                animator.start()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //        paint.setColor(Color.MAGENTA);
//        float cx = getWidth() / 2;
//        float cy = getHeight() / 2;
//        float fraction = (float) valueAnimator.getAnimatedValue();
        for (i in 0 until pointCount) {
//            float currentFraction = animationOffset[i] + fraction;
//            if (currentFraction >= 1) {
//                currentFraction = currentFraction - 1;
//            }
            val currentFraction = if (useAnimation) animators[i]?.animatedValue as? Float ?: 0f else 0f
            val radius = firstRadius[i] * (1 - currentFraction) + secondRadius[i] * currentFraction
            val angle = (Math.PI * 2 / pointCount * i).toFloat() + angleOffset
            xs[i] = (currentCx + radius * cos(angle.toDouble())).toFloat()
            ys[i] = (currentCy + radius * sin(angle.toDouble())).toFloat()
        }

        path.reset()
        path.moveTo(xs[0], ys[0])
        for (i in 0 until pointCount) {
            val currX = getFromArray(xs, i)
            val currY = getFromArray(ys, i)
            val nextX = getFromArray(xs, i + 1)
            val nextY = getFromArray(ys, i + 1)

            getVector(xs, ys, i, temp)

            val vx = temp[0]
            val vy = temp[1]

            getVector(xs, ys, i + 1, temp)
            val vxNext = temp[0]
            val vyNext = temp[1]

            path.cubicTo(currX + vx, currY + vy, nextX - vxNext, nextY - vyNext, nextX, nextY)
        }

        canvas.drawPath(path, paint)

        //        float firstFraction = (float) animators[0].getAnimatedValue();
//        paint.setColor(Color.BLACK);
//        float r = 30;
//        canvas.drawCircle((getWidth() - r * 2) * firstFraction + r, getHeight() / 2, r, paint);
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (useAnimation) {
            for (animator in animators) {
                animator?.end()
            }
        }
    }

    companion object {
        private const val LINE_SMOOTHNESS = 0.16f
        fun getFromArray(arr: FloatArray, pos: Int): Float {
            return arr[(pos + arr.size) % arr.size]
        }

        fun getVector(xs: FloatArray, ys: FloatArray, i: Int, out: FloatArray) {
            val nextX = getFromArray(xs, i + 1)
            val nextY = getFromArray(ys, i + 1)
            val prevX = getFromArray(xs, i - 1)
            val prevY = getFromArray(ys, i - 1)

            val vx = (nextX - prevX) * LINE_SMOOTHNESS
            val vy = (nextY - prevY) * LINE_SMOOTHNESS

            out[0] = vx
            out[1] = vy
        }
    }
}
