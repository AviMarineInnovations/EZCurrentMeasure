package `in`.avimarine.seawatercurrentmeasure

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONException

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 27/12/2019.
 */
internal object Preferences {
    fun getPreferences(context: Context): Triple<Boolean, Boolean, String> {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        var magnetic = false
        var fromNotation = false
        var speedUnit : String = "m_per_min"
        if (sharedPreferences != null) {
            magnetic = sharedPreferences.getBoolean("magnetic", false)
            fromNotation = sharedPreferences.getBoolean("from_notation", false)
            speedUnit = sharedPreferences.getString("speed_unit", "m_per_min").toString()
        }
        return Triple(magnetic, fromNotation, speedUnit)
    }

    fun getDelayedStartInterval(context: Context): Long {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        if (sharedPreferences != null) {
            return (sharedPreferences.getString("delayed_start", "0")?.toLong() ?: 0) * 1000
        }
        return 0
    }

    fun getAutoFinishInterval(context: Context): Long {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        if (sharedPreferences != null) {
            return (sharedPreferences.getString("auto_finish", "0")?.toLong() ?: 0) * 1000
        }
        return 0
    }

    fun addHistory (entry: String, context: Context){
        val history = getHistory(context)
        val historyJson: JSONArray = try { JSONArray(history) } catch (e: JSONException) { JSONArray() }
        historyJson.put(entry)
        saveHistory(historyJson.toString(),context)
    }
    fun saveHistory(history: String, context: Context){
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putString("history",history)
        editor.apply()
    }

    fun getHistory(context: Context): String {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        if (sharedPreferences != null) {
            return sharedPreferences.getString("history", "")?: ""
        }
        return ""
    }
}