package com.francisco.raidorun

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import androidx.appcompat.widget.Toolbar

class UsersManagementActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val usersList = mutableListOf<UserData>()
    private lateinit var adapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users_management)

        // Configurar Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar_users)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Users Management"

        // Configurar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UsersAdapter(usersList)
        recyclerView.adapter = adapter

        loadUsers()
    }

    private fun loadUsers() {
        val db = FirebaseFirestore.getInstance()
        val collections = listOf("runsBike", "runsRunning", "runsRollerSkate")
        val userStats = mutableMapOf<String, UserData>()

        var completedQueries = 0

        collections.forEach { collection ->
            db.collection(collection)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val email = document.getString("user") ?: continue
                        val userData = userStats.getOrPut(email) { 
                            UserData(email, 0, 0.0, 0) 
                        }
                        
                        // Actualizar estadísticas
                        userData.totalSessions++
                        userData.totalDistance += document.getDouble("distance") ?: 0.0
                        document.getString("duration")?.let {
                            userData.totalMinutes += durationToMinutes(it)
                        }
                    }

                    completedQueries++
                    if (completedQueries == collections.size) {
                        // Convertir el mapa a lista y actualizar el RecyclerView
                        usersList.clear()
                        usersList.addAll(userStats.values.sortedByDescending { it.totalSessions })
                        adapter.notifyDataSetChanged()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error loading users data", Toast.LENGTH_SHORT).show()
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

data class UserData(
    val email: String,
    var totalSessions: Int = 0,
    var totalDistance: Double = 0.0,
    var totalMinutes: Int = 0
) 