package com.francisco.raidorun.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.raidorun.onboarding.OnBoardingAdapter
import com.francisco.raidorun.MainActivity
import com.francisco.raidorun.R
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class OnBoardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: DotsIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.onboardingViewPager)
        dotsIndicator = findViewById(R.id.dotsIndicator)

        val adapter = OnBoardingAdapter(this)
        viewPager.adapter = adapter
        dotsIndicator.attachTo(viewPager)

        findViewById<View>(R.id.skipButton).setOnClickListener {
            navigateToMainActivity()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 2) { // Last page
                    findViewById<View>(R.id.skipButton).setOnClickListener {
                        navigateToMainActivity()
                    }
                }
            }
        })
    }

    private fun navigateToMainActivity() {
        // Save that user has seen onboarding
        getSharedPreferences("onboarding_pref", MODE_PRIVATE)
            .edit()
            .putBoolean("has_seen_onboarding", true)
            .apply()

        // Navigate to MainActivity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
} 