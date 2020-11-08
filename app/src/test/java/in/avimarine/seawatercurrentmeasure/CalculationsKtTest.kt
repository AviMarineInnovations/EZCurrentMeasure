package `in`.avimarine.seawatercurrentmeasure

import android.location.Location
import junit.framework.Assert
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.runners.MockitoJUnitRunner

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 26/12/2019.
 */
@RunWith(MockitoJUnitRunner::class)
class CalculationsKtTest {

    @Mock
    private lateinit var loca: Location
    @Mock
    private lateinit var locb: Location
    @Test
    fun getDirError() {
        `when`(loca.latitude).thenReturn(0.0)
        `when`(loca.longitude).thenReturn(0.0)
        `when`(locb.latitude).thenReturn(0.0)
        `when`(locb.longitude).thenReturn(0.0166667)
        assertEquals(63.43,getDirError(loca,locb,1852.0,1852.0),0.1)
    }
}