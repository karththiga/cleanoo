package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONArray

class CollectorHistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_collector_history, container, false)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadJobs(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadJobs(view)
    }

    private fun loadJobs(view: View) {
        val prefs = requireContext().getSharedPreferences("auth_prefs", 0)
        val collectorId = prefs.getString("COLLECTOR_ID", null)

        if (collectorId.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvCollectorJobsEmpty).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvCollectorJobsEmpty).text = "Collector session not found"
            return
        }

        view.findViewById<View>(R.id.progressCollectorJobs).visibility = View.VISIBLE
        MobileBackendApi.getCollectorJobHistory(collectorId) { success, data, message ->
            activity?.runOnUiThread {
                view.findViewById<View>(R.id.progressCollectorJobs).visibility = View.GONE
                if (!success || data == null) {
                    view.findViewById<TextView>(R.id.tvCollectorJobsEmpty).visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.tvCollectorJobsEmpty).text = message ?: "Failed to load jobs"
                    return@runOnUiThread
                }

                bindJobs(view, data)
            }
        }
    }

    private fun bindJobs(view: View, jobs: JSONArray) {
        val recent = view.findViewById<LinearLayout>(R.id.layoutCollectorRecentJobs)
        val completed = view.findViewById<LinearLayout>(R.id.layoutCollectorCompletedJobs)
        val empty = view.findViewById<TextView>(R.id.tvCollectorJobsEmpty)

        recent.removeAllViews()
        completed.removeAllViews()

        if (jobs.length() == 0) {
            empty.visibility = View.VISIBLE
            return
        }

        empty.visibility = View.GONE

        for (i in 0 until jobs.length()) {
            val job = jobs.optJSONObject(i) ?: continue
            val status = job.optString("status", "pending").lowercase()
            val card = layoutInflater.inflate(R.layout.item_collector_job, recent, false)

            card.findViewById<TextView>(R.id.tvCollectorJobTitle).text =
                job.optJSONObject("household")?.optString("name", "Household") ?: "Household"
            card.findViewById<TextView>(R.id.tvCollectorJobMeta).text =
                "Pickup: ${job.optString("wasteType", "Waste")}"
            card.findViewById<TextView>(R.id.tvCollectorJobStatus).text = statusLabel(status)

            val btn = card.findViewById<Button>(R.id.btnCollectorJobPrimary)
            btn.text = "View details"
            btn.isEnabled = true
            btn.setOnClickListener { openJobDetails(job.optString("_id")) }
            card.setOnClickListener { openJobDetails(job.optString("_id")) }

            if (status == "completed") {
                completed.addView(card)
            } else {
                recent.addView(card)
            }
        }
    }

    private fun openJobDetails(pickupId: String) {
        if (pickupId.isBlank()) return
        parentFragmentManager.beginTransaction()
            .replace(R.id.collectorDashboardContainer, CollectorJobDetailFragment.newInstance(pickupId))
            .addToBackStack(null)
            .commit()
    }

    private fun statusLabel(status: String): String {
        return when (status) {
            "assigned", "approved" -> "Ready to start route"
            "picked" -> "Route started • Upload evidence"
            "collector_completed" -> "Waiting for household confirmation"
            "completed" -> "Completed"
            else -> status.replaceFirstChar { it.uppercase() }
        }
    }
}
