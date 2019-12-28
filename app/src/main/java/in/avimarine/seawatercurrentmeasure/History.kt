package `in`.avimarine.seawatercurrentmeasure

import android.content.Context
import android.location.Location
import org.json.JSONObject

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 28/12/2019.
 */
internal object History {
    val MAX_HISTORY = 10
    /**
     * Speed in metres per minute, dir on degrees
     */
    fun addHistory(loc1:Location ,loc2:Location, spd: Double, dir: Double, context: Context){
        var m : HashMap<Any?, Any?> = HashMap ()
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
}