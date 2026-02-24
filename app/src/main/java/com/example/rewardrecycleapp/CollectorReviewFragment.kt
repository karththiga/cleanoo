package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class CollectorReviewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_collector_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pickupId = arguments?.getString(ARG_PICKUP_ID)
        if (pickupId.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvCollectorReviewStatus).text = "Pickup not selected"
            return
        }

        view.findViewById<Button>(R.id.btnSubmitCollectorReview).setOnClickListener {
            val rating = view.findViewById<EditText>(R.id.edtCollectorReviewRating).text.toString().trim()
            val comment = view.findViewById<EditText>(R.id.edtCollectorReviewComment).text.toString().trim()
            val status = view.findViewById<TextView>(R.id.tvCollectorReviewStatus)

            if (rating.isBlank()) {
                status.text = "Please enter rating"
                return@setOnClickListener
            }

            val btn = view.findViewById<Button>(R.id.btnSubmitCollectorReview)
            btn.isEnabled = false
            btn.text = "Submitting..."

            MobileBackendApi.collectorSubmitReview(pickupId, rating, comment) { success, message ->
                activity?.runOnUiThread {
                    btn.isEnabled = true
                    btn.text = "Submit review"
                    status.text = if (success) "Review submitted successfully" else (message ?: "Review submission failed")
                }
            }
        }
    }

    companion object {
        private const val ARG_PICKUP_ID = "pickup_id"

        fun newInstance(pickupId: String): CollectorReviewFragment {
            return CollectorReviewFragment().apply {
                arguments = Bundle().apply { putString(ARG_PICKUP_ID, pickupId) }
            }
        }
    }
}
