package com.kimoxcallsecurity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.FileProvider

class KimoxRecoveryActivity : Activity() {
    private lateinit var logView: TextView
    private lateinit var nativeLogView: TextView
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val autoRefresh = object : Runnable {
        override fun run() {
            refresh()
            refreshHandler.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KimoxDiagnostics.record(this, "INFO", "RECOVERY_OPENED")
        setContentView(buildContent())
        refreshHandler.post(autoRefresh)
    }

    override fun onDestroy() {
        refreshHandler.removeCallbacks(autoRefresh)
        super.onDestroy()
    }

    private fun buildContent(): ScrollView {
        val density = resources.displayMetrics.density
        val padding = (22 * density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.rgb(247, 249, 252))
        }
        container.addView(text("Kimox Recovery", 27f, true, Color.rgb(9, 26, 51)))
        container.addView(text("Esta herramienta funciona sin React Native y permite recuperar el diagnóstico de un cierre al iniciar.", 14f, false, Color.DKGRAY))
        logView = text(KimoxDiagnostics.latestText(this), 11f, false, Color.rgb(30, 42, 58)).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(padding / 2, padding / 2, padding / 2, padding / 2)
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
        }
        container.addView(logView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (280 * density).toInt()).apply { topMargin = padding })
        container.addView(text("Registro nativo del último intento", 16f, true, Color.rgb(9, 26, 51)))
        nativeLogView = text(KimoxDiagnostics.latestNativeText(this), 10f, false, Color.rgb(30, 42, 58)).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(padding / 2, padding / 2, padding / 2, padding / 2)
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
        }
        container.addView(nativeLogView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (280 * density).toInt()).apply { topMargin = padding / 2 })
        container.addView(button("Actualizar diagnóstico") { refresh() })
        container.addView(button("Compartir diagnóstico TXT") { shareDiagnostics() })
        container.addView(button("Compartir monitor nativo TXT") { shareNativeDiagnostics() })
        container.addView(button("Intentar abrir Kimox") { retryKimox() })
        container.addView(button("Abrir ajustes de Android") { openSettings() })
        container.addView(button("Restablecer datos protegidos", destructive = true) { confirmReset() })
        container.addView(button("Borrar registros", destructive = true) {
            KimoxDiagnostics.clear(this)
            refresh()
        })
        return ScrollView(this).apply { addView(container) }
    }

    private fun text(value: String, size: Float, bold: Boolean, color: Int) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 8, 0, 8)
    }

    private fun button(label: String, destructive: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        if (destructive) setTextColor(Color.rgb(185, 35, 50))
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 10 }
    }

    private fun refresh() {
        logView.text = KimoxDiagnostics.latestText(this)
        nativeLogView.text = KimoxDiagnostics.latestNativeText(this)
    }

    private fun shareDiagnostics() {
        KimoxDiagnostics.record(this, "INFO", "DIAGNOSTICS_SHARED")
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", KimoxDiagnostics.completeReport(this))
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Kimox Call Security diagnostics")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Compartir diagnóstico"))
    }

    private fun shareNativeDiagnostics() {
        KimoxDiagnostics.record(this, "INFO", "NATIVE_DIAGNOSTICS_SHARED")
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            KimoxDiagnostics.nativeLog(this),
        )
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Kimox Call Security native monitor")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Compartir monitor nativo"))
    }

    private fun retryKimox() {
        KimoxDiagnostics.record(this, "INFO", "RECOVERY_RETRY_REQUESTED")
        KimoxNativeLogCollector.monitorNextLaunch(this)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    private fun openSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Restablecer datos protegidos")
            .setMessage("Se eliminarán reglas, historial y preferencias cifradas. Los registros de diagnóstico se conservarán.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Restablecer") { _, _ -> resetProtectedData() }
            .show()
    }

    private fun resetProtectedData() {
        getSharedPreferences(KimoxSecurityModule.PREFERENCES_NAME, MODE_PRIVATE).edit().clear().commit()
        SecurePreferences.resetEncryptionKey()
        KimoxDiagnostics.record(this, "WARN", "PROTECTED_DATA_RESET")
        refresh()
        AlertDialog.Builder(this).setMessage("Datos protegidos restablecidos. Ya puedes intentar abrir Kimox.").setPositiveButton("Aceptar", null).show()
    }
}
