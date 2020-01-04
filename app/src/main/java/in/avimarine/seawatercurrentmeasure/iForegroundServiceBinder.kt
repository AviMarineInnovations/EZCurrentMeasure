package `in`.avimarine.seawatercurrentmeasure

import android.os.Binder

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 21/09/2019.
 */
interface iForegroundServiceBinder {
    public fun updateTime(autoFinishInterbal: Long, delayedStartInterval: Long, delayedStartTime:Long, startTime: Long) //TODO : CALL from MAin!!
    public fun stop() //TODO : CALL from MAin!!

}