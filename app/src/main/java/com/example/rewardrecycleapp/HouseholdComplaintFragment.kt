package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class HouseholdComplaintFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_household_complaint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pickupId = arguments?.getString(ARG_PICKUP_ID)
        if (pickupId.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvHouseholdComplaintStatus).text = "Pickup not selected"
            return
        }

        view.findViewById<Button>(R.id.btnSubmitHouseholdComplaint).setOnClickListener {
            val category = view.findViewById<EditText>(R.id.edtHouseholdComplaintCategory).text.toString().trim()
            val detail = view.findViewById<EditText>(R.id.edtHouseholdComplaintDetail).text.toString().trim()
            val status = view.findViewById<TextView>(R.id.tvHouseholdComplaintStatus)

            if (category.isBlank() || detail.isBlank()) {
                status.text = "Please provide category and details"
                return@setOnClickListener
            }

            val btn = view.findViewById<Button>(R.id.btnSubmitHouseholdComplaint)
            btn.isEnabled = false
            btn.text = "Submitting..."

            MobileBackendApi.householdSubmitComplaint(pickupId, category, detail) { success, message ->
                activity?.runOnUiThread {
                    btn.isEnabled = true
                    btn.text = "Submit complaint"
                    if (!success) {
                        status.text = message ?: "Complaint submission failed"
                        return@runOnUiThread
                    }
                    status.text = "Complaint submitted successfully"
                }
            }
        }
    }

    companion object {
        private const val ARG_PICKUP_ID = "pickup_id"

        fun newInstance(pickupId: String): HouseholdComplaintFragment {
            return HouseholdComplaintFragment().apply {
                arguments = Bundle().apply { putString(ARG_PICKUP_ID, pickupId) }
            }
        }
    }
}
