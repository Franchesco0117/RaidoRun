package com.francisco.raidorun

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TermsConditionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_conditions)

        val toolbar : androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_terms)
        setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.bar_title_terms)
    }
}