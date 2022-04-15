package `in`.avimarine.seawatercurrentmeasure

import org.json.JSONArray
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 28/12/2019.
 */
@RunWith(
    RobolectricTestRunner::class)
    class PreferencesTest {

    @Test
    fun remoevOldEntries() {
        val ja = JSONArray("[{\"a\":1},{\"a\":2},{\"a\":3},{\"a\":4}]")
        val jd = Preferences.removeOldEntries(ja,2)
        assertEquals(2,jd.length())
    }
}