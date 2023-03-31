package `in`.avimarine.seawatercurrentmeasure.ui

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import `in`.avimarine.androidutils.getGPSReceptionDrawable


class GPSViewModel(
    private val location: Location,


    ) : ViewModel() {

    fun getGPSAccuracy(): Int {
        return getGPSReceptionDrawable(location)
    }
}