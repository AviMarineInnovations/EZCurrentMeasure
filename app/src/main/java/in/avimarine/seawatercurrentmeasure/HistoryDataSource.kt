package `in`.avimarine.seawatercurrentmeasure

import android.content.Context
import android.location.Location
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
        val m: HashMap<String, Any> = HashMap()
        m["Lon1"] = measurement.loc1.longitude
        m["Lat1"] = measurement.loc1.latitude
        m["Lon2"] = measurement.loc2.longitude
        m["Lat2"] = measurement.loc2.latitude
        m["time1"] = measurement.loc1.time
        m["time2"] = measurement.loc2.time
        m["speedKnots"] = measurement.spd.getValue(SpeedUnits.Knots)
        m["direction"] = measurement.dir.getTrueValue()
        m["spdErr"] = measurement.spdError.getValue(SpeedUnits.Knots)
        m["dirErr"] = measurement.dirError
        val o = JSONObject(m as Map<*, *>)
        Preferences.addHistory(o, context, MAX_HISTORY)
    }

    fun getHistoryList(context: Context): List<Measurement> {
        val ret = ArrayList<Measurement>()
        return try {
            val ja = JSONArray(Preferences.getHistory(context))
            for (i in 0 until ja.length()) {
                val entry = ja.getJSONObject(i)
                ret.add(getHistoryItem(entry))
            }
            ret.reversed()
        } catch (e: JSONException) {
            ret
        }
    }

    fun getLastMeasurements(context: Context): Measurement? {
        return try {
            val ja = JSONArray(Preferences.getHistory(context))
            if (ja.length()==0)
                return null
            getHistoryItem(ja.getJSONObject(ja.length()-1))
        } catch (e: JSONException) {
            return null
        }
    }

    private fun getHistoryItem(jo: JSONObject): Measurement {
        val l1 = Location("")
        l1.longitude = jo.getDouble("Lon1")
        l1.latitude = jo.getDouble("Lat1")
        l1.time = jo.getLong("time1")
        val l2 = Location("")
        l2.longitude = jo.getDouble("Lon2")
        l2.latitude = jo.getDouble("Lat2")
        l2.time = jo.getLong("time2")
        var spdError = 0.0
        var dirError = 0.0
        if (jo.has("spdErr")){
            spdError = jo.getDouble("spdErr")
        }
        if (jo.has("dirErr")){
            dirError = jo.getDouble("dirErr")
        }
        return if (jo.has("speedKnots")) {
            val spd = jo.getDouble("speedKnots")
            Measurement(
                l1, l2, Speed(spd, SpeedUnits.Knots), Direction(
                    jo.getDouble("direction"),
                    l2
                ), Speed(spdError, SpeedUnits.Knots), dirError
            )
        } else {
            val spd = jo.getDouble("speed")
            Measurement(
                l1,
                l2,
                Speed(spd, SpeedUnits.MetersPerMinute),
                Direction(jo.getDouble("direction"), l2),
                Speed(spdError, SpeedUnits.Knots), dirError
            )
        }
    }
}