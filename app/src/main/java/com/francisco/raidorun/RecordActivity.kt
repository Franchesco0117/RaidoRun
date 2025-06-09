package com.francisco.raidorun

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.francisco.raidorun.LoginActivity.Companion.userEmail
import com.francisco.raidorun.MainActivity.Companion.mainContext
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.local.QueryResult

/**
 * RecordActivity
 *
 * Activity responsible for displaying the user's sport activity history filtered by sport type
 * (Running, Bike, or RollerSkate) and sorted by different fields like date, duration, distance,
 * average speed, and max speed. Data is retrieved from Firebase Firestore.
 *
 * Requirements:
 * - Firebase Firestore configured with collections: runsRunning, runsBike, and runsRollerSkate.
 * - Each collection should include documents with fields like user, date, duration, distance, avgSpeed, and maxSpeed.
 *
 * Author: Francisco Castro
 * Created: 30/APR/2025
 */
class RecordActivity : AppCompatActivity() {

    private var sportSelected: String = "Running"

    private lateinit var ivBike: ImageView
    private lateinit var ivRollerSkate: ImageView
    private lateinit var ivRunning: ImageView

    private lateinit var recyclerView: RecyclerView
    private lateinit var runsArrayList: ArrayList<Runs>

    private lateinit var myAdapter: RunsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_record)
        setSupportActionBar(toolbar)

        toolbar.title = getString(R.string.bar_title_record)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.gray_dark))
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_light))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        ivBike = findViewById<ImageView>(R.id.ivBike)
        ivRollerSkate = findViewById<ImageView>(R.id.ivRollerSkate)
        ivRunning = findViewById<ImageView>(R.id.ivRunning)

        recyclerView = findViewById(R.id.rvRecords)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        runsArrayList = arrayListOf()
        myAdapter = RunsAdapter(runsArrayList)
        recyclerView.adapter = myAdapter
    }

    /**
     * Loads the RecyclerView with the most recent sorting (default: by date descending)
     * when the activity comes to the foreground.
     */
    override fun onResume() {
        super.onResume()
        loadRecyclerView("date", Query.Direction.DESCENDING)
    }

    /**
     * Clears the run list to free memory when activity is paused.
     */
    override fun onPause() {
        super.onPause()
        runsArrayList.clear()
    }

    /**
     * Handles back navigation using the toolbar back arrow.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * Inflates the menu containing sorting options.
     *
     * @param menu The options menu in which items are placed.
     * @return True for the menu to be displayed; false otherwise.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.order_records_by, menu)
        return true //super.onCreateOptionsMenu(menu)
    }

    /**
     * Handles menu item selection to toggle sorting order (ascending/descending)
     * for the selected field, then reloads the RecyclerView data accordingly.
     *
     * Supported sort fields: date, duration, distance, avgSpeed, maxSpeed.
     *
     * @param item The selected menu item.
     * @return True if the event was handled, otherwise delegates to superclass.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var order: Query.Direction = Query.Direction.DESCENDING

        when (item.itemId) {
            R.id.orderby_date -> {
                if (item.title == getString(R.string.orderby_dateZA)) {
                    item.title = getString(R.string.orderby_dateAZ)
                    order = Query.Direction.DESCENDING
                } else {
                    item.title = getString(R.string.orderby_dateZA)
                    order = Query.Direction.ASCENDING
                }
                loadRecyclerView("date", order)
                return true
            }

            R.id.orderby_duration -> {
                var option = getString(R.string.orderby_durationZA)
                if (item.title == getString(R.string.orderby_durationZA)) {
                    item.title = getString(R.string.orderby_durationAZ)
                    order = Query.Direction.DESCENDING
                } else {
                    item.title = getString(R.string.orderby_durationZA)
                    order = Query.Direction.ASCENDING
                }
                loadRecyclerView("duration", order)
                return true
            }

            R.id.orderby_distance -> {
                var option = getString(R.string.orderby_distanceZA)
                if (item.title == option) {
                    item.title = getString(R.string.orderby_distanceAZ)
                    order = Query.Direction.ASCENDING
                } else {
                    item.title = getString(R.string.orderby_distanceZA)
                    order = Query.Direction.DESCENDING
                }
                loadRecyclerView("distance", order)
                return true
            }

            R.id.orderby_avgspeed -> {
                var option = getString(R.string.orderby_avgspeedZA)
                if (item.title == getString(R.string.orderby_avgspeedZA)) {
                    item.title = getString(R.string.orderby_avgspeedAZ)
                    order = Query.Direction.ASCENDING
                } else {
                    item.title = getString(R.string.orderby_avgspeedZA)
                    order = Query.Direction.DESCENDING
                }
                loadRecyclerView("avgSpeed", order)
                return true
            }

            R.id.orderby_maxspeed -> {
                var option = getString(R.string.orderby_maxspeedZA)
                if (item.title == getString(R.string.orderby_maxspeedZA)) {
                    item.title = getString(R.string.orderby_maxspeedAZ)
                    order = Query.Direction.ASCENDING
                } else {
                    item.title = getString(R.string.orderby_maxspeedZA)
                    order = Query.Direction.DESCENDING
                }
                loadRecyclerView("maxSpeed", order)
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Loads records from Firestore based on the current sportSelected value,
     * filtering by the logged-in user and ordering by the specified field and direction.
     *
     * @param field The Firestore field to sort by.
     * @param order The sorting direction (ASCENDING or DESCENDING).
     */
    private fun loadRecyclerView(field: String, order: Query.Direction) {
        runsArrayList.clear()

        var dbRuns = FirebaseFirestore.getInstance()
        dbRuns.collection("runs$sportSelected").orderBy(field, order)
            .whereEqualTo("user", userEmail)
            .get()
            .addOnSuccessListener { documents ->
                for (run in documents) {
                    runsArrayList.add(run.toObject(Runs::class.java))
                }

                myAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents WHERE EQUAL TO: ", exception)
            }
    }

    /**
     * Loads "Bike" sport records, updates button styles accordingly,
     * and reloads the RecyclerView with runs sorted by date descending.
     *
     * @param v The view that triggered this function.
     */
    fun loadRunsBike(v: View) {
        sportSelected = "Bike"
        ivBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.wii_blue))
        ivRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
        ivRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))

        loadRecyclerView("date", Query.Direction.DESCENDING)
    }


    /**
     * Loads "RollerSkate" sport records, updates button styles accordingly,
     * and reloads the RecyclerView with runs sorted by date descending.
     *
     * @param v The view that triggered this function.
     */
    fun loadRunsRollerSkate(v: View) {
        sportSelected = "RollerSkate"
        ivBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
        ivRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.wii_blue))
        ivRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))

        loadRecyclerView("date", Query.Direction.DESCENDING)
    }

    /**
     * Loads "Running" sport records, updates button styles accordingly,
     * and reloads the RecyclerView with runs sorted by date descending.
     *
     * @param v The view that triggered this function.
     */
    fun loadRunsRunning(v: View) {
        sportSelected = "Running"
        ivBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
        ivRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
        ivRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.wii_blue))

        loadRecyclerView("date", Query.Direction.DESCENDING)
    }

    /**
     * Navigates the user back to the MainActivity screen.
     *
     * @param v The view that triggered this function.
     */
    fun callHome(v: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}