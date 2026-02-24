package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONArray
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

        view.findViewById<Button>(R.id.btnPrimaryAction)?.setOnClickListener {
            val job = activeJob ?: return@setOnClickListener
            val pickupId = job.optString("_id")
            if (pickupId.isBlank()) return@setOnClickListener

            when (job.optString("status").lowercase()) {
                "assigned", "approved" -> startRoute(view, pickupId)
                "picked" -> openEvidence(pickupId)
                else -> openJobDetails(pickupId)
            }
        }

        view.findViewById<Button>(R.id.btnViewJobDetails)?.setOnClickListener {
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

        MobileBackendApi.getCollectorIncomingRequests(collectorEmail) { success: Boolean, data: JSONArray?, message: String? ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = message ?: "Failed to load incoming requests"
                    return@runOnUiThread
                }

                view.findViewById<TextView>(R.id.tvAssignedCount).text = "${data.length()} pending"
                if (data.length() > 0) {
                    bindActiveJob(view, data)
                } else {
                    activeJob = null
                    view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = "No active jobs right now"
                    view.findViewById<TextView>(R.id.tvActiveJobTitle).text = "No active pickup"
                    view.findViewById<TextView>(R.id.tvActiveJobMeta).text = "New assigned jobs will appear here"
                    view.findViewById<Button>(R.id.btnPrimaryAction).visibility = View.GONE
                    view.findViewById<Button>(R.id.btnViewJobDetails).visibility = View.GONE
                }
            }
        }
    }

    private fun bindActiveJob(view: View, jobs: JSONArray?) {
        val job = jobs?.optJSONObject(0) ?: return
        activeJob = job

        val household = job.optJSONObject("household")?.optString("name") ?: "Household"
        val address = job.optString("address", "Unknown address")
        val status = job.optString("status", "assigned").lowercase()

        view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = "Current active workflow"
        view.findViewById<TextView>(R.id.tvActiveJobTitle).text = household
        view.findViewById<TextView>(R.id.tvActiveJobMeta).text = "${job.optString("wasteType", "Waste")} • $address"

        val primary = view.findViewById<Button>(R.id.btnPrimaryAction)
        primary.visibility = View.VISIBLE
        when (status) {
            "assigned", "approved" -> {
                primary.text = "Start route"
                primary.isEnabled = true
            }
            "picked" -> {
                primary.text = "Add evidence"
                primary.isEnabled = true
            }
            "collector_completed" -> {
                primary.text = "Collector completed"
                primary.isEnabled = false
            }
            "completed" -> {
                primary.text = "Completed"
                primary.isEnabled = false
            }
            else -> {
                primary.text = "Open details"
                primary.isEnabled = true
            }
        }

        view.findViewById<Button>(R.id.btnViewJobDetails).visibility = View.VISIBLE
    }

    private fun startRoute(view: View, pickupId: String) {
        val button = view.findViewById<Button>(R.id.btnPrimaryAction)
        button.isEnabled = false
        button.text = "Starting..."

        MobileBackendApi.startCollectorRoute(
            pickupId,
            "Collector is near Jaffna Town (dummy location)"
        ) { success, data, message ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    button.isEnabled = true
                    button.text = "Start route"
                    Toast.makeText(requireContext(), message ?: "Start route failed", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                activeJob = data
                val jobs = JSONArray().put(data)
                bindActiveJob(view, jobs)
                Toast.makeText(requireContext(), "Route started", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openEvidence(pickupId: String) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.collectorDashboardContainer, CollectorEvidenceFragment.newInstance(pickupId))
            .addToBackStack(null)
            .commit()
    }

    private fun openJobDetails(pickupId: String) {
        if (pickupId.isBlank()) return
        parentFragmentManager.beginTransaction()
            .replace(R.id.collectorDashboardContainer, CollectorJobDetailFragment.newInstance(pickupId))
            .addToBackStack(null)
            .commit()
    }
}
