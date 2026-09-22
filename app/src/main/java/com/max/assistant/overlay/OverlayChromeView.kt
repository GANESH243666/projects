package com.max.assistant.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class OverlayChromeView(context: Context, private val reducedMotion: Boolean) : View(context) {
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(3f) }
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var rotation = 0f
    private var pulse = 0.35f
    private var state = MaxOverlayProtocol.State.IDLE
    private var animator: ValueAnimator? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        if (!reducedMotion) {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 4200L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    rotation = it.animatedFraction * 360f
                    pulse = 0.25f + it.animatedFraction * 0.35f
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        if (state == MaxOverlayProtocol.State.IDLE) {
            borderPaint.style = Paint.Style.FILL
            borderPaint.shader = null
            borderPaint.color = 0xff5ed9ff.toInt()
            borderPaint.setShadowLayer(dp(10f), 0f, 0f, 0xff5ed9ff.toInt())
            canvas.drawCircle(width / 2f, dp(22f), dp(5f), borderPaint)
            return
        }
        scrimPaint.shader = LinearGradient(0f, height, 0f, height * 0.68f, 0x55000000, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, height * 0.62f, width, height, scrimPaint)

        val inset = dp(7f)
        val radius = dp(24f)
        val colors = intArrayOf(0xff168cff.toInt(), 0xff7657ff.toInt(), 0xffff3c9e.toInt(), 0xffffb12b.toInt(), 0xff12d9d0.toInt(), 0xff168cff.toInt())
        val shader = SweepGradient(width / 2f, height / 2f, colors, null)
        val matrix = Matrix().apply { setRotate(rotation, width / 2f, height / 2f) }
        shader.setLocalMatrix(matrix)
        borderPaint.shader = shader
        borderPaint.alpha = 190
        borderPaint.setShadowLayer(dp(12f) * pulse, 0f, 0f, 0xff4c8dff.toInt())
        canvas.drawRoundRect(inset, inset, width - inset, height - inset, radius, radius, borderPaint)
    }

    fun setState(value: MaxOverlayProtocol.State) {
        state = value
        borderPaint.style = Paint.Style.STROKE
        invalidate()
    }

    override fun onDetachedFromWindow() { animator?.cancel(); super.onDetachedFromWindow() }
    private fun dp(value: Float) = value * resources.displayMetrics.density
}