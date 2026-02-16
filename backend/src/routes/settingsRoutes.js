const express = require("express");
const router = express.Router();

const {
  getSettings,
  addCategory,
  deleteCategory,
  updateReward,
  updateProfile,
  addPartner,
  deletePartner
} = require("../controllers/settingsController");

const upload = require("../middleware/upload");
const authMiddleware = require("../middleware/authMiddleware");

// Apply Auth Middleware to ALL settings routes
router.use(authMiddleware);

router.get("/", getSettings);

router.post("/categories", addCategory);
router.delete("/categories/:name", deleteCategory);

router.put("/rewards", updateReward);
router.put("/profile", upload.single("image"), updateProfile);

router.post("/partners", addPartner);
router.delete("/partners/:id", deletePartner);

module.exports = router;
