const mongoose = require("mongoose");

const HouseholdSchema = new mongoose.Schema(
  {
    name: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    firebaseUid: { type: String, required: true, unique: true },
    phone: { type: String, required: true },

    address: { type: String, required: true },
    zone: { type: String, default: "Unassigned" },

    status: {
      type: String,
      enum: ["active", "blocked"],
      default: "active"
    },

    // ✅ TOTAL REWARD POINTS
    points: {
      type: Number,
      default: 0
    },

    // ✅ OPTIONAL (future use)
    warningHistory: [
      {
        reason: String,
        date: Date
      }
    ],

    warnings: {
      type: Number,
      default: 0
    }
  },
  { timestamps: true }
);

module.exports =
  mongoose.models.Household ||
  mongoose.model("Household", HouseholdSchema);
