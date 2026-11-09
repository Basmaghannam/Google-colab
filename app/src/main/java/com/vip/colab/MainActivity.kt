package com.vip.colab

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var toggle: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun button(label: String, color: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.textSize = 16f
        b.setTextColor(Color.WHITE)
        b.setBackgroundColor(Color.parseColor(color))
        b.setOnClickListener { onClick() }
        return b
    }

    private fun add(root: LinearLayout, v: android.view.View, top: Int = 12, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) {
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        lp.topMargin = dp(top)
        root.addView(v, lp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(dp(24), dp(56), dp(24), dp(24))
        root.setBackgroundColor(Color.parseColor("#0E0E12"))

        val title = TextView(this)
        title.text = "Colab"
        title.textSize = 30f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        add(root, title, 0)

        status = TextView(this)
        status.textSize = 16f
        status.gravity = Gravity.CENTER
        add(root, status, 8)

        add(root, button("فتح Colab في Chrome", "#8E7CFF") { openColab() }, 32, dp(56))
        toggle = button("", "#2A2A36") { toggleKeepAlive() }
        add(root, toggle, 12, dp(56))
        add(root, button("إعفاء التطبيق من توفير البطارية", "#2A2A36") { batteryExempt() }, 24, dp(48))
        add(root, button("إعدادات بطارية Chrome", "#2A2A36") { chromeSettings() }, 12, dp(48))

        val hint = TextView(this)
        hint.text = "اترك Chrome مفتوحًا على دفترك ولا تمسحه من التطبيقات الحديثة.\n" +
            "Keep Alive يبقي الجهاز والواي فاي نشطين، ولا يتحكم في Chrome نفسه."
        hint.textSize = 13f
        hint.setTextColor(Color.parseColor("#9A9AA8"))
        hint.gravity = Gravity.CENTER
        add(root, hint, 24)

        setContentView(root)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh(KeepAliveService.running)
    }

    private fun refresh(on: Boolean) {
        status.text = if (on) "Keep Alive: يعمل ✅" else "Keep Alive: متوقف"
        status.setTextColor(Color.parseColor(if (on) "#66BB6A" else "#9A9AA8"))
        toggle.text = if (on) "إيقاف Keep Alive" else "تشغيل Keep Alive"
        toggle.setBackgroundColor(Color.parseColor(if (on) "#2E7D32" else "#2A2A36"))
    }

    private fun toggleKeepAlive() {
        if (KeepAliveService.running) {
            startService(Intent(this, KeepAliveService::class.java).setAction(KeepAliveService.ACTION_STOP))
            refresh(false)
        } else {
            startForegroundService(Intent(this, KeepAliveService::class.java))
            refresh(true)
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) batteryExempt()
        }
    }

    private fun openColab() {
        val uri = Uri.parse("https://colab.research.google.com/")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.chrome"))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e2: Exception) {
                Toast.makeText(this, "لا يوجد متصفح", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun batteryExempt() {
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
            Toast.makeText(this, "تعذّر فتح الإعداد", Toast.LENGTH_SHORT).show()
        }
    }

    private fun chromeSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.android.chrome"))
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Chrome غير مثبت", Toast.LENGTH_SHORT).show()
        }
    }
}
