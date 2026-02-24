const mongoose = require("mongoose");

const pickupRequestSchema = new mongoose.Schema(
  {
    /* ===============================
       RELATIONS
    =============================== */
    household: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Household",
      required: true
    },

    assignedCollector: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Collector",
      default: null
    },

    /* ===============================
       PICKUP DETAILS
    =============================== */
    address: {
      type: String,
      required: true
    },

    wasteType: {
      type: String,
      required: true
    },

    weight: {
      type: Number,
      default: 0
    },

    /* ===============================
       IMAGES
    =============================== */
    // uploaded by household
    householdImage: {
      type: String,
      required: true
    },

    // uploaded by collector after pickup
    collectorImage: {
      type: String,
      default: ""
    },


    collectorLiveLocation: {
      type: String,
      default: ""
    },

    collectorLatitude: {
      type: Number
    },

    collectorLongitude: {
      type: Number
    },

    /* ===============================
       STATUS FLOW
    =============================== */
    status: {
      type: String,
      enum: [
        "pending",
        "approved",
        "assigned",
        "picked",
        "collector_completed",
        "household_confirmed",
        "completed",
        "rejected",
        "cancelled"
      ],
      default: "pending"
    },

    /* ===============================
       ADMIN VERIFICATION
    =============================== */
    verifiedByAdmin: {
      type: Boolean,
      default: false
    },

    verificationDate: {
      type: Date
    },

    rejectionReason: {
      type: String,
      default: ""
    },

    cancelReason: {
      type: String,
      default: ""
    },

    /* ===============================
       STATUS TIMELINE DATES 🔥
    =============================== */
    requestDate: {
      type: Date,
      default: Date.now
    },

    approvedDate: {
      type: Date
    },

    assignedDate: {
      type: Date
    },

    pickedDate: {
      type: Date
    },

    completedDate: {
      type: Date
    },

    householdReviewRating: {
      type: Number,
      min: 1,
      max: 5
    },

    householdReviewComment: {
      type: String,
      default: ""
    },

    householdComplaintCategory: {
      type: String,
      default: ""
    },

    householdComplaintDetail: {
      type: String,
      default: ""
    },

    householdConfirmedDate: {
      type: Date
    },

    cancelledDate: {
      type: Date
    }
  },
  { timestamps: true }
);

/* ===============================
   INDEXES
=============================== */
pickupRequestSchema.index({ status: 1 });
pickupRequestSchema.index({ requestDate: -1 });
pickupRequestSchema.index({ wasteType: 1 });

/* ===============================
   EXPORT SAFE MODEL
=============================== */
module.exports =
  mongoose.models.PickupRequest ||
  mongoose.model("PickupRequest", pickupRequestSchema);
