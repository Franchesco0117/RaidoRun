package com.francisco.raidorun

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var tvHelloWorld: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvHelloWorld = findViewById(R.id.tvHelloWorld)

        tvHelloWorld.setOnClickListener {
            signOut()
        }
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent (this, LoginActivity::class.java))
    }
}