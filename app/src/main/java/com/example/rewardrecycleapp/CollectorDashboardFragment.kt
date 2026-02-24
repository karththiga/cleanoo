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

class CollectorDashboardFragment : Fragment() {

    private var activeJob: JSONObject? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_collector_dashboard, container, false)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadIncomingJobs(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<android.widget.Button>(R.id.btnViewJobDetails)?.setOnClickListener {
            openJobDetails(activeJob?.optString("_id").orEmpty())
        }

        loadIncomingJobs(view)
    }

    private fun loadIncomingJobs(view: View) {
        val collectorEmail = requireContext().getSharedPreferences("auth_prefs", 0)
            .getString("COLLECTOR_EMAIL", null)

        if (collectorEmail.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = "Collector session not found"
            return
        }

        MobileBackendApi.getCollectorIncomingRequests(collectorEmail) { success, data, message ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = message ?: "Failed to load incoming requests"
                    return@runOnUiThread
                }

                view.findViewById<TextView>(R.id.tvAssignedCount).text = "${data.length()} pending"
                if (data.length() > 0) {
                    val first = data.optJSONObject(0)
                    if (first != null) {
                        activeJob = first
                        bindActiveJob(view, first)
                    }
                } else {
                    activeJob = null
                    view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = "No active jobs right now"
                    view.findViewById<TextView>(R.id.tvActiveJobTitle).text = "No active pickup"
                    view.findViewById<TextView>(R.id.tvActiveJobMeta).text = "New assigned jobs will appear here"
                    view.findViewById<android.widget.Button>(R.id.btnViewJobDetails).visibility = View.GONE
                }

                activeJob = data
                bindActiveJob(view, data)
                Toast.makeText(requireContext(), "Route started", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindActiveJob(view: View, job: JSONObject) {
        val household = job.optJSONObject("household")?.optString("name") ?: "Household"
        val address = job.optString("address", "Unknown address")
        val status = job.optString("status", "assigned").lowercase()

        view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = "Current active workflow"
        view.findViewById<TextView>(R.id.tvActiveJobTitle).text = household
        view.findViewById<TextView>(R.id.tvActiveJobMeta).text = "${job.optString("wasteType", "Waste")} • $address"

        val detailsButton = view.findViewById<android.widget.Button>(R.id.btnViewJobDetails)
        detailsButton.visibility = if (status == "assigned" || status == "approved") View.VISIBLE else View.GONE
    }

    private fun openJobDetails(pickupId: String) {
        if (pickupId.isBlank()) return
        parentFragmentManager.beginTransaction()
            .replace(R.id.collectorDashboardContainer, CollectorJobDetailFragment.newInstance(pickupId))
            .addToBackStack(null)
            .commit()
    }
}
