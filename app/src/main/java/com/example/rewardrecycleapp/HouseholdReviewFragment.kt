package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class HouseholdReviewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_household_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pickupId = arguments?.getString(ARG_PICKUP_ID)
        if (pickupId.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvHouseholdReviewStatus).text = "Pickup not selected"
            return
        }

        view.findViewById<Button>(R.id.btnSubmitHouseholdReview).setOnClickListener {
            val rating = view.findViewById<EditText>(R.id.edtHouseholdReviewRating).text.toString().trim()
            val comment = view.findViewById<EditText>(R.id.edtHouseholdReviewComment).text.toString().trim()
            val status = view.findViewById<TextView>(R.id.tvHouseholdReviewStatus)

            if (rating.isBlank()) {
                status.text = "Please enter rating"
                return@setOnClickListener
            }

            val btn = view.findViewById<Button>(R.id.btnSubmitHouseholdReview)
            btn.isEnabled = false
            btn.text = "Submitting..."

            MobileBackendApi.householdSubmitReview(pickupId, rating, comment) { success, message ->
                activity?.runOnUiThread {
                    btn.isEnabled = true
                    btn.text = "Submit review"
                    if (!success) {
                        status.text = message ?: "Review submission failed"
                        return@runOnUiThread
                    }
                    status.text = "Review submitted successfully"
                }
            }
        }
    }

    companion object {
        private const val ARG_PICKUP_ID = "pickup_id"

        fun newInstance(pickupId: String): HouseholdReviewFragment {
            return HouseholdReviewFragment().apply {
                arguments = Bundle().apply { putString(ARG_PICKUP_ID, pickupId) }
            }
        }
    }
}
