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

    override fun onResume() {
        super.onResume()
        loadRecyclerView("date", Query.Direction.DESCENDING)
    }

    override fun onPause() {
        super.onPause()
        runsArrayList.clear()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.order_records_by, menu)
        return true //super.onCreateOptionsMenu(menu)
    }

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

    fun loadRunsBike(v: View) {
        sportSelected = "Bike"
        ivBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.wii_blue))
        ivRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
        ivRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))

        loadRecyclerView("date", Query.Direction.DESCENDING)
    }

    fun loadRunsRollerSkate(v: View) {
        sportSelected = "RollerSkate"
        ivBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
        ivRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.wii_blue))
        ivRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))

        loadRecyclerView("date", Query.Direction.DESCENDING)
    }

    fun loadRunsRunning(v: View) {
        sportSelected = "Running"
        ivBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
        ivRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
        ivRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.wii_blue))

        loadRecyclerView("date", Query.Direction.DESCENDING)
    }

    fun callHome(v: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}