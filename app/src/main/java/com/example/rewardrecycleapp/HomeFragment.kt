import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rewardrecycleapp.Announcement
import com.example.rewardrecycleapp.AnnouncementsAdapter
import com.example.rewardrecycleapp.MobileBackendApi
import com.example.rewardrecycleapp.R
import com.example.rewardrecycleapp.RequestPickupActivity
import com.example.rewardrecycleapp.databinding.FragmentHomeBinding

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
        setupAnnouncements()
        setupClicks()
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
                } else if (!message.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClicks() {
        binding.fabRequestPickup.setOnClickListener {
            val intent = Intent(requireContext(), RequestPickupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupAnnouncements() {
        val announcements = listOf(
            Announcement(
                title = "Pickup Schedule Update",
                description = "Saturday pickups move to 10 AM this week.",
                imageUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=600"
            ),
            Announcement(
                title = "Bonus Points Week",
                description = "Earn 2x points on glass and metal recycling.",
                imageUrl = "https://images.unsplash.com/photo-1489515217757-5fd1be406fef?w=600"
            ),
            Announcement(
                title = "Neighborhood Cleanup",
                description = "Join the cleanup drive and get rewarded.",
                imageUrl = "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=600"
            )
        )
        binding.recyclerAnnouncements.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerAnnouncements.adapter = AnnouncementsAdapter(announcements)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
