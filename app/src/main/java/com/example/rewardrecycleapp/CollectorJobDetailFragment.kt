package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class CollectorJobDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_collector_job_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pickupId = arguments?.getString(ARG_PICKUP_ID)
        if (pickupId.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvJobSubtitle).text = "Pickup id missing"
            return
        }

        view.findViewById<Button>(R.id.btnAddEvidence)?.setOnClickListener {
            openEvidence(pickupId)
        }

        view.findViewById<Button>(R.id.btnMarkCompleted)?.setOnClickListener {
            openEvidence(pickupId)
        }

        view.findViewById<Button>(R.id.btnCollectorDetailReview)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.collectorDashboardContainer, CollectorReviewFragment.newInstance(pickupId))
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.btnCollectorDetailComplaint)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.collectorDashboardContainer, CollectorComplaintFragment.newInstance(pickupId))
                .addToBackStack(null)
                .commit()
        }

        MobileBackendApi.getPickupById(pickupId) { success, data, message ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    view.findViewById<TextView>(R.id.tvJobSubtitle).text = message ?: "Unable to load job details"
                    return@runOnUiThread
                }

                val householdName = data.optJSONObject("household")?.optString("name", "Household") ?: "Household"
                val wasteType = data.optString("wasteType", "Waste")
                val status = data.optString("status", "assigned")

                view.findViewById<TextView>(R.id.tvJobTitle).text = householdName
                view.findViewById<TextView>(R.id.tvJobMeta).text = "Pickup: $wasteType"
                view.findViewById<TextView>(R.id.tvJobAddress).text = "Address: ${data.optString("address", "-")}"
                view.findViewById<TextView>(R.id.tvJobStatusDetail).text = "Status: ${status.replaceFirstChar { it.uppercase() }}"
                view.findViewById<TextView>(R.id.tvJobLiveLocation).text =
                    "Live location: ${data.optString("collectorLiveLocation", "Not shared")}"
                view.findViewById<TextView>(R.id.tvJobRequestedDate).text =
                    "Requested: ${data.optString("requestDate", "-").take(16)}"

                view.findViewById<TextView>(R.id.tvJobSubtitle).text = subtitleForStatus(status)

                val allowEvidence = status == "picked"
                val allowPostActions = status == "collector_completed" || status == "completed"

                view.findViewById<Button>(R.id.btnAddEvidence).apply {
                    visibility = if (allowEvidence) View.VISIBLE else View.GONE
                    isEnabled = allowEvidence
                    text = "Upload evidence"
                }

                view.findViewById<Button>(R.id.btnMarkCompleted).apply {
                    visibility = if (allowEvidence) View.VISIBLE else View.GONE
                    isEnabled = allowEvidence
                    text = "Mark as completed"
                }

                view.findViewById<View>(R.id.layoutCollectorDetailCompletedActions).visibility =
                    if (allowPostActions) View.VISIBLE else View.GONE
            }
        }
    }

    private fun subtitleForStatus(status: String): String {
        return when (status.lowercase()) {
            "assigned", "approved" -> "Start route first from Collector Home before adding proof."
            "picked" -> "Upload evidence to finish your part. Household will confirm final completion."
            "collector_completed" -> "Evidence uploaded. Waiting for household confirmation."
            "completed" -> "Pickup is fully completed and confirmed by household."
            else -> "Status: ${status.replaceFirstChar { it.uppercase() }}"
        }
    }

    private fun openEvidence(pickupId: String) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.collectorDashboardContainer, CollectorEvidenceFragment.newInstance(pickupId))
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val ARG_PICKUP_ID = "pickup_id"

        fun newInstance(pickupId: String): CollectorJobDetailFragment {
            val frag = CollectorJobDetailFragment()
            frag.arguments = Bundle().apply { putString(ARG_PICKUP_ID, pickupId) }
            return frag
        }
    }
}
