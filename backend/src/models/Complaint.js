const mongoose = require("mongoose");

const ComplaintSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    required: true
  },

  userType: {
    type: String,
    enum: ["household", "collector"],
    required: true
  },

  message: {
    type: String,
    required: true
  },

  image: {
    type: String
  },

  status: {
    type: String,
    enum: ["pending", "resolved", "rejected"],
    default: "pending"
  },

  adminNote: {
    type: String,
    default: ""
  },

  createdAt: {
    type: Date,
    default: Date.now
  }
});

module.exports = mongoose.model("Complaint", ComplaintSchema);
