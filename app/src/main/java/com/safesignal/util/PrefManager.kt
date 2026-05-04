package com.safesignal.util

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("safesignal_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ROLE        = "role"        // "protected" | "guardian"
        private const val KEY_PAIR_CODE   = "pair_code"
        private const val KEY_MY_NUMBER   = "my_number"
        private const val KEY_PARTNER_NUM = "partner_number"
        private const val KEY_SETUP_DONE  = "setup_done"
    }

    var role: String
        get() = prefs.getString(KEY_ROLE, "") ?: ""
        set(v) = prefs.edit().putString(KEY_ROLE, v).apply()

    var pairCode: String
        get() = prefs.getString(KEY_PAIR_CODE, "") ?: ""
        set(v) = prefs.edit().putString(KEY_PAIR_CODE, v).apply()

    var myNumber: String
        get() = prefs.getString(KEY_MY_NUMBER, "") ?: ""
        set(v) = prefs.edit().putString(KEY_MY_NUMBER, v).apply()

    var partnerNumber: String
        get() = prefs.getString(KEY_PARTNER_NUM, "") ?: ""
        set(v) = prefs.edit().putString(KEY_PARTNER_NUM, v).apply()

    var setupDone: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        set(v) = prefs.edit().putBoolean(KEY_SETUP_DONE, v).apply()

    fun clear() = prefs.edit().clear().apply()
}
