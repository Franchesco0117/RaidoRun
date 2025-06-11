package com.francisco.raidorun

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UsersAdapter(private val users: List<UserData>) : 
    RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emailText: TextView = view.findViewById(R.id.textViewEmail)
        val sessionsText: TextView = view.findViewById(R.id.textViewSessions)
        val distanceText: TextView = view.findViewById(R.id.textViewDistance)
        val timeText: TextView = view.findViewById(R.id.textViewTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.emailText.text = user.email.substringBefore("@")
        holder.sessionsText.text = "Sessions: ${user.totalSessions}"
        holder.distanceText.text = "Distance: ${String.format("%.1f", user.totalDistance)} km"
        holder.timeText.text = "Time: ${formatMinutes(user.totalMinutes)}"
    }

    override fun getItemCount() = users.size

    private fun formatMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
} 