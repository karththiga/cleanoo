package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONObject

class CollectorRouteFragment : Fragment() {

    private var activePickup: JSONObject? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_collector_route, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnStartRoute).setOnClickListener {
            val pickupId = activePickup?.optString("_id")
            if (pickupId.isNullOrBlank()) {
                Toast.makeText(requireContext(), "No active pickup to start", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val startButton = view.findViewById<Button>(R.id.btnStartRoute)
            startButton.isEnabled = false
            startButton.text = "Starting..."

            val dummyLocation = "Collector is near Jaffna Town (dummy location)"
            MobileBackendApi.startCollectorRoute(pickupId, dummyLocation) { success, data, message ->
                activity?.runOnUiThread {
                    if (!success || data == null) {
                        startButton.isEnabled = true
                        startButton.text = "Start route"
                        Toast.makeText(requireContext(), message ?: "Start route failed", Toast.LENGTH_LONG).show()
                        return@runOnUiThread
                    }

                    activePickup = data
                    bindActivePickup(view, data)
                    Toast.makeText(requireContext(), "Route started. Household will see live status.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loadCurrentAssignedPickup(view)
    }

    private fun loadCurrentAssignedPickup(view: View) {
        val collectorEmail = requireContext().getSharedPreferences("auth_prefs", 0)
            .getString("COLLECTOR_EMAIL", null)

        if (collectorEmail.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvRouteSummary).text = "Collector session missing"
            return
        }

        MobileBackendApi.getCollectorIncomingRequests(collectorEmail) { success, data, message ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    view.findViewById<TextView>(R.id.tvRouteSummary).text = message ?: "Failed to load assigned jobs"
                    return@runOnUiThread
                }

                if (data.length() == 0) {
                    view.findViewById<TextView>(R.id.tvRouteSummary).text = "No assigned jobs right now"
                    view.findViewById<Button>(R.id.btnStartRoute).isEnabled = false
                    return@runOnUiThread
                }

                val first = data.optJSONObject(0)
                if (first != null) {
                    activePickup = first
                    bindActivePickup(view, first)
                }
            }
        }
    }

    private fun bindActivePickup(view: View, pickup: JSONObject) {
        view.findViewById<TextView>(R.id.tvRouteSummary).text = "Tap start route to share live status with household"
        view.findViewById<TextView>(R.id.tvRouteJobTitle).text = "${pickup.optString("wasteType", "Waste")} Pickup"
        view.findViewById<TextView>(R.id.tvRouteAddress).text = pickup.optString("address", "Unknown address")
        view.findViewById<TextView>(R.id.tvRouteStatus).text = "Status: ${pickup.optString("status", "assigned").replaceFirstChar { it.uppercase() }}"

        val location = pickup.optString("collectorLiveLocation", "")
        view.findViewById<TextView>(R.id.tvDummyLocation).text =
            if (location.isBlank()) "Live location: Not shared" else "Live location: $location"

        val status = pickup.optString("status")
        val isAlreadyStarted = status == "picked" || status == "household_confirmed" || status == "completed"
        view.findViewById<Button>(R.id.btnStartRoute).text = if (isAlreadyStarted) "Route started" else "Start route"
        view.findViewById<Button>(R.id.btnStartRoute).isEnabled = !isAlreadyStarted
    }
}
