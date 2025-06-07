package com.example.run_app_rma.services

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.run_app_rma.MainActivity
import com.example.run_app_rma.R

object ShortcutManager {

    private const val SHORTCUT_ID_START_RUN = "start_run"
    private const val SHORTCUT_ID_STOP_RUN = "stop_run"

    fun updateShortcuts(context: Context, isTracking: Boolean) {
        val shortcut = if (isTracking) {
            createStopRunShortcut(context)
        } else {
            createStartRunShortcut(context)
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, listOf(shortcut))
    }

    private fun createStartRunShortcut(context: Context): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_START_RUN"
        }
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID_START_RUN)
            .setShortLabel("Start Run")
            .setLongLabel("Start a new run")
            .setIcon(IconCompat.createWithResource(context, R.drawable.baseline_arrow_right_24))
            .setIntent(intent)
            .build()
    }

    private fun createStopRunShortcut(context: Context): ShortcutInfoCompat {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_STOP_RUN"
        }
        return ShortcutInfoCompat.Builder(context, SHORTCUT_ID_STOP_RUN)
            .setShortLabel("Stop Run")
            .setLongLabel("Stop the current run")
            .setIcon(IconCompat.createWithResource(context, R.drawable.baseline_crop_square_24))
            .setIntent(intent)
            .build()
    }
} 