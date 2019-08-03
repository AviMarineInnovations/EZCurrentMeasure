package `in`.avimarine.seawatercurrentmeasure

import android.hardware.GeomagneticField
import android.location.Location
import android.util.Log

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 03/08/2019.
 */

private val TAG = "Calculations"

fun getDirString(dir: Double, magnetic: Boolean, fromNotation: Boolean, location: Location, time: Long): String {
    var calcDir = dir
    if (magnetic) {
        val geomagneticField = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitude.toFloat(),
            time
        )
        Log.d(TAG, "Declination is: " + geomagneticField.declination)
        calcDir += geomagneticField.declination
    }
    if (fromNotation) calcDir = calcDir - 180
    if (calcDir < 0) calcDir = 360 + calcDir
    return String.format("%03d", calcDir.toInt()) + if (magnetic) " M" else ""
}

fun getSpeedString(firstTime: Long, secondTime: Long, dist: Double, units: String = "m_per_min"): String {
    val speed = getSpeed(dist, firstTime, secondTime)
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
        return (if (speed < 10) String.format("%.1f", speed) else String.format("%.0f", speed)) + " m/min"
    }
}

fun toKnots(speed: Double): Double {
    return speed * 0.0323974
}

fun toMPerSec(speed: Double): Double {
    return speed / 60
}

fun getSpeed(dist: Double, firstTime: Long, secondTime: Long, units: String = "m_per_min"): Double {
    if (units == "m_per_sec") {
        val duration = (secondTime - firstTime).toDouble() / (1000)
        return dist / duration
    } else if (units == "knots") {
        val duration = (secondTime - firstTime).toDouble() / (1000 * 3600)
        return (dist * 0.000539957) / duration
    } else {
        val duration = (secondTime - firstTime).toDouble() / (1000 * 60)
        return dist / duration
    }
}