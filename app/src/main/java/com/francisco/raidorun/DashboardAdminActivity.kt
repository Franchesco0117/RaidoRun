package com.francisco.raidorun

import android.content.Intent
import android.graphics.Color
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
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
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
    private lateinit var barChartExerciseTime: BarChart
    private lateinit var pieChartIntervalMode: PieChart
    private lateinit var barChartAverageDuration: BarChart
    private lateinit var barChartUserFrequency: HorizontalBarChart
    private lateinit var pieChartTargetUsage: PieChart

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
        barChartExerciseTime = findViewById(R.id.barChartExerciseTime)
        pieChartIntervalMode = findViewById(R.id.pieChartIntervalMode)
        barChartAverageDuration = findViewById(R.id.barChartAverageDuration)
        barChartUserFrequency = findViewById(R.id.barChartUserFrequency)
        pieChartTargetUsage = findViewById(R.id.pieChartTargetUsage)
        
        loadKpiRunsPieChart()
        loadActiveUsersBarChart()
        loadKilometersBarChart()
        loadExerciseTimeBarChart()
        loadIntervalModePieChart()
        loadAverageDurationBarChart()
        loadUserFrequencyBarChart()
        loadTargetUsagePieChart()
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
        val labels = listOf("Bike", "Running", "RollerSkate")
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
        pieChartKpiRuns.centerText = "Total runs"
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

        val dataSet = BarDataSet(entries, "Active users")
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
        val labels = listOf("Bike", "Running", "RollerSkate")
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

        val dataSet = BarDataSet(entries, "Kilometers")
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

    private fun loadExerciseTimeBarChart() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = SimpleDateFormat("yyyy/MM/dd").format(calendar.time)
        
        // Mapa para almacenar los minutos totales por día
        val dailyMinutes = TreeMap<String, Int>()
        var loadedCollections = 0

        for (collection in collections) {
            db.collection(collection)
                .whereGreaterThanOrEqualTo("date", sevenDaysAgo)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val date = document.getString("date") ?: continue
                        val duration = document.getString("duration") ?: continue
                        
                        // Convertir duración HH:mm:ss a minutos
                        val minutes = durationToMinutes(duration)
                        
                        // Agregar al total del día
                        dailyMinutes[date] = (dailyMinutes[date] ?: 0) + minutes
                    }
                    
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        showExerciseTimeBarChart(dailyMinutes)
                    }
                }
                .addOnFailureListener {
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        showExerciseTimeBarChart(dailyMinutes)
                    }
                }
        }
    }

    private fun durationToMinutes(duration: String): Int {
        val parts = duration.split(":")
        if (parts.size != 3) return 0
        
        return try {
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            val seconds = parts[2].toInt()
            
            hours * 60 + minutes + (seconds / 60)
        } catch (e: NumberFormatException) {
            0
        }
    }

    private fun formatMinutesToHoursAndMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            "${hours}h ${mins}m"
        } else {
            "${mins}m"
        }
    }

    private fun showExerciseTimeBarChart(dailyMinutes: TreeMap<String, Int>) {
        val entries = ArrayList<BarEntry>()
        val dateLabels = ArrayList<String>()
        
        // Obtener los últimos 7 días
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale("es", "ES"))
        
        for (i in 6 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(calendar.time)
            val dayName = dayFormat.format(calendar.time).capitalize()
            
            entries.add(BarEntry((6 - i).toFloat(), (dailyMinutes[date] ?: 0).toFloat()))
            dateLabels.add(dayName)
            
            calendar.add(Calendar.DAY_OF_YEAR, i) // Restaurar la fecha
        }

        val dataSet = BarDataSet(entries, "Exercise time")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        
        val data = BarData(dataSet)
        data.setValueTextSize(12f)
        data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return formatMinutesToHoursAndMinutes(value.toInt())
            }
        })
        
        barChartExerciseTime.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            
            // Configurar eje X
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(dateLabels)
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
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatMinutesToHoursAndMinutes(value.toInt())
                    }
                }
            }
            
            animateY(1000)
            invalidate()
        }
    }

    private fun loadIntervalModePieChart() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        var intervalModeCount = 0
        var normalModeCount = 0
        var loadedCollections = 0

        for (collection in collections) {
            db.collection(collection)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val isIntervalMode = document.getBoolean("intervalMode") ?: false
                        if (isIntervalMode) {
                            intervalModeCount++
                        } else {
                            normalModeCount++
                        }
                    }
                    
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        showIntervalModePieChart(intervalModeCount, normalModeCount)
                    }
                }
                .addOnFailureListener {
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        showIntervalModePieChart(intervalModeCount, normalModeCount)
                    }
                }
        }
    }

    private fun showIntervalModePieChart(intervalModeCount: Int, normalModeCount: Int) {
        val entries = ArrayList<PieEntry>()
        val total = intervalModeCount + normalModeCount
        
        if (total > 0) {
            val intervalPercentage = (intervalModeCount.toFloat() / total) * 100
            val normalPercentage = (normalModeCount.toFloat() / total) * 100
            
            entries.add(PieEntry(intervalPercentage, "Interval mode"))
            entries.add(PieEntry(normalPercentage, "Normal mode"))
            
            val dataSet = PieDataSet(entries, "")
            dataSet.colors = listOf(
                Color.rgb(104, 159, 56),  // Verde para intervalos
                Color.rgb(3, 169, 244)     // Azul para normal
            )
            
            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter(pieChartIntervalMode))
            data.setValueTextSize(14f)
            data.setValueTextColor(Color.WHITE)
            
            pieChartIntervalMode.apply {
                this.data = data
                description.isEnabled = false
                setUsePercentValues(true)
                setEntryLabelColor(Color.WHITE)
                setEntryLabelTextSize(12f)
                legend.textSize = 12f
                legend.textColor = Color.BLACK
                centerText = "Total: $total\nruns"
                setCenterTextSize(14f)
                setHoleColor(Color.TRANSPARENT)
                animateY(1000)
                invalidate()
            }
        } else {
            // Si no hay datos, mostrar un mensaje o gráfico vacío
            pieChartIntervalMode.setNoDataText("No data available")
            pieChartIntervalMode.invalidate()
        }
    }

    private fun loadAverageDurationBarChart() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        val labels = listOf("Bike", "Running", "RollerSkate")
        val totalMinutes = mutableListOf(0L, 0L, 0L)
        val counts = mutableListOf(0, 0, 0)
        var loadedCollections = 0

        for ((i, collection) in collections.withIndex()) {
            db.collection(collection)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val duration = document.getString("duration") ?: continue
                        val minutes = durationToMinutes(duration)
                        totalMinutes[i] += minutes
                        counts[i]++
                    }
                    
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        val averages = totalMinutes.zip(counts) { total, count ->
                            if (count > 0) total.toFloat() / count else 0f
                        }
                        showAverageDurationBarChart(labels, averages)
                    }
                }
                .addOnFailureListener {
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        val averages = totalMinutes.zip(counts) { total, count ->
                            if (count > 0) total.toFloat() / count else 0f
                        }
                        showAverageDurationBarChart(labels, averages)
                    }
                }
        }
    }

    private fun showAverageDurationBarChart(labels: List<String>, averageMinutes: List<Float>) {
        val entries = ArrayList<BarEntry>()
        for (i in averageMinutes.indices) {
            entries.add(BarEntry(i.toFloat(), averageMinutes[i]))
        }

        val dataSet = BarDataSet(entries, "Average duration")
        dataSet.colors = listOf(
            Color.rgb(233, 30, 99),  // Rosa para Bicicleta
            Color.rgb(156, 39, 176),  // Morado para Running
            Color.rgb(255, 193, 7)    // Amarillo para RollerSkate
        )

        val data = BarData(dataSet)
        data.setValueTextSize(12f)
        data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return formatMinutesToHoursAndMinutes(value.toInt())
            }
        })
        
        barChartAverageDuration.apply {
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
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatMinutesToHoursAndMinutes(value.toInt())
                    }
                }
            }
            
            animateY(1000)
            invalidate()
        }
    }

    private fun loadUserFrequencyBarChart() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        val userRunCounts = HashMap<String, Int>()
        var loadedCollections = 0

        for (collection in collections) {
            db.collection(collection)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val user = document.getString("user") ?: continue
                        userRunCounts[user] = (userRunCounts[user] ?: 0) + 1
                    }
                    
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        // Ordenar usuarios por cantidad de carreras (descendente)
                        val sortedUsers = userRunCounts.entries
                            .sortedByDescending { it.value }
                            .take(10) // Mostrar solo los 10 usuarios más activos
                        
                        showUserFrequencyBarChart(sortedUsers)
                    }
                }
                .addOnFailureListener {
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        val sortedUsers = userRunCounts.entries
                            .sortedByDescending { it.value }
                            .take(10)
                        
                        showUserFrequencyBarChart(sortedUsers)
                    }
                }
        }
    }

    private fun showUserFrequencyBarChart(sortedUsers: List<Map.Entry<String, Int>>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        
        // Crear entradas para el gráfico
        sortedUsers.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
            // Simplificar el correo para la visualización (quitar el dominio)
            labels.add(entry.key.substringBefore("@"))
        }

        val dataSet = BarDataSet(entries, "Runs per user")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        
        val data = BarData(dataSet)
        data.setValueTextSize(12f)
        data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        })
        
        barChartUserFrequency.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            
            // Configurar eje X (vertical en gráfico horizontal)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = 45f // Rotar etiquetas para mejor lectura
                textSize = 10f
            }
            
            // Configurar eje Y (horizontal en gráfico horizontal)
            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
                granularity = 1f
                // Asegurar que solo se muestren valores enteros
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            }
            
            // Ajustar márgenes para las etiquetas
            setExtraOffsets(10f, 10f, 30f, 30f)
            
            animateY(1000)
            invalidate()
        }
    }

    private fun loadTargetUsagePieChart() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        var runsWithTarget = 0
        var runsWithoutTarget = 0
        var loadedCollections = 0

        for (collection in collections) {
            db.collection(collection)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val hasDistanceTarget = document.contains("distanceTarget") && 
                                              document.get("distanceTarget") != null
                        val hasDurationTarget = document.contains("challengeDuration") && 
                                              document.getString("challengeDuration")?.isNotEmpty() == true
                        
                        if (hasDistanceTarget || hasDurationTarget) {
                            runsWithTarget++
                        } else {
                            runsWithoutTarget++
                        }
                    }
                    
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        showTargetUsagePieChart(runsWithTarget, runsWithoutTarget)
                    }
                }
                .addOnFailureListener {
                    loadedCollections++
                    if (loadedCollections == collections.size) {
                        showTargetUsagePieChart(runsWithTarget, runsWithoutTarget)
                    }
                }
        }
    }

    private fun showTargetUsagePieChart(runsWithTarget: Int, runsWithoutTarget: Int) {
        val entries = ArrayList<PieEntry>()
        val total = runsWithTarget + runsWithoutTarget
        
        if (total > 0) {
            val withTargetPercentage = (runsWithTarget.toFloat() / total) * 100
            val withoutTargetPercentage = (runsWithoutTarget.toFloat() / total) * 100
            
            entries.add(PieEntry(withTargetPercentage, "With target"))
            entries.add(PieEntry(withoutTargetPercentage, "Without target"))
            
            val dataSet = PieDataSet(entries, "")
            dataSet.colors = listOf(
                Color.rgb(255, 193, 7),  // Amarillo para carreras con objetivo
                Color.rgb(158, 158, 158)  // Gris para carreras sin objetivo
            )
            
            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter(pieChartTargetUsage))
            data.setValueTextSize(14f)
            data.setValueTextColor(Color.WHITE)
            
            pieChartTargetUsage.apply {
                this.data = data
                description.isEnabled = false
                setUsePercentValues(true)
                setEntryLabelColor(Color.WHITE)
                setEntryLabelTextSize(12f)
                legend.textSize = 12f
                legend.textColor = Color.BLACK
                centerText = "Total: $total\nruns"
                setCenterTextSize(14f)
                setHoleColor(Color.TRANSPARENT)
                
                // Agregar detalle en el texto central
                val withTargetText = "${runsWithTarget} with target"
                val withoutTargetText = "${runsWithoutTarget} without target"
                centerText = "Total: $total runs\n$withTargetText\n$withoutTargetText"
                
                animateY(1000)
                invalidate()
            }
        } else {
            pieChartTargetUsage.setNoDataText("No data available")
            pieChartTargetUsage.invalidate()
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