const express = require("express");
const router = express.Router();

const authMiddleware = require("../middleware/authMiddleware");
const {
  createHouseholdProfile,
  getMyHouseholdProfile
} = require("../controllers/householdController");

router.use(authMiddleware);

router.post("/signup", createHouseholdProfile);
router.get("/me", getMyHouseholdProfile);

module.exports = router;
