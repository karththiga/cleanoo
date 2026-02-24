package com.example.rewardrecycleapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PickupDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pickup_details)

        val pickupId = intent.getStringExtra("pickup_id")
        if (pickupId.isNullOrBlank()) {
            findViewById<TextView>(R.id.tvPickupDetailsError).text = "Pickup details unavailable"
            return
        }

        MobileBackendApi.getPickupById(pickupId) { success, data, message ->
            runOnUiThread {
                if (!success || data == null) {
                    findViewById<TextView>(R.id.tvPickupDetailsError).text = message ?: "Unable to load pickup details"
                    return@runOnUiThread
                }

                val collectorName = data.optJSONObject("assignedCollector")?.optString("name") ?: "Awaiting assignment"
                val collectorPhone = data.optJSONObject("assignedCollector")?.optString("phone") ?: "-"

                findViewById<TextView>(R.id.tvDetailsWasteType).text = data.optString("wasteType", "-")
                findViewById<TextView>(R.id.tvDetailsAddress).text = data.optString("address", "-")
                findViewById<TextView>(R.id.tvDetailsStatus).text = data.optString("status", "pending")
                    .replaceFirstChar { it.uppercase() }
                findViewById<TextView>(R.id.tvDetailsCollector).text = collectorName
                findViewById<TextView>(R.id.tvDetailsCollectorPhone).text = collectorPhone

                val reviewRating = data.optInt("householdReviewRating", 0)
                val reviewComment = data.optString("householdReviewComment", "")
                findViewById<TextView>(R.id.tvDetailsReview).text =
                    if (reviewRating > 0) "$reviewRating/5 - $reviewComment".trim() else "No review yet"

                val complaintCategory = data.optString("householdComplaintCategory", "")
                val complaintDetail = data.optString("householdComplaintDetail", "")
                findViewById<TextView>(R.id.tvDetailsComplaint).text =
                    if (complaintCategory.isNotBlank() || complaintDetail.isNotBlank()) {
                        listOf(complaintCategory, complaintDetail).filter { it.isNotBlank() }.joinToString(" - ")
                    } else {
                        "No complaint yet"
                    }

                findViewById<TextView>(R.id.tvPickupDetailsError).text = ""
            }
        }
    }
}
