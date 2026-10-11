package com.fortune.os

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var fortuneView: FortuneView

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Some document providers do not offer persistent grants.
            }
            val prefs = getSharedPreferences("fortuneos", MODE_PRIVATE)
            val saved = prefs.getString("saved_files", "").orEmpty().lines()
                .filter { it.isNotBlank() && it != uri.toString() }
            prefs.edit().putString("saved_files", (saved + uri.toString()).joinToString("\n")).apply()
            Toast.makeText(this, "Archivo guardado en Tu espacio", Toast.LENGTH_SHORT).show()
        }
    }

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
        if (::fortuneView.isInitialized) fortuneView.startAnimation()
    }

    override fun onPause() {
        if (::fortuneView.isInitialized) fortuneView.stopAnimation()
        super.onPause()
    }

    fun onShortcut(index: Int) {
        when (index) {
            0 -> launchIntent(Intent(Intent.ACTION_DIAL), "No se encontró la aplicación de teléfono.")
            1 -> launchIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")), "No se encontró una aplicación de mensajes.")
            2 -> launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")), "No se encontró un navegador.")
            3 -> {
                val music = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
                launchIntent(music, "No se encontró una aplicación de música.")
            }
            4 -> showAppDrawer()
        }
    }

    private fun launchIntent(intent: Intent, errorMessage: String) {
        try {
            if (intent.resolveActivity(packageManager) != null) startActivity(intent)
            else Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    fun openPersonalSpace() {
        val prefs = getSharedPreferences("fortuneos", MODE_PRIVATE)
        val projects = linkedMapOf(
            "Fortune Web" to "https://github.com/ffortuneoficial-crypto/WebFortune",
            "GitHub · proyectos" to "https://github.com/ffortuneoficial-crypto",
            "Supabase · datos" to "https://supabase.com/dashboard",
            "FortuneOS · Código" to "https://github.com/ffortuneoficial-crypto/FortuneOS"
        )
        prefs.getString("projects", "").orEmpty().lines().filter { it.contains("|") }.forEach {
            val p = it.split("|", limit = 2)
            if (p.size == 2 && p[0].isNotBlank() && p[1].startsWith("https://")) projects[p[0]] = p[1]
        }
        val lastProject = prefs.getString("last_project", null)
        val projectNames = projects.keys.sortedWith(
            compareBy<String> { if (it == lastProject) 0 else 1 }
                .thenBy { it.lowercase(Locale.getDefault()) }
        )
        val savedFiles = prefs.getString("saved_files", "").orEmpty().lines().filter { it.isNotBlank() }
        val options = projectNames + listOf(
            "＋ Añadir proyecto o enlace",
            "＋ Guardar un archivo del teléfono",
            "▤ Mis archivos guardados (${savedFiles.size})"
        )
        AlertDialog.Builder(this).setTitle("Tu espacio")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    projectNames.size -> addProjectLink()
                    projectNames.size + 1 -> openFiles()
                    projectNames.size + 2 -> openSavedFiles()
                    else -> {
                        val name = projectNames[which]
                        prefs.edit().putString("last_project", name).apply()
                        launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse(projects[name])), "No se pudo abrir el enlace.")
                    }
                }
            }.setNegativeButton("Cerrar", null).show()
    }

    private fun addProjectLink() {
        val nameInput = android.widget.EditText(this).apply { hint = "Nombre del proyecto" }
        val urlInput = android.widget.EditText(this).apply { hint = "Enlace https://..." }
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(36, 8, 36, 0)
            addView(nameInput)
            addView(urlInput)
        }
        AlertDialog.Builder(this).setTitle("Añadir proyecto").setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val name = nameInput.text.toString().trim()
                val url = urlInput.text.toString().trim()
                if (name.isNotBlank() && url.startsWith("https://")) {
                    val prefs = getSharedPreferences("fortuneos", MODE_PRIVATE)
                    val old = prefs.getString("projects", "").orEmpty().lines().filter { it.contains("|") && !it.startsWith("$name|") }
                    prefs.edit().putString("projects", (old + "$name|$url").joinToString("\n")).apply()
                    Toast.makeText(this, "Proyecto guardado", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(this, "Revisa el nombre y el enlace https.", Toast.LENGTH_LONG).show()
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun openFiles() {
        filePicker.launch(arrayOf("*/*"))
    }

    private fun openSavedFiles() {
        val prefs = getSharedPreferences("fortuneos", MODE_PRIVATE)
        val files = prefs.getString("saved_files", "").orEmpty().lines().filter { it.isNotBlank() }
        if (files.isEmpty()) {
            Toast.makeText(this, "Todavía no has guardado archivos.", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = files.mapIndexed { index, value ->
            val uri = Uri.parse(value)
            val rawName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
            "${index + 1}. ${rawName?.takeIf { it.isNotBlank() } ?: "Archivo guardado"}"
        }
        AlertDialog.Builder(this).setTitle("Mis archivos")
            .setItems(labels.toTypedArray()) { _, which ->
                val uri = Uri.parse(files[which])
                launchIntent(Intent(Intent.ACTION_VIEW).setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "No se pudo abrir el archivo.")
            }
            .setPositiveButton("Quitar referencia") { _, _ ->
                AlertDialog.Builder(this).setTitle("¿Qué archivo quieres quitar?")
                    .setItems(labels.toTypedArray()) { _, which ->
                        val updated = files.filterIndexed { index, _ -> index != which }
                        prefs.edit().putString("saved_files", updated.joinToString("\\n")).apply()
                        Toast.makeText(this, "Referencia eliminada; el archivo original no se borra.", Toast.LENGTH_LONG).show()
                    }
                    .setNegativeButton("Cancelar", null).show()
            }
            .setNeutralButton("Añadir archivo") { _, _ -> openFiles() }
            .setNegativeButton("Cerrar", null).show()
    }

    private fun showAppDrawer() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
             .sortedWith(compareByDescending<android.content.pm.ResolveInfo> { getSharedPreferences("fortuneos_usage", MODE_PRIVATE).getInt("use_" + it.activityInfo.packageName, 0) }.thenBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) })
        if (apps.isEmpty()) {
            Toast.makeText(this, "No se encontraron aplicaciones.", Toast.LENGTH_SHORT).show()
            return
        }
        val search = android.widget.EditText(this).apply {
            hint = "Buscar aplicación"
            setSingleLine(true)
            setPadding(28, 18, 28, 18)
            setTextColor(Color.WHITE)
        }
        val list = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL }
        val scroll = android.widget.ScrollView(this).apply { addView(list) }
        val content = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 4, 24, 8)
            addView(search)
            addView(scroll, android.widget.LinearLayout.LayoutParams(-1, 0, 1f))
        }
        val dialog = AlertDialog.Builder(this).setTitle("Aplicaciones").setView(content)
            .setNegativeButton("Cerrar", null).create()
        fun render(filter: String) {
            list.removeAllViews()
            apps.filter { it.loadLabel(packageManager).toString().contains(filter, true) }.forEach { item ->
                val row = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(12, 12, 12, 12)
                }
                val icon = android.widget.ImageView(this).apply {
                    setImageDrawable(item.loadIcon(packageManager))
                    layoutParams = android.widget.LinearLayout.LayoutParams(72, 72)
                }
                val label = android.widget.TextView(this).apply {
                    text = item.loadLabel(packageManager).toString()
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    setPadding(20, 0, 0, 0)
                }
                row.addView(icon)
                row.addView(label, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
                row.setOnClickListener {
                    val usage = getSharedPreferences("fortuneos_usage", MODE_PRIVATE)
                    val key = "use_" + item.activityInfo.packageName
                    usage.edit().putInt(key, usage.getInt(key, 0) + 1).apply()
                    val launch = packageManager.getLaunchIntentForPackage(item.activityInfo.packageName)
                    if (launch != null) launchIntent(launch, "No se pudo abrir esa aplicación.")
                    dialog.dismiss()
                }
                list.addView(row)
            }
        }
        render("")
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        dialog.setOnShowListener { dialog.window?.setBackgroundDrawableResource(android.R.color.black) }
        dialog.show()
    }
}

