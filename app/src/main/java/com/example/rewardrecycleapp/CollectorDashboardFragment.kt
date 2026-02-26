package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
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

        view.findViewById<Button>(R.id.btnCollectorReview)?.setOnClickListener {
            val pickupId = activeJob?.optString("_id").orEmpty()
            if (pickupId.isBlank()) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .replace(R.id.collectorDashboardContainer, CollectorReviewFragment.newInstance(pickupId))
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.btnCollectorComplaint)?.setOnClickListener {
            val pickupId = activeJob?.optString("_id").orEmpty()
            if (pickupId.isBlank()) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .replace(R.id.collectorDashboardContainer, CollectorComplaintFragment.newInstance(pickupId))
                .addToBackStack(null)
                .commit()
        }

        loadIncomingJobs(view)
        loadDashboardImages(view)
    }

    private fun loadDashboardImages(view: View) {
        Glide.with(this)
            .load("https://www.elanka.com.au/wp-content/uploads/2025/01/Clean-minds-essential-for-a-Clean-Sri-Lanka.-The-system-change-must-begin-with-mind-set-change.-By-Aubrey-Joachim.png")
            .centerCrop()
            .into(view.findViewById<ImageView>(R.id.ivCollectorHero))

        Glide.with(this)
            .load("https://www.navy.lk/assets/img/cleanSL/36.webp")
            .centerCrop()
            .into(view.findViewById<ImageView>(R.id.ivCollectorGalleryOne))

        Glide.with(this)
            .load("https://cleanupsrilanka.lk/wp-content/uploads/2024/07/cleanup-sri-lanka-1-1.webp")
            .centerCrop()
            .into(view.findViewById<ImageView>(R.id.ivCollectorGalleryTwo))
    }

    private fun loadIncomingJobs(view: View) {
        val prefs = requireContext().getSharedPreferences("auth_prefs", 0)
        val collectorEmail = prefs.getString("COLLECTOR_EMAIL", null)

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
                    bindActiveJob(view, data.optJSONObject(0))
                } else {
                    // If there are no pending jobs, show latest completed job to allow review/complaint actions.
                    loadLatestCompletedJob(view)
                }
            }
        }
    }

    private fun loadLatestCompletedJob(view: View) {
        val collectorId = requireContext().getSharedPreferences("auth_prefs", 0)
            .getString("COLLECTOR_ID", null)

        if (collectorId.isNullOrBlank()) {
            showNoJobState(view)
            return
        }

        MobileBackendApi.getCollectorJobHistory(collectorId) { success, data, _ ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    showNoJobState(view)
                    return@runOnUiThread
                }

                var latestCompleted: JSONObject? = null
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    if (item.optString("status", "").lowercase() == "completed") {
                        latestCompleted = item
                        break
                    }
                }

                if (latestCompleted != null) {
                    bindActiveJob(view, latestCompleted)
                } else {
                    showNoJobState(view)
                }
            }
        }
    }

    private fun showNoJobState(view: View) {
        activeJob = null
        view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = "No active jobs right now"
        view.findViewById<TextView>(R.id.tvActiveJobTitle).text = "No active pickup"
        view.findViewById<TextView>(R.id.tvActiveJobMeta).text = "New assigned jobs will appear here"
        view.findViewById<Button>(R.id.btnPrimaryAction).visibility = View.GONE
        view.findViewById<Button>(R.id.btnViewJobDetails).visibility = View.GONE
        view.findViewById<View>(R.id.layoutCollectorCompletedActions).visibility = View.GONE
    }

    private fun bindActiveJob(view: View, job: JSONObject?) {
        if (job == null) {
            showNoJobState(view)
            return
        }
        activeJob = job

        val household = job.optJSONObject("household")?.optString("name") ?: "Household"
        val address = job.optString("address", "Unknown address")
        val status = job.optString("status", "assigned").lowercase()

        view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = "Current active workflow"
        view.findViewById<TextView>(R.id.tvActiveJobTitle).text = household
        view.findViewById<TextView>(R.id.tvActiveJobMeta).text = "${job.optString("wasteType", "Waste")} • $address"

        val primary = view.findViewById<Button>(R.id.btnPrimaryAction)
        val details = view.findViewById<Button>(R.id.btnViewJobDetails)
        val completedActions = view.findViewById<View>(R.id.layoutCollectorCompletedActions)

        primary.visibility = View.VISIBLE
        details.visibility = View.VISIBLE
        completedActions.visibility = View.GONE

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
            "collector_completed", "completed" -> {
                primary.visibility = View.GONE
                details.visibility = View.VISIBLE
                completedActions.visibility = View.VISIBLE
            }
            else -> {
                primary.text = "Open details"
                primary.isEnabled = true
            }
        }
    }

    private fun startRoute(view: View, pickupId: String) {
        val button = view.findViewById<Button>(R.id.btnPrimaryAction)
        button.isEnabled = false
        button.text = "Starting..."

        MobileBackendApi.startCollectorRoute(
            pickupId,
            "Collector started route"
        ) { success, data, message ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    button.isEnabled = true
                    button.text = "Start route"
                    Toast.makeText(requireContext(), message ?: "Start route failed", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }

                activeJob = data
                bindActiveJob(view, data)
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
