const express = require("express");
const router = express.Router();
const {
  getRewards,
  approveReward,
  rejectReward,
  redeemReward
} = require("../controllers/rewardController");

// Admin reward routes
router.get("/", getRewards);
router.put("/approve/:id", approveReward);
router.put("/reject/:id", rejectReward);
router.put("/redeem/:id", redeemReward);

module.exports = router;
