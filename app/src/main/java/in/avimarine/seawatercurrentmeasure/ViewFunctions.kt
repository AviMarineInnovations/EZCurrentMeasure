package `in`.avimarine.seawatercurrentmeasure

import android.location.Location
import android.widget.TextView

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 13/09/2019.
 */
fun locationIntoTextViews(
    loc: Location,
    lat_tv: TextView?,
    lon_tv: TextView?,
    time_tv: TextView,
    acc_tv: TextView? = null,
    empty: Boolean = false
) {
    if (empty) {
        lat_tv?.text = "?"
        lon_tv?.text = "?"
        time_tv.text = "?"
        if (acc_tv != null)
            acc_tv.text = "?"
    } else {
        lat_tv?.text = String.format("%.6f", loc.latitude)
        lon_tv?.text = String.format("%.6f", loc.longitude)
        time_tv.text = timeStamptoDateString(loc.time)
        if (acc_tv != null)
            acc_tv.text = String.format("%.1f m", loc.accuracy)
    }
}



fun getSpeedString(
    firstTime: Long,
    secondTime: Long,
    dist: Double,
    units: String = "m_per_min"
): String {
    val speed = getSpeed(dist, firstTime, secondTime)
    return getSpeedString(speed,units)
}

fun getSpeedString(speed: Double, units:String="m_per_minute") : String{
    if (speed > 3000) { //The current is over 95 kts
        return "Error"
    }
    if (units == "m_per_sec") {
        return (if (speed < 10) String.format("%.1f", toMPerSec(speed)) else String.format(
            "%.0f",
            toMPerSec(speed)
        )) + " m/sec"
    } else if (units == "knots") {
        return (if (speed < 10) String.format("%.1f", toKnots(speed)) else String.format(
            "%.0f",
            toKnots(speed)
        )) + " kts"
    } else {
        return (if (speed < 10) String.format("%.1f", speed) else String.format(
            "%.0f",
            speed
        )) + " m/min"
    }
}

fun getTimerString(milliseconds: Long): String {
    val hours = milliseconds / 1000 / 3600
    val minutes = (milliseconds / 1000 / 60) % 60
    val seconds = milliseconds / 1000 % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

fun getLatString(lat: Double): String {
    if (lat>90 || lat<-90)
        return "Error"
    return String.format("%02d", Math.abs(Math.floor(lat)).toInt()) + "\u00B0 " +  String.format("%04.1f", Math.abs(
        lat - Math.floor(lat)
    ) * 60) + "'"+ if (lat>0) "N" else "S"
}
fun getLonString(lon: Double): String {
    if (lon > 180 || lon < -180)
        return "Error"
    return String.format("%03d", Math.abs(Math.floor(lon)).toInt()) + "\u00B0 " + String.format("%04.1f", Math.abs(
        lon - Math.floor(lon)
    ) * 60) + "'"+ if (lon > 0) "E" else "W"
}

fun getShortLocation(location:Location): String{
    return getLatString(location.latitude) + " " + getLonString(location.longitude)
}