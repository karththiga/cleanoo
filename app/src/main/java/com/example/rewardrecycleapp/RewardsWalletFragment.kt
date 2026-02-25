package com.example.rewardrecycleapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.util.Locale

class RewardsWalletFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rewards_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadWalletSummary(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadWalletSummary(it) }
    }

    private fun loadWalletSummary(root: View) {
        val pointsView = root.findViewById<TextView>(R.id.tvWalletAvailablePoints)
        val bonusHintView = root.findViewById<TextView>(R.id.tvWalletBonusHint)

        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val cachedPoints = prefs.getString("HOUSEHOLD_POINTS", "0")?.toIntOrNull() ?: 0
        val idToken = prefs.getString("ID_TOKEN", null)

        pointsView.text = formatPoints(cachedPoints)

        if (idToken.isNullOrBlank()) {
            bonusHintView.text = "Sign in again to refresh rewards"
            return
        }

        MobileBackendApi.getMyRewardsSummary(idToken) { ok, data, err ->
            activity?.runOnUiThread {
                if (!ok || data == null) {
                    bonusHintView.text = err ?: "Failed to load rewards wallet"
                    return@runOnUiThread
                }

                val totalPoints = data.optInt("currentPoints", 0)
                val nextMilestone = data.optInt("nextMilestone", 0)
                val pointsToNext = data.optInt("pointsToNextMilestone", 0)

                pointsView.text = formatPoints(totalPoints)
                bonusHintView.text = if (pointsToNext > 0) {
                    "$pointsToNext points to reach $nextMilestone"
                } else {
                    "Great! You reached the current milestone"
                }

                prefs.edit().putString("HOUSEHOLD_POINTS", totalPoints.toString()).apply()
            }
        }
    }

    private fun formatPoints(points: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(points)
    }
}
