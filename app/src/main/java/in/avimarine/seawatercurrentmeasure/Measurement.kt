package `in`.avimarine.seawatercurrentmeasure

import android.location.Location
import `in`.avimarine.androidutils.*
import `in`.avimarine.androidutils.geo.Direction
import `in`.avimarine.androidutils.geo.Speed
import `in`.avimarine.androidutils.units.SpeedUnits
import kotlin.math.floor

data class Measurement(
    val loc1: Location,
    val loc2: Location,
    val spd: Speed,
    val dir: Direction,
    val spdError: Speed = Speed(0.0, SpeedUnits.Knots),
    val dirError: Double = 0.0) {
    constructor(
        loc1: Location,
        loc2: Location):
            this(loc1,
                loc2,
                getSpeed(loc1, loc2),
                getDirection(loc1, loc2),
                getSpeedError(loc1, loc2),
                getDirError(loc1, loc2)
            )

    fun isError(): Boolean{
        return spd.isNaN()
    }

    fun isCurrentSpeedValid(): Boolean{
        return spd.getValue(SpeedUnits.Knots) <= 30
    }
}