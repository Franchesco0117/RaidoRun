package com.francisco.raidorun

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Camera
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsService
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.francisco.raidorun.Constants.INTERVAL_LOCATION
import com.francisco.raidorun.Constants.LIMIT_DISTANCE_ACCEPTED_BIKE
import com.francisco.raidorun.Constants.LIMIT_DISTANCE_ACCEPTED_ROLLERSKATE
import com.francisco.raidorun.Constants.LIMIT_DISTANCE_ACCEPTED_RUNNING
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
import com.francisco.raidorun.LoginActivity.Companion.providerSession
import com.francisco.raidorun.LoginActivity.Companion.userEmail
import com.francisco.raidorun.Utility.animateViewOfFloat
import com.francisco.raidorun.Utility.animateViewOfInt
import com.francisco.raidorun.Utility.deleteRunAndLinkedData
import com.francisco.raidorun.Utility.getFormattedStopWatch
import com.francisco.raidorun.Utility.getFormattedTotalTime
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.common.hash.HashCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import me.tankery.lib.circularseekbar.CircularSeekBar
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.round
import kotlin.text.get

/**
 * Main activity for the app, responsible for initializing the UI, handling navigation,
 * setting up permissions, and managing core functionality like GPS tracking, metrics,
 * and interval mode logic.
 *
 * Implements several interfaces for navigation, map readiness, and location interactions.
 *
 * Author: Francisco Castro
 * Created: 9/MAR/2025
 */
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    companion object {
        lateinit var mainContext: Context

        /** Required GPS permissions for accessing device location. */
        val REQUIRED_PERMISSIONS_GPS =
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        /** Total stats depending on selected sport or activity type. */
        lateinit var totalsSelectedSport: Totals
        lateinit var totalsBike: Totals
        lateinit var totalsRollerSkate: Totals
        lateinit var totalsRunning: Totals

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

    private var minAltitude: Double? = null
    private var maxAltitude: Double? = null
    private var minLatitude: Double? = null
    private var maxLatitude: Double? = null
    private var minLongitude: Double? = null
    private var maxLongitude: Double? = null

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private var LIMIT_DISTANCE_ACCEPTED: Double = 0.0
    private lateinit var sportSelected : String

    private lateinit var cbNotify: CheckBox
    private lateinit var cbAutoFinish: CheckBox

    private lateinit var levelBike: Level
    private lateinit var levelRollerSkate: Level
    private lateinit var levelRunning: Level
    private lateinit var levelSelectedSport: Level

    private lateinit var levelsListBike: ArrayList<Level>
    private lateinit var levelsListRollerSkate: ArrayList<Level>
    private lateinit var levelsListRunning: ArrayList<Level>

    private var sportsLoaded: Int = 0

    private lateinit var dateRun: String
    private lateinit var startTimeRun: String

    private lateinit var medalsListBikeDistance: ArrayList<Double>
    private lateinit var medalsListBikeAvgSpeed: ArrayList<Double>
    private lateinit var medalsListBikeMaxSpeed: ArrayList<Double>

    private lateinit var medalsListRollerSkateDistance: ArrayList<Double>
    private lateinit var medalsListRollerSkateAvgSpeed: ArrayList<Double>
    private lateinit var medalsListRollerSkateMaxSpeed: ArrayList<Double>

    private lateinit var medalsListRunningDistance: ArrayList<Double>
    private lateinit var medalsListRunningAvgSpeed: ArrayList<Double>
    private lateinit var medalsListRunningMaxSpeed: ArrayList<Double>

    private lateinit var medalsListSportSelectedDistance: ArrayList<Double>
    private lateinit var medalsListSportSelectedAvgSpeed: ArrayList<Double>
    private lateinit var medalsListSportSelectedMaxSpeed: ArrayList<Double>

    private var recDistanceGold: Boolean = false
    private var recDistanceSilver: Boolean = false
    private var recDistanceBronze: Boolean = false
    private var recAvgSpeedGold: Boolean = false
    private var recAvgSpeedSilver: Boolean = false
    private var recAvgSpeedBronze: Boolean = false
    private var recMaxSpeedGold: Boolean = false
    private var recMaxSpeedSilver: Boolean = false
    private var recMaxSpeedBronze: Boolean = false

    private lateinit var map: GoogleMap
    private var mapCentered = true

    private val LOCATION_PERMISSION_REQ_CODE = 1000

    private lateinit var listPoints: Iterable<LatLng>

    /**
     * Initializes the activity, setting up the toolbar, navigation drawer, permissions,
     * and loading persisted data from the database.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainContext = this

        initToolBar()
        initNavigationView()

        initObjects()

        initPermissionGPS()

        loadFromDB()
    }

    /**
     * Initializes the chronometer display and stopwatch logic, as well as setting up
     * the UI layout and reset behavior.
     */
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

    /**
     * Hides specific layout components off-screen to prepare the UI for later animations or states.
     */
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

    /**
     * Initializes UI elements for tracking various exercise metrics like distance,
     * average speed, and maximum speed, and sets their default states.
     */
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

    /**
     * Initializes toggle switches for interval mode and challenge mode features.
     */
    private fun initSwitches() {
        swIntervalMode = findViewById(R.id.swIntervalMode)
        swChallenges = findViewById(R.id.swChallenges)
    }

    /**
     * Initializes the interval mode UI components including the duration picker, running and
     * walking time displays, and the circular seek bar. Sets up listeners to update the interval
     * duration, progress, and related UI elements, enforcing step increments and enabling/disabling
     * controls based on the current interval values.
     */
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

    /**
     * Sets up the challenge mode UI components such as distance and duration pickers, and
     * notification/autofinish checkboxes. Initializes value ranges and listeners to update
     * challenge parameters dynamically as users adjust the controls.
     */
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

        cbNotify = findViewById<CheckBox>(R.id.cbNotify)
        cbAutoFinish = findViewById<CheckBox>(R.id.cbAutoFinish)
    }

    /**
     * Calls all initialization functions to set up chronometer, UI layouts, metrics, switches,
     * interval mode, challenge mode, map fragment, totals, levels, medals, preferences, and
     * recovers saved preferences.
     */
    private fun initObjects() {
        initChrono()
        hideLayouts()
        initMetrics()
        initSwitches()
        initIntervalMode()
        initChallengeMode()

        hidePopUpRun()

        initMap()

        initTotals()
        initLevels()
        initMedals()

        initPreferences()
        recoveryPreferences()
    }

    /**
     * Prepares the map by clearing existing points, creating the map fragment, and enabling UI
     * controls conditionally based on GPS permission status.
     */
    private fun initMap() {
        listPoints = arrayListOf()
        (listPoints as ArrayList<LatLng>).clear()

        createMapFragment()

        var lyOpenerButton = findViewById<LinearLayout>(R.id.lyOpenerButton)
        if (allPermissionGrantedGPS()) {
            lyOpenerButton.isEnabled = true
        } else {
            lyOpenerButton.isEnabled = false
        }
    }

    /**
     * Handles clicks on the map’s “My Location” button. Returns false to allow default behavior.
     */
    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    /**
     * Callback triggered when the user’s location on the map is clicked. (Empty implementation)
     *
     * @param p0 The Location object representing the user's current location.
     */
    override fun onMyLocationClick(p0: Location) {

    }

    /**
     * Creates and initializes the map fragment by obtaining it from the support fragment manager
     * and setting the map ready callback.
     */
    private fun createMapFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    /**
     * Called when the map is ready to be used. Sets map type, enables location, configures
     * listeners for location buttons and map interactions, and centers the map on initial coordinates.
     *
     * @param googleMap The GoogleMap instance that is ready for use.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        enableMyLocation()
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        map.setOnMapLongClickListener { mapCentered = false }
        map.setOnMapClickListener { mapCentered = false }

        manageLocation()
        centerMap (init_lt, init_ln)
    }

    /**
     * Handles the result of location permission requests. Enables or disables map-related UI
     * components accordingly.
     *
     * @param requestCode The request code passed in requestPermissions().
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions, either
     *                     PackageManager.PERMISSION_GRANTED or PackageManager.PERMISSION_DENIED.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQ_CODE -> {
                var lyOpenerButton = findViewById<LinearLayout>(R.id.lyOpenerButton)

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    lyOpenerButton.isEnabled = true
                } else {
                    var lyMap = findViewById<LinearLayout>(R.id.lyMap)
                    if (lyMap.height > 0) {
                        setHeightLinearLayout(lyMap, 0)

                        var lyFragmentMap = findViewById<LinearLayout>(R.id.lyFragmentMap)
                        lyFragmentMap.translationY = -300f

                        var ivOpenClose = findViewById<ImageView>(R.id.ivOpenClose)
                        ivOpenClose.setRotation(0f)
                    }

                    lyOpenerButton.isEnabled = false
                }
            }
        }
    }

    /**
     * Enables the user’s location display on the map if permissions are granted; otherwise,
     * requests location permission.
     */
    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionLocation()
            return
        } else {
            map.isMyLocationEnabled = true
        }
    }

    /**
     * Toggles the map type between hybrid and normal views and updates the corresponding icon.
     *
     * @param v The view that triggered this method (typically a button or icon).
     */
    fun changeTypeMap(v: View) {
        var ivTypeMap = findViewById<ImageView>(R.id.ivTypeMap)
        if (map.mapType == GoogleMap.MAP_TYPE_HYBRID){
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            ivTypeMap.setImageResource(R.drawable.ic_hybrid_map_type)
        } else {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            ivTypeMap.setImageResource(R.drawable.ic_normal_map_type)
        }
    }

    /**
     * Centers the map view on the current location if available; otherwise, centers on the
     * default initial coordinates.
     *
     * @param v The view that triggered this method (typically a button).
     */
    fun callCenterMap(v: View) {
        mapCentered = true
        if (latitude == 0.0) {
            centerMap(init_lt, init_ln)
        } else {
            centerMap(latitude, longitude)
        }
    }

    /**
     * Shows or hides the map layout with animations based on current visibility and location
     * permissions, requesting permission if necessary.
     *
     * @param v The view that triggered this method (typically a button).
     */
    fun callShowHideMap(v: View) {
        if (allPermissionGrantedGPS()) {
            var lyMap = findViewById<LinearLayout>(R.id.lyMap)
            var lyFragmentMap = findViewById<LinearLayout>(R.id.lyFragmentMap)
            var ivOpenClose = findViewById<ImageView>(R.id.ivOpenClose)

            if (lyMap.height == 0) {
                setHeightLinearLayout(lyMap, 733)
                animateViewOfFloat(lyFragmentMap, "translationY",0f, 350)
                ivOpenClose.setRotation(180f)
            } else {
                setHeightLinearLayout(lyMap, 0)
                lyFragmentMap.translationY = -300f
                ivOpenClose.setRotation(0f)
            }
        } else {
            requestPermissionLocation()
        }
    }

    /**
     * Centers the map on the given latitude and longitude coordinates with animation.
     *
     * @param lt Latitude coordinate.
     * @param ln Longitude coordinate.
     */
    private fun centerMap(lt: Double, ln: Double) {
        val posMap = LatLng(lt, ln)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(posMap, 16f), 1000, null)
    }

    /**
     * Initializes the totals data objects for Bike, RollerSkate, and Running activities,
     * setting all counters and records to zero.
     */
    private fun initTotals() {
        totalsBike = Totals()
        totalsRollerSkate = Totals()
        totalsRunning = Totals()

        totalsBike.totalRuns = 0
        totalsBike.totalDistance = 0.0
        totalsBike.totalTime = 0
        totalsBike.recordDistance = 0.0
        totalsBike.recordSpeed = 0.0
        totalsBike.recordAvgSpeed = 0.0

        totalsRollerSkate.totalRuns = 0
        totalsRollerSkate.totalDistance = 0.0
        totalsRollerSkate.totalTime = 0
        totalsRollerSkate.recordDistance = 0.0
        totalsRollerSkate.recordSpeed = 0.0
        totalsRollerSkate.recordAvgSpeed = 0.0

        totalsRunning.totalRuns = 0
        totalsRunning.totalDistance = 0.0
        totalsRunning.totalTime = 0
        totalsRunning.recordDistance = 0.0
        totalsRunning.recordSpeed = 0.0
        totalsRunning.recordAvgSpeed = 0.0
    }

    /**
     * Initializes level data objects and their corresponding target values for
     * Bike, RollerSkate, and Running activities.
     */
    private fun initLevels() {
        levelSelectedSport = Level()
        levelBike = Level()
        levelRollerSkate = Level()
        levelRunning = Level()

        levelsListBike = arrayListOf()
        levelsListBike.clear()

        levelsListRollerSkate = arrayListOf()
        levelsListRollerSkate.clear()

        levelsListRunning = arrayListOf()
        levelsListRunning.clear()

        levelBike.name = "turtle"
        levelBike.image = "level_1"
        levelBike.runsTarget = 1
        levelBike.distanceTarget = 4

        levelRollerSkate.name = "turtle"
        levelRollerSkate.image = "level_1"
        levelRollerSkate.runsTarget = 1
        levelRollerSkate.distanceTarget = 2

        levelRunning.name = "turtle"
        levelRunning.image = "level_1"
        levelRunning.runsTarget = 1
        levelRunning.distanceTarget= 1
    }

    /**
     * Initializes and clears all medals lists for the selected sport and individual sports.
     */
    private fun initMedals() {
        medalsListSportSelectedDistance = arrayListOf()
        medalsListSportSelectedAvgSpeed = arrayListOf()
        medalsListSportSelectedMaxSpeed = arrayListOf()
        medalsListSportSelectedDistance.clear()
        medalsListSportSelectedAvgSpeed.clear()
        medalsListSportSelectedMaxSpeed.clear()

        medalsListBikeDistance = arrayListOf()
        medalsListBikeAvgSpeed = arrayListOf()
        medalsListBikeMaxSpeed = arrayListOf()
        medalsListBikeDistance.clear()
        medalsListBikeAvgSpeed.clear()
        medalsListBikeMaxSpeed.clear()

        medalsListRollerSkateDistance = arrayListOf()
        medalsListRollerSkateAvgSpeed = arrayListOf()
        medalsListRollerSkateMaxSpeed = arrayListOf()
        medalsListRollerSkateDistance.clear()
        medalsListRollerSkateAvgSpeed.clear()
        medalsListRollerSkateMaxSpeed.clear()

        medalsListRunningDistance = arrayListOf()
        medalsListRunningAvgSpeed = arrayListOf()
        medalsListRunningMaxSpeed = arrayListOf()
        medalsListRunningDistance.clear()
        medalsListRunningAvgSpeed.clear()
        medalsListRunningMaxSpeed.clear()
    }

    /**
     * Resets all medal achievement flags for distance, average speed, and max speed.
     */
    private fun resetMedals() {
        recDistanceGold = false
        recDistanceSilver = false
        recDistanceBronze = false

        recAvgSpeedGold = false
        recAvgSpeedSilver = false
        recAvgSpeedBronze = false

        recMaxSpeedGold = false
        recMaxSpeedSilver = false
        recMaxSpeedBronze = false
    }

    /**
     * Loads user data from the database, including totals and medals.
     */
    private fun loadFromDB() {
        loadTotalUser()
        loadMedalsUser()
    }

    /**
     * Loads total statistics for each sport: Bike, RollerSkate, and Running.
     */
    private fun loadTotalUser() {
        loadTotalSport("Bike")
        loadTotalSport("RollerSkate")
        loadTotalSport("Running")
    }

    /**
     * Loads total statistics from the database for a specific sport.
     * Updates the corresponding totals object or initializes the database entry if missing.
     *
     * @param sport The sport type as a String ("Bike", "RollerSkate", or "Running").
     */
    private fun loadTotalSport(sport: String) {
        var collection = "totals$sport"
        var dbTotalsUser = FirebaseFirestore.getInstance()

        dbTotalsUser.collection(collection).document(userEmail)
            .get()
            .addOnSuccessListener { document ->
                if (document.data?.size != null) {
                    val total = document.toObject(Totals::class.java)
                    when (sport) {
                        "Bike" -> totalsBike = total!!
                        "RollerSkate" -> totalsRollerSkate = total!!
                        "Running" -> totalsRunning = total!!
                    }

                } else {
                    val dbTotal: FirebaseFirestore = FirebaseFirestore.getInstance()
                    dbTotal.collection(collection).document(userEmail).set(hashMapOf(
                        "recordAvgSpeed" to 0.0,
                        "recordDistance" to 0.0,
                        "recordSpeed" to 0.0,
                        "totalDistance" to 0.0,
                        "totalRuns" to 0,
                        "totalTime" to 0
                    ))
                }

                sportsLoaded++
                setLevelSport(sport)
                if (sportsLoaded == 3) {
                    selectSport(sportSelected)
                }

            }.addOnFailureListener { exception ->
                Log.d("Error - loadTotalUser", "get failed with ", exception)
            }
    }

    /**
     * Retrieves and sets the level data for a specific sport from the database.
     *
     * @param sport The sport type as a String ("Bike", "RollerSkate", or "Running").
     */
    private fun setLevelSport(sport: String) {
        val dbLevels: FirebaseFirestore = FirebaseFirestore.getInstance()
        dbLevels.collection("levels$sport")
            .get()
            .addOnSuccessListener {documents ->
                for (document in documents) {
                    when (sport) {
                        "Bike" -> levelsListBike.add(document.toObject(Level::class.java))
                        "RollerSkate" -> levelsListRollerSkate.add(document.toObject(Level::class.java))
                        "Running" -> levelsListRunning.add(document.toObject(Level::class.java))
                    }
                }

                when (sport) {
                    "Bike" -> setLevelBike()
                    "RollerSkate" -> setLevelRollerSkate()
                    "Running" -> setLevelRunning()
                }
            }
            .addOnFailureListener {exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    /**
     * Updates the UI and level data for the Bike sport based on the loaded totals
     * and available levels.
     */
    private fun setLevelBike() {
        var lyNavLevelBike = findViewById<LinearLayout>(R.id.lyNavLevelBike)
        if (totalsBike.totalTime!! == 0) {
            setHeightLinearLayout(lyNavLevelBike, 0)
        } else {
            setHeightLinearLayout(lyNavLevelBike, 275)
            for (level in levelsListBike) {
                if (totalsBike.totalRuns!! < level.runsTarget!!
                    || totalsBike.totalDistance!! < level.distanceTarget!!) {

                    levelBike.name = level.name!!
                    levelBike.image = level.image!!
                    levelBike.runsTarget = level.runsTarget!!
                    levelBike.distanceTarget = level.distanceTarget!!

                    break
                }
            }

            var ivLevelBike = findViewById<ImageView>(R.id.ivLevelBike)
            var tvTotalTimeBike = findViewById<TextView>(R.id.tvTotalTimeBike)
            var tvTotalRunsBike = findViewById<TextView>(R.id.tvTotalRunsBike)
            var tvTotalDistanceBike = findViewById<TextView>(R.id.tvTotalDistanceBike)
            var tvNumberLevelBike = findViewById<TextView>(R.id.tvNumberLevelBike)

            var levelText = "${getString(R.string.level)} ${levelBike.image!!.subSequence(6, 7).toString()}"
            tvNumberLevelBike.text = levelText

            var totalTime = getFormattedTotalTime(totalsBike.totalTime!!.toLong())
            tvTotalTimeBike.text = totalTime

            when (levelBike.image) {
                "level_1" -> ivLevelBike.setImageResource(R.drawable.level_1)
                "level_2" -> ivLevelBike.setImageResource(R.drawable.level_2)
                "level_3" -> ivLevelBike.setImageResource(R.drawable.level_3)
                "level_4" -> ivLevelBike.setImageResource(R.drawable.level_4)
                "level_5" -> ivLevelBike.setImageResource(R.drawable.level_5)
                "level_6" -> ivLevelBike.setImageResource(R.drawable.level_6)
                "level_7" -> ivLevelBike.setImageResource(R.drawable.level_7)
            }

            tvTotalRunsBike.text = "${totalsBike.totalRuns}/${levelBike.runsTarget}"
            val porcent = totalsBike.totalDistance!!.toInt() * 100 / levelBike.distanceTarget!!.toInt()
            tvTotalDistanceBike.text = "${porcent.toInt()}%"

            var csbDistanceBike = findViewById<CircularSeekBar>(R.id.csbDistanceBike)
            csbDistanceBike.max = levelBike.distanceTarget!!.toFloat()
            if (totalsBike.totalDistance!! >= levelBike.distanceTarget!!.toDouble()){
                csbDistanceBike.progress = csbDistanceBike.max
            } else {
                csbDistanceBike.progress = totalsBike.totalDistance!!.toFloat()
            }

            var csbRunsBike = findViewById<CircularSeekBar>(R.id.csbRunsBike)
            csbRunsBike.max = levelBike.runsTarget!!.toFloat()
            if (totalsBike.totalRuns!! >= levelBike.runsTarget!!.toInt()) {
                csbRunsBike.progress = csbRunsBike.max
            } else {
                csbRunsBike.progress = totalsBike.totalRuns!!.toFloat()
            }
        }
    }

    /**
     * Updates the UI and progress indicators for the Roller Skate level based on the user's total 
     * runs, total distance, and total time. It adjusts layout visibility, sets the current level, 
     * updates images, progress bars, and textual information accordingly.
     */
    private fun setLevelRollerSkate(){
        var lyNavLevelRollerSkate = findViewById<LinearLayout>(R.id.lyNavLevelRollerSkate)
        if (totalsRollerSkate.totalTime!! == 0) {
            setHeightLinearLayout(lyNavLevelRollerSkate, 0)
        } else {
            setHeightLinearLayout(lyNavLevelRollerSkate, 275)
            for (level in levelsListRollerSkate){
                if (totalsRollerSkate.totalRuns!! < level.runsTarget!!.toInt()
                    || totalsRollerSkate.totalDistance!! < level.distanceTarget!!.toDouble()) {

                    levelRollerSkate.name = level.name!!
                    levelRollerSkate.image = level.image!!
                    levelRollerSkate.runsTarget = level.runsTarget!!
                    levelRollerSkate.distanceTarget = level.distanceTarget!!

                    break
                }
            }

            var ivLevelRollerSkate = findViewById<ImageView>(R.id.ivLevelRollerSkate)
            var tvTotalTimeRollerSkate = findViewById<TextView>(R.id.tvTotalTimeRollerSkate)
            var tvTotalRunsRollerSkate = findViewById<TextView>(R.id.tvTotalRunsRollerSkate)
            var tvTotalDistanceRollerSkate = findViewById<TextView>(R.id.tvTotalDistanceRollerSkate)

            var tvNumberLevelRollerSkate = findViewById<TextView>(R.id.tvNumberLevelRollerSkate)
            var levelText = "${getString(R.string.level)} ${levelRollerSkate.image!!.subSequence(6,7).toString()}"
            tvNumberLevelRollerSkate.text = levelText

            var tt = getFormattedTotalTime(totalsRollerSkate.totalTime!!.toLong())
            tvTotalTimeRollerSkate.text = tt

            when (levelRollerSkate.image){
                "level_1" -> ivLevelRollerSkate.setImageResource(R.drawable.level_1)
                "level_2" -> ivLevelRollerSkate.setImageResource(R.drawable.level_2)
                "level_3" -> ivLevelRollerSkate.setImageResource(R.drawable.level_3)
                "level_4" -> ivLevelRollerSkate.setImageResource(R.drawable.level_4)
                "level_5" -> ivLevelRollerSkate.setImageResource(R.drawable.level_5)
                "level_6" -> ivLevelRollerSkate.setImageResource(R.drawable.level_6)
                "level_7" -> ivLevelRollerSkate.setImageResource(R.drawable.level_7)
            }

            tvTotalRunsRollerSkate.text = "${totalsRollerSkate.totalRuns}/${levelRollerSkate.runsTarget}"

            var porcent = totalsRollerSkate.totalDistance!!.toInt() * 100 / levelRollerSkate.distanceTarget!!.toInt()
            tvTotalDistanceRollerSkate.text = "${porcent.toInt()}%"

            var csbDistanceRollerSkate = findViewById<CircularSeekBar>(R.id.csbDistanceRollerSkate)
            csbDistanceRollerSkate.max = levelRollerSkate.distanceTarget!!.toFloat()
            if (totalsRollerSkate.totalDistance!! >= levelRollerSkate.distanceTarget!!.toDouble()) {
                csbDistanceRollerSkate.progress = csbDistanceRollerSkate.max
            } else {
                csbDistanceRollerSkate.progress = totalsRollerSkate.totalDistance!!.toFloat()
            }

            var csbRunsRollerSkate = findViewById<CircularSeekBar>(R.id.csbRunsRollerSkate)
            csbRunsRollerSkate.max = levelRollerSkate.runsTarget!!.toFloat()
            if (totalsRollerSkate.totalRuns!! >= levelRollerSkate.runsTarget!!.toInt()) {
                csbRunsRollerSkate.progress = csbRunsRollerSkate.max
            } else {
                csbRunsRollerSkate.progress = totalsRollerSkate.totalRuns!!.toFloat()
            }
        }
    }

    /**
     * Updates the UI and progress indicators for the Running level based on the user's total runs, 
     * total distance, and total time. It adjusts layout visibility, sets the current level, 
     * updates images, progress bars, and textual information accordingly.
     */
    private fun setLevelRunning(){
        var lyNavLevelRunning = findViewById<LinearLayout>(R.id.lyNavLevelRunning)
        if (totalsRunning.totalTime!! == 0) {
            setHeightLinearLayout(lyNavLevelRunning, 0)
        } else{

            setHeightLinearLayout(lyNavLevelRunning, 275)
            for (level in levelsListRunning){
                if (totalsRunning.totalRuns!! < level.runsTarget!!.toInt()
                    || totalsRunning.totalDistance!! < level.distanceTarget!!.toDouble()){

                    levelRunning.name = level.name!!
                    levelRunning.image = level.image!!
                    levelRunning.runsTarget = level.runsTarget!!
                    levelRunning.distanceTarget = level.distanceTarget!!

                    break
                }
            }

            var ivLevelRunning = findViewById<ImageView>(R.id.ivLevelRunning)
            var tvTotalTimeRunning = findViewById<TextView>(R.id.tvTotalTimeRunning)
            var tvTotalRunsRunning = findViewById<TextView>(R.id.tvTotalRunsRunning)
            var tvTotalDistanceRunning = findViewById<TextView>(R.id.tvTotalDistanceRunning)

            var tvNumberLevelRunning = findViewById<TextView>(R.id.tvNumberLevelRunning)
            var levelText = "${getString(R.string.level)} ${levelRunning.image!!.subSequence(6,7).toString()}"
            tvNumberLevelRunning.text = levelText

            var tt = getFormattedTotalTime(totalsRunning.totalTime!!.toLong())
            tvTotalTimeRunning.text = tt

            when (levelRunning.image){
                "level_1" -> ivLevelRunning.setImageResource(R.drawable.level_1)
                "level_2" -> ivLevelRunning.setImageResource(R.drawable.level_2)
                "level_3" -> ivLevelRunning.setImageResource(R.drawable.level_3)
                "level_4" -> ivLevelRunning.setImageResource(R.drawable.level_4)
                "level_5" -> ivLevelRunning.setImageResource(R.drawable.level_5)
                "level_6" -> ivLevelRunning.setImageResource(R.drawable.level_6)
                "level_7" -> ivLevelRunning.setImageResource(R.drawable.level_7)
            }

            tvTotalRunsRunning.text = "${totalsRunning.totalRuns}/${levelRunning.runsTarget}"
            var porcent = totalsRunning.totalDistance!!.toInt() * 100 / levelRunning.distanceTarget!!.toInt()
            tvTotalDistanceRunning.text = "${porcent.toInt()}%"

            var csbDistanceRunning = findViewById<CircularSeekBar>(R.id.csbDistanceRunning)
            csbDistanceRunning.max = levelRunning.distanceTarget!!.toFloat()
            if (totalsRunning.totalDistance!! >= levelRunning.distanceTarget!!.toDouble()) {
                csbDistanceRunning.progress = csbDistanceRunning.max
            } else {
                csbDistanceRunning.progress = totalsRunning.totalDistance!!.toFloat()
            }

            var csbRunsRunning = findViewById<CircularSeekBar>(R.id.csbRunsRunning)
            csbRunsRunning.max = levelRunning.runsTarget!!.toFloat()
            if (totalsRunning.totalRuns!! >= levelRunning.runsTarget!!.toInt()) {
                csbRunsRunning.progress = csbRunsRunning.max
            } else {
                csbRunsRunning.progress = totalsRunning.totalRuns!!.toFloat()
            }
        }
    }

    /**
     * Initiates loading of medals data for all sports (Bike, Roller Skate, Running)
     * by fetching top distance, average speed, and max speed achievements from Firestore.
     */
    private fun loadMedalsUser() {
        loadMedalsBike()
        loadMedalsRollerSkate()
        loadMedalsRunning()
    }

    /**
     * Loads the user's top 3 medals data for Bike runs from Firestore, including distance, 
     * average speed, and max speed. Missing medals are filled with zero values.
     */
    private fun loadMedalsBike() {
        var dbRecords = FirebaseFirestore.getInstance()

        dbRecords.collection("runsBike")
            .orderBy("distance", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener {documents ->
                for (document in documents) {
                    if (document["user"] == userEmail) {
                        medalsListBikeDistance.add(document["distance"].toString().toDouble())
                    }

                    if (medalsListBikeDistance.size == 3) {
                        break
                    }
                }

                while (medalsListBikeDistance.size < 3) {
                    medalsListBikeDistance.add(0.0)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents", exception)
            }

        dbRecords.collection("runsBike")
            .orderBy("avgSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == userEmail) {
                        medalsListBikeAvgSpeed.add(document["avgSpeed"].toString().toDouble())
                    }

                    if (medalsListBikeAvgSpeed.size == 3) {
                        break
                    }
                }

                while (medalsListBikeAvgSpeed.size < 3) {
                    medalsListBikeAvgSpeed.add(0.0)
                }

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsBike")
            .orderBy("maxSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == userEmail) {
                        medalsListBikeMaxSpeed.add(document["maxSpeed"].toString().toDouble())
                    }

                    if (medalsListBikeMaxSpeed.size == 3) {
                        break
                    }
                }

                while (medalsListBikeMaxSpeed.size < 3) {
                    medalsListBikeMaxSpeed.add(0.0)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    /**
     * Loads the user's top 3 medals data for Roller Skate runs from Firestore, including distance,
     * average speed, and max speed. Missing medals are filled with zero values.
     */
    private fun loadMedalsRollerSkate() {
        var dbRecords = FirebaseFirestore.getInstance()

        dbRecords.collection("runsRollerSkate")
            .orderBy("distance", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener {documents ->
                for (document in documents) {
                    if (document["user"] == userEmail) {
                        medalsListRollerSkateDistance.add(document["distance"].toString().toDouble())
                    }

                    if (medalsListRollerSkateDistance.size == 3) {
                        break
                    }
                }

                while (medalsListRollerSkateDistance.size < 3) {
                    medalsListRollerSkateDistance.add(0.0)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents", exception)
            }

        dbRecords.collection("runsRollerSkate")
            .orderBy("avgSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == userEmail) {
                        medalsListRollerSkateAvgSpeed.add(document["avgSpeed"].toString().toDouble())
                    }

                    if (medalsListRollerSkateAvgSpeed.size == 3) {
                        break
                    }
                }

                while (medalsListRollerSkateAvgSpeed.size < 3) {
                    medalsListRollerSkateAvgSpeed.add(0.0)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsRollerSkate")
            .orderBy("maxSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == userEmail) {
                        medalsListRollerSkateMaxSpeed.add(document["maxSpeed"].toString().toDouble())
                    }

                    if (medalsListRollerSkateMaxSpeed.size == 3) {
                        break
                    }
                }

                while (medalsListRollerSkateMaxSpeed.size < 3) {
                    medalsListRollerSkateMaxSpeed.add(0.0)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    /**
     * Loads the user's top 3 medals data for Running runs from Firestore, including distance,
     * average speed, and max speed. Missing medals are filled with zero values.
     */
    private fun loadMedalsRunning() {
        var dbRecords = FirebaseFirestore.getInstance()

        dbRecords.collection("runsRunning")
            .orderBy("distance", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener {documents ->
                for (document in documents) {
                    if (document["user"] == userEmail) {
                        medalsListRunningDistance.add(document["distance"].toString().toDouble())
                    }

                    if (medalsListRunningDistance.size == 3) {
                        break
                    }
                }

                while (medalsListRunningDistance.size < 3) {
                    medalsListRunningDistance.add(0.0)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents", exception)
            }

        dbRecords.collection("runsRunning")
            .orderBy("avgSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == userEmail) {
                        medalsListRunningAvgSpeed.add(document["avgSpeed"].toString().toDouble())
                    }

                    if (medalsListRunningAvgSpeed.size == 3) {
                        break
                    }
                }

                while (medalsListRunningAvgSpeed.size < 3) {
                    medalsListRunningAvgSpeed.add(0.0)
                }

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsRunning")
            .orderBy("maxSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == userEmail) {
                        medalsListRunningMaxSpeed.add(document["maxSpeed"].toString().toDouble())
                    }

                    if (medalsListRunningMaxSpeed.size == 3) {
                        break
                    }
                }

                while (medalsListRunningMaxSpeed.size < 3) {
                    medalsListRunningMaxSpeed.add(0.0)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    /**
     * Initializes SharedPreferences and its editor for the current user.
     */
    private fun initPreferences() {
        sharedPreferences = getSharedPreferences("sharedPrefs_$userEmail", MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    /**
     * Recovers saved user preferences such as selected sport, interval mode, challenge mode,
     * notification settings, and updates the UI accordingly.
     */
    private fun recoveryPreferences() {
        if (sharedPreferences.getString(key_userApp, "null") == userEmail) {

            sportSelected = sharedPreferences.getString(key_selectedSport, "Running").toString()

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

            cbNotify.isChecked = sharedPreferences.getBoolean(key_challengeNotify, true)
            cbAutoFinish.isChecked = sharedPreferences.getBoolean(key_challengeAutoFinish, false)
        } else {
            sportSelected = "Running"
        }

    }

    /**
     * Clears previous preferences, then stores updated values such as user email, provider,
     * selected sport, interval mode settings, challenge mode settings, and UI component states.
     */
    private fun savePreferences() {
        editor.clear()

        editor.apply{

            putString(key_userApp, userEmail)
            putString(key_provider, providerSession)

            putString(key_selectedSport, sportSelected)

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

            putBoolean(key_challengeNotify, cbNotify.isChecked)
            putBoolean(key_challengeAutoFinish, cbAutoFinish.isChecked)

        }.apply()
    }

    /**
     * Shows an AlertDialog with "OK" and "Cancel" options.
     * If the user confirms, preferences are cleared.
     */
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

    /**
     * Shows a Toast notification confirming that settings have been reset.
     */
    private fun callClearPreferences() {
        editor.clear().apply()
        Toast.makeText(this, R.string.settings_reset, Toast.LENGTH_LONG).show()
    }

    /**
     * Initializes the stopwatch display to its default start value.
     */
    private fun initStopWatch() {
        tvChrono.text = getString(R.string.init_stop_watch_value)
    }

    /**
     * Inflates and animates the interval mode UI elements based on the toggle state.
     *
     * @param v The View that triggers this function (typically the interval mode switch).
     */
    fun inflateIntervalMode(v: View) {
        val lyIntervalMode = findViewById<LinearLayout>(R.id.lyIntervalMode)
        val lyIntervalModeSpace = findViewById<LinearLayout>(R.id.lyIntervalModeSpace)
        var tvRounds = findViewById<TextView>(R.id.tvRounds)

        if (swIntervalMode.isChecked) {
            animateViewOfInt(
                swIntervalMode,
                "textColor",
                ContextCompat.getColor(this, R.color.wii_blue),
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
            swIntervalMode.setTextColor(ContextCompat.getColor(this, R.color.wii_blue))
            setHeightLinearLayout(lyIntervalModeSpace, 0)
            lyIntervalMode.translationY = -200f
            animateViewOfFloat(tvChrono, "translationX", 0f, 500)
            tvRounds.text = ""
        }
    }


    /**
     * Inflates and animates the challenges UI elements based on the toggle state.
     *
     * @param v The View that triggers this function (typically the challenges switch).
     */
    fun inflateChallenges(v: View) {
        val lyChallengesSpace = findViewById<LinearLayout>(R.id.lyChallengesSpace)
        val lyChallenges = findViewById<LinearLayout>(R.id.lyChallenges)
        if (swChallenges.isChecked) {
            animateViewOfInt(
                swChallenges,
                "textColor",
                ContextCompat.getColor(this, R.color.wii_blue),
                500
            )
            setHeightLinearLayout(lyChallengesSpace, 500)
            animateViewOfFloat(lyChallenges, "translationY", 0f, 500)
        } else {
            swChallenges.setTextColor(ContextCompat.getColor(this, R.color.wii_blue))
            setHeightLinearLayout(lyChallengesSpace, 0)
            lyChallenges.translationY = -100f

            challengeDistance = 0f
            challengeDuration = 0
        }
    }

    /**
     * Updates the user's totals for the currently selected sport, including runs, distance,
     * time, and personal records, then syncs the data with Firestore.
     */
    private fun updateTotalsUsers() {
        totalsSelectedSport.totalRuns = totalsSelectedSport.totalRuns!! + 1
        totalsSelectedSport.totalDistance = totalsSelectedSport.totalDistance!! + distance
        totalsSelectedSport.totalTime = totalsSelectedSport.totalTime!! + timeInSeconds.toInt()

        if (distance > totalsSelectedSport.recordDistance!!) {
            totalsSelectedSport.recordDistance = distance
        }

        if (maxSpeed > totalsSelectedSport.recordSpeed!!) {
            totalsSelectedSport.recordSpeed = maxSpeed
        }

        if (avgSpeed > totalsSelectedSport.recordAvgSpeed!!) {
            totalsSelectedSport.recordAvgSpeed = avgSpeed
            totalsSelectedSport.recordAvgSpeed = avgSpeed
        }

        totalsSelectedSport.totalDistance = roundNumber(totalsSelectedSport.totalDistance.toString(), 1).toDouble()
        totalsSelectedSport.recordDistance = roundNumber(totalsSelectedSport.recordDistance.toString(), 1).toDouble()
        totalsSelectedSport.recordSpeed = roundNumber(totalsSelectedSport.recordSpeed.toString(), 1).toDouble()
        totalsSelectedSport.recordAvgSpeed = roundNumber(totalsSelectedSport.recordAvgSpeed.toString(), 1).toDouble()

        var collection = "totals$sportSelected"
        var dbUpdateTotals = FirebaseFirestore.getInstance()
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("recordAvgSpeed", totalsSelectedSport.recordAvgSpeed)
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("recordDistance", totalsSelectedSport.recordDistance)
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("recordSpeed", totalsSelectedSport.recordSpeed)
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("totalDistance", totalsSelectedSport.totalDistance)
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("totalRuns", totalsSelectedSport.totalRuns)
        dbUpdateTotals.collection(collection).document(userEmail)
            .update("totalTime", totalsSelectedSport.totalTime)

        when (sportSelected) {
            "Bike" -> {
                totalsBike = totalsSelectedSport
                medalsListBikeDistance = medalsListSportSelectedDistance
                medalsListBikeAvgSpeed = medalsListSportSelectedAvgSpeed
                medalsListBikeMaxSpeed = medalsListSportSelectedMaxSpeed
            }
            "RollerSkate" -> {
                totalsRollerSkate = totalsSelectedSport
                medalsListRollerSkateDistance = medalsListSportSelectedDistance
                medalsListRollerSkateAvgSpeed = medalsListSportSelectedAvgSpeed
                medalsListRollerSkateMaxSpeed = medalsListSportSelectedMaxSpeed
            }
            "Running" -> {
                totalsRunning = totalsSelectedSport
                medalsListRunningDistance = medalsListSportSelectedDistance
                medalsListRunningAvgSpeed = medalsListSportSelectedAvgSpeed
                medalsListRunningMaxSpeed = medalsListSportSelectedMaxSpeed
            }
        }
    }

    /**
     * Converts hours, minutes, and seconds to a total challenge duration in seconds,
     * formatting the time as a string in HH:MM:SS format before conversion.
     *
     * @param hh Hours part of challenge duration.
     * @param mm Minutes part of challenge duration.
     * @param ss Seconds part of challenge duration.
     */
    private fun getChallengeDuration(hh: Int, mm: Int, ss: Int) {
        var hours: String = hh.toString()
        if (hh < 10) hours = "0" + hours
        var minutes: String = mm.toString()
        if (mm < 10) minutes = "0" + minutes
        var seconds: String = ss.toString()
        if (ss < 10) seconds = "0" + seconds

        challengeDuration = getSecFromWatch("${hours}:${minutes}:${seconds}")
    }

    /**
     * Shows and highlights the selected challenge option (duration or distance)
     * and updates UI accordingly.
     *
     * @param option Either "duration" or "distance" to select the challenge type.
     */
    private fun showChallenge(option: String) {
        var lyChallengeDuration = findViewById<LinearLayout>(R.id.lyChallengeDuration)
        var lyChallengeDistance = findViewById<LinearLayout>(R.id.lyChallengeDistance)
        var tvChallengeDuration = findViewById<TextView>(R.id.tvChallengeDuration)
        var tvChallengeDistance = findViewById<TextView>(R.id.tvChallengeDistance)

        when (option) {
            "duration" -> {
                lyChallengeDuration.translationZ = 5f
                lyChallengeDistance.translationZ = 0f

                tvChallengeDuration.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvChallengeDuration.setBackgroundResource(R.drawable.bg_challenge_duration_selected)

                tvChallengeDistance.setTextColor(ContextCompat.getColor(this, R.color.gray_strong))
                tvChallengeDistance.setBackgroundResource(R.drawable.bg_challenge_distance_unselected)

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

                tvChallengeDuration.setTextColor(ContextCompat.getColor(this, R.color.gray_strong))
                tvChallengeDuration.setBackgroundResource(R.drawable.bg_challenge_duration_unselected)

                tvChallengeDistance.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvChallengeDistance.setBackgroundResource(R.drawable.bg_challenge_distance_selected)

                challengeDuration = 0
                challengeDistance = npChallengeDistance.value.toFloat()
            }
        }
    }

    /**
     * UI onClick handler to show the challenge duration option.
     *
     * @param v The view that triggers this function.
     */
    fun showDuration(v: View) {
        showChallenge("duration")
    }

    /**
     * UI onClick handler to show the challenge distance option.
     *
     * @param v The view that triggers this function.
     */
    fun showDistance(v: View) {
        showChallenge("distance")
    }

    /**
     * Handles the click event for the start/stop button.
     *
     * @param v The view that triggers this function.
     */
    fun startOrStopButtonClicked(v: View) {
        manageStartStop()
    }

    /**
     * Initializes GPS location permission by checking if permissions are granted.
     * If granted, initializes the fused location client; otherwise requests permissions.
     */
    private fun initPermissionGPS() {
        if (allPermissionGrantedGPS()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        } else {
            requestPermissionLocation()
        }

    }

    /**
     * Checks if all required GPS permissions are granted.
     *
     * @return true if all permissions are granted, false otherwise.
     */
    private fun allPermissionGrantedGPS() = REQUIRED_PERMISSIONS_GPS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED

    }

    /**
     * Requests the necessary location permissions from the user.
     */
    private fun requestPermissionLocation() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_IO)
    }

    /**
     * Opens the device's Location Settings screen to allow user to enable GPS.
     */
    private fun activationLocation() {
        val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    /**
     * Requests a single high-accuracy location update from the fused location provider.
     * Requires location permissions to be granted.
     */
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

    /**
     * Calculates the distance in kilometers between the last known latitude/longitude
     * and the new latitude/longitude provided.
     * Only adds the distance to the total if it is below the accepted limit.
     *
     * @param n_lt new latitude in degrees
     * @param n_lg new longitude in degrees
     * @return distance calculated between points in kilometers
     */
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

        if (n_distance < LIMIT_DISTANCE_ACCEPTED) {
            distance += n_distance
        }

        //distance += n_distance
        return n_distance
    }

    /**
     * Updates the current speed, max speed, and average speed based on the distance covered
     * during the last location interval.
     *
     * @param d Distance in kilometers covered during the last interval
     */
    private fun updateSpeeds(d: Double) {
        //la distancia se calcula en km, asi que la pasamos a metros para el calculo de velocidadr
        //convertirmos m/s a km/h multiplicando por 3.6
        speed = ((d * 1000) / INTERVAL_LOCATION) * 3.6
        if (speed > maxSpeed) maxSpeed = speed
        avgSpeed = ((distance * 1000) / timeInSeconds) * 3.6
    }

    /**
     * Sets the selected sport to "Bike" if the timer hasn't started.
     *
     * @param v The clicked view
     */
    fun selectBike(v: View) {
        if (timeInSeconds.toInt() == 0) selectSport("Bike")
    }

    /**
     * Sets the selected sport to "RollerSkate" if the timer hasn't started.
     *
     * @param v The clicked view
     */
    fun selectRollerSkate(v: View) {
        if (timeInSeconds.toInt() == 0) selectSport("RollerSkate")
    }

    /**
     * Sets the selected sport to "Running" if the timer hasn't started.
     *
     * @param v The clicked view
     */
    fun selectRunning(v: View) {
        if (timeInSeconds.toInt() == 0) selectSport("Running")
    }

    /**
     * Updates UI, internal variables, and limits based on the selected sport.
     *
     * @param sport The sport selected by the user, e.g., "Bike", "RollerSkate", or "Running"
     */
    private fun selectSport(sport: String) {
        sportSelected = sport

        var lySportBike = findViewById<CardView>(R.id.cvSportBike)
        var lySportRollerSkate = findViewById<CardView>(R.id.cvSportRollerSkate)
        var lySportRunning = findViewById<CardView>(R.id.cvSportRunning)

        when(sport) {
            "Bike" -> {
                LIMIT_DISTANCE_ACCEPTED = LIMIT_DISTANCE_ACCEPTED_BIKE
                lySportBike.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.wii_blue))
                lySportRollerSkate.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRunning.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))

                levelSelectedSport = levelBike
                totalsSelectedSport = totalsBike

                medalsListSportSelectedDistance = medalsListBikeDistance
                medalsListSportSelectedAvgSpeed = medalsListBikeAvgSpeed
                medalsListSportSelectedMaxSpeed = medalsListBikeMaxSpeed
            }

            "RollerSkate" -> {
                LIMIT_DISTANCE_ACCEPTED = LIMIT_DISTANCE_ACCEPTED_ROLLERSKATE
                lySportBike.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRollerSkate.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.wii_blue))
                lySportRunning.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))

                levelSelectedSport = levelRollerSkate
                totalsSelectedSport = totalsRollerSkate

                medalsListSportSelectedDistance = medalsListRollerSkateDistance
                medalsListSportSelectedAvgSpeed = medalsListRollerSkateAvgSpeed
                medalsListSportSelectedMaxSpeed = medalsListRollerSkateMaxSpeed
            }

            "Running" -> {
                LIMIT_DISTANCE_ACCEPTED = LIMIT_DISTANCE_ACCEPTED_RUNNING
                lySportBike.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRollerSkate.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRunning.setCardBackgroundColor(ContextCompat.getColor(mainContext, R.color.wii_blue))

                levelSelectedSport = levelRunning
                totalsSelectedSport = totalsRunning

                medalsListSportSelectedDistance = medalsListRunningDistance
                medalsListSportSelectedAvgSpeed = medalsListRunningAvgSpeed
                medalsListSportSelectedMaxSpeed = medalsListRunningMaxSpeed
            }
        }

        refreshCircularSeekBarSport()
        refreshRecords()
    }

    /**
     * Updates circular progress bars based on the current selected sport's records.
     */
    private fun refreshCircularSeekBarSport() {
        csbRecordDistance.max = totalsSelectedSport.recordDistance?.toFloat()!!
        csbRecordDistance.progress = totalsSelectedSport.recordDistance?.toFloat()!!

        csbRecordAvgSpeed.max = totalsSelectedSport.recordAvgSpeed?.toFloat()!!
        csbRecordAvgSpeed.progress = totalsSelectedSport.recordAvgSpeed?.toFloat()!!

        csbRecordSpeed.max = totalsSelectedSport.recordSpeed?.toFloat()!!
        csbRecordSpeed.progress = totalsSelectedSport.recordSpeed?.toFloat()!!

        csbCurrentDistance.max = csbRecordDistance.max
        csbCurrentAvgSpeed.max = csbRecordAvgSpeed.max
        csbCurrentSpeed.max = csbRecordSpeed.max
        csbCurrentMaxSpeed.max = csbRecordSpeed.max
        csbCurrentMaxSpeed.progress = 0f

    }


    /**
     * Updates text views showing record values for distance, average speed, and max speed.
     */
    private fun refreshRecords() {
        if (totalsSelectedSport.recordDistance!! > 0) {
            tvDistanceRecord.text = totalsSelectedSport.recordDistance.toString()
        } else {
            tvDistanceRecord.text = ""
        }

        if (totalsSelectedSport.recordAvgSpeed!! > 0) {
            tvAvgSpeedRecord.text = totalsSelectedSport.recordAvgSpeed.toString()
        } else {
            tvAvgSpeedRecord.text = ""
        }

        if (totalsSelectedSport.recordSpeed!! > 0) {
            tvMaxSpeedRecord.text = totalsSelectedSport.recordSpeed.toString()
        } else {
            tvMaxSpeedRecord.text = ""
        }
    }

    /**
     * Refreshes UI elements to display current distance, average speed, and speed,
     * and updates progress bars accordingly.
     */
    private fun refreshInterfaceData() {
        var tvCurrentDistance = findViewById<TextView>(R.id.tvCurrentDistance)
        var tvCurrentAvgSpeed = findViewById<TextView>(R.id.tvCurrentAvgSpeed)
        var tvCurrentSpeed = findViewById<TextView>(R.id.tvCurrentSpeed)

        tvCurrentDistance.text = roundNumber(distance.toString(), 2)
        tvCurrentAvgSpeed.text = roundNumber(avgSpeed.toString(), 1)
        tvCurrentSpeed.text = roundNumber(speed.toString(), 1)

        csbCurrentDistance.progress = distance.toFloat()
        if (distance > totalsSelectedSport.recordDistance!!) {
            tvDistanceRecord.text = roundNumber(distance.toString(), 1)
            tvDistanceRecord.setTextColor(ContextCompat.getColor(this, R.color.wii_blue_dark))

            csbCurrentDistance.max = distance.toFloat()
            csbCurrentDistance.progress = distance.toFloat()

            totalsSelectedSport.recordDistance = distance
        }

        csbCurrentAvgSpeed.progress = avgSpeed.toFloat()
        if (avgSpeed > totalsSelectedSport.recordAvgSpeed!!) {
            tvAvgSpeedRecord.text = roundNumber(avgSpeed.toString(), 1)
            tvAvgSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.wii_blue_dark))

            csbRecordAvgSpeed.max = avgSpeed.toFloat()
            csbRecordAvgSpeed.progress = avgSpeed.toFloat()
            csbCurrentAvgSpeed.max = avgSpeed.toFloat()

            totalsSelectedSport.recordAvgSpeed = avgSpeed
        }

        if (speed > totalsSelectedSport.recordSpeed!!) {
            tvMaxSpeedRecord.text = roundNumber(speed.toString(), 1)
            tvMaxSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.wii_blue_dark))


            csbRecordSpeed.max = speed.toFloat()
            csbRecordSpeed.progress = speed.toFloat()

            csbCurrentMaxSpeed.max = speed.toFloat()
            csbCurrentMaxSpeed.progress = speed.toFloat()

            csbCurrentSpeed.max = speed.toFloat()

            totalsSelectedSport.recordSpeed = speed
        } else {
            if (speed == maxSpeed) {
                csbCurrentSpeed.max = csbRecordSpeed.max
                csbCurrentMaxSpeed.progress = speed.toFloat()

                csbCurrentSpeed.max = csbRecordSpeed.max
            }
        }

        csbCurrentSpeed.progress = speed.toFloat()
    }

    /**
     * Processes a new location update, calculating distance, updating speeds,
     * refreshing UI, saving location to database, and updating map polylines.
     *
     * @param location The new location update from GPS
     */
    private fun registerNewLocation(location: Location) {
        var new_latitude: Double = location.latitude
        var new_longitude: Double = location.longitude

        if (flagSavedLocation) {
            if (timeInSeconds >= INTERVAL_LOCATION) {
                var distanceInterval = calculateDistance(new_latitude, new_longitude)

                if (distanceInterval <= LIMIT_DISTANCE_ACCEPTED) {
                    updateSpeeds(distanceInterval)
                    refreshInterfaceData()

                    saveLocation(location)

                    var newPos = LatLng (new_latitude, new_longitude)
                    (listPoints as ArrayList<LatLng>).add(newPos)
                    createPolyLines(listPoints)

                    checkMedals(distance, avgSpeed, maxSpeed)
                }
            }
        }

        latitude = new_latitude
        longitude = new_longitude

        if (mapCentered == true) {
            centerMap(latitude, longitude)
        }

        // Center Map
        if (minLatitude == null) {
            minLatitude = latitude
            maxLatitude = latitude
            minLongitude = longitude
            maxLongitude = longitude
        }

        if (latitude < minLatitude!!) minLatitude = latitude
        if (latitude > maxLatitude!!) maxLatitude = latitude
        if (longitude < minLongitude!!) minLongitude = longitude
        if (longitude > maxLongitude!!) maxLongitude = longitude

        if (location.hasAltitude()) {
            if (maxAltitude == null) {
                maxAltitude = location.altitude
                minAltitude = location.altitude
            }

            if (location.latitude > maxAltitude!!) maxAltitude = location.altitude
            if (location.latitude > minAltitude!!) minAltitude = location.altitude
        }
    }

    /**
     * Draws a polyline on the map from a list of LatLng points.
     *
     * @param listPosition Iterable collection of LatLng points to be connected
     */
    private fun createPolyLines(listPosition: Iterable<LatLng>) {
        val polyLineOptions = PolylineOptions()
            .width(25f)
            .color(ContextCompat.getColor(this, R.color.wii_blue))
            .addAll(listPoints)

        var polyLine = map.addPolyline(polyLineOptions)
        polyLine.startCap = RoundCap()
    }

    /**
     * Saves the current location data to Firebase Firestore under a path
     * based on the user's email and the date/time of the run.
     *
     * @param location The location to save
     */
    private fun saveLocation(location: Location) {
        var dirName = dateRun + startTimeRun
        dirName = dirName.replace("/", "")
        dirName = dirName.replace(":", "")

        var docName = timeInSeconds.toString()
        while (docName.length < 4) docName = "0" + docName

        var ms: Boolean
        ms = speed == maxSpeed && speed > 0

        var dbLocation = FirebaseFirestore.getInstance()
        dbLocation.collection("locations/$userEmail/$dirName").document(docName).set(hashMapOf(
            "time" to SimpleDateFormat("HH:MM:SS").format(Date()),
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "altitude" to location.altitude,
            "hasAltitude" to location.hasAltitude(),
            "speedFromGoogle" to location.speed,
            "speedFromMe" to speed,
            "maxSpeed" to ms,
            "color" to tvChrono.currentTextColor

        ))
    }

    /**
     * Checks if the user has reached any medal thresholds for distance, average speed, and max speed.
     * Updates medal flags and triggers notifications accordingly.
     *
     * @param distance Current distance covered (in km)
     * @param avgSpeed Current average speed (in km/h)
     * @param maxSpeed Current max speed (in km/h)
     */
    private fun checkMedals(distance: Double, avgSpeed: Double, maxSpeed: Double) {
        if (distance > 0) {
            if (distance >= medalsListSportSelectedDistance.get(0)) {
                recDistanceGold = true
                recDistanceSilver = false
                recDistanceBronze = false
                notifyMedal("distance", "gold", "personal")

            } else {
                if (distance >= medalsListSportSelectedDistance.get(1)) {
                    recDistanceGold = false
                    recDistanceSilver = true
                    recDistanceBronze = false
                    notifyMedal("distance", "silver", "personal")

                } else {
                    if (distance >= medalsListSportSelectedDistance.get(2)) {
                        recDistanceGold = false
                        recDistanceSilver = false
                        recDistanceBronze = true
                        notifyMedal("distance", "bronze", "personal")
                    }
                }
            }
        }

        if (avgSpeed > 0) {
            if (avgSpeed >= medalsListSportSelectedAvgSpeed.get(0)) {
                recAvgSpeedGold = true
                recAvgSpeedSilver = false
                recAvgSpeedBronze = false
                notifyMedal("avgSpeed", "gold", "personal")

            } else if (avgSpeed >= medalsListSportSelectedAvgSpeed.get(1)) {
                recAvgSpeedGold = false
                recAvgSpeedSilver = true
                recAvgSpeedBronze = false
                notifyMedal("avgSpeed", "silver", "personal")

            } else if (avgSpeed >= medalsListSportSelectedAvgSpeed.get(2)) {
                recAvgSpeedGold = false
                recAvgSpeedSilver = false
                recAvgSpeedBronze = true
                notifyMedal("avgSpeed", "bronze", "personal")
            }
        }

        if (maxSpeed >= 0) {
            if (maxSpeed >= medalsListSportSelectedMaxSpeed.get(0)) {
                recMaxSpeedGold = true
                recMaxSpeedSilver = false
                recMaxSpeedBronze = false
                notifyMedal("maxSpeed", "gold", "personal")

            } else if (maxSpeed >= medalsListSportSelectedMaxSpeed.get(1)) {
                recMaxSpeedGold = false
                recMaxSpeedSilver = true
                recMaxSpeedBronze = false
                notifyMedal("maxSpeed", "silver", "personal")

            } else if (maxSpeed >= medalsListSportSelectedMaxSpeed.get(2)) {
                recMaxSpeedGold = false
                recMaxSpeedSilver = false
                recMaxSpeedBronze = true
                notifyMedal("maxSpeed", "bronze", "personal")
            }
        }
    }

    /**
     * Creates and displays a notification about a new medal record.
     *
     * @param category The category of the record ("distance", "avgSpeed", or "maxSpeed")
     * @param medal The medal level ("gold", "silver", or "bronze")
     * @param scope The scope of the record, e.g., "personal"
     */
    private fun notifyMedal(category: String, medal: String, scope: String) {

        var CHANNEL_ID = "NEW $scope RECORD - $sportSelected"
        val CHANNEL_NAME = "Personal Records"
        val CHANNEL_DESCRIPTION = "New personal record notifications"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel if running on Android Oreo or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESCRIPTION
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Prepare notification text based on medal and category
        var textNotification = ""
        when (medal) {
            "gold" -> textNotification = "1"
            "silver" -> textNotification = "2"
            "bronze" -> textNotification = "3"
        }

        textNotification += R.string.personal_best_in
        when (category) {
            "distance" -> textNotification += R.string.distance_covered
            "avgSpeed" -> textNotification += R.string.average_speed
            "maxSpeed" -> textNotification += R.string.maximum_speed_reached
        }

        // Select icon based on medal type
        var iconNotification: Int = 0
        when (medal) {
            "gold" -> iconNotification = R.drawable.medalgold
            "silver" -> iconNotification = R.drawable.medalsilver
            "bronze" -> iconNotification = R.drawable.medalbronze
        }

        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconNotification)
            .setContentTitle(CHANNEL_ID)
            .setContentText(textNotification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Unique notification ID per category and medal
        var notificationId = 0
        when (category) {
            "distance" ->
                when (medal) {
                    "gold" -> notificationId = 11
                    "silver" -> notificationId = 12
                    "bronze" -> notificationId = 14
                }
            "avgSpeed" ->
                when (medal) {
                    "gold" -> notificationId = 21
                    "silver" -> notificationId = 22
                    "bronze" -> notificationId = 23
                }
            "maxSpeed" ->
                when (medal) {
                    "gold" -> notificationId = 31
                    "silver" -> notificationId = 32
                    "bronze" -> notificationId = 33
                }
        }

        // Check notification permission and send notification
        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return@with
            }
            // notificationId is a unique int for each notification that you must define.
            notify(notificationId, builder.build())
        }
    }

    /**
     * Location callback used to receive location updates.
     * When a new location is received, registers it if the timer is running.
     */
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

        /**
     * Checks whether the app has the necessary location permissions.
     *
     * @return true if both coarse and fine location permissions are granted, false otherwise
     */
    private fun checkPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Manages location updates by checking permissions and whether location services are enabled.
     * Requests location updates if everything is set, otherwise prompts the user to enable permissions or GPS.
     */
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

    /**
     * Checks if either GPS or Network location providers are enabled.
     *
     * @return true if either GPS or Network provider is enabled, false otherwise
     */
    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager
        = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Handles the logic when the user starts or stops the run.
     * If GPS is disabled at start, prompts the user to enable it.
     * Otherwise, starts or stops the run and updates UI accordingly.
     */
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

    /**
     * Starts or stops the running session.
     * Initializes run data and disables UI elements when starting.
     * Enables UI elements and stops timing when stopping.
     */
    private fun manageRun() {
        if (timeInSeconds.toInt() == 0) {

            dateRun = SimpleDateFormat("yyyy/MM/dd").format(Date())
            startTimeRun = SimpleDateFormat("HH:mm:ss").format(Date())

            fbCamera.isVisible = true

            swIntervalMode.isClickable = false
            npDurationInterval.isEnabled = false
            csbRunWalk.isEnabled = false

            swChallenges.isClickable = false
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

    /**
     * Enables or disables the run-related buttons and updates their appearance accordingly.
     *
     * @param e_reset Whether the reset button should be enabled
     * @param e_run Whether the start/stop run button should be enabled
     */
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

    /**
     * Starts the chronometer by scheduling the runnable that updates the timer.
     */
    private fun startTime() {
        mHandler = Handler(Looper.getMainLooper())
        chronometer.run()
    }

    /**
     * Stops the chronometer by removing scheduled callbacks to the runnable.
     */
    private fun stopTime() {
        mHandler?.removeCallbacks(chronometer)
    }

    /**
     * Runnable that increments the stopwatch timer every second,
     * checks for location updates, and handles interval mode logic.
     */
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

    /**
     * Updates the stopwatch display text based on the current elapsed time.
     */
    private fun updateStopWatch() {
        tvChrono.text = getFormattedStopWatch(timeInSeconds * 1000)
    }

    /**
     * Called when the reset button is clicked.
     * Saves the run data, updates UI and internal variables to their initial state.
     */
    private fun resetClicked() {

        savePreferences()
        saveDataRun()

        updateTotalsUsers()
        setLevelSport(sportSelected)

        showPopUp()

        resetTimeView()
        resetInterface()
        resetMedals()
    }

    /**
     * Saves the current run data to the Firestore database.
     * Includes duration, distance, speeds, GPS bounds, and medals earned.
     */
    private fun saveDataRun() {
        var id: String = userEmail + dateRun + startTimeRun
        id = id.replace("/", "")
        id = id.replace(":", "")

        var saveDuration = tvChrono.text.toString()

        var saveDistance = roundNumber(distance.toString(), 1)
        var saveAvgSpeed = roundNumber(avgSpeed.toString(), 1)
        var saveMaxSpeed = roundNumber(maxSpeed.toString(), 1)

        var centerLatitude = (minLatitude!! + maxLatitude!!) / 2
        var centerLongitude = (minLongitude!! + maxLongitude!!) / 2

        var medalsDistance = "none"
        var medalsAvgSpeed = "none"
        var medalsMaxSpeed = "none"

        if (recDistanceGold) medalsDistance = "gold"
        if (recDistanceSilver) medalsDistance = "silver"
        if (recDistanceBronze) medalsDistance = "bronze"

        if (recAvgSpeedGold) medalsAvgSpeed = "gold"
        if (recAvgSpeedSilver) medalsAvgSpeed = "silver"
        if (recAvgSpeedBronze) medalsAvgSpeed = "bronze"

        if (recMaxSpeedGold) medalsMaxSpeed = "gold"
        if (recMaxSpeedSilver) medalsMaxSpeed = "silver"
        if (recMaxSpeedBronze) medalsMaxSpeed = "bronze"

        var collection = "runs$sportSelected"
        var dbRun = FirebaseFirestore.getInstance()
        dbRun.collection(collection).document(id).set(hashMapOf(
            "user" to userEmail,
            "date" to dateRun,
            "startTime" to startTimeRun,
            "sport" to sportSelected,
            "activatedGPS" to activatedGPS,
            "duration" to saveDuration,
            "distance" to saveDistance.toDouble(),
            "avgSpeed" to saveAvgSpeed.toDouble(),
            "maxSpeed" to saveMaxSpeed.toDouble(),
            "minAltitude" to minAltitude,
            "maxAltitude" to maxAltitude,
            "minLatitude" to minLatitude,
            "maxLatitude" to maxLatitude,
            "minLongitude" to minLongitude,
            "maxLongitude" to maxLongitude,
            "centerLatitude" to centerLatitude,
            "centerLongitude" to centerLongitude,
            "medalDistance" to medalsDistance,
            "medalAvgSpeed" to medalsAvgSpeed,
            "medalMaxSpeed" to medalsMaxSpeed
        ))

        if (swIntervalMode.isChecked) {
            dbRun.collection(collection).document(id).update("intervalMode", true)
            dbRun.collection(collection).document(id).update("intervalDuration", npDurationInterval.value)
            dbRun.collection(collection).document(id).update("runningTime", tvRunningTime.text.toString())
            dbRun.collection(collection).document(id).update("walkingTime", tvWalkingTime.text.toString())
        }

        if (swChallenges.isChecked) {
            if (challengeDistance > 0f) {
                dbRun.collection(collection).document(id).update("challengeDistance", roundNumber(challengeDistance.toString(), 1).toDouble())
            }
            if (challengeDuration > 0) {
                dbRun.collection(collection).document(id).update("challengeDuration", getFormattedStopWatch(challengeDuration.toLong()))
            }
        }
    }

    /**
     * Deletes the current run and its linked data from Firestore.
     *
     * @param v The view that triggered the delete (usually a button)
     */
    fun deleteRun(v: View) {
        var id: String = userEmail + dateRun + startTimeRun
        id = id.replace(":", "")
        id = id.replace("/", "")

        var lyPopUpRun = findViewById<LinearLayout>(R.id.lyPopupRun)

        var currentRun = Runs()
        currentRun.distance = roundNumber(distance.toString(), 1).toDouble()
        currentRun.avgSpeed = roundNumber(avgSpeed.toString(), 1).toDouble()
        currentRun.maxSpeed = roundNumber(maxSpeed.toString(), 1).toDouble()
        currentRun.duration = tvChrono.text.toString()

        deleteRunAndLinkedData(id, sportSelected, lyPopUpRun, currentRun)
        loadMedalsUser()
        setLevelSport(sportSelected)
        closePopUpRun()
    }

    /**
     * Resets variables used during a running session to their initial default values.
     */
    private fun resetVariablesRun() {
        timeInSeconds = 0
        rounds = 1

        distance = 0.0
        maxSpeed = 0.0
        avgSpeed = 0.0

        minAltitude = null
        maxAltitude = null
        minLatitude = null
        maxLatitude = null
        minLongitude = null
        maxLongitude = null

        (listPoints as ArrayList<LatLng>).clear()

        challengeDistance = 0f
        challengeDuration = 0

        activatedGPS = true
        flagSavedLocation = false

    }

    /**
     * Resets the stopwatch UI and disables reset button while enabling run button.
     */
    private fun resetTimeView() {
        initStopWatch()
        manageEnableButtonRun(false, true)

        tvChrono.setTextColor(ContextCompat.getColor(this, R.color.white))
    }

    /**
     * Resets UI elements related to the running session, including distances, speeds, progress bars, and controls.
     */
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

    /**
     * Checks if the stopwatch is within the stopping time period and updates UI accordingly.
     *
     * @param secs Current stopwatch time in seconds
     */
    private fun checkStopRun(secs: Long) {
        var secAux: Long = secs
        while (secAux.toInt() > ROUND_INTERVAL) {
            secAux -= ROUND_INTERVAL
        }

        if (secAux.toInt() == TIME_RUNING) {
            tvChrono.setTextColor(ContextCompat.getColor(this, R.color.wii_yellow))

            val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
            lyRoundProgressBg.setBackgroundColor(ContextCompat.getColor(this, R.color.wii_yellow))
            lyRoundProgressBg.translationX = -widthAnimations.toFloat()
        } else {
            updateProgressBarRound(secs)
        }
    }

    /**
     * Checks if a new round has started and updates UI for round count and progress bar.
     *
     * @param secs Current stopwatch time in seconds
     */
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

    /**
     * Updates the position of a round progress bar background based on the elapsed seconds.
     * It calculates the translation movement depending on the current chrono color state
     * (running or walking) and animates the background accordingly.
     *
     * @param secs The total elapsed time in seconds.
     */
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

    /**
     * Initializes the navigation drawer view and sets up the header with the current user's email.
     */
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

    /**
     * Initializes the toolbar and sets up the drawer toggle to open/close the navigation drawer.
     */
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

    /**
     * Handles navigation item selections from the drawer menu.
     *
     * @param item The selected menu item.
     * @return True if the selection was handled.
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_item_record -> callRecordActivity()
            R.id.nav_item_clearPreferences -> alertClearPreferences()
            R.id.nav_item_signOut -> signOut()
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Signs out the current Firebase user and navigates back to the LoginActivity.
     */
    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    /**
     * Shows a confirmation dialog asking the user if they want to exit (sign out).
     */
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.exit_title)
            .setMessage(R.string.exit_message)
            .setPositiveButton(R.string.positive_yes) { _, _ ->
                signOut()
            }
            .setNegativeButton(R.string.negative_no, null)
            .show()
    }

    /**
     * Handles the back button press behavior.
     * Closes popups if visible, closes navigation drawer if open, resets running timer if active,
     * or shows exit confirmation dialog.
     */
    override fun onBackPressed() {
        //super.onBackPressed()

        if (lyPopUpRun.isVisible) {
            closePopUpRun()
        } else {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START)
                super.onBackPressed()
            } else {
                if (timeInSeconds > 0L) {
                    resetClicked()
                }
                showExitConfirmationDialog()
            }
        }
    }

    /**
     * Navigates to the RecordActivity.
     * If the start button is clicked and running, stops it before navigating.
     */
    private fun callRecordActivity() {
        if (startButtonClicked) {
            manageStartStop()
        }

        val intent = Intent(this, RecordActivity::class.java)
        startActivity(intent)
    }

    /**
     * Hides the running popup by translating its window out of view and setting visibility to false.
     */
    private fun hidePopUpRun() {
        var lyWindow = findViewById<LinearLayout>(R.id.lyWindow)
        lyWindow.translationX = 400f

        lyPopUpRun = findViewById(R.id.lyPopupRun)
        lyPopUpRun.isVisible = false
    }

    /**
     * Called when a generic popup close button is clicked.
     */
    fun closePopUp(v: View) {
        closePopUpRun()
    }

    /**
     * Closes the running popup, enables main layout interaction, resets variables related to
     * running, and reselects the current sport.
     */
    private fun closePopUpRun() {
        hidePopUpRun()
        val rlMain = findViewById<RelativeLayout>(R.id.rlMain)
        rlMain.isEnabled = true

        resetVariablesRun()
        selectSport(sportSelected)
    }

    /**
     * Shows the running popup, disables main layout interaction, animates the popup sliding in,
     * and loads its data.
     */
    private fun showPopUp() {
        val rlMain = findViewById<RelativeLayout>(R.id.rlMain)
        rlMain.isEnabled = false

        lyPopUpRun.isVisible = true

        var lyWindow = findViewById<LinearLayout>(R.id.lyWindow)
        ObjectAnimator.ofFloat(lyWindow, "translationX", 0f).apply {
            duration = 350L
            start()
        }

        loadDataPopUp()
    }

    /**
     * Loads and displays all relevant data in the running popup:
     * header info, medals, and run statistics.
     */
    private fun loadDataPopUp() {

        showHeaderPopUp()
        showMedals()
        showDataRun()
    }


    /**
     * Displays header information for the popup, including selected sport, current level, runs,
     * distance progress, and total time.
     */
    private fun showHeaderPopUp() {
        var csbRunsLevel = findViewById<CircularSeekBar>(R.id.csbRunsLevel)
        var csbDistanceLevel = findViewById<CircularSeekBar>(R.id.csbDistanceLevel)
        var tvTotalRunsLevel = findViewById<TextView>(R.id.tvTotalRunsLevel)
        var tvTotalDistanceLevel = findViewById<TextView>(R.id.tvTotalDistanceLevel)

        var ivSportSelected = findViewById<ImageView>(R.id.ivSportSelected)
        var ivCurrentLevel = findViewById<ImageView>(R.id.ivCurrentLevel)
        var tvTotalDistance = findViewById<TextView>(R.id.tvTotalDistance)
        var tvTotalTime = findViewById<TextView>(R.id.tvTotalTime)

        when (sportSelected) {
            "Bike" -> {
                levelSelectedSport = levelBike
                setLevelBike()
                ivSportSelected.setImageResource(R.mipmap.bike)
            }
            "RollerSkate" -> {
                levelSelectedSport = levelRollerSkate
                setLevelRollerSkate()
                ivSportSelected.setImageResource(R.mipmap.rollerskate)
            }
            "Running" -> {
                levelSelectedSport = levelRunning
                setLevelRunning()
                ivSportSelected.setImageResource(R.mipmap.running)
            }
        }

        var tvNumberLevel = findViewById<TextView>(R.id.tvNumberLevel)
        var levelText = "${getString(R.string.level)} ${levelSelectedSport.image!!.subSequence(6 ,7).toString()}"
        tvNumberLevel.text = levelText

        csbRunsLevel.max = levelSelectedSport.runsTarget!!.toFloat()
        csbRunsLevel.progress = totalsSelectedSport.totalRuns!!.toFloat()
        if (totalsSelectedSport.totalRuns!! > levelSelectedSport.runsTarget!!.toInt()) {
            csbRunsLevel.max = levelSelectedSport.runsTarget!!.toFloat()
            csbRunsLevel.progress = csbRunsLevel.max
        }

        tvTotalRunsLevel.text = "${totalsSelectedSport.totalRuns!!}/${levelSelectedSport.runsTarget!!}"

        var td = totalsSelectedSport.totalDistance!!
        var td_k: String = td.toString()
        if (td > 1000) td_k = (td / 1000).toInt().toString() + "K"

        var ld = levelSelectedSport.distanceTarget!!.toDouble()
        var ld_k: String = ld.toInt().toString()
        if (ld > 1000) ld_k = (ld / 1000).toInt().toString() + "K"

        tvTotalDistance.text = "${td_k}/${ld_k} kms"

        var porcent = (totalsSelectedSport.totalDistance!!.toDouble() * 100 / levelSelectedSport.distanceTarget!!.toDouble()).toInt()
        tvTotalDistanceLevel.text = "$porcent%"

        when (levelSelectedSport.image) {
            "level_1" -> ivCurrentLevel.setImageResource(R.drawable.level_1)
            "level_2" -> ivCurrentLevel.setImageResource(R.drawable.level_2)
            "level_3" -> ivCurrentLevel.setImageResource(R.drawable.level_3)
            "level_4" -> ivCurrentLevel.setImageResource(R.drawable.level_4)
            "level_5" -> ivCurrentLevel.setImageResource(R.drawable.level_5)
            "level_6" -> ivCurrentLevel.setImageResource(R.drawable.level_6)
            "level_7" -> ivCurrentLevel.setImageResource(R.drawable.level_7)
        }

        var formatedTime = getFormattedTotalTime(totalsSelectedSport.totalTime!!.toLong())
        tvTotalTime.text = getString(R.string.PopUpTotalTime) + formatedTime

    }

    /**
     * Displays medal icons and descriptions based on achieved records if GPS is activated,
     * otherwise hides medal section.
     */
    private fun showMedals() {
        if (activatedGPS) {
            val ivMedalDistance = findViewById<ImageView>(R.id.ivMedalDistance)
            val ivMedalAvgSpeed = findViewById<ImageView>(R.id.ivMedalAvgSpeed)
            val ivMedalMaxSpeed = findViewById<ImageView>(R.id.ivMedalMaxSpeed)

            val tvMedalDistanceTitle = findViewById<TextView>(R.id.tvMedalDistanceTitle)
            val tvMedalAvgSpeedTitle = findViewById<TextView>(R.id.tvMedalAvgSpeedTitle)
            val tvMedalMaxSpeedTitle = findViewById<TextView>(R.id.tvMedalMaxSpeedTitle)

            if (recDistanceGold) ivMedalDistance.setImageResource(R.drawable.medalgold)
            if (recDistanceSilver) ivMedalDistance.setImageResource(R.drawable.medalsilver)
            if (recDistanceBronze) ivMedalDistance.setImageResource(R.drawable.medalbronze)

            if (recDistanceGold || recDistanceSilver || recDistanceBronze) {
                tvMedalDistanceTitle.setText(R.string.medalDistanceDescription)
            }

            if (recAvgSpeedGold) ivMedalAvgSpeed.setImageResource(R.drawable.medalgold)
            if (recAvgSpeedSilver) ivMedalAvgSpeed.setImageResource(R.drawable.medalsilver)
            if (recAvgSpeedBronze) ivMedalAvgSpeed.setImageResource(R.drawable.medalbronze)

            if (recAvgSpeedGold || recAvgSpeedSilver || recAvgSpeedBronze) {
                tvMedalAvgSpeedTitle.setText(R.string.medalAvgSpeedDescription)
            }

            if (recMaxSpeedGold) ivMedalMaxSpeed.setImageResource(R.drawable.medalgold)
            if (recMaxSpeedSilver) ivMedalMaxSpeed.setImageResource(R.drawable.medalsilver)
            if (recMaxSpeedBronze) ivMedalMaxSpeed.setImageResource(R.drawable.medalbronze)

            if (recMaxSpeedGold || recMaxSpeedSilver || recMaxSpeedBronze) {
                tvMedalMaxSpeedTitle.setText(R.string.medalMaxSpeedDescription)
            }

        } else {
            val lyMedalsRun = findViewById<LinearLayout>(R.id.lyMedalsRun)
            setHeightLinearLayout(lyMedalsRun, 0)
        }
    }

    /**
     * Displays run-specific data in the popup, such as duration, challenge duration, intervals,
     * distance, unevenness, and speeds.
     */
    private fun showDataRun() {
        var tvDurationRun = findViewById<TextView>(R.id.tvDurationRun)
        var lyChallengeDurationRun = findViewById<LinearLayout>(R.id.lyChallengeDurationRun)
        var tvChallengeDurationRun = findViewById<TextView>(R.id.tvChallengeDurationRun)
        var lyIntervalRun = findViewById<LinearLayout>(R.id.lyIntervalRun)
        var tvIntervalRun = findViewById<TextView>(R.id.tvIntervalRun)
        var lyCurrentDistance = findViewById<LinearLayout>(R.id.lyCurrentDistance)
        var tvDistanceRun = findViewById<TextView>(R.id.tvDistanceRun)
        var lyChallengeDistancePopUp = findViewById<LinearLayout>(R.id.lyChallengeDistancePopUp)
        var tvChallengeDistanceRun = findViewById<TextView>(R.id.tvChallengeDistanceRun)
        var lyUnevennessRun = findViewById<LinearLayout>(R.id.lyUnevennessRun)
        var tvMaxUnevennessRun = findViewById<TextView>(R.id.tvMaxUnevennessRun)
        var tvMinUnevennessRun = findViewById<TextView>(R.id.tvMinUnevennessRun)
        var lyCurrentSpeeds = findViewById<LinearLayout>(R.id.lyCurrentSpeeds)
        var tvAvgSpeedRun = findViewById<TextView>(R.id.tvAvgSpeedRun)
        var tvMaxSpeedRun = findViewById<TextView>(R.id.tvMaxSpeedRun)

        tvDurationRun.setText(tvChrono.text)
        if (challengeDuration > 0) {
            setHeightLinearLayout(lyChallengeDurationRun, 140)
            tvChallengeDurationRun.setText(getFormattedStopWatch((challengeDuration * 1000).toLong()))
        } else {
            setHeightLinearLayout(lyChallengeDurationRun, 0)
        }

        if (swIntervalMode.isChecked) {
            setHeightLinearLayout(lyIntervalRun, 150)
            var details: String = "${npDurationInterval.value}mins. ("
            details += "${tvRunningTime.text} / ${tvWalkingTime.text})"

            tvIntervalRun.setText(details)
        } else {
            setHeightLinearLayout(lyIntervalRun, 0)
        }

        if (activatedGPS) {
            tvDistanceRun.setText(roundNumber(distance.toString(), 2))
            if (challengeDistance > 0f) {
                setHeightLinearLayout(lyChallengeDistancePopUp, 70)
                tvChallengeDistanceRun.setText(challengeDistance.toString())
            } else {
                setHeightLinearLayout(lyChallengeDistancePopUp, 0)
            }

            if (maxAltitude == null) {
                setHeightLinearLayout(lyUnevennessRun, 0)
            } else {
                setHeightLinearLayout(lyUnevennessRun, 120)
                tvMaxUnevennessRun.setText(maxAltitude!!.toInt().toString())
                tvMinUnevennessRun.setText(minAltitude!!.toInt().toString())
            }

            tvAvgSpeedRun.setText(roundNumber(avgSpeed.toString(), 1))
            tvMaxSpeedRun.setText(roundNumber(maxSpeed.toString(), 1))
        } else {

            setHeightLinearLayout(lyCurrentDistance, 0)
            setHeightLinearLayout(lyCurrentSpeeds, 0)
        }
    }

    /**
     * Starts the CameraActivity to take a picture associated with the current run.
     *
     * @param v The view triggering this action.
     */
    fun takePicture(v: View) {
        val intent = Intent(this, CameraActivity::class.java)
        val inParameter = intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        inParameter.putExtra("dateRun", dateRun)
        inParameter.putExtra("startTimeRun", startTimeRun)
        startActivity(intent)
    }
}