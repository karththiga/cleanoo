const express = require("express");
const router = express.Router();

const {
  getHouseholds,
  deleteHousehold,
  updateHousehold,
  getHouseholdHistory,
  sendWarning   // ✅ NEW
} = require("../controllers/householdController");

// GET ALL HOUSEHOLDS
router.get("/", getHouseholds);

// GET HOUSEHOLD HISTORY
router.get("/:id/history", getHouseholdHistory);

// SEND WARNING (Admin)
router.put("/:id/warning", sendWarning);

// UPDATE HOUSEHOLD
router.put("/:id", updateHousehold);

// DELETE HOUSEHOLD
router.delete("/:id", deleteHousehold);

module.exports = router;
