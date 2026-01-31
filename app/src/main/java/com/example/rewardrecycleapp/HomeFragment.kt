import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rewardrecycleapp.R
import com.example.rewardrecycleapp.RequestPickupActivity
import com.example.rewardrecycleapp.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {



        private var _binding: FragmentHomeBinding? = null
        private val binding get() = _binding!!
        private val auth = FirebaseAuth.getInstance()

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

            loadUserName()
            setupAnnouncements()
            setupClicks()
        }

        private fun loadUserName() {
            val user = auth.currentUser ?: return
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val name = doc.getString("name") ?: "User"
                        binding.tvGreeting.text = "Hello, $name 👋"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to load name", Toast.LENGTH_SHORT).show()
                }
        }

        private fun setupClicks() {

            // Navigate to Request Pickup
            binding.fabRequestPickup.setOnClickListener {
                val intent = Intent(requireContext(), RequestPickupActivity::class.java)
                startActivity(intent)
            }

            // Placeholder for future dashboard actions
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
