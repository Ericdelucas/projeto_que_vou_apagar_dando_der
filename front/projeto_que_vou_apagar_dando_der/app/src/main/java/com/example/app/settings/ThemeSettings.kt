package com.example.app.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeSettings {
    val modes = intArrayOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)

    fun selected(context: Context): Int = modes.indexOf(context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
        .getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)).coerceAtLeast(0)

    fun select(context: Context, index: Int) {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit()
            .putInt("night_mode", modes[index]).apply()
        apply(context)
    }

    @JvmStatic fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(modes[selected(context)])
    }
}
