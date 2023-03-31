package `in`.avimarine.seawatercurrentmeasure

import android.location.Location
import android.widget.ImageView
import android.widget.TextView
import `in`.avimarine.androidutils.getGPSReceptionDrawable
import `in`.avimarine.androidutils.timeStampToDateString

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 13/09/2019.
 */
fun locationIntoTextViews(
    loc: Location,
    time_tv: TextView?,
    gps: ImageView,
    empty: Boolean = false
) {
    if (empty) {
        time_tv?.text = "?"
        gps.setImageResource(getGPSReceptionDrawable(loc))
    } else {
        time_tv?.text = timeStampToDateString(loc.time)
        gps.setImageResource(R.drawable.gps_reception_0_bars)
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