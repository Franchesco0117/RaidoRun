package com.francisco.raidorun

import android.animation.ObjectAnimator
import android.view.View
import android.widget.LinearLayout
import java.util.concurrent.TimeUnit

object Utility {

    fun getFormattedStopWatch(ms: Long): String {
        var milliSeconds = ms

        val hours = TimeUnit.MILLISECONDS.toHours(milliSeconds)
        milliSeconds -= TimeUnit.HOURS.toMillis(hours)

        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliSeconds)
        milliSeconds -= TimeUnit.MINUTES.toMillis(minutes)

        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliSeconds)

        return "${if (hours < 10) "0" else ""}$hours:" +
                "${if (minutes < 10) "0" else ""}$minutes:" +
                "${if (seconds < 10) "0" else ""}$seconds"

    }

    fun getSecFromWatch (watch: String): Int{

        var secs = 0
        var w: String = watch
        if (w.length == 5) w= "00:" + w

        // 00:00:00
        secs += w.subSequence(0,2).toString().toInt() * 3600
        secs += w.subSequence(3,5).toString().toInt() * 60
        secs += w.subSequence(6,8).toString().toInt()

        return secs
    }

    /* FUNCIONES DE ANIMACION Y CAMBIOS DE ATRIBUTOS */
    fun setHeightLinearLayout(ly: LinearLayout, value: Int){
        val params: LinearLayout.LayoutParams = ly.layoutParams as LinearLayout.LayoutParams
        params.height = value
        ly.layoutParams = params
    }
    fun animateViewOfInt(v: View, attr: String, value: Int, time: Long){
        ObjectAnimator.ofInt(v, attr, value).apply{
            duration = time
            start()
        }
    }
    fun animateViewOfFloat(v: View, attr: String, value: Float, time: Long){
        ObjectAnimator.ofFloat(v, attr, value).apply{
            duration = time
            start()
        }
    }
}