const express = require("express");
const router = express.Router();
const upload = require("../middleware/upload");

const {
  getRequests,
  getPickupById,
  createRequest,
  approvePickup,
  rejectPickup,
  assignCollector,
  collectorStartRoute,
  updateCollectorLiveLocation,
  collectorPickup,
  updateStatus,
  exportPickups,
  sendWarning,
  getHouseholdRequests,
  getCollectorIncomingRequests
} = require("../controllers/pickupController");

// ===============================
// EXPORT CSV (ADMIN)
// ===============================
router.get("/export", exportPickups);

// ===============================
// GET ALL PICKUPS (ADMIN)
// ===============================
router.get("/", getRequests);

// ===============================
// MOBILE: HOUSEHOLD REQUEST HISTORY
// ===============================
router.get("/household/:householdId", getHouseholdRequests);

// ===============================
// MOBILE: COLLECTOR INCOMING REQUESTS
// ===============================
router.get("/collector/incoming", getCollectorIncomingRequests);

// ===============================
// GET SINGLE PICKUP (BY ID)
// ===============================
router.get("/:id", getPickupById);

// ===============================
// CREATE PICKUP (HOUSEHOLD - MOBILE)
// ===============================
router.post(
  "/upload",
  upload.single("image"),
  createRequest
);

// ===============================
// ADMIN APPROVE / REJECT
// ===============================
router.put("/approve/:id", approvePickup);
router.put("/reject/:id", rejectPickup);

// ===============================
// ADMIN ASSIGN COLLECTOR
// ===============================
router.put("/assign/:id", assignCollector);

// ===============================
// COLLECTOR START ROUTE / LIVE LOCATION
// ===============================
router.put("/collector/start-route/:id", collectorStartRoute);
router.put("/collector/location/:id", updateCollectorLiveLocation);

// ===============================
// COLLECTOR UPLOAD PICK PROOF
// ===============================
router.post(
  "/collector/pick/:id",
  upload.single("image"),
  collectorPickup
);

// ===============================
// ADMIN OVERRIDE STATUS
// (completed / cancelled)
// ===============================
router.put("/status/:id", updateStatus);

// ===============================
// SEND WARNING TO HOUSEHOLD
// ===============================
router.put("/warning/:id", sendWarning);

module.exports = router;
