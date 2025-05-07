package com.francisco.raidorun

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.francisco.raidorun.LoginActivity.Companion.userEmail
import com.francisco.raidorun.Utility.animateViewOfFloat
import com.francisco.raidorun.Utility.animateViewOfInt
import com.francisco.raidorun.Utility.getSecFromWatch
import com.francisco.raidorun.Utility.setHeightLinearLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import me.tankery.lib.circularseekbar.CircularSeekBar

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawer: DrawerLayout

    private lateinit var swIntervalMode: Switch
    private lateinit var swChallenges: Switch

    private lateinit var npChallengeDistance: NumberPicker
    private lateinit var npChallengeDurationHH: NumberPicker
    private lateinit var npChallengeDurationMM: NumberPicker
    private lateinit var npChallengeDurationSS: NumberPicker

    private var challengeDistance: Float = 0f
    private var challengeDuration: Int = 0

    private lateinit var tvChrono: TextView

    private lateinit var csbRunWalk: CircularSeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initToolBar()
        initNavigationView()
        initObjects()
    }

    fun showDuration(v: View) {
        showChallenge("duration")
    }

    fun showDistance(v: View) {
        showChallenge("distance")
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

    private fun getChallengeDuration(hh: Int, mm: Int, ss: Int) {
        var hours: String = hh.toString()
        if (hh < 10) hours = "0" + hours
        var minutes: String = mm.toString()
        if (mm < 10) minutes = "0" + minutes
        var seconds: String = ss.toString()
        if (ss < 10) seconds = "0" + seconds

        challengeDuration = getSecFromWatch("${hours}:${minutes}:${seconds}")
    }

    private fun initStopWatch() {
        tvChrono.text = getString(R.string.init_stop_watch_value)
    }

    private fun initObjects() {
        tvChrono = findViewById(R.id.tvChrono)
        tvChrono.setTextColor(ContextCompat.getColor(this, R.color.white))
        initStopWatch()

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

        swIntervalMode = findViewById(R.id.swIntervalMode)
        swChallenges = findViewById(R.id.swChallenges)

        csbRunWalk = findViewById(R.id.csbRunWalk)

        npChallengeDistance = findViewById(R.id.npChallengeDistance)
        npChallengeDurationHH = findViewById(R.id.npChallengeDurationHH)
        npChallengeDurationMM = findViewById(R.id.npChallengeDurationMM)
        npChallengeDurationSS = findViewById(R.id.npChallengeDurationSS)

        csbRunWalk.setOnSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(
                circularSeekBar: CircularSeekBar?,
                progress: Float,
                fromUser: Boolean
            ) {
                var STEPS_UX: Int = 15
                var set: Int = 0
                var p = progress.toInt()

                if (p%STEPS_UX != 0) {
                    while (p >= 60) p -= 60
                    while (p >= STEPS_UX) p -= STEPS_UX
                    if (STEPS_UX - p > STEPS_UX / 2) set = -1 * p
                    else set = STEPS_UX - p

                    csbRunWalk.progress = csbRunWalk.progress + set
                }
            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {
            }

        })
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

    override fun onBackPressed() {
        //super.onBackPressed()

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
            super.onBackPressed()
        } else {
            showExitConfirmationDialog()
        }
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

    private fun initNavigationView() {
        var navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        var headerView: View = LayoutInflater.from(this).inflate(R.layout.nav_header_main, navigationView, false)
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
            this, drawer, toolBar, R.string.bar_title, R.string.navigation_drawer_close)

        drawer.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent (this, LoginActivity::class.java))
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.nav_item_record -> callRecordActivity()
            R.id.nav_item_signOut -> signOut()
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun callRecordActivity() {
        val intent = Intent(this, RecordActivity::class.java)
        startActivity(intent)
    }
}