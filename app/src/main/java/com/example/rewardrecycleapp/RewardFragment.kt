package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class RewardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reward, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnHouseholdReview)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.dashboardContainer, HouseholdReviewFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.btnHouseholdComplaint)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.dashboardContainer, HouseholdComplaintFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.btnReportDelay)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.dashboardContainer, HouseholdComplaintFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.btnRaiseComplaintTop)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.dashboardContainer, HouseholdComplaintFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.cardCompletedJob)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.dashboardContainer, HouseholdJobDetailFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
