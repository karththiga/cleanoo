package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import org.json.JSONArray

class NotificationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadNotifications(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadNotifications(view)
    }

    private fun loadNotifications(view: View) {
        val prefs = requireContext().getSharedPreferences("auth_prefs", 0)
        val role = prefs.getString("USER_ROLE", "household") ?: "household"

        val userId: String?
        val userType: String
        if (role == "collector") {
            userId = prefs.getString("COLLECTOR_ID", null)
            userType = "Collector"
        } else {
            userId = prefs.getString("HOUSEHOLD_ID", null)
            userType = "Household"
        }

        if (userId.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvNotificationEmpty).apply {
                visibility = View.VISIBLE
                text = "Login again to see notifications"
            }
            return
        }

        view.findViewById<View>(R.id.progressNotifications).visibility = View.VISIBLE
        MobileBackendApi.getMyNotifications(userId, userType) { success, data, message ->
            activity?.runOnUiThread {
                view.findViewById<View>(R.id.progressNotifications).visibility = View.GONE
                if (!success || data == null) {
                    view.findViewById<TextView>(R.id.tvNotificationEmpty).apply {
                        visibility = View.VISIBLE
                        text = message ?: "Failed to load notifications"
                    }
                    return@runOnUiThread
                }

                bindNotifications(view, data)
            }
        }
    }

    private fun bindNotifications(view: View, data: JSONArray) {
        val container = view.findViewById<LinearLayout>(R.id.layoutNotifications)
        val empty = view.findViewById<TextView>(R.id.tvNotificationEmpty)
        container.removeAllViews()

        if (data.length() == 0) {
            empty.visibility = View.VISIBLE
            return
        }

        empty.visibility = View.GONE

        for (i in 0 until data.length()) {
            val n = data.optJSONObject(i) ?: continue
            val card = layoutInflater.inflate(R.layout.item_pickup_job, container, false) as CardView
            card.findViewById<TextView>(R.id.tvItemWasteType).text = n.optString("title", "Notification")
            card.findViewById<TextView>(R.id.tvItemAddress).text = n.optString("message", "")
            card.findViewById<TextView>(R.id.tvItemCollector).text = "Type: ${n.optString("type", "info")}" 
            card.findViewById<TextView>(R.id.tvItemLiveLocation).visibility = View.GONE
            card.findViewById<TextView>(R.id.tvItemStatus).text = "${n.optString("createdAt", "").take(16)}"
            container.addView(card)
        }
    }
}
