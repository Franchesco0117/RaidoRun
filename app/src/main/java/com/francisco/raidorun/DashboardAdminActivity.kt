package com.francisco.raidorun

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToInt

class DashboardAdminActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawer: DrawerLayout
    private lateinit var userEmail: String

    private lateinit var pieChartKpiRuns: PieChart
    private lateinit var barChartActiveUsers: BarChart
    private lateinit var barChartKilometers: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)

        // Get current user email
        userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

        initToolBar()
        initNavigationView()

        pieChartKpiRuns = findViewById(R.id.pieChartKpiRuns)
        barChartActiveUsers = findViewById(R.id.barChartActiveUsers)
        barChartKilometers = findViewById(R.id.barChartKilometers)
        
        loadKpiRunsPieChart()
        loadActiveUsersBarChart()
        loadKilometersBarChart()
    }

    private fun initToolBar() {
        val toolBar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolBar_admin)
        setSupportActionBar(toolBar)

        drawer = findViewById(R.id.drawer_layout_admin)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolBar, R.string.bar_title, R.string.navigation_drawer_close
        )

        drawer.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun initNavigationView() {
        val navigationView: NavigationView = findViewById(R.id.nav_view_admin)
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_item_signOut -> signOut()
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadKpiRunsPieChart() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        val labels = listOf("Bicicleta", "Running", "RollerSkate")
        val counts = mutableListOf(0, 0, 0)

        var loaded = 0

        for ((i, col) in collections.withIndex()) {
            db.collection(col).get().addOnSuccessListener { documents ->
                counts[i] = documents.size()
                loaded++
                if (loaded == collections.size) {
                    showPieChart(labels, counts)
                }
            }.addOnFailureListener {
                loaded++
                if (loaded == collections.size) {
                    showPieChart(labels, counts)
                }
            }
        }
    }

    private fun showPieChart(labels: List<String>, counts: List<Int>) {
        val entries = ArrayList<PieEntry>()
        for (i in labels.indices) {
            entries.add(PieEntry(counts[i].toFloat(), labels[i]))
        }
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        val data = PieData(dataSet)
        data.setValueTextSize(16f)
        pieChartKpiRuns.data = data
        pieChartKpiRuns.description.isEnabled = false
        pieChartKpiRuns.centerText = "Total de carreras"
        pieChartKpiRuns.animateY(1000)
        pieChartKpiRuns.invalidate()
    }

    private fun loadActiveUsersBarChart() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = SimpleDateFormat("yyyy/MM/dd").format(calendar.time)
        
        val activeUsers = HashMap<String, Int>()
        var loadedCollections = 0

        for (collection in collections) {
            db.collection(collection)
                .whereGreaterThanOrEqualTo("date", sevenDaysAgo)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val user = document.getString("user") ?: continue
                        activeUsers[user] = (activeUsers[user] ?: 0) + 1
                    }
                    
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        showActiveUsersBarChart(activeUsers.size)
                    }
                }
                .addOnFailureListener {
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        showActiveUsersBarChart(activeUsers.size)
                    }
                }
        }
    }

    private fun showActiveUsersBarChart(activeUsersCount: Int) {
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, activeUsersCount.toFloat()))

        val dataSet = BarDataSet(entries, "Usuarios activos")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        
        val data = BarData(dataSet)
        data.setValueTextSize(16f)
        
        barChartActiveUsers.apply {
            this.data = data
            description.isEnabled = false
            xAxis.setDrawLabels(false)
            axisRight.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            animateY(1000)
            invalidate()
        }
    }

    private fun loadKilometersBarChart() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        val labels = listOf("Bicicleta", "Running", "RollerSkate")
        val distances = mutableListOf(0.0, 0.0, 0.0)
        
        var loadedCollections = 0

        for ((i, collection) in collections.withIndex()) {
            db.collection(collection)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val distance = document.getDouble("distance") ?: 0.0
                        distances[i] += distance
                    }
                    
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        showKilometersBarChart(labels, distances)
                    }
                }
                .addOnFailureListener {
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        showKilometersBarChart(labels, distances)
                    }
                }
        }
    }

    private fun showKilometersBarChart(labels: List<String>, distances: List<Double>) {
        val entries = ArrayList<BarEntry>()
        for (i in distances.indices) {
            // Redondear a 1 decimal
            val roundedDistance = (distances[i] * 10.0).roundToInt() / 10.0
            entries.add(BarEntry(i.toFloat(), roundedDistance.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Kilómetros")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        
        val data = BarData(dataSet)
        data.setValueTextSize(12f)
        data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value}km"
            }
        })
        
        barChartKilometers.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            
            // Configurar eje X
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            
            // Configurar eje Y derecho
            axisRight.isEnabled = false
            
            // Configurar eje Y izquierdo
            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
            }
            
            animateY(1000)
            invalidate()
        }
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
} 