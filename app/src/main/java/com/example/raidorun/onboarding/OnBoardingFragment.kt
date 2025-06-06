package com.example.raidorun.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.francisco.raidorun.R

class OnBoardingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding, container, false)
        
        val position = arguments?.getInt(ARG_POSITION) ?: 0
        
        val imageResId = when(position) {
            0 -> R.drawable.onboarding_1
            1 -> R.drawable.onboarding_2
            2 -> R.drawable.onboarding_3
            else -> R.drawable.onboarding_1
        }
        
        val titleResId = when(position) {
            0 -> R.string.onboarding_title_1
            1 -> R.string.onboarding_title_2
            2 -> R.string.onboarding_title_3
            else -> R.string.onboarding_title_1
        }
        
        val descriptionResId = when(position) {
            0 -> R.string.onboarding_desc_1
            1 -> R.string.onboarding_desc_2
            2 -> R.string.onboarding_desc_3
            else -> R.string.onboarding_desc_1
        }

        view.findViewById<ImageView>(R.id.onboardingImage).setImageResource(imageResId)
        view.findViewById<TextView>(R.id.onboardingTitle).setText(titleResId)
        view.findViewById<TextView>(R.id.onboardingDescription).setText(descriptionResId)

        return view
    }

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): OnBoardingFragment {
            val fragment = OnBoardingFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }
} 