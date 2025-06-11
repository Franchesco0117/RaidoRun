package com.francisco.raidorun

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.francisco.raidorun.LoginActivity.Companion.userEmail
import com.francisco.raidorun.Utility.setHeightLinearLayout
import com.francisco.raidorun.Utility.animateViewOfFloat
import com.francisco.raidorun.Utility.deleteRunAndLinkedData

/**
 * RunsAdapter
 *
 * Adapter for displaying a list of sport activities in a RecyclerView.
 * Each item is represented as a card with expandable details and medals.
 * Allows deletion of a run from the Firestore database and updates the UI accordingly.
 *
 * Features:
 * - Card layout with collapsible/expandable details.
 * - Displays formatted date, time, challenge/interval data, speeds, altitudes, and medals.
 * - Handles deletion of run with confirmation dialog.
 *
 * Author: Francisco Castro
 * Created: 23/MAY/2025
 */
class RunsAdapter(
    private val runsList: ArrayList<Runs>
): RecyclerView.Adapter<MyViewHolder>() {

    private lateinit var context: Context

    private var minimized = true

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type.
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        context = parent.context
        val itemView = LayoutInflater.from(context).inflate(R.layout.card_run, parent, false)
        return MyViewHolder(itemView)
    }

    /**
     * Binds data to the views in each item card. Also handles:
     * - Formatting the date display
     * - Assigning medals based on run performance
     * - Managing visibility of interval/challenge indicators
     * - Animating expansion and collapse of additional details
     * - Handling deletion logic for runs
     *
     * @param holder The ViewHolder containing the views to bind data to.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val run: Runs = runsList[position]

        setHeightLinearLayout(holder.lyDataRunBody, 0)
        holder.lyDataRunBodyContainer.translationY = -200f

        holder.ivHeaderOpenClose.setOnClickListener {
            if (minimized) {
                setHeightLinearLayout(holder.lyDataRunBody, 300)
                animateViewOfFloat(holder.lyDataRunBodyContainer, "translationY", 0f, 300L)
                holder.ivHeaderOpenClose.setRotation(180f)
                minimized = false
            } else {
                holder.lyDataRunBodyContainer.translationY = -200f
                setHeightLinearLayout(holder.lyDataRunBody, 0)
                holder.ivHeaderOpenClose.setRotation(0f)
                minimized = true
            }
        }

        var day = run.date?.subSequence(8, 10)
        var monthNumber = run.date?.subSequence(5, 7)
        var month: String ?= null
        var year = run.date?.subSequence(0, 4)

        when (monthNumber) {
            "01" -> month = "ENE"
            "02" -> month = "FEB"
            "03" -> month = "MAR"
            "04" -> month = "ABR"
            "05" -> month = "MAY"
            "06" -> month = "JUN"
            "07" -> month = "JUL"
            "08" -> month = "AGO"
            "09" -> month = "SEP"
            "10" -> month = "OCT"
            "11" -> month = "NOV"
            "12" -> month = "DIC"
        }

        var date: String = "$day-$month-$year"
        holder.tvDate.text = date
        holder.tvHeaderDate.text = date

        holder.tvStartTime.text = run.startTime?.subSequence(0, 5)
        holder.tvDurationRun.text = run.duration
        holder.tvHeaderDuration.text = run.duration!!.subSequence(0, 5).toString() + "HH"

        if (!run.challengeDuration.isNullOrEmpty()) {
            holder.tvChallengeDurationRun.text = run.challengeDuration
        } else {
            setHeightLinearLayout(holder.lyChallengeDurationRun, 0)
        }

        if (run.challengeDistance != null) {
            holder.tvChallengeDistanceRun.text = run.challengeDistance.toString()
        } else {
            setHeightLinearLayout(holder.lyChallengeDistance, 0)
        }

        if (run.intervalMode != null) {
            var details: String = "${run.intervalDuration}mins. ("
            details += "${run.runningTime}/${run.walkingTime})"
            holder.tvIntervalRun.text = details
        } else {
            setHeightLinearLayout(holder.lyIntervalRun, 0)
        }

        holder.tvDistanceRun.setText(run.distance.toString())
        holder.tvHeaderDistance.setText(run.distance.toString() + "KM")

        holder.tvMaxUnevennessRun.setText(run.maxAltitude.toString())
        holder.tvMinUnevennessRun.setText(run.minAltitude.toString())

        holder.tvAvgSpeedRun.setText(run.avgSpeed.toString())
        holder.tvHeaderAvgSpeed.setText(run.avgSpeed.toString() + "KM/H")
        holder.tvMaxSpeedRun.setText(run.maxSpeed.toString())

        when (run.medalsDistance) {
            "gold" -> {
                holder.ivMedalDistance.setImageResource(R.drawable.medalgold)
                holder.ivHeaderMedalDistance.setImageResource(R.drawable.medalgold)
                holder.tvMedalDistanceTitle.setText(R.string.CardMedalDistance)
            }
            "silver" -> {
                holder.ivMedalDistance.setImageResource(R.drawable.medalsilver)
                holder.ivHeaderMedalDistance.setImageResource(R.drawable.medalsilver)
                holder.tvMedalDistanceTitle.setText(R.string.CardMedalDistance)
            }
            "bronze" -> {
                holder.ivMedalDistance.setImageResource(R.drawable.medalbronze)
                holder.ivHeaderMedalDistance.setImageResource(R.drawable.medalbronze)
                holder.tvMedalDistanceTitle.setText(R.string.CardMedalDistance)
            }
        }

        when(run.medalsAvgSpeed){
            "gold"->{
                holder.ivMedalAvgSpeed.setImageResource(R.drawable.medalgold)
                holder.ivHeaderMedalAvgSpeed.setImageResource(R.drawable.medalgold)
                holder.tvMedalAvgSpeedTitle.setText(R.string.CardMedalAvgSpeed)
            }
            "silver"->{
                holder.ivMedalAvgSpeed.setImageResource(R.drawable.medalsilver)
                holder.ivHeaderMedalAvgSpeed.setImageResource(R.drawable.medalsilver)
                holder.tvMedalAvgSpeedTitle.setText(R.string.CardMedalAvgSpeed)
            }
            "bronze"->{
                holder.ivMedalAvgSpeed.setImageResource(R.drawable.medalbronze)
                holder.ivHeaderMedalAvgSpeed.setImageResource(R.drawable.medalbronze)
                holder.tvMedalAvgSpeedTitle.setText(R.string.CardMedalAvgSpeed)
            }
        }
        when(run.medalsMaxSpeed){
            "gold"->{
                holder.ivMedalMaxSpeed.setImageResource(R.drawable.medalgold)
                holder.ivHeaderMedalMaxSpeed.setImageResource(R.drawable.medalgold)
                holder.tvMedalMaxSpeedTitle.setText(R.string.CardMedalMaxSpeed)
            }
            "silver"->{
                holder.ivMedalMaxSpeed.setImageResource(R.drawable.medalsilver)
                holder.ivHeaderMedalMaxSpeed.setImageResource(R.drawable.medalsilver)
                holder.tvMedalMaxSpeedTitle.setText(R.string.CardMedalMaxSpeed)
            }
            "bronze"->{
                holder.ivMedalMaxSpeed.setImageResource(R.drawable.medalbronze)
                holder.ivHeaderMedalMaxSpeed.setImageResource(R.drawable.medalbronze)
                holder.tvMedalMaxSpeedTitle.setText(R.string.CardMedalMaxSpeed)
            }
        }

        holder.tvDelete.setOnClickListener {
            var id: String = userEmail + run.date + run.startTime
            id = id.replace(":", "")
            id = id.replace("/", "")

            var currentRun = Runs()
            currentRun.distance = run.distance
            currentRun.avgSpeed = run.avgSpeed
            currentRun.maxSpeed = run.maxSpeed
            currentRun.duration = run.duration
            currentRun.activatedGPS = run.activatedGPS
            currentRun.date = run.date
            currentRun.startTime = run.startTime
            currentRun.user = run.user
            currentRun.sport = run.sport

            AlertDialog.Builder(context)
                .setTitle(R.string.alertDeleteRecordRaceTitle)
                .setMessage(R.string.alertDeleteRecordRaceDescription)
                .setPositiveButton(android.R.string.ok,
                    DialogInterface.OnClickListener { dialog, which ->
                        deleteRunAndLinkedData(id, currentRun.sport!!, holder.lyDataRunHeader, currentRun)

                        runsList.removeAt(position)
                        notifyItemRemoved(position)
                    })
                .setNegativeButton(android.R.string.cancel,
                    DialogInterface.OnClickListener{ dialog, which ->

                    })
                .setCancelable(true)
                .show()
        }
    }

    /**
     * Returns the total number of runs to display.
     */
    override fun getItemCount(): Int {
        return runsList.size
    }
}