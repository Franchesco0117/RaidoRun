package com.francisco.raidorun

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.francisco.raidorun.LoginActivity.Companion.userEmail
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawer: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initToolBar()
        initNavigationView()
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