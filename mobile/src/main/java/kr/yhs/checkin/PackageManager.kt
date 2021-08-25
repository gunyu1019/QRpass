package kr.yhs.checkin

import android.content.Context
import android.content.SharedPreferences


class PackageManager(private val preferencesName: String, private val context: Context) {

    private fun getPreferences(): SharedPreferences {
        return context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    fun setString(key: String, value: String) {
        val prefs = getPreferences()
        val editors = prefs.edit()
        editors.putString(key, value)
        editors.apply()
    }

    fun setBoolean(key: String, value: Boolean) {
        val prefs = getPreferences()
        val editors = prefs.edit()
        editors.putBoolean(key, value)
        editors.apply()
    }

    fun setInt(key: String, value: Int) {
        val prefs = getPreferences()
        val editors = prefs.edit()
        editors.putInt(key, value)
        editors.apply()
    }

    fun getString(key: String): String? {
        val prefs = getPreferences()
        return prefs.getString(key, "")
    }

    fun getBoolean(key: String): Boolean {
        val prefs = getPreferences()
        return prefs.getBoolean(key, false)
    }

    fun getInt(key: String): Int {
        val prefs = getPreferences()
        return prefs.getInt(key, 0)
    }

    fun removeKey(key: String) {
        val prefs = getPreferences()
        val editors = prefs.edit()
        editors.remove(key)
        editors.apply()
    }

    fun clear(context: Context) {
        val prefs = getPreferences()
        val editors = prefs.edit()
        editors.clear()
        editors.apply()
    }
}