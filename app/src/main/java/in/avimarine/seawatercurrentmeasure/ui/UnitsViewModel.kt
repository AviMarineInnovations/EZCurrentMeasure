package `in`.avimarine.seawatercurrentmeasure.ui

import androidx.lifecycle.ViewModel
import `in`.avimarine.androidutils.units.SpeedUnits


class UnitsViewModel(
    private val speedUnits: SpeedUnits,
    private val magnetic: Boolean
) : ViewModel() {

    fun getSpeedUnits(): String {
        return SpeedUnits.getSpeedUnitShortDisplayName(speedUnits)
    }

    fun getDirUnits(): String {
        return if (magnetic) {
            "\u00B0 M"
        } else {
            "\u00B0"
        }
    }
}