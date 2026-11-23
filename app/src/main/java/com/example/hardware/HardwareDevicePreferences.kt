package com.example.hardware

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

data class ScannerSettings(
    val preferredMode: String = "AUTO", // AUTO (Kamera + Scanner Fisik), KAMERA, HARDWARE
    val selectedScannerId: String = "",
    val selectedScannerName: String = "",
    val continuousMode: Boolean = true,
    val beepOnScan: Boolean = true,
    val vibrateOnScan: Boolean = true,
    val defaultTorchOn: Boolean = false,
    val scanCooldownMs: Long = 1200L,
    val autoCaptureHardwareInput: Boolean = true,
    val stripWhitespace: Boolean = true
)

class HardwareDevicePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "kaslite_hardware_prefs",
        Context.MODE_PRIVATE
    )

    fun loadScannerSettings(): ScannerSettings {
        return ScannerSettings(
            preferredMode = prefs.getString(KEY_SCANNER_MODE, "AUTO") ?: "AUTO",
            selectedScannerId = prefs.getString(KEY_SELECTED_SCANNER_ID, "") ?: "",
            selectedScannerName = prefs.getString(KEY_SELECTED_SCANNER_NAME, "") ?: "",
            continuousMode = prefs.getBoolean(KEY_CONTINUOUS_MODE, true),
            beepOnScan = prefs.getBoolean(KEY_BEEP_ON_SCAN, true),
            vibrateOnScan = prefs.getBoolean(KEY_VIBRATE_ON_SCAN, true),
            defaultTorchOn = prefs.getBoolean(KEY_DEFAULT_TORCH_ON, false),
            scanCooldownMs = prefs.getLong(KEY_SCAN_COOLDOWN_MS, 1200L).coerceIn(300L, 4000L),
            autoCaptureHardwareInput = prefs.getBoolean(KEY_AUTO_CAPTURE_HID, true),
            stripWhitespace = prefs.getBoolean(KEY_STRIP_WHITESPACE, true)
        )
    }

    fun saveScannerSettings(settings: ScannerSettings) {
        prefs.edit()
            .putString(KEY_SCANNER_MODE, settings.preferredMode)
            .putString(KEY_SELECTED_SCANNER_ID, settings.selectedScannerId)
            .putString(KEY_SELECTED_SCANNER_NAME, settings.selectedScannerName)
            .putBoolean(KEY_CONTINUOUS_MODE, settings.continuousMode)
            .putBoolean(KEY_BEEP_ON_SCAN, settings.beepOnScan)
            .putBoolean(KEY_VIBRATE_ON_SCAN, settings.vibrateOnScan)
            .putBoolean(KEY_DEFAULT_TORCH_ON, settings.defaultTorchOn)
            .putLong(KEY_SCAN_COOLDOWN_MS, settings.scanCooldownMs.coerceIn(300L, 4000L))
            .putBoolean(KEY_AUTO_CAPTURE_HID, settings.autoCaptureHardwareInput)
            .putBoolean(KEY_STRIP_WHITESPACE, settings.stripWhitespace)
            .apply()
    }

    fun getPreferredPrinterId(): Long = prefs.getLong(KEY_PREFERRED_PRINTER_ID, 0L)

    fun setPreferredPrinterId(printerId: Long) {
        prefs.edit().putLong(KEY_PREFERRED_PRINTER_ID, printerId).apply()
    }

    companion object {
        private const val KEY_SCANNER_MODE = "scanner_preferred_mode"
        private const val KEY_SELECTED_SCANNER_ID = "selected_scanner_id"
        private const val KEY_SELECTED_SCANNER_NAME = "selected_scanner_name"
        private const val KEY_CONTINUOUS_MODE = "scanner_continuous_mode"
        private const val KEY_BEEP_ON_SCAN = "scanner_beep_on_scan"
        private const val KEY_VIBRATE_ON_SCAN = "scanner_vibrate_on_scan"
        private const val KEY_DEFAULT_TORCH_ON = "scanner_default_torch"
        private const val KEY_SCAN_COOLDOWN_MS = "scanner_cooldown_ms"
        private const val KEY_AUTO_CAPTURE_HID = "scanner_auto_capture_hid"
        private const val KEY_STRIP_WHITESPACE = "scanner_strip_whitespace"
        private const val KEY_PREFERRED_PRINTER_ID = "preferred_printer_id"
    }
}

object ScannerFeedbackPlayer {
    fun playSuccess(context: Context, enableBeep: Boolean, enableVibrate: Boolean) {
        if (enableBeep) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 110)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    runCatching { tone.release() }
                }, 180L)
            }
        }
        if (enableVibrate) {
            vibrateShort(context, 45L)
        }
    }

    fun playWarning(context: Context, enableBeep: Boolean, enableVibrate: Boolean) {
        if (enableBeep) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
                tone.startTone(ToneGenerator.TONE_PROP_NACK, 180)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    runCatching { tone.release() }
                }, 260L)
            }
        }
        if (enableVibrate) {
            vibrateShort(context, 90L)
        }
    }

    private fun vibrateShort(context: Context, durationMs: Long) {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        }
    }
}
