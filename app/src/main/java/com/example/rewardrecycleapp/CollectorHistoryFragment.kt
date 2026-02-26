package com.example.rewardrecycleapp

import android.content.res.ColorStateList
import android.graphics.Color
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
        val activeSectionEmpty = view.findViewById<TextView>(R.id.tvActiveSectionEmpty)
        val completedSectionEmpty = view.findViewById<TextView>(R.id.tvCompletedSectionEmpty)
        val activeCount = view.findViewById<TextView>(R.id.tvActiveCount)
        val completedCount = view.findViewById<TextView>(R.id.tvCompletedCount)

        recent.removeAllViews()
        completed.removeAllViews()

        if (jobs.length() == 0) {
            empty.visibility = View.VISIBLE
            activeCount.text = "0"
            completedCount.text = "0"
            activeSectionEmpty.visibility = View.VISIBLE
            completedSectionEmpty.visibility = View.VISIBLE
            return
        }

        empty.visibility = View.GONE

        var activeTotal = 0
        var completedTotal = 0

        for (i in 0 until jobs.length()) {
            val job = jobs.optJSONObject(i) ?: continue
            val status = job.optString("status", "pending").lowercase()
            val card = layoutInflater.inflate(R.layout.item_collector_job, recent, false)

            card.findViewById<TextView>(R.id.tvCollectorJobTitle).text =
                job.optJSONObject("household")?.optString("name", "Household") ?: "Household"
            card.findViewById<TextView>(R.id.tvCollectorJobMeta).text =
                "Pickup: ${job.optString("wasteType", "Waste")}"

            val statusTextView = card.findViewById<TextView>(R.id.tvCollectorJobStatus)
            statusTextView.text = statusLabel(status)
            styleStatusBadge(statusTextView, status)

            val statusDot = card.findViewById<View>(R.id.viewCollectorStatusDot)
            statusDot.backgroundTintList = ColorStateList.valueOf(statusDotColor(status))

            val btn = card.findViewById<Button>(R.id.btnCollectorJobPrimary)
            btn.text = "View details"
            btn.isEnabled = true
            btn.backgroundTintList = ColorStateList.valueOf(buttonColor(status))
            btn.setOnClickListener { openJobDetails(job.optString("_id")) }
            card.setOnClickListener { openJobDetails(job.optString("_id")) }

            if (status == "completed") {
                completed.addView(card)
                completedTotal++
            } else {
                recent.addView(card)
                activeTotal++
            }
        }

        activeCount.text = activeTotal.toString()
        completedCount.text = completedTotal.toString()
        activeSectionEmpty.visibility = if (activeTotal == 0) View.VISIBLE else View.GONE
        completedSectionEmpty.visibility = if (completedTotal == 0) View.VISIBLE else View.GONE
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
            "assigned", "approved" -> "Ready to start"
            "picked" -> "Route started"
            "collector_completed" -> "Waiting confirmation"
            "completed" -> "Completed"
            else -> status.replaceFirstChar { it.uppercase() }
        }
    }

    private fun styleStatusBadge(statusTextView: TextView, status: String) {
        val (bg, text) = when (status) {
            "assigned", "approved" -> "#DBEAFE" to "#1D4ED8"
            "picked" -> "#DCFCE7" to "#166534"
            "collector_completed" -> "#FEF3C7" to "#92400E"
            "completed" -> "#EDE9FE" to "#5B21B6"
            else -> "#E2E8F0" to "#334155"
        }

        statusTextView.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bg))
        statusTextView.setTextColor(Color.parseColor(text))
    }

    private fun statusDotColor(status: String): Int {
        val color = when (status) {
            "assigned", "approved" -> "#2563EB"
            "picked" -> "#16A34A"
            "collector_completed" -> "#D97706"
            "completed" -> "#7C3AED"
            else -> "#64748B"
        }
        return Color.parseColor(color)
    }

    private fun buttonColor(status: String): Int {
        val color = when (status) {
            "completed" -> "#0F172A"
            "collector_completed" -> "#1E293B"
            "picked" -> "#14532D"
            "assigned", "approved" -> "#1D4ED8"
            else -> "#334155"
        }
        return Color.parseColor(color)
    }
}
