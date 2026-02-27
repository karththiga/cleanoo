package com.example.rewardrecycleapp

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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
        view?.let {
            loadIncomingJobs(it)
            loadRecentJobs(it)
            setupCollectorAnnouncements()
        }
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
        loadRecentJobs(view)
        loadDashboardHeroImage(view)
        setupCollectorAnnouncements()
    }

    private fun loadDashboardHeroImage(view: View) {
        Glide.with(this)
            .load("https://asianmirror.lk/wp-content/uploads/2025/02/10.jpg")
            .centerCrop()
            .into(view.findViewById<ImageView>(R.id.ivCollectorHero))
    }

    private fun setupCollectorAnnouncements() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val savedCollectorId = prefs.getString("COLLECTOR_ID", null)

        if (!savedCollectorId.isNullOrBlank()) {
            fetchCollectorAnnouncements(savedCollectorId)
            return
        }

        val idToken = prefs.getString("ID_TOKEN", null)
        if (idToken.isNullOrBlank()) {
            bindCollectorAnnouncements(emptyList())
            return
        }

        MobileBackendApi.getMyCollectorProfile(idToken) { success, profile, _ ->
            activity?.runOnUiThread {
                if (!success || profile == null) {
                    bindCollectorAnnouncements(emptyList())
                    return@runOnUiThread
                }

                val collectorId = profile.optString("_id")
                if (collectorId.isBlank()) {
                    bindCollectorAnnouncements(emptyList())
                    return@runOnUiThread
                }

                prefs.edit().putString("COLLECTOR_ID", collectorId).apply()
                fetchCollectorAnnouncements(collectorId)
            }
        }
    }

    private fun fetchCollectorAnnouncements(collectorId: String) {
        MobileBackendApi.getMyNotifications(collectorId, "Collector") { success, data, _ ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    bindCollectorAnnouncements(emptyList())
                    return@runOnUiThread
                }

                val announcements = mutableListOf<Announcement>()
                for (i in 0 until data.length()) {
                    val notification = data.optJSONObject(i) ?: continue
                    if (!isCollectorAdminAnnouncement(notification)) continue

                    announcements += Announcement(
                        title = notification.optString("title", "Announcement"),
                        description = notification.optString("message", ""),
                        imageUrl = ""
                    )
                }

                bindCollectorAnnouncements(announcements)
            }
        }
    }

    private fun isCollectorAdminAnnouncement(notification: JSONObject): Boolean {
        val target = notification.optString("target", "")
        val targetValue = notification.optString("targetValue", "")

        return when (target) {
            "all", "all_collectors" -> true
            "single_collector" -> targetValue.isNotBlank()
            else -> false
        }
    }

    private fun bindCollectorAnnouncements(announcements: List<Announcement>) {
        val recycler = view?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerCollectorAnnouncements)
            ?: return

        val items = if (announcements.isEmpty()) {
            listOf(
                Announcement(
                    title = "No announcements yet",
                    description = "Admin broadcast announcements will appear here.",
                    imageUrl = ""
                )
            )
        } else {
            announcements
        }

        recycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recycler.adapter = AnnouncementsAdapter(items) { announcement ->
            AlertDialog.Builder(requireContext())
                .setTitle(announcement.title)
                .setMessage(announcement.description.ifBlank { "No details available" })
                .setPositiveButton("Close", null)
                .show()
        }
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
                    loadLatestCompletedJob(view)
                }
            }
        }
    }

    private fun setupAnnouncements(view: View) {
        val collectorId = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("COLLECTOR_ID", null)

        if (collectorId.isNullOrBlank()) {
            bindAnnouncements(view, emptyList())
            return
        }

        MobileBackendApi.getMyNotifications(collectorId, "Collector") { success, data, _ ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    bindAnnouncements(view, emptyList())
                    return@runOnUiThread
                }

                val announcements = mutableListOf<Announcement>()
                for (i in 0 until data.length()) {
                    val notification = data.optJSONObject(i) ?: continue
                    if (!isBroadcastAnnouncement(notification)) continue

                    announcements += Announcement(
                        title = notification.optString("title", "Announcement"),
                        description = notification.optString("message", ""),
                        imageUrl = ""
                    )
                }

                bindAnnouncements(view, announcements)
            }
        }
    }

    private fun isBroadcastAnnouncement(notification: JSONObject): Boolean {
        return when (notification.optString("target")) {
            "all", "all_collectors" -> true
            else -> false
        }
    }

    private fun bindAnnouncements(view: View, announcements: List<Announcement>) {
        val items = if (announcements.isEmpty()) {
            listOf(
                Announcement(
                    title = "No announcements yet",
                    description = "Admin broadcast announcements will appear here.",
                    imageUrl = ""
                )
            )
        } else {
            announcements
        }

        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerCollectorAnnouncements)
        recycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recycler.adapter = AnnouncementsAdapter(items) { announcement ->
            AlertDialog.Builder(requireContext())
                .setTitle(announcement.title)
                .setMessage(announcement.description.ifBlank { "No details available" })
                .setPositiveButton("Close", null)
                .show()
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

    private fun loadRecentJobs(view: View) {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val cachedCollectorId = prefs.getString("COLLECTOR_ID", null)

        if (!cachedCollectorId.isNullOrBlank()) {
            fetchAndBindRecentJobs(view, cachedCollectorId)
            return
        }

        val idToken = prefs.getString("ID_TOKEN", null)
        if (idToken.isNullOrBlank()) {
            showRecentJobsMessage(view, "Collector session not found")
            return
        }

        MobileBackendApi.getMyCollectorProfile(idToken) { success, profile, _ ->
            activity?.runOnUiThread {
                if (!success || profile == null) {
                    showRecentJobsMessage(view, "Unable to load recent jobs")
                    return@runOnUiThread
                }

                val collectorId = profile.optString("_id")
                if (collectorId.isBlank()) {
                    showRecentJobsMessage(view, "Collector session not found")
                    return@runOnUiThread
                }

                prefs.edit().putString("COLLECTOR_ID", collectorId).apply()
                fetchAndBindRecentJobs(view, collectorId)
            }
        }
    }

    private fun fetchAndBindRecentJobs(view: View, collectorId: String) {
        val progress = view.findViewById<View>(R.id.progressCollectorRecentJobs)
        progress.visibility = View.VISIBLE

        MobileBackendApi.getCollectorJobHistory(collectorId) { success, data, message ->
            activity?.runOnUiThread {
                progress.visibility = View.GONE
                if (!success || data == null) {
                    showRecentJobsMessage(view, message ?: "Unable to load recent jobs")
                    return@runOnUiThread
                }

                bindRecentJobs(view, sortJobsByRecency(data))
            }
        }
    }

    private fun sortJobsByRecency(data: JSONArray): List<JSONObject> {
        val jobs = mutableListOf<JSONObject>()
        for (i in 0 until data.length()) {
            data.optJSONObject(i)?.let { jobs += it }
        }

        jobs.sortByDescending {
            it.optString("updatedAt").ifBlank { it.optString("createdAt") }
        }
        return jobs
    }

    private fun bindRecentJobs(view: View, jobs: List<JSONObject>) {
        val container = view.findViewById<LinearLayout>(R.id.layoutCollectorRecentJobs)
        val empty = view.findViewById<TextView>(R.id.tvCollectorRecentJobsEmpty)
        container.removeAllViews()

        val limit = minOf(3, jobs.size)
        if (limit == 0) {
            empty.visibility = View.VISIBLE
            empty.text = "No recent jobs"
            return
        }

        empty.visibility = View.GONE
        for (i in 0 until limit) {
            val job = jobs[i]
            val item = layoutInflater.inflate(R.layout.item_recent_request_home, container, false)

            val household = job.optJSONObject("household")?.optString("name")
                ?.takeIf { it.isNotBlank() }
                ?: "Household"

            item.findViewById<TextView>(R.id.tvRecentWasteType).text =
                "$household • ${job.optString("wasteType", "Waste")}"
            item.findViewById<TextView>(R.id.tvRecentAddress).text =
                job.optString("address", "No address")
            item.findViewById<TextView>(R.id.tvRecentStatus).text =
                when (job.optString("status", "").lowercase()) {
                    "completed" -> "Completed"
                    "picked" -> "On The Way"
                    "household_confirmed" -> "Awaiting Evidence"
                    "approved", "assigned" -> "Assigned"
                    else -> "Pending"
                }

            container.addView(item)
        }
    }

    private fun showRecentJobsMessage(view: View, message: String) {
        view.findViewById<LinearLayout>(R.id.layoutCollectorRecentJobs).removeAllViews()
        view.findViewById<TextView>(R.id.tvCollectorRecentJobsEmpty).apply {
            visibility = View.VISIBLE
            text = message
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
