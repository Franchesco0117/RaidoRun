package com.francisco.raidorun

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

public class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

    val lyDataRunHeader: LinearLayout = itemView.findViewById<LinearLayout>(R.id.lyDataRunHeader)
    val tvHeaderDate: TextView = itemView.findViewById<TextView>(R.id.tvHeaderDate)
    val tvHeaderDuration: TextView = itemView.findViewById<TextView>(R.id.tvHeaderDuration)
    val tvHeaderDistance: TextView = itemView.findViewById<TextView>(R.id.tvHeaderDistance)
    val tvHeaderAvgSpeed: TextView = itemView.findViewById<TextView>(R.id.tvHeaderAvgSpeed)
    val ivHeaderMedalDistance: ImageView = itemView.findViewById<ImageView>(R.id.ivHeaderMedalDistance)
    val ivHeaderMedalAvgSpeed: ImageView = itemView.findViewById<ImageView>(R.id.ivHeaderMedalAvgSpeed)
    val ivHeaderMedalMaxSpeed: ImageView = itemView.findViewById<ImageView>(R.id.ivHeaderMedalMaxSpeed)
    val ivHeaderOpenClose: ImageView = itemView.findViewById<ImageView>(R.id.ivHeaderOpenClose)

    val lyDataRunBody: LinearLayout = itemView.findViewById<LinearLayout>(R.id.lyDataRunBody)
    val lyDataRunBodyContainer: LinearLayout = itemView.findViewById<LinearLayout>(R.id.lyDataRunBodyContainer)

    val tvDate: TextView = itemView.findViewById<TextView>(R.id.tvDate)
    val tvStartTime: TextView = itemView.findViewById<TextView>(R.id.tvStartTime)

    val tvDurationRun: TextView = itemView.findViewById<TextView>(R.id.tvDurationRun)
    val lyChallengeDurationRun: LinearLayout = itemView.findViewById<LinearLayout>(R.id.lyChallengeDurationRun)
    val tvChallengeDurationRun: TextView = itemView.findViewById<TextView>(R.id.tvChallengeDurationRun)
    val lyIntervalRun: LinearLayout = itemView.findViewById<LinearLayout>(R.id.lyIntervalRun)
    val tvIntervalRun: TextView = itemView.findViewById<TextView>(R.id.tvIntervalRun)

    val tvDistanceRun: TextView = itemView.findViewById<TextView>(R.id.tvDistanceRun)
    val lyChallengeDistance: LinearLayout = itemView.findViewById<LinearLayout>(R.id.lyChallengeDistance)
    val tvChallengeDistanceRun: TextView = itemView.findViewById<TextView>(R.id.tvChallengeDistanceRun)
    val lyUnevennessRun: LinearLayout = itemView.findViewById<LinearLayout>(R.id.lyUnevennessRun)
    val tvMaxUnevennessRun: TextView = itemView.findViewById<TextView>(R.id.tvMaxUnevennessRun)
    val tvMinUnevennessRun: TextView = itemView.findViewById<TextView>(R.id.tvMinUnevennessRun)

    val tvAvgSpeedRun: TextView = itemView.findViewById<TextView>(R.id.tvAvgSpeedRun)
    val tvMaxSpeedRun: TextView = itemView.findViewById<TextView>(R.id.tvMaxSpeedRun)

    val ivMedalDistance: ImageView = itemView.findViewById<ImageView>(R.id.ivMedalDistance)
    val tvMedalDistanceTitle: TextView = itemView.findViewById<TextView>(R.id.tvMedalDistanceTitle)
    val ivMedalAvgSpeed: ImageView = itemView.findViewById<ImageView>(R.id.ivMedalAvgSpeed)
    val tvMedalAvgSpeedTitle: TextView = itemView.findViewById<TextView>(R.id.tvMedalAvgSpeedTitle)
    val ivMedalMaxSpeed: ImageView = itemView.findViewById<ImageView>(R.id.ivMedalMaxSpeed)
    val tvMedalMaxSpeedTitle: TextView = itemView.findViewById<TextView>(R.id.tvMedalMaxSpeedTitle)

    val tvPlay: TextView = itemView.findViewById<TextView>(R.id.tvPlay)
    val tvDelete: TextView = itemView.findViewById<TextView>(R.id.tvDelete)
}