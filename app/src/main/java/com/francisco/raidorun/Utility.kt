package com.francisco.raidorun

import android.animation.ObjectAnimator
import android.content.ContentValues
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.francisco.raidorun.LoginActivity.Companion.userEmail
import com.francisco.raidorun.MainActivity.Companion.totalsBike
import com.francisco.raidorun.MainActivity.Companion.totalsRollerSkate
import com.francisco.raidorun.MainActivity.Companion.totalsSelectedSport
import com.francisco.raidorun.MainActivity.Companion.totalsRunning
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.concurrent.TimeUnit

object Utility {

    private var totalsChecked: Int = 0

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
    fun setHeightLinearLayout(ly: View, value: Int) {
        val params = ly.layoutParams
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

    fun roundNumber(data: String, decimals: Int) : String {
        var d : String = data
        var p = d.indexOf(".", 0)

        if (p != null) {
            var limit: Int = p + decimals + 1
            if (d.length <= p + decimals + 1) {
                limit = d.length // -1
            }

            d = d.subSequence(0, limit).toString()
        }

        return d
    }

    fun getFormattedTotalTime(secs: Long): String {
        var seconds: Long = secs
        var total: String =""

        //1 dia = 86400s
        //1 mes (30 dias) = 2592000s
        //365 dias = 31536000s

        var years: Int = 0
        while (seconds >=  31536000) { years++; seconds-=31536000; }

        var months: Int = 0
        while (seconds >=  2592000) { months++; seconds-=2592000; }

        var days: Int = 0
        while (seconds >=  86400) { days++; seconds-=86400; }

        if (years > 0) total += "${years}y "
        if (months > 0) total += "${months}m "
        if (days > 0) total += "${days}d "

        total += getFormattedStopWatch(seconds*1000)

        return total
    }

    /* FUNCIONES DE BORRADO DE CARRERA */
    fun deleteRunAndLinkedData(idRun: String, sport: String, layout: LinearLayout, currentRun: Runs) {
        // If GPS On, deleting locations
        // If we had pictures, delete all of them

        // Review all Totals and Records
        updateTotals(currentRun)
        checkRecords(currentRun, sport, userEmail)

        // Delete race
        deleteRun(idRun, sport, layout)
    }

    private fun updateTotals(currentRun: Runs) {
        totalsSelectedSport.totalDistance = totalsSelectedSport.totalDistance!! - currentRun.distance!!
        totalsSelectedSport.totalRuns = totalsSelectedSport.totalRuns!! - 1
        totalsSelectedSport.totalTime = totalsSelectedSport.totalTime!! - getSecFromWatch(currentRun.duration!!)
    }

    private fun checkRecords(currentRun: Runs, sport: String, user: String) {
        totalsChecked = 0

        checkDistanceRecord(currentRun, sport, user)
        checkAvgSpeedRecord(currentRun, sport, user)
        checkMaxSpeedRecord(currentRun, sport, user)
    }

    private fun checkDistanceRecord(currentRun: Runs, sport: String, user: String) {
        if (currentRun.distance!! == totalsSelectedSport.recordDistance) {
            var dbRecords = FirebaseFirestore.getInstance()
            dbRecords.collection("runs$sport")
                .orderBy("distance", Query.Direction.DESCENDING)
                .whereEqualTo("user", user)
                .get()
                .addOnSuccessListener {documents ->
                    if (documents.size() == 1) {
                        totalsSelectedSport.recordDistance = 0.0
                    } else {
                        totalsSelectedSport.recordDistance = documents.documents[1].get("distance").toString().toDouble()
                    }

                    val collection = "totals$sport"
                    var dbUpdateTotals = FirebaseFirestore.getInstance()
                    dbUpdateTotals.collection(collection).document(user)
                        .update("recordDistance", totalsSelectedSport.recordDistance)

                    totalsChecked++
                    if (totalsChecked == 3) {
                        refreshTotalsSport(sport)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(ContentValues.TAG, "Error getting documents WHERE EQUAL TO: ", exception)
                }
        }
    }

    private fun checkAvgSpeedRecord(currentRun: Runs, sport: String, user: String) {
        if (currentRun.avgSpeed!! == totalsSelectedSport.recordAvgSpeed) {
            var dbRecords = FirebaseFirestore.getInstance()
            dbRecords.collection("runs$sport")
                .orderBy("avgSpeed", Query.Direction.DESCENDING)
                .whereEqualTo("user", user)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.size() == 1) {
                        totalsSelectedSport.recordAvgSpeed = 0.0
                    }
                    else {
                        totalsSelectedSport.recordAvgSpeed = documents.documents[1].get("avgSpeed").toString().toDouble()
                    }

                    var collection = "totals$sport"
                    var dbUpdateTotals = FirebaseFirestore.getInstance()
                    dbUpdateTotals.collection(collection).document(user)
                        .update("recordAvgSpeed", totalsSelectedSport.recordAvgSpeed)

                    totalsChecked++
                    if (totalsChecked == 3) {
                        refreshTotalsSport(sport)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(ContentValues.TAG, "Error getting documents WHERE EQUAL TO: ", exception)
                }
        }
    }

    private fun checkMaxSpeedRecord(currentRun: Runs, sport: String, user: String) {
        if (currentRun.maxSpeed!! == totalsSelectedSport.recordSpeed) {
            var dbRecord = FirebaseFirestore.getInstance()
            dbRecord.collection("runs$sport")
                .orderBy("maxSpeed", Query.Direction.DESCENDING)
                .whereEqualTo("user", user)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.size() == 1) {
                        totalsSelectedSport.recordSpeed = 0.0
                    } else {
                        totalsSelectedSport.recordSpeed = documents.documents[1].get("maxSpeed").toString().toDouble()
                    }

                    var collection = "totals$sport"
                    var dbUpdateTotals = FirebaseFirestore.getInstance()
                    dbUpdateTotals.collection(collection).document(user)
                        .update("recordSpeed", totalsSelectedSport.recordSpeed)

                    totalsChecked++
                    if (totalsChecked == 3) {
                        refreshTotalsSport(sport)
                    }

                }
                .addOnFailureListener { exception ->
                    Log.w(ContentValues.TAG, "Error getting documents WHERE EQUAL TO: ", exception)
                }
        }
    }

    private fun refreshTotalsSport(sport: String) {
        when (sport) {
            "Bike" -> totalsBike
            "RollerSkate" -> totalsRollerSkate
            "Running" -> totalsRunning
        }
    }

    private fun deleteRun(idRun: String, sport: String, layout: LinearLayout) {
        var dbRun = FirebaseFirestore.getInstance()
        dbRun.collection("runs$sport").document(idRun)
            .delete()
            .addOnSuccessListener {
                Snackbar.make(layout, "Registro borrado", Snackbar.LENGTH_LONG).setAction("OK") {
                    layout.setBackgroundColor(Color.CYAN)
                }.show()
            }
            .addOnFailureListener {
                Snackbar.make(layout, "Error al borrar registro", Snackbar.LENGTH_LONG).setAction("OK") {
                    layout.setBackgroundColor(Color.CYAN)
                }.show()
            }
    }
}