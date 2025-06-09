package com.francisco.raidorun

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * TermsConditionsActivity
 *
 * Activity responsible for displaying the Terms and Conditions screen.
 * Initializes the toolbar with a back button and appropriate styling.
 *
 * Author: Francisco Castro
 * Created: 9/MAR/2025
 */
class TermsConditionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_conditions)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_terms)
        setSupportActionBar(toolbar)
        
        // Set title from string resource
        supportActionBar?.title = getString(R.string.bar_title_terms)
        
        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // Set toolbar colors
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_light))
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.gray_dark))
    }

    /**
     * Handles the toolbar back button click event to navigate back to the previous screen.
     *
     * @param item The menu item that was selected.
     * @return True if the back button was handled, false otherwise.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}