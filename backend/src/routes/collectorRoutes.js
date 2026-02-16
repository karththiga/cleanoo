const express = require("express");
const router = express.Router();

const {
  getCollectors,
  addCollector,
  updateCollector,
  deleteCollector,
  toggleCollectorStatus,
  getCollectorHistory   
} = require("../controllers/collectorController");

router.get("/", getCollectors);

router.post("/", addCollector);

router.put("/status/:id", toggleCollectorStatus);

router.put("/:id", updateCollector);

router.delete("/:id", deleteCollector);

//  COLLECTOR JOB HISTORY
router.get("/:id/history", getCollectorHistory);

module.exports = router;
