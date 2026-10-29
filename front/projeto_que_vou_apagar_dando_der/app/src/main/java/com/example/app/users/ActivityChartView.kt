package com.example.app.users

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.app.R

class ActivityChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var values: List<Int> = List(14) { 0 }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.red_primary)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * resources.displayMetrics.density
        strokeJoin = Paint.Join.ROUND
    }
    fun setActivity(activity: List<Int>?) {
        values = activity?.takeLast(14)?.map { it.coerceAtLeast(0) }?.takeIf { it.isNotEmpty() } ?: List(14) { 0 }
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = 3f * resources.displayMetrics.density
        val chartWidth = (width - 2 * inset).coerceAtLeast(0f)
        val chartHeight = (height - 2 * inset).coerceAtLeast(0f)
        val max = maxOf(5, values.maxOrNull() ?: 0).toFloat()
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = inset + index * chartWidth / maxOf(1, values.size - 1)
            val y = height - inset - value / max * chartHeight
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, line)
    }
}
