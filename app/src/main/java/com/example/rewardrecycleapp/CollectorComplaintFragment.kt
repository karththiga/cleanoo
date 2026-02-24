package com.example.rewardrecycleapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class CollectorComplaintFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_collector_complaint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pickupId = arguments?.getString(ARG_PICKUP_ID)
        if (pickupId.isNullOrBlank()) {
            view.findViewById<TextView>(R.id.tvCollectorComplaintStatus).text = "Pickup not selected"
            return
        }

        view.findViewById<Button>(R.id.btnSubmitCollectorComplaint).setOnClickListener {
            val category = view.findViewById<EditText>(R.id.edtCollectorComplaintCategory).text.toString().trim()
            val detail = view.findViewById<EditText>(R.id.edtCollectorComplaintDetail).text.toString().trim()
            val status = view.findViewById<TextView>(R.id.tvCollectorComplaintStatus)

            if (category.isBlank() || detail.isBlank()) {
                status.text = "Please provide category and details"
                return@setOnClickListener
            }

            val btn = view.findViewById<Button>(R.id.btnSubmitCollectorComplaint)
            btn.isEnabled = false
            btn.text = "Submitting..."

            MobileBackendApi.collectorSubmitComplaint(pickupId, category, detail) { success, message ->
                activity?.runOnUiThread {
                    btn.isEnabled = true
                    btn.text = "Submit complaint"
                    status.text = if (success) "Complaint submitted successfully" else (message ?: "Complaint submission failed")
                }
            }
        }
    }

    companion object {
        private const val ARG_PICKUP_ID = "pickup_id"

        fun newInstance(pickupId: String): CollectorComplaintFragment {
            return CollectorComplaintFragment().apply {
                arguments = Bundle().apply { putString(ARG_PICKUP_ID, pickupId) }
            }
        }
    }
}
