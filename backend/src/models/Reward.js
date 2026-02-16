const mongoose = require("mongoose");

const rewardSchema = new mongoose.Schema(
  {
    household: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "Household",
      required: true
    },

    pickup: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "PickupRequest",
      required: true,
      unique: true // 🚫 prevent duplicate rewards for same pickup
    },

    wasteType: {
      type: String,
      required: true
    },

    points: {
      type: Number,
      required: true
    },

    status: {
      type: String,
      enum: ["pending", "approved", "rejected"],
      default: "pending"
    },

    approvedBy: {
      type: String,
      default: null
    },

    isExpired: {
      type: Boolean,
      default: false
    },

    isRedeemed: {
      type: Boolean,
      default: false
    }
  },
  { timestamps: true }
);

/* ===============================
   INDEXES
=============================== */
rewardSchema.index({ status: 1 });
rewardSchema.index({ household: 1 });

/* ===============================
   EXPORT SAFE MODEL
=============================== */
module.exports =
  mongoose.models.Reward ||
  mongoose.model("Reward", rewardSchema);
