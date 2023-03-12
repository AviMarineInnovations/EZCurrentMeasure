package `in`.avimarine.seawatercurrentmeasure

import android.location.Location
import `in`.avimarine.androidutils.geo.Speed

data class History (val loc1: Location, val loc2: Location, val spd: Speed, val dir: Double )