package com.sbi.globalmarkets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class CandleChart(context: Context, private val bars: List<PriceActionV18.Candle>) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (240 * resources.displayMetrics.density).toInt())
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val items = bars.takeLast(55)
        if (items.isEmpty()) return
        val hi = items.maxOf { it.high }
        val lo = items.minOf { it.low }
        val span = (hi - lo).coerceAtLeast(0.00000001)
        val pad = 18f * resources.displayMetrics.density
        val usable = height - 2 * pad
        fun y(v: Double): Float = pad + ((hi - v) / span * usable).toFloat()
        paint.color = Color.rgb(35, 48, 68)
        paint.strokeWidth = 1f
        for (i in 0..4) { val yy = pad + usable * i / 4f; canvas.drawLine(0f, yy, width.toFloat(), yy, paint) }
        val step = width.toFloat() / items.size
        for ((i, b) in items.withIndex()) {
            paint.color = if (b.close >= b.open) Color.rgb(69,207,160) else Color.rgb(244,92,105)
            paint.strokeWidth = 2f
            val x = (i + 0.5f) * step
            canvas.drawLine(x, y(b.high), x, y(b.low), paint)
            val top = minOf(y(b.open), y(b.close))
            val bottom = maxOf(y(b.open), y(b.close))
            canvas.drawRect(x - step * 0.29f, top, x + step * 0.29f, maxOf(top + 1.5f, bottom), paint)
        }
    }
}