package `in`.avimarine.seawatercurrentmeasure

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.preference.PreferenceManager
import java.text.DateFormat
import java.util.*

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 14/09/2019.
 */
internal object Utils {
    val Any.TAG: String
        get() {
            return javaClass.simpleName
        }
    val KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates"
    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    fun requestingLocationUpdates(context: Context):Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false)
    }
    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    fun setRequestingLocationUpdates(context:Context, requestingLocationUpdates:Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
            .apply()
    }
    /**
     * Returns the {@code location} object as a human readable string.
     * @param location The {@link Location}.
     */
    fun getLocationText(location: Location?):String {
        return if (location == null)
            "Unknown location"
        else
            "(" + location.getLatitude() + ", " + location.getLongitude() + ")"
    }
    fun getLocationTitle(context:Context, startTime:Long , autoFinishInterval: Long, delayedStartTime:Long, delayedStartInterval:Long):String {
        if (autoFinishInterval - (System.currentTimeMillis() - startTime) < 0){
            return "Auto start in progress..."
        }
        return "Autofinish in: " + getTimerString(autoFinishInterval - (System.currentTimeMillis() - startTime))
    }
}