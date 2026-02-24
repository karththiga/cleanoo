package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class CollectorDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_collector_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnStartRoute)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.collectorDashboardContainer, CollectorRouteFragment())
                .addToBackStack(null)
                .commit()
        }

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

                view.findViewById<TextView>(R.id.tvAssignedCount).text = "${data.length()} assigned"
                if (data.length() > 0) {
                    val first = data.optJSONObject(0)
                    val household = first?.optJSONObject("household")?.optString("name") ?: "Household"
                    val address = first?.optString("address") ?: "Unknown address"
                    view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = "Next: $household • $address"
                } else {
                    view.findViewById<TextView>(R.id.tvCollectorIncomingStatus).text = "No incoming jobs yet"
                }
            }
        }
    }
}
