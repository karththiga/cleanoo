package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class CollectorHistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_collector_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navigateToDetails = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.collectorDashboardContainer, CollectorJobDetailFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.btnPickedUpGreenview)?.setOnClickListener(navigateToDetails)
        view.findViewById<View>(R.id.btnPickedUpLakeside)?.setOnClickListener(navigateToDetails)

        view.findViewById<View>(R.id.btnCollectorComplaint)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.collectorDashboardContainer, CollectorComplaintFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.btnCollectorReviewHousehold)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.collectorDashboardContainer, CollectorReviewFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.cardCollectorCompletedJob)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.collectorDashboardContainer, CollectorJobDetailFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
