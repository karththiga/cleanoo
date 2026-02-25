package com.example.rewardrecycleapp

import android.os.Bundle
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class RedeemPointsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_redeem_points, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadRewardsSummary(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadRewardsSummary(it) }
    }

    private fun loadRewardsSummary(root: View) {
        val pointsView = root.findViewById<TextView>(R.id.tvAvailablePoints)
        val nextRewardHintView = root.findViewById<TextView>(R.id.tvNextRewardHint)

        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val cachedPoints = prefs.getString("HOUSEHOLD_POINTS", "0") ?: "0"
        val idToken = prefs.getString("ID_TOKEN", null)

        pointsView.text = "$cachedPoints Points"

        if (idToken.isNullOrBlank()) {
            nextRewardHintView.text = "Sign in again to refresh points"
            return
        }

        MobileBackendApi.getMyRewardsSummary(idToken) { ok, data, err ->
            activity?.runOnUiThread {
                if (!ok || data == null) {
                    nextRewardHintView.text = err ?: "Failed to load reward balance"
                    return@runOnUiThread
                }

                val totalPoints = data.optInt("currentPoints", 0)
                val nextMilestone = data.optInt("nextMilestone", 0)
                val pointsToNext = data.optInt("pointsToNextMilestone", 0)

                pointsView.text = "$totalPoints Points"
                nextRewardHintView.text = if (pointsToNext > 0) {
                    "$pointsToNext points to reach $nextMilestone"
                } else {
                    "Great! You reached the current milestone"
                }

                prefs.edit().putString("HOUSEHOLD_POINTS", totalPoints.toString()).apply()
            }
        }
    }
}
