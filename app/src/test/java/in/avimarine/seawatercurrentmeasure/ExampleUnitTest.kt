package `in`.avimarine.seawatercurrentmeasure

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
val startLat = 85.574602932498
val startLon = 0.0
val endLat = 85.575843671373712949
val endLon = 0.035777598086013202
//from a testset: https://zenodo.org/record/32156#.XUXDfegzaUl
//85.574602932498 0 65.778981045327 85.575843671373712949 .035777598086013202 65.814652007683180661 338.0087876 .003036445501586881 338.0087874428325 25269506384.540334

class CalculationsUnitTest {
    @Test
    fun getDistance_isCorrect() {
        assertEquals(getDistance(startLat, startLon, endLat, endLon), 338.0087876, 0.00005)
    }

    @Test
    fun getBearing_isCorrect() {
        assertEquals(getDirection(startLat, startLon, endLat, endLon), 65.778981045327, 0.00005)
    }


    @Test
    fun getSpeed_isCorrect(){
        assertEquals(getSpeed(getDistance(startLat, startLon, endLat, endLon),0,60000),338.0087876,0.005)
    }

    @Test
    fun toKnots_isCorrect(){
        assertEquals(toKnots(30.866666666666666666666666666667),1.0, 0.001)
    }
}
