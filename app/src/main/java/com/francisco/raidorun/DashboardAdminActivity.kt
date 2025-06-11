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

/**
 * DashboardAdminActivity
 *
 * This activity displays an administrative dashboard with visual statistics
 * related to exercise sessions recorded by users in the following Firebase collections:
 * runsBike, runsRunning, and runsRollerSkate.
 *
 * Includes the following charts:
 * - Bar chart showing total exercise time per day over the last 7 days.
 * - Pie chart displaying the proportion of interval mode usage.
 * - Bar chart showing the average duration per exercise type.
 * - Bar chart highlighting the top 10 most active users.
 * - Pie chart showing the use of exercise targets (distance or duration).
 *
 * Also handles side navigation (NavigationDrawer) and allows the admin to sign out.
 *
 * Implements:
 * - NavigationView.OnNavigationItemSelectedListener: to handle side menu item selection.
 *
 * Requirements:
 * - Access to Firebase Firestore to retrieve activity data.
 * - Access to FirebaseAuth to manage the administrator session.
 *
 * Author: [Francisco Castro]
 * Created: [7/JUN/2025]
 */
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

    /**
     * Initializes and configures the app toolbar with a navigation drawer toggle.
     */
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

    /**
     * Sets up the navigation view and its item selection listener.
     */
    private fun initNavigationView() {
        val navigationView: NavigationView = findViewById(R.id.nav_view_admin)
        navigationView.setNavigationItemSelectedListener(this)
    }

    /**
     * Handles navigation drawer item selection.
     *
     * @param item The selected menu item.
     * @return True if the event was handled.
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_item_statistics -> {
                // Ya estamos en la pantalla de estadísticas, solo cerramos el drawer
                drawer.closeDrawer(GravityCompat.START)
            }
            R.id.nav_item_users -> {
                startActivity(Intent(this, UsersManagementActivity::class.java))
            }
            R.id.nav_item_signOut -> signOut()
        }

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * Loads and displays a pie chart representing the number of runs by type (Bike, Running, RollerSkate).
     * Aggregates data from multiple Firestore collections.
     */
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

    /**
     * Displays the KPI pie chart using the provided labels and counts.
     *
     * @param labels List of labels for each chart segment.
     * @param counts Corresponding run counts per label.
     */
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

    /**
     * Loads and counts users active in the past 7 days from all run collections.
     * Then renders a bar chart showing the total number of active users.
     */
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

    /**
     * Displays a bar chart with the total number of active users in the past 7 days.
     *
     * @param activeUsersCount The number of unique users.
     */
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

    /**
     * Loads the total kilometers recorded in each run type collection (Bike, Running, RollerSkate),
     * then displays them in a grouped bar chart.
     */
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

    /**
     * Displays a bar chart with total kilometers per sport type.
     *
     * @param labels Labels for each sport type.
     * @param distances List of total distances in kilometers per sport.
     */
    private fun showKilometersBarChart(labels: List<String>, distances: List<Double>) {
        val entries = ArrayList<BarEntry>()
        for (i in distances.indices) {
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

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }

            axisRight.isEnabled = false

            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
            }
            
            animateY(1000)
            invalidate()
        }
    }

    /**
     * Loads data from Firebase for the past 7 days to calculate the total exercise time per day
     * (in minutes), and displays this data in a bar chart.
     */
    private fun loadExerciseTimeBarChart() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = SimpleDateFormat("yyyy/MM/dd").format(calendar.time)

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

                        val minutes = durationToMinutes(duration)

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

    /**
     * Converts a time duration string in "HH:mm:ss" format to its total equivalent in minutes.
     *
     * @param duration The duration string to convert.
     * @return Total duration in minutes, or 0 if the input is invalid.
     */
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

    /**
     * Converts a number of minutes into a formatted string like "1h 30m" or "45m".
     *
     * @param minutes The total number of minutes.
     * @return A human-readable string representing hours and minutes.
     */
    private fun formatMinutesToHoursAndMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            "${hours}h ${mins}m"
        } else {
            "${mins}m"
        }
    }

    /**
     * Displays a bar chart of total daily exercise time using data from the last 7 days.
     *
     * @param dailyMinutes A map of dates to total exercise minutes per day.
     */
    private fun showExerciseTimeBarChart(dailyMinutes: TreeMap<String, Int>) {
        val entries = ArrayList<BarEntry>()
        val dateLabels = ArrayList<String>()

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("EEE", Locale("es", "ES"))
        
        for (i in 6 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(calendar.time)
            val dayName = dayFormat.format(calendar.time).capitalize()
            
            entries.add(BarEntry((6 - i).toFloat(), (dailyMinutes[date] ?: 0).toFloat()))
            dateLabels.add(dayName)

            calendar.add(Calendar.DAY_OF_YEAR, i)
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

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(dateLabels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }

            axisRight.isEnabled = false

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

    /**
     * Queries Firebase to count how many times interval mode was used versus normal mode, then
     * displays the results in a pie chart with percentages.
     */
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

    /**
     * Displays a pie chart representing the percentage of exercise sessions
     * that used interval mode vs. normal mode.
     *
     * @param intervalModeCount Number of sessions using interval mode.
     * @param normalModeCount Number of sessions using normal mode.
     */
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
                Color.rgb(104, 159, 56),
                Color.rgb(3, 169, 244)
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
            pieChartIntervalMode.setNoDataText("No data available")
            pieChartIntervalMode.invalidate()
        }
    }

    /**
     * Calculates the average session duration for each activity type (Bike, Running, RollerSkate),
     * and visualizes the results in a bar chart.
     */
    private fun loadAverageDurationBarChart() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        val labels = listOf("Bike", "Running", "RollerSkate")
        val totalMinutes = mutableListOf(0.0, 0.0, 0.0)
        val counts = mutableListOf(0, 0, 0)
        var loadedCollections = 0

        for ((i, collection) in collections.withIndex()) {
            db.collection(collection)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val duration = document.getString("duration")
                        if (duration != null && duration.isNotEmpty()) {
                            val minutes = durationToMinutes(duration)
                            if (minutes > 0) {
                                totalMinutes[i] += minutes.toDouble()
                                counts[i]++
                            }
                        }
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

    /**
     * Displays a bar chart showing the average exercise duration (in minutes)
     * per activity type using distinct colors for each category.
     *
     * @param labels A list of activity labels (e.g., "Bike", "Running", "RollerSkate").
     * @param averageMinutes A list of average durations in minutes for each activity type.
     */
    private fun showAverageDurationBarChart(labels: List<String>, averageMinutes: List<Float>) {
        val entries = ArrayList<BarEntry>()
        for (i in averageMinutes.indices) {
            val roundedValue = (averageMinutes[i] * 10).roundToInt() / 10f
            entries.add(BarEntry(i.toFloat(), roundedValue))
        }

        val dataSet = BarDataSet(entries, "Average duration per sport")
        dataSet.colors = listOf(
            Color.rgb(233, 30, 99),
            Color.rgb(156, 39, 176),
            Color.rgb(255, 193, 7)
        )

        val data = BarData(dataSet)
        data.setValueTextSize(12f)
        data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return formatMinutesToHoursAndMinutes(value.roundToInt())
            }
        })
        
        barChartAverageDuration.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = true
            legend.textSize = 12f
            setFitBars(true)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 12f
            }

            axisRight.isEnabled = false

            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
                textSize = 12f
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return formatMinutesToHoursAndMinutes(value.roundToInt())
                    }
                }
            }
            
            animateY(1000)
            invalidate()
        }
    }

    /**
     * Queries Firebase to count how many sessions each user has logged, and shows a bar chart with
     * the top 10 most active users.
     */
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
                        val sortedUsers = userRunCounts.entries
                            .sortedByDescending { it.value }
                            .take(10)
                        
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

    /**
     * Displays a bar chart representing the most active users based on the number of recorded sessions.
     * Only the email prefix (text before "@") is shown as the label for each user.
     *
     * @param sortedUsers A list of user-session count pairs sorted in descending order by frequency.
     */
    private fun showUserFrequencyBarChart(sortedUsers: List<Map.Entry<String, Int>>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        sortedUsers.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
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

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
                labelRotationAngle = 45f
                textSize = 10f
            }

            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
                granularity = 1f
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            }

            setExtraOffsets(10f, 10f, 30f, 30f)
            
            animateY(1000)
            invalidate()
        }
    }

    /**
     * Counts how many exercise sessions include a target (either distanceTarget or challengeDuration)
     * and how many do not, then visualizes this in a pie chart.
     */
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

    /**
     * Generates and displays a pie chart showing the proportion of recorded sessions that had a target
     * set versus those that did not. The chart uses distinct colors and displays a breakdown in the center.
     *
     * @param runsWithTarget Number of sessions that had a defined target.
     * @param runsWithoutTarget Number of sessions without any defined target.
     */
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
                Color.rgb(255, 193, 7),
                Color.rgb(158, 158, 158)
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

    /**
     * Logs the current user out of the app and redirects them to the login screen (LoginActivity).
     */
    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    /**
     * Handles the "back" button behavior: if the navigation drawer is open, it closes it;
     * otherwise, it performs the default action.
     */
    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
} 