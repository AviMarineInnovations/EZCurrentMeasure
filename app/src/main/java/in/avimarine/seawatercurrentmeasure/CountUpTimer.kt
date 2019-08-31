package `in`.avimarine.seawatercurrentmeasure

import android.os.Handler
import android.os.Message
import android.os.SystemClock



/**
 * This file is part of an
 * Avi Marine Innovations project: SeaWaterCurrentMeasure
 * first created by aayaffe on 31/08/2019.
 * Simple timer class which count up until stopped.
 * Inspired by [android.os.CountDownTimer]
 */
abstract class CountUpTimer(private val interval: Long) {
    private var base: Long = 0

    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            synchronized(this@CountUpTimer) {
                val elapsedTime = SystemClock.elapsedRealtime() - base
                onTick(elapsedTime)
                sendMessageDelayed(obtainMessage(MSG), interval)
            }
        }
    }

    fun start() : CountUpTimer {
        base = SystemClock.elapsedRealtime()
        handler.sendMessage(handler.obtainMessage(MSG))
        return this;
    }

    fun stop() {
        handler.removeMessages(MSG)
    }

    fun reset() {
        synchronized(this) {
            base = SystemClock.elapsedRealtime()
        }
    }

    abstract fun onTick(elapsedTime: Long)

    companion object {

        private val MSG = 1
    }
}