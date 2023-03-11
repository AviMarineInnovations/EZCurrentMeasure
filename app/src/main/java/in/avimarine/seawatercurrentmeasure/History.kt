package `in`.avimarine.seawatercurrentmeasure

import android.location.Location

data class History (val loc1: Location, val loc2: Location, val spd: Double, val dir: Double )