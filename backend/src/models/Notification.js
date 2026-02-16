const mongoose = require("mongoose");

const NotificationSchema = new mongoose.Schema(
  {
    title: { type: String, required: true },
    message: { type: String, required: true },

    target: {
      type: String,
      enum: [
        "all",
        "all_households",
        "all_collectors",
        "single_collector",
        "single_household",
        "admin"
      ],
      required: true
    },

    type: {
      type: String,
      enum: ["alert", "info", "admin_alert"],
      default: "info"
    },

    isHidden: {
      type: Boolean,
      default: false
    },

    isRead: {
      type: Boolean,
      default: false
    },

    targetValue: {
      type: String,
      default: null
    },

    userId: {
      type: mongoose.Schema.Types.ObjectId,
      refPath: "userType"
    },

    userType: {
      type: String,
      enum: ["Household", "Collector"]
    }
  },
  { timestamps: true }
);

module.exports =
  mongoose.models.Notification ||
  mongoose.model("Notification", NotificationSchema);
