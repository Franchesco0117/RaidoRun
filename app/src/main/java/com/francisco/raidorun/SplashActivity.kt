package com.francisco.raidorun

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.francisco.raidorun.LoginActivity.Companion.providerSession
import com.francisco.raidorun.LoginActivity.Companion.userEmail
import com.google.firebase.auth.FirebaseAuth

/**
 * SplashActivity
 *
 * Activity that serves as the splash screen for the application.
 * Displays a splash screen for a fixed duration and redirects the user
 * based on authentication status:
 * - If user is authenticated: navigates to MainActivity or DashboardAdminActivity.
 * - If not authenticated: navigates to LoginActivity.
 *
 * Requirements:
 * - Firebase Authentication configured.
 * - Splash screen compatible with Android 12+ (API 31) using SplashScreen API.
 *
 * Author: Francisco Castro
 * Created: 6/JUN/2025
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val SPLASH_DELAY = 4000L // 4 seconds
    private lateinit var auth: FirebaseAuth
    private var keepSplashOnScreen = true

    /**
     * Initializes the splash screen and determines user navigation flow after delay.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        // Only keep splash on screen conditionally for Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        }
        
        setContentView(R.layout.activity_splash)
        auth = FirebaseAuth.getInstance()

        // Delay before moving to the next activity
        Handler(Looper.getMainLooper()).postDelayed({
            keepSplashOnScreen = false
            checkUserLoginStatus()
        }, SPLASH_DELAY)
    }

    /**
     * Checks if user is logged in and navigates accordingly:
     * - Admin email → DashboardAdminActivity
     * - Regular user → MainActivity
     * - Not logged in → LoginActivity
     */
    private fun checkUserLoginStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is logged in
            userEmail = currentUser.email ?: ""
            providerSession = currentUser.providerId
            
            if (userEmail == "admin@raidhorun.com") {
                startActivity(Intent(this, DashboardAdminActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        } else {
            // User is not logged in
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
} 