package `in`.avimarine.seawatercurrentmeasure

import android.content.Context
import android.location.Location
import `in`.avimarine.androidutils.createLocation
import `in`.avimarine.androidutils.geo.Direction
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


    fun addHistory(measurement: Measurement, context: Context) {
        addHistory(measurement.loc1,
                measurement.loc2,
                measurement.spd,
                measurement.dir,
                context)
    }
    /**
     * Speed in knots, dir in degrees
     */
    fun addHistory(loc1: Location, loc2: Location, spd: Speed, dir: Direction, context: Context) {
        val m: HashMap<String, Any> = HashMap()
        m["Lon1"] = loc1.longitude
        m["Lat1"] = loc1.latitude
        m["Lon2"] = loc2.longitude
        m["Lat2"] = loc2.latitude
        m["time1"] = loc1.time
        m["time2"] = loc2.time
        m["speedKnots"] = spd.getValue(SpeedUnits.Knots)
        m["direction"] = dir.getTrueValue()
        val o = JSONObject(m as Map<*, *>)
        Preferences.addHistory(o, context, MAX_HISTORY)
    }

    fun getHistoryList(context: Context): List<Measurement> {
        val ret = ArrayList<Measurement>()
        try {
            val ja = JSONArray(Preferences.getHistory(context))
            for (i in 0 until ja.length()) {
                val entry = ja.getJSONObject(i)
                val l1 = Location("")
                l1.longitude = entry.getDouble("Lon1")
                l1.latitude = entry.getDouble("Lat1")
                l1.time = entry.getLong("time1")
                val l2 = Location("")
                l2.longitude = entry.getDouble("Lon2")
                l2.latitude = entry.getDouble("Lat2")
                l2.time = entry.getLong("time2")
                if (entry.has("speedKnots")) {
                    val spd = entry.getDouble("speedKnots")
                    val h = Measurement(l1, l2, Speed(spd, SpeedUnits.Knots), Direction(entry.getDouble("direction"),
                        l2
                    ))
                    ret.add(h)
                } else {
                    val spd = entry.getDouble("speed")
                    val h = Measurement(l1, l2, Speed(spd, SpeedUnits.MetersPerMinute), Direction(entry.getDouble("direction"),l2))
                    ret.add(h)
                }
            }
            return ret
        } catch (e: JSONException) {
            return ret
        }
    }
}