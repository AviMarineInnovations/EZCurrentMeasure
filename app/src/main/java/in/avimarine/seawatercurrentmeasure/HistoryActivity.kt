package `in`.avimarine.seawatercurrentmeasure

import android.location.Location
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.content_history.*
import org.json.JSONArray

class HistoryActivity : AppCompatActivity() {
    val Any.TAG: String
        get() {
            return javaClass.simpleName
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        setSupportActionBar(toolbar)
        historyText.text = try { getFormattedHistory() } catch (e: Exception) { Log.e(TAG, "Error", e)
            "No History  Available"
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun getFormattedHistory():String{
        val (magnetic, fromNotation, speedUnit) = Preferences.getPreferences(this)
        val ja = JSONArray(Preferences.getHistory(this))
        var ret = ""
        for (i in 0 until ja.length()) {
            val entry =  ja.getJSONObject(i)
            val l = Location("")
            l.longitude = entry.getDouble("Lon2")
            l.latitude = entry.getDouble("Lat2")
            val time2 = entry.getLong("time2")
            ret += timeStamptoDateString(time2) + ", " + getDirString(entry.getDouble("direction"),false,fromNotation,l,time2) + "T, " +  getSpeedString(entry.getDouble("speed"),speedUnit) + "\n"
        }
        return ret
    }

}
