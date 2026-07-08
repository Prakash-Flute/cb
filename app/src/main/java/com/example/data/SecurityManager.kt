package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)

    var password: String?
        get() = prefs.getString("password", null)
        set(value) = prefs.edit().putString("password", value).apply()

    var phoneNumber: String?
        get() = prefs.getString("phone_number", null)
        set(value) = prefs.edit().putString("phone_number", value).apply()

    var lastUnlockedTime: Long
        get() = prefs.getLong("last_unlocked_time", 0L)
        set(value) = prefs.edit().putLong("last_unlocked_time", value).apply()

    var rememberMe: Boolean
        get() = prefs.getBoolean("remember_me", false)
        set(value) = prefs.edit().putBoolean("remember_me", value).apply()

    fun isLocked(): Boolean {
        if (password == null) return false
        if (!rememberMe) return true
        val twelveHoursMillis = 12 * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastUnlockedTime > twelveHoursMillis
    }

    fun unlock() {
        lastUnlockedTime = System.currentTimeMillis()
    }
}
