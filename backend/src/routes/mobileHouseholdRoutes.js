const express = require("express");
const router = express.Router();

const authMiddleware = require("../middleware/authMiddleware");
const {
  createHouseholdProfile,
  getMyHouseholdProfile,
  getMyRewardSummary,
  updateMyHouseholdProfile
} = require("../controllers/householdController");

router.use(authMiddleware);

router.post("/signup", createHouseholdProfile);
router.get("/me", getMyHouseholdProfile);
router.get("/me/rewards", getMyRewardSummary);
router.put("/me", updateMyHouseholdProfile);

module.exports = router;
