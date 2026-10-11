package com.fortune.os

import android.os.Bundle
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.content.Context
import android.os.BatteryManager
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var fortuneView: FortuneView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.BLACK

        fortuneView = FortuneView(this)
        setContentView(fortuneView)
    }

    override fun onResume() {
        super.onResume()
        fortuneView.startAnimation()
    }

    override fun onPause() {
        super.onPause()
        fortuneView.stopAnimation()
    }
}

class FortuneView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val particles = ArrayList<Particle>()

    private var running = false
    private var animationThread: Thread? = null

    private var widthF = 0f
    private var heightF = 0f

    private var centerX = 0f
    private var centerY = 0f

    private var batteryLevel = 0

    private val cyan = Color.rgb(70, 225, 255)
    private val purple = Color.rgb(155, 75, 255)
    private val softPurple = Color.rgb(100, 55, 180)

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        repeat(75) {
            particles.add(Particle())
        }

        updateBattery()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        widthF = w.toFloat()
        heightF = h.toFloat()

        centerX = widthF / 2f
        centerY = heightF * 0.43f

        particles.forEach {
            it.randomize(widthF, heightF)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackground(canvas)
        drawParticles(canvas)
        drawConnections(canvas)
        drawCore(canvas)
        drawClock(canvas)
        drawStatus(canvas)
        drawApps(canvas)
        drawGesture(canvas)

        if (running) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawBackground(canvas: Canvas) {

        val gradient = RadialGradient(
            centerX,
            centerY,
            max(widthF, heightF) * 0.75f,
            intArrayOf(
                Color.rgb(24, 8, 42),
                Color.rgb(9, 5, 20),
                Color.BLACK
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawRect(0f, 0f, widthF, heightF, paint)
        paint.shader = null
    }

    private fun drawParticles(canvas: Canvas) {

        particles.forEach { p ->

            p.update()

            val alpha = (80 + 100 * p.brightness).toInt()

            paint.color = Color.argb(
                alpha,
                110,
                150,
                255
            )

            paint.style = Paint.Style.FILL

            canvas.drawCircle(
                p.x,
                p.y,
                p.size,
                paint
            )
        }
    }

    private fun drawConnections(canvas: Canvas) {

        val maxDistance = 190f

        for (i in particles.indices) {

            val a = particles[i]

            for (j in i + 1 until particles.size) {

                val b = particles[j]

                val dx = a.x - b.x
                val dy = a.y - b.y

                val distance = sqrt(dx * dx + dy * dy)

                if (distance < maxDistance) {

                    val alpha =
                        ((1f - distance / maxDistance) * 45f).toInt()

                    paint.color = Color.argb(
                        alpha,
                        120,
                        80,
                        220
                    )

                    paint.strokeWidth = 1f
                    paint.style = Paint.Style.STROKE

                    canvas.drawLine(
                        a.x,
                        a.y,
                        b.x,
                        b.y,
                        paint
                    )
                }
            }
        }
    }

    private fun drawCore(canvas: Canvas) {

        val radius = min(widthF, heightF) * 0.145f

        glowPaint.shader = RadialGradient(
            centerX,
            centerY,
            radius * 2.3f,
            intArrayOf(
                Color.argb(130, 70, 225, 255),
                Color.argb(60, 155, 75, 255),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(
            centerX,
            centerY,
            radius * 2.3f,
            glowPaint
        )

        glowPaint.shader = null

        paint.shader = LinearGradient(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            cyan,
            purple,
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(
            centerX,
            centerY,
            radius,
            paint
        )

        paint.shader = null

        paint.color = Color.BLACK
        canvas.drawCircle(
            centerX,
            centerY,
            radius * 0.82f,
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = Color.argb(210, 70, 225, 255)

        canvas.drawCircle(
            centerX,
            centerY,
            radius * 0.92f,
            paint
        )

        paint.style = Paint.Style.FILL

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )

        textPaint.textSize = radius * 0.27f
        textPaint.color = Color.WHITE

        canvas.drawText(
            "FORTUNE",
            centerX,
            centerY + radius * 0.08f,
            textPaint
        )

        textPaint.textSize = radius * 0.10f
        textPaint.color = cyan

        canvas.drawText(
            "CORE ACTIVE",
            centerX,
            centerY + radius * 0.36f,
            textPaint
        )
    }

    private fun drawClock(canvas: Canvas) {

        val time = SimpleDateFormat(
            "HH:mm",
            Locale.getDefault()
        ).format(Date())

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(
            "sans-serif",
            Typeface.NORMAL
        )

        textPaint.textSize = 52f
        textPaint.color = Color.WHITE

        canvas.drawText(
            time,
            centerX,
            centerY - 150f,
            textPaint
        )

        val date = SimpleDateFormat(
            "EEEE, d MMM",
            Locale.getDefault()
        ).format(Date())

        textPaint.textSize = 13f
        textPaint.color = Color.argb(
            180,
            190,
            190,
            210
        )

        canvas.drawText(
            date.uppercase(),
            centerX,
            centerY - 123f,
            textPaint
        )
    }

    private fun drawStatus(canvas: Canvas) {

        updateBattery()

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 12f
        textPaint.color = Color.argb(
            190,
            210,
            210,
            230
        )

        canvas.drawText(
            "SYSTEM ACTIVE",
            28f,
            54f,
            textPaint
        )

        textPaint.textAlign = Paint.Align.RIGHT

        canvas.drawText(
            "$batteryLevel%",
            widthF - 28f,
            54f,
            textPaint
        )

        paint.color = cyan
        canvas.drawCircle(
            widthF - 70f,
            49f,
            3f,
            paint
        )
    }

    private fun drawApps(canvas: Canvas) {

        val y = heightF - 125f
        val spacing = widthF / 6f

        val names = arrayOf(
            "PHONE",
            "CHAT",
            "WEB",
            "MUSIC",
            "APPS"
        )

        for (i in names.indices) {

            val x = spacing * (i + 1)

            glowPaint.color = Color.argb(
                35,
                70,
                225,
                255
            )

            canvas.drawCircle(
                x,
                y,
                31f,
                glowPaint
            )

            paint.color = Color.argb(
                220,
                18,
                16,
                30
            )

            canvas.drawCircle(
                x,
                y,
                27f,
                paint
            )

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = Color.argb(
                150,
                100,
                90,
                180
            )

            canvas.drawCircle(
                x,
                y,
                27f,
                paint
            )

            paint.style = Paint.Style.FILL

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 8f
            textPaint.color = Color.argb(
                210,
                220,
                220,
                235
            )

            canvas.drawText(
                names[i],
                x,
                y + 4f,
                textPaint
            )
        }
    }

    private fun drawGesture(canvas: Canvas) {

        paint.color = Color.argb(
            120,
            130,
            100,
            220
        )

        canvas.drawRoundRect(
            centerX - 35f,
            heightF - 42f,
            centerX + 35f,
            heightF - 38f,
            4f,
            4f,
            paint
        )

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 9f
        textPaint.color = Color.argb(
            100,
            180,
            180,
            200
        )

        canvas.drawText(
            "SWIPE UP",
            centerX,
            heightF - 52f,
            textPaint
        )
    }

    private fun updateBattery() {

        val manager =
            context.getSystemService(
                Context.BATTERY_SERVICE
            ) as BatteryManager

        batteryLevel =
            manager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CAPACITY
            )
    }

    fun startAnimation() {

        if (running) return

        running = true

        animationThread = Thread {

            while (running) {

                particles.forEach {
                    it.time += 0.008f
                }

                try {
                    Thread.sleep(16)
                } catch (_: Exception) {
                }
            }
        }

        animationThread?.start()
    }

    fun stopAnimation() {

        running = false
        animationThread = null
    }

    inner class Particle {

        var x = 0f
        var y = 0f

        var vx = 0f
        var vy = 0f

        var size = 1.5f

        var brightness = 0.5f

        var time = Random.nextFloat() * 10f

        fun randomize(w: Float, h: Float) {

            x = Random.nextFloat() * w
            y = Random.nextFloat() * h

            vx =
                (Random.nextFloat() - 0.5f) *
                0.18f

            vy =
                (Random.nextFloat() - 0.5f) *
                0.18f

            size =
                0.8f +
                Random.nextFloat() * 2.2f

            brightness =
                0.3f +
                Random.nextFloat() * 0.7f
        }

        fun update() {

            x += vx
            y += vy

            time += 0.01f

            brightness =
                0.45f +
                sin(time) * 0.35f

            if (x < -10f) x = widthF + 10f
            if (x > widthF + 10f) x = -10f

            if (y < -10f) y = heightF + 10f
            if (y > heightF + 10f) y = -10f
        }
    }
}