package `in`.avimarine.seawatercurrentmeasure

/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 21/09/2019.
 */
interface ForegroundServiceBinder {
    public fun updateTime(autoFinishInterval: Long, delayedStartInterval: Long, delayedStartTime:Long, startTime: Long)
    public fun stop()

}