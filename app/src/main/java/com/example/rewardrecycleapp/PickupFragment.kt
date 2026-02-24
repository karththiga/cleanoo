package com.example.rewardrecycleapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject

class PickupFragment : Fragment() {

    private var latestPickup: JSONObject? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_pickup, container, false)
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

        view.findViewById<CardView>(R.id.cardCurrentRequest)?.setOnClickListener {
            val pickupId = latestPickup?.optString("_id")
            if (!pickupId.isNullOrBlank()) {
                startActivity(
                    Intent(requireContext(), PickupDetailsActivity::class.java)
                        .putExtra("pickup_id", pickupId)
                )
            }
        }

        val householdId = requireContext()
            .getSharedPreferences("auth_prefs", 0)
            .getString("HOUSEHOLD_ID", null)

        if (householdId.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvActiveJobMeta).text = "Login again to load pickup requests"
            return
        }

        MobileBackendApi.getHouseholdPickupHistory(householdId) { success, data, message ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    view.findViewById<TextView>(R.id.tvActiveJobMeta).text = message ?: "Unable to load pickup history"
                    return@runOnUiThread
                }

                bindPickupData(view, data)
            }
        }
    }

    private fun bindPickupData(view: View, pickups: JSONArray) {
        var pending = 0
        var completed = 0

        for (i in 0 until pickups.length()) {
            val status = pickups.optJSONObject(i)?.optString("status")?.lowercase().orEmpty()
            if (status == "completed") completed++ else pending++
        }

        view.findViewById<TextView>(R.id.tvPendingCount).text = pending.toString()
        view.findViewById<TextView>(R.id.tvCompletedCount).text = completed.toString()

        val latest = pickups.optJSONObject(0)
        latestPickup = latest
        if (latest != null) {
            view.findViewById<TextView>(R.id.tvActiveJobTitle).text = "${latest.optString("wasteType", "Waste")} Pickup"
            view.findViewById<TextView>(R.id.tvActiveJobMeta).text = latest.optString("address", "No address")

            val collector = latest.optJSONObject("assignedCollector")?.optString("name") ?: "Awaiting assignment"
            view.findViewById<TextView>(R.id.tvActiveJobCollector).text = "Collector: $collector"

            val status = latest.optString("status", "pending")
            view.findViewById<TextView>(R.id.tvActiveJobStatus).text = status.replaceFirstChar { it.uppercase() }

            view.findViewById<TextView>(R.id.tvRequestDetailsHint).text = "Tap to view full request details"
        }
    }
}
