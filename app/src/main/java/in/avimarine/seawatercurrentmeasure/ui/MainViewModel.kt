package `in`.avimarine.seawatercurrentmeasure.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import `in`.avimarine.androidutils.*
import `in`.avimarine.androidutils.geo.Speed
import `in`.avimarine.androidutils.units.SpeedUnits
import `in`.avimarine.seawatercurrentmeasure.Measurement
import kotlin.math.floor

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
            measurement.loc2,
            false
        )
    }

    fun getCurrentSpeed(): String{
        if (!measurement.isCurrentSpeedValid()){
            val maxSpeed = Speed(30.0, SpeedUnits.Knots)
            return ">" + floor(maxSpeed.getValue(speedUnit)).toInt().toString()
        }
        return getSpeedString(measurement.spd, speedUnit, false)
    }

    fun getSpeedError() : String {
       return "\u00B1" + getSpeedString(
           measurement.spdError, speedUnit, false
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