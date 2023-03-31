package `in`.avimarine.seawatercurrentmeasure.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import `in`.avimarine.androidutils.*
import `in`.avimarine.androidutils.units.SpeedUnits
import `in`.avimarine.seawatercurrentmeasure.Measurement

class MainViewModel(
    private val measurement: Measurement,
    private val magnetic: Boolean,
    private val fromNotation: Boolean,
    private val speedUnit: SpeedUnits
) : ViewModel() {

    fun getCurrentDirection() : String {
        return getDirString(measurement.dir,
            magnetic,
            fromNotation,
            measurement.loc2
        )
    }

    fun getCurrentSpeed(): String{
        return getSpeedString(measurement.spd, speedUnit)
    }

    fun getSpeedError() : String {
       return "\u00B1" + getSpeedString(
           measurement.spdError, speedUnit
       )
    }

    fun getDirectionError() : String {
        return "\u00B1" + getDirErrorString(
            measurement.dirError
        )
    }

    fun getMeasurementEndTime() : String {
        return timeStampToDateString(measurement.loc2.time)
    }

}