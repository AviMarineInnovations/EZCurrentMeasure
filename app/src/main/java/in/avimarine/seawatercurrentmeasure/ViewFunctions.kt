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
    lat_tv: TextView,
    lon_tv: TextView,
    time_tv: TextView,
    acc_tv: TextView? = null,
    empty: Boolean = false
) {
    if (empty) {
        lat_tv.text = "?"
        lon_tv.text = "?"
        time_tv.text = "?"
        if (acc_tv != null)
            acc_tv.text = "?"
    } else {
        lat_tv.text = String.format("%.6f", loc.latitude)
        lon_tv.text = String.format("%.6f", loc.longitude)
        time_tv.text = timeStamptoDateString(loc.time)
        if (acc_tv != null)
            acc_tv.text = String.format("%.1f m", loc.accuracy)
    }
}