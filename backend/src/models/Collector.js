const mongoose = require("mongoose");

const CollectorSchema = new mongoose.Schema({
  name: { type: String, required: true, trim: true },
  email: { type: String, required: true, unique: true, lowercase: true },
  password: { type: String, required: true },
  uid: { type: String, unique: true }, // Firebase UID link
  phone: { type: String, required: true },
  zone: { type: String, default: "Not Assigned" },
  latitude: { type: Number },
  longitude: { type: Number },
  status: { type: String, enum: ["active", "blocked"], default: "active" },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model("Collector", CollectorSchema);
