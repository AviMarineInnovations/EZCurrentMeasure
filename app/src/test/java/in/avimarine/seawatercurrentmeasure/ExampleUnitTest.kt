package `in`.avimarine.seawatercurrentmeasure

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */


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
    fun toKnots_isCorrect(){
        assertEquals(toKnots(30.866666666666666666666666666667),1.0, 0.001)
    }
}
