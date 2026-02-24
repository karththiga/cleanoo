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
                view.findViewById<TextView>(R.id.tvJobSubtitle).text =
                    if (status == "household_confirmed") "Household confirmed. Add photo evidence now."
                    else "Status: ${status.replaceFirstChar { it.uppercase() }}"

                val allowEvidence = status == "household_confirmed"
                view.findViewById<Button>(R.id.btnAddEvidence).isEnabled = allowEvidence
                view.findViewById<Button>(R.id.btnMarkCompleted).isEnabled = allowEvidence
            }
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
