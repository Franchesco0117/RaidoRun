package com.francisco.raidorun

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.francisco.raidorun.Constants.INTERVAL_LOCATION
import com.francisco.raidorun.Constants.key_challengeAutoFinish
import com.francisco.raidorun.Constants.key_intervalDuration
import com.francisco.raidorun.Constants.key_maxCircularSeekBar
import com.francisco.raidorun.Constants.key_modeInterval
import com.francisco.raidorun.Constants.key_progressCircularSeekBar
import com.francisco.raidorun.Constants.key_modeChallenge
import com.francisco.raidorun.Constants.key_runningTime
import com.francisco.raidorun.Constants.key_walkingTime
import com.francisco.raidorun.Constants.key_modeChallengeDuration
import com.francisco.raidorun.Constants.key_challengeDurationHH
import com.francisco.raidorun.Constants.key_challengeDurationMM
import com.francisco.raidorun.Constants.key_challengeDurationSS
import com.francisco.raidorun.Constants.key_modeChallengeDistance
import com.francisco.raidorun.Constants.key_challengeDistance
import com.francisco.raidorun.Constants.key_challengeNotify
import com.francisco.raidorun.Constants.key_provider
import com.francisco.raidorun.Constants.key_selectedSport
import com.francisco.raidorun.Constants.key_userApp
import com.francisco.raidorun.LoginActivity.Companion.userEmail
import com.francisco.raidorun.Utility.animateViewOfFloat
import com.francisco.raidorun.Utility.animateViewOfInt
import com.francisco.raidorun.Utility.getFormattedStopWatch
import com.francisco.raidorun.Utility.getSecFromWatch
import com.francisco.raidorun.Utility.roundNumber
import com.francisco.raidorun.Utility.setHeightLinearLayout
import com.google.android.gms.common.api.GoogleApi.Settings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import me.tankery.lib.circularseekbar.CircularSeekBar
import org.w3c.dom.Text
import kotlin.math.round

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        val REQUIRED_PERMISSIONS_GPS =
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
    }

    private lateinit var drawer: DrawerLayout

    private lateinit var csbChallengeDistance: CircularSeekBar
    private lateinit var csbCurrentDistance: CircularSeekBar
    private lateinit var csbRecordDistance: CircularSeekBar

    private lateinit var csbCurrentAvgSpeed: CircularSeekBar
    private lateinit var csbRecordAvgSpeed: CircularSeekBar

    private lateinit var csbCurrentSpeed: CircularSeekBar
    private lateinit var csbCurrentMaxSpeed: CircularSeekBar
    private lateinit var csbRecordSpeed: CircularSeekBar

    private lateinit var tvDistanceRecord: TextView
    private lateinit var tvAvgSpeedRecord: TextView
    private lateinit var tvMaxSpeedRecord: TextView

    private lateinit var swIntervalMode: Switch
    private lateinit var swChallenges: Switch

    private var challengeDistance: Float = 0f

    private var challengeDuration: Int = 0
    private lateinit var tvChrono: TextView

    private lateinit var npDurationInterval: NumberPicker

    private lateinit var tvRunningTime: TextView
    private lateinit var tvWalkingTime: TextView
    private lateinit var csbRunWalk: CircularSeekBar

    private lateinit var npChallengeDistance: NumberPicker
    private lateinit var npChallengeDurationHH: NumberPicker
    private lateinit var npChallengeDurationMM: NumberPicker
    private lateinit var npChallengeDurationSS: NumberPicker

    private var ROUND_INTERVAL = 300
    private var TIME_RUNING: Int = 0

    private lateinit var lyPopUpRun: LinearLayout

    private var widthScreenPixels: Int = 0
    private var heightScreenPixels: Int = 0
    private var widthAnimations: Int = 0

    private var mHandler: Handler? = null
    private var mInterval = 1000

    private var timeInSeconds = 0L
    private var rounds: Int = 1
    private var startButtonClicked = false

    private lateinit var fbCamera: FloatingActionButton

    private var activatedGPS: Boolean = true
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val PERMISSION_IO = 42
    private var flagSavedLocation = false

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var init_lt: Double = 0.0
    private var init_ln: Double = 0.0

    private var distance: Double = 0.0
    private var maxSpeed: Double = 0.0
    private var avgSpeed: Double = 0.0
    private var speed: Double = 0.0

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initToolBar()
        initNavigationView()

        initObjects()

        initPermissionGPS()
    }

    private fun initChrono() {
        tvChrono = findViewById(R.id.tvChrono)
        tvChrono.setTextColor(ContextCompat.getColor(this, R.color.white))
        initStopWatch()

        widthScreenPixels = resources.displayMetrics.widthPixels
        heightScreenPixels = resources.displayMetrics.heightPixels

        widthAnimations = widthScreenPixels

        val lyChronoProgressBg = findViewById<LinearLayout>(R.id.lyChronoProgressBg)
        val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
        lyChronoProgressBg.translationX = -widthAnimations.toFloat()
        lyRoundProgressBg.translationX = -widthAnimations.toFloat()

        val tvReset: TextView = findViewById(R.id.tvReset)
        tvReset.setOnClickListener {
            resetClicked()
        }

        fbCamera = findViewById(R.id.fbCamera)
        fbCamera.isVisible = false
    }

    private fun hideLayouts() {
        var lyMap = findViewById<LinearLayout>(R.id.lyMap)
        var lyFragmentMap = findViewById<LinearLayout>(R.id.lyFragmentMap)

        val lyIntervalModeSpace = findViewById<LinearLayout>(R.id.lyIntervalModeSpace)
        val lyIntervalMode = findViewById<LinearLayout>(R.id.lyIntervalMode)

        val lyChallengesSpace = findViewById<LinearLayout>(R.id.lyChallengesSpace)
        val lyChallenges = findViewById<LinearLayout>(R.id.lyChallenges)

        setHeightLinearLayout(lyMap, 0)
        setHeightLinearLayout(lyIntervalModeSpace, 0)
        setHeightLinearLayout(lyChallengesSpace, 0)

        lyFragmentMap.translationY = -300f
        lyIntervalMode.translationY = -300f
        lyChallenges.translationY = -300f
    }

    private fun initMetrics() {
        csbCurrentDistance = findViewById(R.id.csbCurrentDistance)
        csbChallengeDistance = findViewById(R.id.csbChallengeDistance)
        csbRecordDistance = findViewById(R.id.csbRecordDistance)

        csbCurrentAvgSpeed = findViewById(R.id.csbCurrentAvgSpeed)
        csbRecordAvgSpeed = findViewById(R.id.csbRecordAvgSpeed)

        csbCurrentSpeed = findViewById(R.id.csbCurrentSpeed)
        csbCurrentMaxSpeed = findViewById(R.id.csbCurrentMaxSpeed)
        csbRecordSpeed = findViewById(R.id.csbRecordSpeed)

        csbCurrentDistance.progress = 0f
        csbChallengeDistance.progress = 0f

        csbCurrentAvgSpeed.progress = 0f

        csbCurrentSpeed.progress = 0f
        csbCurrentMaxSpeed.progress = 0f

        tvDistanceRecord = findViewById(R.id.tvDistanceRecord)
        tvAvgSpeedRecord = findViewById(R.id.tvAvgSpeedRecord)
        tvMaxSpeedRecord = findViewById(R.id.tvMaxSpeedRecord)

        tvDistanceRecord.text = ""
        tvAvgSpeedRecord.text = ""
        tvMaxSpeedRecord.text = ""
    }

    private fun initSwitches() {
        swIntervalMode = findViewById(R.id.swIntervalMode)
        swChallenges = findViewById(R.id.swChallenges)
    }

    private fun initIntervalMode() {
        npDurationInterval = findViewById(R.id.npDurationInterval)

        tvRunningTime = findViewById(R.id.tvRunningTime)
        tvWalkingTime = findViewById(R.id.tvWalkingTime)
        csbRunWalk = findViewById(R.id.csbRunWalk)

        npDurationInterval.minValue = 1
        npDurationInterval.maxValue = 60
        npDurationInterval.value = 5
        npDurationInterval.wrapSelectorWheel = true
        npDurationInterval.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        npDurationInterval.setOnValueChangedListener { picker, olvValue, newValue ->
            csbRunWalk.max = (newValue * 60).toFloat()
            csbRunWalk.progress = csbRunWalk.max / 2

            tvRunningTime.text =
                getFormattedStopWatch(((newValue * 60 / 2) * 1000).toLong()).subSequence(3, 8)
            tvWalkingTime.text = tvRunningTime.text

            ROUND_INTERVAL = newValue * 60
            TIME_RUNING = ROUND_INTERVAL / 2
        }

        csbRunWalk.max = 300f
        csbRunWalk.progress = 150f

        csbRunWalk.setOnSeekBarChangeListener(object :
            CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(
                circularSeekBar: CircularSeekBar?,
                progress: Float,
                fromUser: Boolean
            ) {

                if (fromUser) {
                    var STEPS_UX: Int = 15
                    if (ROUND_INTERVAL > 600) {
                        STEPS_UX = 60
                    }
                    if (ROUND_INTERVAL > 1800) {
                        STEPS_UX = 300
                    }
                    var set: Int = 0
                    var p = progress.toInt()

                    var limit = 60
                    if (ROUND_INTERVAL > 1800) {
                        limit = 300
                    }

                    if (p % STEPS_UX != 0 && progress != csbRunWalk.max) {
                        while (p >= limit) p -= limit
                        while (p >= STEPS_UX) p -= STEPS_UX
                        if (STEPS_UX - p > STEPS_UX / 2) {
                            set = -1 * p
                        } else {
                            set = STEPS_UX - p
                        }

                        if (csbRunWalk.progress + set > csbRunWalk.max) {
                            csbRunWalk.progress = csbRunWalk.max
                        } else {
                            csbRunWalk.progress = csbRunWalk.progress + set
                        }
                    }
                }

                if (csbRunWalk.progress == 0f) {
                    manageEnableButtonRun(false, false)
                } else {
                    manageEnableButtonRun(false, true)
                }

                tvRunningTime.text =
                    getFormattedStopWatch((csbRunWalk.progress.toInt() * 1000).toLong()).subSequence(3, 8)
                tvWalkingTime.text =
                    getFormattedStopWatch(((ROUND_INTERVAL - csbRunWalk.progress.toInt()) * 1000).toLong()).subSequence(3, 8)
                TIME_RUNING = getSecFromWatch(tvRunningTime.text.toString())
            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {
            }

        })
    }

    private fun initChallengeMode() {
        npChallengeDistance = findViewById(R.id.npChallengeDistance)
        npChallengeDurationHH = findViewById(R.id.npChallengeDurationHH)
        npChallengeDurationMM = findViewById(R.id.npChallengeDurationMM)
        npChallengeDurationSS = findViewById(R.id.npChallengeDurationSS)

        npChallengeDistance.minValue = 1
        npChallengeDistance.maxValue = 300
        npChallengeDistance.value = 10
        npChallengeDistance.wrapSelectorWheel = true

        npChallengeDistance.setOnValueChangedListener { picker, oldValue, newValue ->
            challengeDistance = newValue.toFloat()
            csbChallengeDistance.max = newValue.toFloat()
            csbChallengeDistance.progress = newValue.toFloat()
            challengeDuration = 0

            if (csbChallengeDistance.max > csbRecordDistance.max) {
                csbCurrentDistance.max = csbChallengeDistance.max
            }
        }

        val npChallengeDurationHH = findViewById<NumberPicker>(R.id.npChallengeDurationHH)
        val npChallengeDurationMM = findViewById<NumberPicker>(R.id.npChallengeDurationMM)
        val npChallengeDurationSS = findViewById<NumberPicker>(R.id.npChallengeDurationSS)

        npChallengeDurationHH.minValue = 0
        npChallengeDurationHH.maxValue = 23
        npChallengeDurationHH.value = 1
        npChallengeDurationHH.wrapSelectorWheel = true
        npChallengeDurationHH.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        npChallengeDurationMM.minValue = 0
        npChallengeDurationMM.maxValue = 59
        npChallengeDurationMM.value = 0
        npChallengeDurationMM.wrapSelectorWheel = true
        npChallengeDurationMM.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        npChallengeDurationSS.minValue = 0
        npChallengeDurationSS.maxValue = 59
        npChallengeDurationSS.value = 0
        npChallengeDurationSS.wrapSelectorWheel = true
        npChallengeDurationSS.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        npChallengeDurationHH.setOnValueChangedListener { picker, oldValue, newValue ->
            getChallengeDuration(newValue, npChallengeDurationMM.value, npChallengeDurationSS.value)
        }

        npChallengeDurationMM.setOnValueChangedListener { picker, oldValue, newValue ->
            getChallengeDuration(npChallengeDurationHH.value, newValue, npChallengeDurationSS.value)
        }

        npChallengeDurationSS.setOnValueChangedListener { picker, oldValue, newValue ->
            getChallengeDuration(npChallengeDurationHH.value, npChallengeDurationMM.value, newValue)
        }
    }

    private fun hidePopUpRun() {
        var lyWindow = findViewById<LinearLayout>(R.id.lyWindow)
        lyWindow.translationX = 400f

        lyPopUpRun = findViewById(R.id.lyPopupRun)
        lyPopUpRun.isVisible = false
    }

    private fun initObjects() {
        initChrono()
        hideLayouts()
        initMetrics()
        initSwitches()
        initIntervalMode()
        initChallengeMode()

        hidePopUpRun()

        initPreferences()
        recoveryPreferences()
    }

    private fun initPreferences() {
        sharedPreferences = getSharedPreferences("sharedPrefs_$userEmail", MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    private fun recoveryPreferences() {
        if (sharedPreferences.getString(key_userApp, "null") == userEmail) {
            //sportSelected = sharedPreferences.getString(key_selectedSport, "Running").toString()

            swIntervalMode.isChecked = sharedPreferences.getBoolean(key_modeInterval, false)
            if (swIntervalMode.isChecked) {
                npDurationInterval.value = sharedPreferences.getInt(key_intervalDuration, 5)
                ROUND_INTERVAL = npDurationInterval.value * 60
                csbRunWalk.progress = sharedPreferences.getFloat(key_progressCircularSeekBar, 150.0f)
                csbRunWalk.max = sharedPreferences.getFloat(key_maxCircularSeekBar, 300.0f)
                tvRunningTime.text = sharedPreferences.getString(key_runningTime, "2:30")
                tvWalkingTime.text = sharedPreferences.getString(key_walkingTime, "2:30")
                swIntervalMode.callOnClick()
            }

            swChallenges.isChecked = sharedPreferences.getBoolean(key_modeChallenge, false)
            if (swChallenges.isChecked) {
                swChallenges.callOnClick()
                if (sharedPreferences.getBoolean(key_modeChallengeDuration, false)) {
                    npChallengeDurationHH.value = sharedPreferences.getInt(key_challengeDurationHH, 1)
                    npChallengeDurationMM.value = sharedPreferences.getInt(key_challengeDurationMM, 0)
                    npChallengeDurationSS.value = sharedPreferences.getInt(key_challengeDurationSS, 0)
                    getChallengeDuration(npChallengeDurationHH.value, npChallengeDurationMM.value, npChallengeDurationSS.value)
                    challengeDistance = 0f

                    showChallenge("duration")
                }

                if (sharedPreferences.getBoolean(key_modeChallengeDistance, false)) {
                    npChallengeDistance.value = sharedPreferences.getInt(key_challengeDistance, 10)
                    challengeDistance = npChallengeDistance.value.toFloat()
                    challengeDuration = 0

                    showChallenge("distance")
                }
            }

            // cbNotify.isChecked = sharedPreferences.getBoolean(key_challengeNotify, true)
            // cbAutoFinish.isChecked = sharedPreferences.getBoolean(key_challengeAutoFinish, false)
        }

    }

    private fun savePreferences() {
        editor.clear()

        editor.apply{

            putString(key_userApp, userEmail)
            //putString(key_provider, providerSession)

            //putString(key_selectedSport, sportSelected)

            putBoolean(key_modeInterval, swIntervalMode.isChecked)
            putInt(key_intervalDuration, npDurationInterval.value)
            putFloat(key_progressCircularSeekBar, csbRunWalk.progress)
            putFloat(key_maxCircularSeekBar, csbRunWalk.max)
            putString(key_runningTime, tvRunningTime.text.toString())
            putString(key_walkingTime, tvWalkingTime.text.toString())

            putBoolean(key_modeChallenge, swChallenges.isChecked)
            putBoolean(key_modeChallengeDuration, !(challengeDuration == 0))
            putInt(key_challengeDurationHH, npChallengeDurationMM.value)
            putInt(key_challengeDurationMM, npChallengeDurationMM.value)
            putInt(key_challengeDurationSS, npChallengeDurationSS.value)
            putBoolean(key_modeChallengeDistance, !(challengeDistance == 0f))
            putInt(key_challengeDistance, npChallengeDistance.value)

            //putBoolean(key_challengeNotify, cbNotify.isChecked)
            //putBoolean(key_challengeAutoFinish, cbAutoFinish.isChecked)

        }.apply()
    }

    private fun alertClearPreferences() {
        AlertDialog.Builder(this)
            .setTitle(R.string.alertClearPreferencesTitle)
            .setMessage(R.string.alertClearPreferencesDescription)
            .setPositiveButton(android.R.string.ok,
                DialogInterface.OnClickListener { dialog, which ->
                    callClearPreferences()
                })
            .setNegativeButton(android.R.string.cancel,
                DialogInterface.OnClickListener{ dialog, which ->

                })
            .setCancelable(true)
            .show()
    }

    private fun callClearPreferences() {
        editor.clear().apply()
        Toast.makeText(this, "Tus ajustes han sido restablecidos :)", Toast.LENGTH_LONG).show()
    }

    private fun initStopWatch() {
        tvChrono.text = getString(R.string.init_stop_watch_value)
    }

    fun inflateIntervalMode(v: View) {
        val lyIntervalMode = findViewById<LinearLayout>(R.id.lyIntervalMode)
        val lyIntervalModeSpace = findViewById<LinearLayout>(R.id.lyIntervalModeSpace)
        var tvRounds = findViewById<TextView>(R.id.tvRounds)

        if (swIntervalMode.isChecked) {
            animateViewOfInt(
                swIntervalMode,
                "textColor",
                ContextCompat.getColor(this, R.color.orange),
                500
            )
            setHeightLinearLayout(lyIntervalModeSpace, 300)
            animateViewOfFloat(lyIntervalMode, "translationY", 0f, 500)
            animateViewOfFloat(tvChrono, "translationX", -60f, 500)
            tvRounds.setText(R.string.rounds)
            animateViewOfInt(
                tvRounds,
                "textColor",
                ContextCompat.getColor(this, R.color.white),
                500
            )

            var tvRunningTime = findViewById<TextView>(R.id.tvRunningTime)
            TIME_RUNING = getSecFromWatch(tvRunningTime.text.toString())

        } else {
            swIntervalMode.setTextColor(ContextCompat.getColor(this, R.color.white))
            setHeightLinearLayout(lyIntervalModeSpace, 0)
            lyIntervalMode.translationY = -200f
            animateViewOfFloat(tvChrono, "translationX", 0f, 500)
            tvRounds.text = ""
        }
    }

    fun inflateChallenges(v: View) {
        val lyChallengesSpace = findViewById<LinearLayout>(R.id.lyChallengesSpace)
        val lyChallenges = findViewById<LinearLayout>(R.id.lyChallenges)
        if (swChallenges.isChecked) {
            animateViewOfInt(
                swChallenges,
                "textColor",
                ContextCompat.getColor(this, R.color.orange),
                500
            )
            setHeightLinearLayout(lyChallengesSpace, 500)
            animateViewOfFloat(lyChallenges, "translationY", 0f, 500)
        } else {
            swChallenges.setTextColor(ContextCompat.getColor(this, R.color.white))
            setHeightLinearLayout(lyChallengesSpace, 0)
            lyChallenges.translationY = -100f

            challengeDistance = 0f
            challengeDuration = 0
        }
    }

    private fun getChallengeDuration(hh: Int, mm: Int, ss: Int) {
        var hours: String = hh.toString()
        if (hh < 10) hours = "0" + hours
        var minutes: String = mm.toString()
        if (mm < 10) minutes = "0" + minutes
        var seconds: String = ss.toString()
        if (ss < 10) seconds = "0" + seconds

        challengeDuration = getSecFromWatch("${hours}:${minutes}:${seconds}")
    }

    private fun showChallenge(option: String) {
        var lyChallengeDuration = findViewById<LinearLayout>(R.id.lyChallengeDuration)
        var lyChallengeDistance = findViewById<LinearLayout>(R.id.lyChallengeDistance)
        var tvChallengeDuration = findViewById<TextView>(R.id.tvChallengeDuration)
        var tvChallengeDistance = findViewById<TextView>(R.id.tvChallengeDistance)

        when (option) {
            "duration" -> {
                lyChallengeDuration.translationZ = 5f
                lyChallengeDistance.translationZ = 0f

                tvChallengeDuration.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvChallengeDuration.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.gray_dark
                    )
                )

                tvChallengeDistance.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvChallengeDistance.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.gray_medium
                    )
                )

                challengeDistance = 0f
                getChallengeDuration(
                    npChallengeDurationHH.value,
                    npChallengeDurationMM.value,
                    npChallengeDurationSS.value
                )
            }

            "distance" -> {
                lyChallengeDuration.translationZ = 0f
                lyChallengeDistance.translationZ = 5f

                tvChallengeDuration.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvChallengeDuration.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.gray_medium
                    )
                )

                tvChallengeDistance.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvChallengeDistance.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.gray_dark
                    )
                )

                challengeDuration = 0
                challengeDistance = npChallengeDistance.value.toFloat()
            }
        }
    }

    fun showDuration(v: View) {
        showChallenge("duration")
    }

    fun showDistance(v: View) {
        showChallenge("distance")
    }

    fun startOrStopButtonClicked(v: View) {
        manageStartStop()
    }

    private fun initPermissionGPS() {
        if (allPermissionGrantedGPS()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        } else {
            requestPermissionLocation()
        }

    }

    private fun allPermissionGrantedGPS() = REQUIRED_PERMISSIONS_GPS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED

    }

    private fun requestPermissionLocation() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_IO)
    }

    private fun activationLocation() {
        val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var myLocationRequest = LocationRequest()
        myLocationRequest.priority = PRIORITY_HIGH_ACCURACY
        myLocationRequest.interval = 0
        myLocationRequest.fastestInterval = 0
        myLocationRequest.numUpdates = 1
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.requestLocationUpdates(myLocationRequest, myLocationCallBack, Looper.myLooper())
    }

    private fun calculateDistance(n_lt: Double, n_lg: Double): Double{
        val radioTierra = 6371.0 //en kilómetros

        val dLat = Math.toRadians(n_lt - latitude)
        val dLng = Math.toRadians(n_lg - longitude)
        val sindLat = Math.sin(dLat / 2)
        val sindLng = Math.sin(dLng / 2)
        val va1 =
            Math.pow(sindLat, 2.0) + (Math.pow(sindLng, 2.0)
                    * Math.cos(Math.toRadians(latitude)) * Math.cos(
                Math.toRadians( n_lt )
            ))
        val va2 = 2 * Math.atan2(Math.sqrt(va1), Math.sqrt(1 - va1))
        var n_distance =  radioTierra * va2

//        if (n_distance < LIMIT_DISTANCE_ACCEPTED) {
//            distance += n_distance
//        }

        distance += n_distance
        return n_distance
    }

    private fun updateSpeeds(d: Double) {
        //la distancia se calcula en km, asi que la pasamos a metros para el calculo de velocidadr
        //convertirmos m/s a km/h multiplicando por 3.6
        speed = ((d * 1000) / INTERVAL_LOCATION) * 3.6
        if (speed > maxSpeed) maxSpeed = speed
        avgSpeed = ((distance * 1000) / timeInSeconds) * 3.6
    }

    private fun refreshInterfaceData() {
        var tvCurrentDistance = findViewById<TextView>(R.id.tvCurrentDistance)
        var tvCurrentAvgSpeed = findViewById<TextView>(R.id.tvCurrentAvgSpeed)
        var tvCurrentSpeed = findViewById<TextView>(R.id.tvCurrentSpeed)

        tvCurrentDistance.text = roundNumber(distance.toString(), 2)
        tvCurrentAvgSpeed.text = roundNumber(avgSpeed.toString(), 1)
        tvCurrentSpeed.text = roundNumber(speed.toString(), 1)

//        if (distance > totalsSelectedSport.recordDistance!!) {
//            tvDistanceRecord.text = roundNumber(distance.toString(), 1)
//            csbCurrentDistance.max =distance.toFloat()
//            csbCurrentDistance.progress = distance.toFloat()
//
//            tvDistanceRecord.setTextColor(ContextCompat.getColor(this, R.color.salmon_dark))
//        }

        csbCurrentDistance.progress = distance.toFloat()

//        if (avgSpeed > totalsSelectedSport.recordAvgSpeed!!) {
//            tvAvgSpeedRecord.text = roundNumber(avgSpeed.toString(), 1)
//            csbRecordAvgSpeed.max = avgSpeed.toFloat()
//            csbRecordAvgSpeed.progress = avgSpeed.toFloat()
//            csbCurrentAvgSpeed.max = avgSpeed.toFloat()
//
//            totalsSelectedSport.recordAvgSpeed = avgSpeed
//            tvAvgSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.salmon_dark))
//        }

        csbCurrentAvgSpeed.progress = avgSpeed.toFloat()

//        if (speed > totalsSelectedSport.recordSpeed!!) {
//            tvMaxSpeedRecord.text = roundNumber(speed.toString(), 1)
//            tvMaxSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.salmon_dark))
//
//            totalsSelectedSport.recordSpeed = speed
//
//            csbRecordSpeed.max = speed.toFloat()
//            csbRecordSpeed.progress = speed.toFloat()
//
//            csbCurrentMaxSpeed.max = speed.toFloat()
//            csbCurrentMaxSpeed.progress = speed.toFloat()
//
//            csbCurrentSpeed.max = speed.toFloat()
//        } else {
//            if (speed == maxSpeed) {
//                csbCurrentSpeed.max = csbRecordSpeed.max
//                csbCurrentMaxSpeed.progress = speed.toFloat()
//
//                csbCurrentSpeed.max = csbRecordSpeed.max
//            }
//        }

        csbCurrentSpeed.progress = speed.toFloat()

        if (speed == maxSpeed) {
            csbCurrentMaxSpeed.max = csbRecordSpeed.max
            csbCurrentMaxSpeed.progress = speed.toFloat()

            csbCurrentSpeed.max = csbRecordSpeed.max
            csbCurrentSpeed.max = csbRecordSpeed.max
        }
    }

    private fun registerNewLocation(location: Location) {
        var new_latitude: Double = location.latitude
        var new_longitude: Double = location.longitude

        if (flagSavedLocation) {
            if (timeInSeconds >= INTERVAL_LOCATION) {
                var distanceInterval = calculateDistance(new_latitude, new_longitude)

                updateSpeeds(distanceInterval)
                refreshInterfaceData()
            }
        }

        latitude = new_latitude
        longitude = new_longitude
    }

    private val myLocationCallBack = object: LocationCallback () {
        override fun onLocationResult(locationResult: LocationResult) {
            var myLastLocation : Location = locationResult.lastLocation

            init_lt = myLastLocation.latitude
            init_ln = myLastLocation.longitude

            if (timeInSeconds > 0L) {
                registerNewLocation(myLastLocation)
            }

        }
    }

    private fun checkPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun manageLocation() {
        if (checkPermission()) {

            if (isLocationEnabled()) {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {

                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        requestNewLocationData()
                    }
                }
            } else {
                activationLocation()
            }
        } else {
            requestPermissionLocation()
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager
        = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun manageStartStop() {
        if (timeInSeconds == 0L && isLocationEnabled() == false) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.alertActivationGPSTitle))
                .setMessage(getString(R.string.alertActivationGPSDescription))
                .setPositiveButton(R.string.acceptActivationGPS,
                    DialogInterface.OnClickListener { dialog, which ->
                        activationLocation()
                    })
                .setNegativeButton(R.string.ignoreActivationGPS,
                    DialogInterface.OnClickListener { dialog, which ->
                        activatedGPS = false
                        manageRun()
                    })
                .setCancelable(true)
                .show()
        } else {
            manageRun()
        }
    }

    private fun manageRun() {

        if (timeInSeconds.toInt() == 0) {

            fbCamera.isVisible = true

            swIntervalMode.isClickable = false
            npDurationInterval.isEnabled = false
            csbRunWalk.isEnabled = false

            swChallenges.isClickable = false
            npChallengeDistance.isEnabled = false
            npChallengeDurationHH.isEnabled = false
            npChallengeDurationMM.isEnabled = false
            npChallengeDurationSS.isEnabled = false

            tvChrono.setTextColor(ContextCompat.getColor(this, R.color.chrono_running))

            if (activatedGPS) {
                flagSavedLocation = false
                manageLocation()

                flagSavedLocation = true
                manageLocation()
            }
        }

        if (!startButtonClicked) {

            startButtonClicked = true
            startTime()
            manageEnableButtonRun(false, true)
        } else {

            startButtonClicked = false
            stopTime()
            manageEnableButtonRun(true, true)
        }
    }

    private fun manageEnableButtonRun(e_reset: Boolean, e_run: Boolean) {
        val tvReset = findViewById<TextView>(R.id.tvReset)
        val btStart = findViewById<LinearLayout>(R.id.btStart)
        val btStartLabel = findViewById<TextView>(R.id.btStartLabel)

        tvReset.setEnabled(e_reset)
        btStart.setEnabled(e_run)

        if (e_reset) {
            tvReset.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            animateViewOfFloat(tvReset, "translationY", 0f, 500)
        } else {
            tvReset.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
            animateViewOfFloat(tvReset, "translationY", 150f, 500)
        }

        if (e_run) {
            if (startButtonClicked) {
                btStart.background = getDrawable(R.drawable.circle_background_topause)
                btStartLabel.setText(R.string.stop)
            } else {
                btStart.background = getDrawable(R.drawable.circle_background_toplay)
                btStartLabel.setText(R.string.start)
            }
        } else {
            btStart.background = getDrawable(R.drawable.circle_background_todisable)
        }

    }

    private fun startTime() {
        mHandler = Handler(Looper.getMainLooper())
        chronometer.run()
    }

    private fun stopTime() {
        mHandler?.removeCallbacks(chronometer)
    }

    private var chronometer: Runnable = object : Runnable {
        override fun run() {
            try {
                if (activatedGPS && timeInSeconds.toInt() % INTERVAL_LOCATION == 0) {
                    manageLocation()
                }

                if (swIntervalMode.isChecked) {
                    checkStopRun(timeInSeconds)
                    checkNewRound(timeInSeconds)
                }

                timeInSeconds += 1
                updateStopWatch()

            } finally {
                mHandler!!.postDelayed(this, mInterval.toLong())
            }
        }
    }

    private fun updateStopWatch() {
        tvChrono.text = getFormattedStopWatch(timeInSeconds * 1000)
    }

    private fun resetClicked() {

        savePreferences()

        resetVariablesRun()
        resetTimeView()
        resetInterface()
    }

    private fun resetVariablesRun() {
        timeInSeconds = 0
        rounds = 1

        challengeDistance = 0f
        challengeDuration = 0

        activatedGPS = true
        flagSavedLocation = false
        initStopWatch()
    }

    private fun resetTimeView() {
        manageEnableButtonRun(false, true)

        tvChrono.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun resetInterface() {
        fbCamera.isVisible = false

        val tvCurrentDistance: TextView = findViewById(R.id.tvCurrentDistance)
        val tvCurrentAvgSpeed: TextView = findViewById(R.id.tvCurrentAvgSpeed)
        val tvCurrentSpeed: TextView = findViewById(R.id.tvCurrentSpeed)
        tvCurrentDistance.text = "0.0"
        tvCurrentAvgSpeed.text = "0.0"
        tvCurrentSpeed.text = "0.0"

        tvDistanceRecord.setTextColor(ContextCompat.getColor(this, R.color.gray_dark))
        tvAvgSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.gray_dark))
        tvMaxSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.gray_dark))

        csbCurrentDistance.progress = 0f
        csbCurrentAvgSpeed.progress = 0f
        csbCurrentSpeed.progress = 0f
        csbCurrentMaxSpeed.progress = 0f

        val lyChronoProgressBg = findViewById<LinearLayout>(R.id.lyChronoProgressBg)
        val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
        lyChronoProgressBg.translationX = -widthAnimations.toFloat()
        lyRoundProgressBg.translationX = -widthAnimations.toFloat()

        swIntervalMode.isClickable = true
        npDurationInterval.isEnabled = true
        csbRunWalk.isEnabled = true

        swChallenges.isClickable = true
        npChallengeDistance.isEnabled = true
        npChallengeDurationHH.isEnabled = true
        npChallengeDurationMM.isEnabled = true
        npChallengeDurationSS.isEnabled = true

    }

    private fun checkStopRun(secs: Long) {
        var secAux: Long = secs
        while (secAux.toInt() > ROUND_INTERVAL) {
            secAux -= ROUND_INTERVAL
        }

        if (secAux.toInt() == TIME_RUNING) {
            tvChrono.setTextColor(ContextCompat.getColor(this, R.color.chrono_walking))

            val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
            lyRoundProgressBg.setBackgroundColor(ContextCompat.getColor(this, R.color.chrono_walking))
            lyRoundProgressBg.translationX = -widthAnimations.toFloat()
        } else {
            updateProgressBarRound(secs)
        }
    }

    private fun checkNewRound(secs: Long) {
        if (secs.toInt() % ROUND_INTERVAL == 0 && secs.toInt() > 0) {
            val tvRounds: TextView = findViewById(R.id.tvRounds) as TextView
            rounds++
            tvRounds.text = "Round $rounds"

            tvChrono.setTextColor(ContextCompat.getColor(this, R.color.chrono_running))
            val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
            lyRoundProgressBg.setBackgroundColor(ContextCompat.getColor(this, R.color.chrono_running))
            lyRoundProgressBg.translationX = -widthAnimations.toFloat()
        } else {
            updateProgressBarRound(secs)
        }
    }

    private fun updateProgressBarRound(secs: Long) {
        var seconds = secs.toInt()
        while (seconds >= ROUND_INTERVAL) {
            seconds -= ROUND_INTERVAL
        }

        seconds++

        var lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
        if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_running)) {
            var movement = -1 * (widthAnimations - (seconds * widthAnimations / TIME_RUNING)).toFloat()
            animateViewOfFloat(lyRoundProgressBg, "translationX", movement, 1000L)
        }

        if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_walking)) {
            seconds -= TIME_RUNING
            var movement = -1 * (widthAnimations - (seconds * widthAnimations / (ROUND_INTERVAL - TIME_RUNING))).toFloat()
            animateViewOfFloat(lyRoundProgressBg, "translationX", movement, 1000L)
        }
    }

    private fun initNavigationView() {
        var navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        var headerView: View =
            LayoutInflater.from(this).inflate(R.layout.nav_header_main, navigationView, false)
        navigationView.removeHeaderView(headerView)
        navigationView.addHeaderView(headerView)

        var tvUser: TextView = headerView.findViewById(R.id.tvUser)
        tvUser.text = userEmail
    }

    private fun initToolBar() {
        val toolBar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolBar_main)
        setSupportActionBar(toolBar)

        drawer = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolBar, R.string.bar_title, R.string.navigation_drawer_close
        )

        drawer.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_item_record -> callRecordActivity()
            R.id.nav_item_clearPreferences -> alertClearPreferences()
            R.id.nav_item_signOut -> signOut()
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                signOut()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onBackPressed() {
        //super.onBackPressed()

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
            super.onBackPressed()
        } else {
            showExitConfirmationDialog()
        }
    }

    private fun callRecordActivity() {
        val intent = Intent(this, RecordActivity::class.java)
        startActivity(intent)
    }
}