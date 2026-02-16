const mongoose = require("mongoose");

const SettingSchema = new mongoose.Schema({

  categories: {
    type: [String],
    default: ["Plastic", "Paper", "Glass", "Organic", "Metal"]
  },

  // Dynamic reward points per waste type
  rewards: {
    type: Map,
    of: Number,
    default: {
      Plastic: 10,
      Paper: 8,
      Glass: 9,
      Metal: 12,
      Organic: 6
    }
  },

  adminProfile: {
    name: { type: String, default: "Admin" },
    email: { type: String, default: "admin@mail.com" },
    password: { type: String, default: "" },
    image: { type: String, default: null }
  },

  // Company Tie-ups for redemptions
  partners: [{
    name: { type: String, required: true },
    type: { type: String, required: true }, // e.g., "Supermarket", "Telecom"
    logo: { type: String, default: null }
  }]

});

module.exports =
  mongoose.models.Setting ||
  mongoose.model("Setting", SettingSchema);
