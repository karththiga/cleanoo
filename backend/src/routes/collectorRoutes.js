const express = require("express");
const router = express.Router();

const {
  getCollectors,
  addCollector,
  updateCollector,
  deleteCollector,
  toggleCollectorStatus,
  getCollectorHistory,
  getMyCollectorProfile,
  updateMyCollectorProfile
} = require("../controllers/collectorController");
const authMiddleware = require("../middleware/authMiddleware");

router.get("/", getCollectors);

router.get("/me", authMiddleware, getMyCollectorProfile);
router.put("/me", authMiddleware, updateMyCollectorProfile);

router.post("/", addCollector);

router.put("/status/:id", toggleCollectorStatus);

router.put("/:id", updateCollector);

router.delete("/:id", deleteCollector);

//  COLLECTOR JOB HISTORY
router.get("/:id/history", getCollectorHistory);

module.exports = router;
