package com.example.rewardrecycleapp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.NumberFormat
import java.util.Locale

class RewardsWalletFragment : Fragment() {

    private var currentPoints: Int = 0
    private val pointsPerCurrencyUnit = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rewards_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btnRedeemPoints).setOnClickListener {
            onRedeemClicked()
        }
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

        currentPoints = cachedPoints
        pointsView.text = formatPoints(cachedPoints)
        updateRedeemEstimate(root)

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

                currentPoints = totalPoints
                pointsView.text = formatPoints(totalPoints)
                updateRedeemEstimate(root)
                bonusHintView.text = if (pointsToNext > 0) {
                    "$pointsToNext points to reach $nextMilestone"
                } else {
                    "Great! You reached the current milestone"
                }

                prefs.edit().putString("HOUSEHOLD_POINTS", totalPoints.toString()).apply()
            }
        }
    }

    private fun onRedeemClicked() {
        if (currentPoints <= 0) {
            Toast.makeText(requireContext(), "No points available to redeem", Toast.LENGTH_SHORT).show()
            return
        }

        val cashAmount = pointsToCash(currentPoints)
        AlertDialog.Builder(requireContext())
            .setTitle("Redeem points")
            .setMessage("Cash amount: Rs. ${formatCash(cashAmount)}\n\nDo you confirm to redeem points?")
            .setNegativeButton("No", null)
            .setPositiveButton("Yes") { _, _ ->
                redeemPoints()
            }
            .show()
    }

    private fun redeemPoints() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val idToken = prefs.getString("ID_TOKEN", null)

        if (idToken.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Sign in again to redeem", Toast.LENGTH_SHORT).show()
            return
        }

        MobileBackendApi.redeemMyPoints(idToken) { ok, data, message ->
            activity?.runOnUiThread {
                if (!ok) {
                    Toast.makeText(requireContext(), message ?: "Redeem failed", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                currentPoints = data?.optInt("remainingPoints", 0) ?: 0
                prefs.edit().putString("HOUSEHOLD_POINTS", currentPoints.toString()).apply()
                view?.let { root ->
                    root.findViewById<TextView>(R.id.tvWalletAvailablePoints).text = formatPoints(currentPoints)
                    updateRedeemEstimate(root)
                }

                Toast.makeText(requireContext(), message ?: "Points added to your account", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateRedeemEstimate(root: View) {
        val estimate = root.findViewById<TextView>(R.id.tvRedeemEstimate)
        estimate.text = if (currentPoints > 0) {
            "Redeemable cash: Rs. ${formatCash(pointsToCash(currentPoints))}"
        } else {
            "Redeemable cash: Rs. 0.00"
        }
    }

    private fun pointsToCash(points: Int): Double {
        return points.toDouble() / pointsPerCurrencyUnit
    }

    private fun formatPoints(points: Int): String {
        return NumberFormat.getNumberInstance(Locale.US).format(points)
    }

    private fun formatCash(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }
}
