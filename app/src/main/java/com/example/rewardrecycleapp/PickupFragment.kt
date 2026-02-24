package com.example.rewardrecycleapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
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

        view.findViewById<CardView>(R.id.cardCurrentRequest)?.setOnClickListener {
            openPickupDetails(latestPickup?.optString("_id"))
        }

        view.findViewById<Button>(R.id.btnConfirmCollected)?.setOnClickListener {
            val pickupId = latestPickup?.optString("_id")
            if (pickupId.isNullOrBlank()) return@setOnClickListener

            val button = view.findViewById<Button>(R.id.btnConfirmCollected)
            button.isEnabled = false
            button.text = "Confirming..."

            MobileBackendApi.householdConfirmCollection(pickupId) { success, _, message ->
                activity?.runOnUiThread {
                    button.isEnabled = true
                    button.text = "Confirm Job Completed"
                    if (!success) {
                        view.findViewById<TextView>(R.id.tvActiveJobMeta).text = message ?: "Confirmation failed"
                        return@runOnUiThread
                    }

                    loadPickupData(view)
                }
            }
        }

        view.findViewById<Button>(R.id.btnComplaintDelay)?.setOnClickListener {
            val pickupId = latestPickup?.optString("_id")
            if (pickupId.isNullOrBlank()) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .replace(R.id.dashboardContainer, HouseholdComplaintFragment.newInstance(pickupId))
                .addToBackStack(null)
                .commit()
        }

        loadPickupData(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadPickupData(it) }
    }

    private fun loadPickupData(view: View) {
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
            val liveLocation = latest.optString("collectorLiveLocation", "")
            val collectorMeta = if (liveLocation.isBlank()) {
                "Collector: $collector"
            } else {
                "Collector: $collector • Live: $liveLocation"
            }
            view.findViewById<TextView>(R.id.tvActiveJobCollector).text = collectorMeta

            val status = latest.optString("status", "pending")
            view.findViewById<TextView>(R.id.tvActiveJobStatus).text = statusLabelForHousehold(status)
            view.findViewById<TextView>(R.id.tvRequestDetailsHint).text = "Tap to view full request details"

            val statusLower = status.lowercase()
            val confirmButton = view.findViewById<Button>(R.id.btnConfirmCollected)
            confirmButton.visibility = if (statusLower == "collector_completed") View.VISIBLE else View.GONE

            val delayButton = view.findViewById<Button>(R.id.btnComplaintDelay)
            delayButton.visibility = if (statusLower == "assigned" || statusLower == "approved") View.VISIBLE else View.GONE
        }

        bindAllPickupCards(view, pickups)
    }

    private fun bindAllPickupCards(view: View, pickups: JSONArray) {
        val container = view.findViewById<LinearLayout>(R.id.layoutAllPickupJobs)
        container.removeAllViews()

        if (pickups.length() == 0) {
            view.findViewById<TextView>(R.id.tvAllJobsEmpty).visibility = View.VISIBLE
            return
        }

        view.findViewById<TextView>(R.id.tvAllJobsEmpty).visibility = View.GONE

        for (i in 0 until pickups.length()) {
            val pickup = pickups.optJSONObject(i) ?: continue
            val item = layoutInflater.inflate(R.layout.item_pickup_job, container, false)

            item.findViewById<TextView>(R.id.tvItemWasteType).text = "${pickup.optString("wasteType", "Waste")} Pickup"
            item.findViewById<TextView>(R.id.tvItemAddress).text = pickup.optString("address", "No address")
            val collectorText = pickup.optJSONObject("assignedCollector")?.optString("name") ?: "Awaiting assignment"
            val liveLocationText = pickup.optString("collectorLiveLocation", "")
            item.findViewById<TextView>(R.id.tvItemCollector).text = "Collector: $collectorText"
            item.findViewById<TextView>(R.id.tvItemLiveLocation).text =
                if (liveLocationText.isBlank()) "Live: Not shared" else "Live: $liveLocationText"
            val status = pickup.optString("status", "pending").lowercase()
            item.findViewById<TextView>(R.id.tvItemStatus).text =
                statusLabelForHousehold(status)

            val actions = item.findViewById<LinearLayout>(R.id.layoutCompletedActions)
            if (status == "completed") {
                actions.visibility = View.VISIBLE
                item.findViewById<Button>(R.id.btnItemReview).setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.dashboardContainer, HouseholdReviewFragment.newInstance(pickup.optString("_id")))
                        .addToBackStack(null)
                        .commit()
                }
                item.findViewById<Button>(R.id.btnItemComplaint).setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.dashboardContainer, HouseholdComplaintFragment.newInstance(pickup.optString("_id")))
                        .addToBackStack(null)
                        .commit()
                }
            } else {
                actions.visibility = View.GONE
            }

            item.setOnClickListener {
                openPickupDetails(pickup.optString("_id"))
            }

            container.addView(item)
        }
    }


    private fun statusLabelForHousehold(rawStatus: String): String {
        return when (rawStatus.lowercase()) {
            "completed" -> "Completed"
            "collector_completed" -> "Collector completed • Please confirm"
            "picked" -> "Collector is on the way"
            "household_confirmed" -> "Confirmed by household"
            else -> "Pending Pickup"
        }
    }

    private fun openPickupDetails(pickupId: String?) {
        if (pickupId.isNullOrBlank()) return
        startActivity(
            Intent(requireContext(), PickupDetailsActivity::class.java)
                .putExtra("pickup_id", pickupId)
        )
    }
}
