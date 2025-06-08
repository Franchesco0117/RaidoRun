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

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val SPLASH_DELAY = 4000L // 4 seconds
    private lateinit var auth: FirebaseAuth
    private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        }
        
        setContentView(R.layout.activity_splash)
        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            keepSplashOnScreen = false
            checkUserLoginStatus()
        }, SPLASH_DELAY)
    }

    private fun checkUserLoginStatus() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuario está logueado, inicializar variables y ir a MainActivity o DashboardAdminActivity
            userEmail = currentUser.email ?: ""
            providerSession = currentUser.providerId
            
            if (userEmail == "admin@raidhorun.com") {
                startActivity(Intent(this, DashboardAdminActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        } else {
            // Usuario no está logueado, ir a LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
} 