package com.example.rewardrecycleapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject

class CollectorHomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_collector_home, container, false)
    }

    override fun onResume() {
        super.onResume()
        view?.let { root ->
            loadRecentJobs(root)
        }
    }

    private fun loadRecentJobs(root: View) {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val cachedCollectorId = prefs.getString("COLLECTOR_ID", null)

        if (!cachedCollectorId.isNullOrBlank()) {
            fetchAndBindJobs(root, cachedCollectorId)
            return
        }

        val idToken = prefs.getString("ID_TOKEN", null)
        if (idToken.isNullOrBlank()) {
            showRecentJobsError(root, "Collector session not found")
            return
        }

        MobileBackendApi.getMyCollectorProfile(idToken) { success, profile, _ ->
            activity?.runOnUiThread {
                if (!success || profile == null) {
                    showRecentJobsError(root, "Unable to load collector profile")
                    return@runOnUiThread
                }

                val collectorId = profile.optString("_id")
                if (collectorId.isBlank()) {
                    showRecentJobsError(root, "Collector session not found")
                    return@runOnUiThread
                }

                prefs.edit().putString("COLLECTOR_ID", collectorId).apply()
                fetchAndBindJobs(root, collectorId)
            }
        }
    }

    private fun fetchAndBindJobs(root: View, collectorId: String) {
        val progress = root.findViewById<View>(R.id.progressRecentJobs)
        progress.visibility = View.VISIBLE

        MobileBackendApi.getCollectorJobHistory(collectorId) { success, data, message ->
            activity?.runOnUiThread {
                progress.visibility = View.GONE
                if (!success || data == null) {
                    showRecentJobsError(root, message ?: "Unable to load recent jobs")
                    return@runOnUiThread
                }

                val sortedJobs = sortJobsByRecency(data)
                bindRecentJobs(root, sortedJobs)
                bindShiftSummary(root, sortedJobs)
            }
        }
    }


    private fun sortJobsByRecency(data: JSONArray): JSONArray {
        val jobs = mutableListOf<JSONObject>()
        for (i in 0 until data.length()) {
            data.optJSONObject(i)?.let { jobs += it }
        }

        fun timestamp(job: JSONObject): String {
            return job.optString("updatedAt")
                .ifBlank { job.optString("createdAt") }
                .ifBlank { "" }
        }

        jobs.sortByDescending { timestamp(it) }

        val sorted = JSONArray()
        jobs.forEach { sorted.put(it) }
        return sorted
    }

    private fun bindRecentJobs(root: View, data: JSONArray) {
        val container = root.findViewById<LinearLayout>(R.id.layoutRecentJobs)
        val emptyView = root.findViewById<TextView>(R.id.tvRecentJobsEmpty)
        container.removeAllViews()

        val limit = minOf(3, data.length())
        if (limit == 0) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = "No recent jobs"
            return
        }

        emptyView.visibility = View.GONE

        for (i in 0 until limit) {
            val job = data.optJSONObject(i) ?: continue
            val itemView = layoutInflater.inflate(R.layout.item_recent_request_home, container, false)

            val householdName = job.optJSONObject("household")?.optString("name")
                ?.takeIf { it.isNotBlank() }
                ?: "Household"

            itemView.findViewById<TextView>(R.id.tvRecentWasteType).text =
                "$householdName • ${job.optString("wasteType", "Waste")}"
            itemView.findViewById<TextView>(R.id.tvRecentAddress).text =
                job.optString("address", "No address")
            itemView.findViewById<TextView>(R.id.tvRecentStatus).text =
                statusLabelForCollector(job.optString("status"))

            container.addView(itemView)
        }
    }

    private fun bindShiftSummary(root: View, data: JSONArray) {
        var assigned = 0
        var completed = 0
        var pending = 0

        for (i in 0 until data.length()) {
            val status = data.optJSONObject(i)?.optString("status", "")?.lowercase() ?: ""
            when (status) {
                "completed" -> completed++
                "assigned", "approved", "picked", "household_confirmed", "in_progress" -> {
                    assigned++
                    if (status != "completed") pending++
                }
            }
        }

        root.findViewById<TextView>(R.id.tvShiftSummaryValue).text =
            "Assigned: $assigned • Completed: $completed • Pending: $pending"
    }

    private fun showRecentJobsError(root: View, message: String) {
        root.findViewById<LinearLayout>(R.id.layoutRecentJobs).removeAllViews()
        root.findViewById<TextView>(R.id.tvRecentJobsEmpty).apply {
            visibility = View.VISIBLE
            text = message
        }
        root.findViewById<TextView>(R.id.tvShiftSummaryValue).text =
            "Assigned: 0 • Completed: 0 • Pending: 0"
    }

    private fun statusLabelForCollector(rawStatus: String): String {
        return when (rawStatus.lowercase()) {
            "completed" -> "Completed"
            "picked" -> "On The Way"
            "household_confirmed" -> "Awaiting Evidence"
            "approved", "assigned" -> "Assigned"
            else -> "Pending"
        }
    }
}