class FortuneView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = ArrayList<Particle>()
    @Volatile private var running = false
    private var widthF = 0f
    private var heightF = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var batteryLevel = 0
    private val cyan = Color.rgb(196, 180, 137)
    private val purple = Color.rgb(105, 132, 119)
    private val shortcuts = arrayOf("PHONE", "CHAT", "WEB", "MUSIC", "APPS")
    private var downX = 0f
    private var downY = 0f

    init {
        repeat(22) { particles.add(Particle()) }
        updateBattery()
        isClickable = true
        contentDescription = "FortuneOS. Desliza hacia arriba para abrir las aplicaciones."
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        widthF = w.toFloat()
        heightF = h.toFloat()
        centerX = widthF / 2f
        centerY = heightF * 0.43f
        particles.forEach { it.randomize(widthF, heightF) }
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
        if (running) postInvalidateOnAnimation()
    }

    private fun drawBackground(canvas: Canvas) {
        paint.shader = RadialGradient(
            centerX, centerY, maxOf(widthF, heightF) * 0.75f,
            intArrayOf(Color.rgb(35, 43, 38), Color.rgb(20, 25, 23), Color.rgb(13, 16, 15)),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, widthF, heightF, paint)
        paint.shader = null
    }

    private fun drawParticles(canvas: Canvas) {
        particles.forEach { p ->
            p.update()
            paint.color = Color.argb((28 + 40 * p.brightness).toInt(), 202, 190, 157)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
    }

    private fun drawConnections(canvas: Canvas) {
        val maxDistance = 150f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        for (i in particles.indices) {
            val a = particles[i]
            for (j in i + 1 until particles.size) {
                val b = particles[j]
                val dx = a.x - b.x
                val dy = a.y - b.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                if (distance < maxDistance) {
                    paint.color = Color.argb(((1f - distance / maxDistance) * 13f).toInt(), 190, 178, 145)
                    canvas.drawLine(a.x, a.y, b.x, b.y, paint)
                }
            }
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawCore(canvas: Canvas) {
        val radius = min(widthF, heightF) * 0.145f
        glowPaint.shader = RadialGradient(
            centerX, centerY, radius * 2.2f,
            intArrayOf(Color.argb(38, 196, 180, 137), Color.argb(22, 105, 132, 119), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, radius * 2.2f, glowPaint)
        glowPaint.shader = null

        paint.shader = LinearGradient(centerX - radius, centerY - radius, centerX + radius, centerY + radius, Color.rgb(71, 83, 72), Color.rgb(32, 42, 36), Shader.TileMode.CLAMP)
        canvas.drawCircle(centerX, centerY, radius, paint)
        paint.shader = null
        paint.color = Color.BLACK
        canvas.drawCircle(centerX, centerY, radius * 0.82f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.argb(180, 196, 180, 137)
        canvas.drawCircle(centerX, centerY, radius * 0.92f, paint)
        paint.style = Paint.Style.FILL

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textPaint.textSize = radius * 0.8f
        textPaint.color = Color.WHITE
        canvas.drawText("F", centerX, centerY + radius * 0.25f, textPaint)
        textPaint.textSize = radius * 0.10f
        textPaint.color = cyan
        canvas.drawText("YOUR SPACE", centerX, centerY + radius * 0.55f, textPaint)
    }

    private fun drawClock(canvas: Canvas) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textPaint.textSize = 50f
        textPaint.color = Color.WHITE
        canvas.drawText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), centerX, centerY - 150f, textPaint)
        textPaint.textSize = 12f
        textPaint.color = Color.argb(185, 190, 190, 210)
        val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
        canvas.drawText(date.uppercase(Locale.getDefault()), centerX, centerY - 124f, textPaint)
    }

    private fun drawStatus(canvas: Canvas) {
        updateBattery()
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textPaint.color = Color.argb(200, 210, 210, 230)
        canvas.drawText("FORTUNEOS", 24f, 48f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("$batteryLevel% BATTERY", widthF - 24f, 48f, textPaint)
        paint.color = cyan
        canvas.drawCircle(widthF - 112f, 44f, 2.5f, paint)
    }

    private fun drawApps(canvas: Canvas) {
        val y = heightF - 126f
        val spacing = widthF / 6f
        for (i in shortcuts.indices) {
            val x = spacing * (i + 1)
            glowPaint.color = Color.argb(18, 196, 180, 137)
            canvas.drawCircle(x, y, 32f, glowPaint)
            paint.color = Color.rgb(35, 43, 38)
            canvas.drawCircle(x, y, 27f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.3f
            paint.color = Color.argb(175, 120, 139, 119)
            canvas.drawCircle(x, y, 27f, paint)
            paint.style = Paint.Style.FILL
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = if (shortcuts[i].length > 4) 8f else 9f
            textPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textPaint.color = Color.WHITE
            canvas.drawText(shortcuts[i], x, y + 4f, textPaint)
        }
    }

    private fun drawGesture(canvas: Canvas) {
        paint.color = Color.argb(150, 196, 180, 137)
        canvas.drawRoundRect(centerX - 32f, heightF - 42f, centerX + 32f, heightF - 38f, 4f, 4f, paint)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 9f
        textPaint.color = Color.argb(150, 180, 180, 200)
        canvas.drawText("SWIPE UP · ALL APPS", centerX, heightF - 54f, textPaint)
    }

    private fun updateBattery() {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val value = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        batteryLevel = if (value in 0..100) value else 0
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (downY - event.y > 70f && kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                    (context as? MainActivity)?.onShortcut(4)
                    return true
                }
                if (kotlin.math.abs(dx) < 35f && kotlin.math.abs(dy) < 35f) {
                    val y = heightF - 126f
                    if (event.y in (y - 50f)..(y + 50f)) {
                        val spacing = widthF / 6f
                        val index = ((event.x / spacing).toInt() - 1)
                        if (index in shortcuts.indices) (context as? MainActivity)?.onShortcut(index)
                    } else if (event.y in (centerY - 100f)..(centerY + 100f) &&
                        event.x in (centerX - 100f)..(centerX + 100f)) {
                        (context as? MainActivity)?.openPersonalSpace()
                    }
                }
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun startAnimation() {
        if (!running) {
            running = true
            invalidate()
        }
    }

    fun stopAnimation() {
        running = false
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
            vx = (Random.nextFloat() - 0.5f) * 0.18f
            vy = (Random.nextFloat() - 0.5f) * 0.18f
            size = 0.8f + Random.nextFloat() * 2.2f
            brightness = 0.3f + Random.nextFloat() * 0.7f
        }

        fun update() {
            x += vx
            y += vy
            time += 0.01f
            brightness = 0.45f + sin(time) * 0.35f
            if (x < -10f) x = widthF + 10f
            if (x > widthF + 10f) x = -10f
            if (y < -10f) y = heightF + 10f
            if (y > heightF + 10f) y = -10f
        }
    }
}