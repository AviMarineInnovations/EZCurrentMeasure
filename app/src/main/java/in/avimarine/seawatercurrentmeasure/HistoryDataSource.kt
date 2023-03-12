package `in`.avimarine.seawatercurrentmeasure

import android.content.Context
import android.location.Location
import `in`.avimarine.androidutils.geo.Speed
import `in`.avimarine.androidutils.units.SpeedUnits
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 28/12/2019.
 */
internal object HistoryDataSource {
    private const val MAX_HISTORY = 10
    /**
     * Speed in knots, dir in degrees
     */
    fun addHistory(loc1:Location ,loc2:Location, spd: Speed, dir: Double, context: Context){
        val m : HashMap<Any?, Any?> = HashMap ()
        m.put("Lon1", loc1.longitude)
        m.put("Lat1", loc1.latitude)
        m.put("Lon2", loc2.longitude)
        m.put("Lat2", loc2.latitude)
        m.put("time1", loc1.time)
        m.put("time2", loc2.time)
        m.put("speed",spd)
        m.put("direction",dir)
        val o = JSONObject(m)
        Preferences.addHistory(o,context,MAX_HISTORY)
    }

    fun getHistoryList(context: Context): List<History>{
        val ret = ArrayList<History>()
        try {
            val ja = JSONArray(Preferences.getHistory(context))
            for (i in 0 until ja.length()) {
                val entry =  ja.getJSONObject(i)
                val l1 = Location("")
                l1.longitude = entry.getDouble("Lon1")
                l1.latitude = entry.getDouble("Lat1")
                l1.time = entry.getLong("time1")
                val l2 = Location("")
                l2.longitude = entry.getDouble("Lon2")
                l2.latitude = entry.getDouble("Lat2")
                l2.time = entry.getLong("time2")
                val speed = when (val spd = entry.get("speed")) {//Backward compatibility (was once saved only as m per min)
                    is Double -> {
                        Speed(spd.toDouble(), SpeedUnits.MetersPerMinute)
                    }
                    is Int -> {
                        Speed(spd.toDouble(), SpeedUnits.MetersPerMinute)
                    }
                    else -> {
                        spd as Speed
                    }
                }
                val h = History(l1,l2, speed, entry.getDouble("direction"))
                ret.add(h)
            }
            return ret
        } catch (e: JSONException) {
            return ret
        }
    }
}