package com.example.audia4b6

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val radius: Float
    private val centerX: Float
    private val centerY: Float
    private var selectedColor: Int = Color.RED

    init {
        val density = context.resources.displayMetrics.density
        radius = 100 * density // Radius des Farbrades
        centerX = radius
        centerY = radius
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Software Rendering, um Anti-Aliasing zu ermöglichen
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rect = RectF(0f, 0f, radius * 2, radius * 2)
        drawColorWheel(canvas, rect)
        drawSelectionCircle(canvas)
    }

    private fun drawColorWheel(canvas: Canvas, rect: RectF) {
        // Farbverlauf für das Farbrad (von außen nach innen)
        val sweepGradient = SweepGradient(centerX, centerY, createColorArray(), null)
        paint.shader = sweepGradient
        canvas.drawArc(rect, 0f, 360f, true, paint)

        // Radialer Gradient für die Helligkeit (von Weiß in der Mitte zu transparent am Rand)
        val radialGradient = RadialGradient(centerX, centerY, radius, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        paint.shader = radialGradient
        canvas.drawCircle(centerX, centerY, radius, paint)
    }

    private fun drawSelectionCircle(canvas: Canvas) {
        paint.color = selectedColor
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, 20f, paint) // Auswahlkreis
    }

    private fun createColorArray(): IntArray {
        val colors = IntArray(360)
        for (i in 0 until 360) {
            val hsv = floatArrayOf(i.toFloat(), 1f, 1f)
            colors[i] = Color.HSVToColor(hsv)
        }
        return colors
    }

    fun getColorAtPosition(x: Float, y: Float): Int {
        val dx = x - centerX
        val dy = y - centerY
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        return if (distance < radius) {
            val angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val normalizedAngle = (angle + 360) % 360

            // Helligkeit berechnen (von 1 für weiß bis 0 für volle Farbe)
            val brightness = 1 - (distance / radius) // von 1 (weiß) bis 0 (volle Farbe)
            val color = Color.HSVToColor(floatArrayOf(normalizedAngle, 1f, 1f))

            mixWithWhite(color, brightness)
        } else {
            selectedColor // Rückgabe der zuletzt ausgewählten Farbe, wenn außerhalb des Rades
        }
    }

    private fun mixWithWhite(color: Int, ratio: Float): Int {
        val alpha = Color.alpha(color)
        val red = (Color.red(color) * (1 - ratio) + 255 * ratio).toInt()
        val green = (Color.green(color) * (1 - ratio) + 255 * ratio).toInt()
        val blue = (Color.blue(color) * (1 - ratio) + 255 * ratio).toInt()
        return Color.argb(alpha, red, green, blue)
    }
}
