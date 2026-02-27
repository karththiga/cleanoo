import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rewardrecycleapp.Announcement
import com.example.rewardrecycleapp.AnnouncementsAdapter
import com.example.rewardrecycleapp.MobileBackendApi
import com.example.rewardrecycleapp.R
import com.example.rewardrecycleapp.RequestPickupActivity
import com.example.rewardrecycleapp.databinding.FragmentHomeBinding
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserFromPrefs()
        refreshHouseholdProfile()
        loadRewardsCardMeta()
        loadRecentRequests()
        setupAnnouncements()
        setupClicks()
    }

    override fun onResume() {
        super.onResume()
        loadRecentRequests()
        loadRewardsCardMeta()
        setupAnnouncements()
    }

    private fun loadUserFromPrefs() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("HOUSEHOLD_NAME", "User") ?: "User"
        val points = prefs.getString("HOUSEHOLD_POINTS", "0") ?: "0"

        binding.tvGreeting.text = "Hello, $name 👋"
        binding.tvTotalPoints.text = "$points Points"
    }

    private fun refreshHouseholdProfile() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("ID_TOKEN", null)

        if (token.isNullOrEmpty()) return

        MobileBackendApi.getMyHouseholdProfile(token) { success, profile, message ->
            activity?.runOnUiThread {
                if (success && profile != null) {
                    val name = profile.optString("name", "User")
                    val points = profile.optInt("points", 0)

                    binding.tvGreeting.text = "Hello, $name 👋"
                    binding.tvTotalPoints.text = "$points Points"

                    prefs.edit()
                        .putString("HOUSEHOLD_ID", profile.optString("_id"))
                        .putString("HOUSEHOLD_NAME", name)
                        .putString("HOUSEHOLD_EMAIL", profile.optString("email"))
                        .putString("HOUSEHOLD_PHONE", profile.optString("phone"))
                        .putString("HOUSEHOLD_ADDRESS", profile.optString("address"))
                        .putString("HOUSEHOLD_ZONE", profile.optString("zone"))
                        .putString("HOUSEHOLD_POINTS", points.toString())
                        .apply()

                    loadRewardsCardMeta()
                } else if (!message.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadRewardsCardMeta() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("ID_TOKEN", null)
        if (token.isNullOrBlank()) return

        MobileBackendApi.getMyRewardsSummary(token) { ok, data, _ ->
            activity?.runOnUiThread {
                if (!ok || data == null) return@runOnUiThread

                val totalPoints = data.optInt("currentPoints", 0)
                val nextMilestone = data.optInt("nextMilestone", 500)
                val pointsToNext = data.optInt("pointsToNextMilestone", 0)
                val rewards = data.optJSONArray("rewards") ?: JSONArray()

                val expiryDate = nearestPointsExpiryDate(rewards) ?: sixMonthsFromNow()
                binding.tvExpiry.text = "$pointsToNext more points to $nextMilestone • Expires ${formatDate(expiryDate)}"
                binding.progressTier.progress = tierProgressPercent(totalPoints, nextMilestone)
            }
        }
    }

    private fun nearestPointsExpiryDate(rewards: JSONArray): Date? {
        var nearest: Date? = null
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }

        for (i in 0 until rewards.length()) {
            val reward = rewards.optJSONObject(i) ?: continue
            if (reward.optString("status") != "approved") continue
            if (reward.optBoolean("isRedeemed", false)) continue

            val createdAt = reward.optString("createdAt")
            if (createdAt.isBlank()) continue

            val addedDate = try {
                parser.parse(createdAt)
            } catch (_: Exception) {
                null
            } ?: continue

            val expiry = Calendar.getInstance().apply {
                time = addedDate
                add(Calendar.MONTH, 6)
            }.time

            if (expiry.after(Date()) && (nearest == null || expiry.before(nearest))) {
                nearest = expiry
            }
        }

        return nearest
    }

    private fun sixMonthsFromNow(): Date {
        return Calendar.getInstance().apply {
            add(Calendar.MONTH, 6)
        }.time
    }

    private fun tierProgressPercent(points: Int, nextMilestone: Int): Int {
        if (nextMilestone <= 0) return 0
        val lowerBand = ((nextMilestone - 1) / 500) * 500
        val span = (nextMilestone - lowerBand).coerceAtLeast(1)
        val within = (points - lowerBand).coerceIn(0, span)
        return ((within * 100f) / span).toInt().coerceIn(0, 100)
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(date)
    }

    private fun loadRecentRequests() {
        val householdId = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("HOUSEHOLD_ID", null)

        if (householdId.isNullOrBlank()) return

        binding.progressRecentRequests.visibility = View.VISIBLE
        MobileBackendApi.getHouseholdPickupHistory(householdId) { success, data, message ->
            activity?.runOnUiThread {
                binding.progressRecentRequests.visibility = View.GONE
                if (!success || data == null) {
                    binding.tvRecentRequestsEmpty.visibility = View.VISIBLE
                    binding.tvRecentRequestsEmpty.text = message ?: "Unable to load recent requests"
                    return@runOnUiThread
                }

                bindRecentRequests(data)
            }
        }
    }

    private fun bindRecentRequests(data: JSONArray) {
        val container = binding.root.findViewById<LinearLayout>(R.id.layoutRecentRequests)
        container.removeAllViews()

        val limit = minOf(3, data.length())
        if (limit == 0) {
            binding.tvRecentRequestsEmpty.visibility = View.VISIBLE
            return
        }

        binding.tvRecentRequestsEmpty.visibility = View.GONE

        for (i in 0 until limit) {
            val pickup = data.optJSONObject(i) ?: continue
            val item = layoutInflater.inflate(R.layout.item_recent_request_home, container, false)
            item.findViewById<TextView>(R.id.tvRecentWasteType).text = "${pickup.optString("wasteType", "Waste")} Pickup"
            item.findViewById<TextView>(R.id.tvRecentAddress).text = pickup.optString("address", "No address")
            item.findViewById<TextView>(R.id.tvRecentStatus).text = statusLabelForHousehold(pickup.optString("status"))
            container.addView(item)
        }
    }

    private fun statusLabelForHousehold(raw: String): String {
        return when (raw.lowercase()) {
            "completed" -> "Completed"
            "household_confirmed" -> "Awaiting Collector Proof"
            "picked" -> "On The Way"
            else -> "Pending Pickup"
        }
    }

    private fun setupClicks() {
        binding.fabRequestPickup.setOnClickListener {
            val intent = Intent(requireContext(), RequestPickupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupAnnouncements() {
        val prefs = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val householdId = prefs.getString("HOUSEHOLD_ID", null)

        if (householdId.isNullOrBlank()) {
            bindAnnouncements(emptyList())
            return
        }

        MobileBackendApi.getMyNotifications(householdId, "Household") { success, data, _ ->
            activity?.runOnUiThread {
                if (!success || data == null) {
                    bindAnnouncements(emptyList())
                    return@runOnUiThread
                }

                val announcements = mutableListOf<Announcement>()
                for (i in 0 until data.length()) {
                    val notification = data.optJSONObject(i) ?: continue
                    if (!isAdminAnnouncement(notification)) continue

                    announcements += Announcement(
                        title = notification.optString("title", "Announcement"),
                        description = notification.optString("message", ""),
                        imageUrl = ""
                    )
                }

                bindAnnouncements(announcements)
            }
        }
    }

    private fun isAdminAnnouncement(notification: JSONObject): Boolean {
        val target = notification.optString("target", "")
        val targetValue = notification.optString("targetValue", "")

        return when (target) {
            "all", "all_households", "zone" -> true
            "single_household" -> targetValue.isNotBlank()
            else -> false
        }
    }

    private fun bindAnnouncements(announcements: List<Announcement>) {
        val items = if (announcements.isEmpty()) {
            listOf(
                Announcement(
                    title = "No announcements yet",
                    description = "Admin announcements will appear here.",
                    imageUrl = ""
                )
            )
        } else {
            announcements
        }

        binding.recyclerAnnouncements.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerAnnouncements.adapter = AnnouncementsAdapter(items) { announcement ->
            showAnnouncementDialog(announcement)
        }
    }

    private fun showAnnouncementDialog(announcement: Announcement) {
        AlertDialog.Builder(requireContext())
            .setTitle(announcement.title)
            .setMessage(announcement.description.ifBlank { "No details available" })
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
