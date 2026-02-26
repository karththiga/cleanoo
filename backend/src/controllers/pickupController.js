const Pickup = require("../models/PickupRequest");
const Collector = require("../models/Collector");
const Household = require("../models/Household");
const Reward = require("../models/Reward");
const Setting = require("../models/Setting");
const Notification = require("../models/Notification");

const ACTIVE_ASSIGNMENT_STATUSES = ["assigned", "approved", "picked", "collector_completed"];



function getWasteTypeRewardRate(settings, wasteType) {
  const normalized = (wasteType || "").toString().trim();
  if (!normalized) return 0;

  const directRate = settings?.rewards?.get(normalized);
  if (typeof directRate === "number") return directRate;

  const lower = normalized.toLowerCase();
  const recyclableTypes = new Set([
    "plastic",
    "paper",
    "cardboard",
    "glass",
    "metal",
    "aluminum",
    "steel",
    "e-waste",
    "ewaste",
    "textile"
  ]);

  const baseRatePerKg = 6;
  return recyclableTypes.has(lower) ? baseRatePerKg * 1.8 : baseRatePerKg;
}

async function applyCompletedPickupReward(pickup, approvedBy = "System") {
  if (!pickup?.household) return { reward: null, pointsAdded: 0 };

  const settings = await Setting.findOne();
  const pickupWeight = Number(pickup.weight || 0);
  const ratePerKg = getWasteTypeRewardRate(settings, pickup.wasteType);
  const points = Math.max(0, Math.round(pickupWeight * ratePerKg));

  let reward = await Reward.findOne({ pickup: pickup._id });
  let pointsAdded = 0;

  if (!reward) {
    reward = await Reward.create({
      household: pickup.household._id || pickup.household,
      pickup: pickup._id,
      wasteType: pickup.wasteType,
      points,
      status: "approved",
      approvedBy
    });
    pointsAdded = points;
  } else if (reward.status !== "approved") {
    reward.status = "approved";
    reward.approvedBy = approvedBy;
    reward.points = points;
    await reward.save();
    pointsAdded = points;
  }

  if (pointsAdded > 0) {
    await Household.findByIdAndUpdate(reward.household, { $inc: { points: pointsAdded } });
  }

  return { reward, pointsAdded };
}

function calculateDistanceKm(lat1, lon1, lat2, lon2) {
  const toRad = (deg) => (deg * Math.PI) / 180;
  const earthRadiusKm = 6371;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadiusKm * c;
}


function normalizeLocationText(value) {
  return (value || "")
    .toString()
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function scoreAddressZoneMatch(pickupAddress, collectorZone) {
  const normalizedAddress = normalizeLocationText(pickupAddress);
  const normalizedZone = normalizeLocationText(collectorZone);
  if (!normalizedAddress || !normalizedZone) return 0;

  if (normalizedAddress.includes(normalizedZone) || normalizedZone.includes(normalizedAddress)) {
    return 1;
  }

  const addressTokens = new Set(normalizedAddress.split(" ").filter(Boolean));
  const zoneTokens = new Set(normalizedZone.split(" ").filter(Boolean));
  if (!addressTokens.size || !zoneTokens.size) return 0;

  let overlap = 0;
  for (const token of zoneTokens) {
    if (addressTokens.has(token)) overlap += 1;
  }

  return overlap / Math.max(zoneTokens.size, 1);
}

async function findBestAvailableCollector(household, pickupAddress = "") {
  if (!household) return null;

  const sourceAddress = (household.address || pickupAddress || "").toString().trim();
  if (!sourceAddress) return null;

  const activeCollectorFilter = { status: { $ne: "blocked" } };
  const collectors = await Collector.find(activeCollectorFilter);
  if (!collectors.length) return null;

  const candidates = [];
  for (const col of collectors) {
    const activeTasks = await Pickup.countDocuments({
      assignedCollector: col._id,
      status: { $in: ACTIVE_ASSIGNMENT_STATUSES }
    });

    let distanceKm = Number.MAX_SAFE_INTEGER;
    if (
      typeof household.latitude === "number" &&
      typeof household.longitude === "number" &&
      typeof col.latitude === "number" &&
      typeof col.longitude === "number"
    ) {
      distanceKm = calculateDistanceKm(
        household.latitude,
        household.longitude,
        col.latitude,
        col.longitude
      );
    }

    const zoneMatchScore = scoreAddressZoneMatch(sourceAddress, col.zone);

    candidates.push({ collector: col, activeTasks, distanceKm, zoneMatchScore });
  }

  const matchingCandidates = candidates.filter((candidate) => candidate.zoneMatchScore > 0);

  if (matchingCandidates.length) {
    // If any household-address text matches collector zone, prefer the nearest matching collector.
    matchingCandidates.sort((a, b) => {
      if (a.distanceKm !== b.distanceKm) return a.distanceKm - b.distanceKm;
      if (a.zoneMatchScore !== b.zoneMatchScore) return b.zoneMatchScore - a.zoneMatchScore;
      return a.activeTasks - b.activeTasks;
    });

    return matchingCandidates[0].collector;
  }

  // No place-name match found: keep request waiting for collector assignment.
  return null;
}

/* ======================================================
   GET ALL PICKUPS (FILTERS)
====================================================== */
exports.getRequests = async (req, res) => {
  try {
    const { q, status, from, to } = req.query;
    let filter = {};

    if (status) filter.status = status;

    if (q) {
      filter.$or = [
        { wasteType: new RegExp(q, "i") },
        { address: new RegExp(q, "i") }
      ];
    }

    if (from || to) {
      filter.requestDate = {};
      if (from) filter.requestDate.$gte = new Date(from);
      if (to) filter.requestDate.$lte = new Date(to);
    }

    const requests = await Pickup.find(filter)
      .populate("household", "name phone address zone warnings")
      .populate("assignedCollector", "name phone zone")
      .sort({ requestDate: -1 });

    res.json({ success: true, data: requests });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Load failed" });
  }
};

/* ======================================================
   GET SINGLE PICKUP (BY ID)
====================================================== */
exports.getPickupById = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id)
      .populate("household", "name phone address zone")
      .populate("assignedCollector", "name phone zone");

    if (!pickup) {
      return res.status(404).json({ success: false, message: "Pickup not found" });
    }

    res.json({ success: true, data: pickup });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Load failed" });
  }
};

/* ======================================================
   CREATE PICKUP (HOUSEHOLD)
====================================================== */
exports.createRequest = async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, message: "Image required" });
    }

    const data = JSON.parse(req.body.data);

    // AUTO-ASSIGN LOGIC (nearest available collector in zone)
    let status = "pending";
    let assignedCollector = null;

    try {
      const household = await Household.findById(data.household);
      const bestCollector = await findBestAvailableCollector(household, data.address);
      if (bestCollector) {
        assignedCollector = bestCollector._id;
        status = "assigned";
      }
    } catch (assignError) {
      console.error("Auto-assign failed:", assignError);
    }

    // 🛡️ SECURITY: Remove sensitive fields households shouldn't set
    const { weight, points, ...safeData } = data;

    const created = await Pickup.create({
      ...safeData,
      weight: 0, // Ensure weight starts at 0 (set by Collector/Admin later)
      householdImage: req.file.filename,
      status: status,
      assignedCollector: assignedCollector,
      assignedDate: assignedCollector ? new Date() : null, // Set date if assigned
      requestDate: new Date()
    });

    // 🔔 NOTIFY ADMIN
    await Notification.create({
      title: "New Pickup Request",
      message: `New request from household for ${data.wasteType} waste.`,
      target: "admin",
      type: "admin_alert"
    });

    if (assignedCollector) {
      await Notification.create({
        title: "New Pickup Assigned",
        message: `A new ${data.wasteType} pickup has been assigned to you.`,
        target: "single_collector",
        userId: assignedCollector,
        userType: "Collector",
        type: "assignment"
      });
    }

    res.json({ success: true, data: created });
  } catch (err) {
    console.error(err);
    res.status(400).json({ success: false, message: "Create failed" });
  }
};

/* ======================================================
   APPROVE PICKUP (AUTO ASSIGN COLLECTOR)
====================================================== */
exports.approvePickup = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id).populate("household");
    if (!pickup) return res.status(404).json({ message: "Pickup not found" });

    pickup.status = "approved";
    pickup.verifiedByAdmin = true;
    pickup.rejectionReason = "";

    const collector = await findBestAvailableCollector(pickup.household, pickup.address);

    if (collector) {
      pickup.assignedCollector = collector._id;
      pickup.status = "assigned";
    }

    await pickup.save();
    res.json({ success: true, data: pickup });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Approval failed" });
  }
};

/* ======================================================
   REJECT PICKUP
====================================================== */
exports.rejectPickup = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id);
    if (!pickup) return res.status(404).json({ message: "Pickup not found" });

    pickup.status = "rejected";
    pickup.rejectionReason = req.body.reason || "Invalid request";
    pickup.cancelledDate = new Date();

    await pickup.save();
    res.json({ success: true });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Reject failed" });
  }
};

/* ======================================================
   HOUSEHOLD CANCEL PICKUP
====================================================== */
exports.householdCancelPickup = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id);
    if (!pickup) return res.status(404).json({ success: false, message: "Pickup not found" });

    const reason = (req.body?.reason || "").toString().trim();
    if (!reason) {
      return res.status(400).json({ success: false, message: "Cancellation reason is required" });
    }

    if (!["pending", "approved", "assigned"].includes((pickup.status || "").toLowerCase())) {
      return res.status(400).json({ success: false, message: "Only pending or assigned pickups can be cancelled" });
    }

    pickup.status = "cancelled";
    pickup.cancelReason = reason;
    pickup.cancelledDate = new Date();
    pickup.assignedCollector = null;

    await pickup.save();

    await Notification.create({
      title: "Pickup request cancelled",
      message: `Household cancelled pickup request. Reason: ${reason}`,
      target: "admin",
      type: "admin_alert"
    });

    res.json({ success: true, data: pickup, message: "Pickup cancelled" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Cancel failed" });
  }
};

/* ======================================================
   ASSIGN COLLECTOR (ADMIN)
====================================================== */
exports.assignCollector = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id);
    if (!pickup) return res.status(404).json({ message: "Pickup not found" });

    pickup.assignedCollector = req.body.collector;
    pickup.status = "assigned";

    await pickup.save();
    res.json({ success: true });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Assign failed" });
  }
};



/* ======================================================
   COLLECTOR START ROUTE (MOBILE)
====================================================== */
exports.collectorStartRoute = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id);
    if (!pickup) return res.status(404).json({ success: false, message: "Pickup not found" });

    const liveLocation = (req.body?.liveLocation || "").toString().trim();
    const latitude = typeof req.body?.latitude === "number" ? req.body.latitude : 9.6615;
    const longitude = typeof req.body?.longitude === "number" ? req.body.longitude : 80.0255;

    pickup.status = "picked";
    pickup.pickedDate = new Date();
    pickup.collectorLiveLocation = liveLocation;
    pickup.collectorLatitude = latitude;
    pickup.collectorLongitude = longitude;

    await pickup.save();

    await Notification.create({
      title: "Collector started route",
      message: "Your assigned collector has started the route.",
      target: "single_household",
      userId: pickup.household,
      userType: "Household",
      type: "info"
    });

    res.json({ success: true, data: pickup });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Start route failed" });
  }
};

/* ======================================================
   COLLECTOR UPDATE LIVE LOCATION (MOBILE)
====================================================== */
exports.updateCollectorLiveLocation = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id);
    if (!pickup) return res.status(404).json({ success: false, message: "Pickup not found" });

    pickup.collectorLiveLocation = req.body?.liveLocation || pickup.collectorLiveLocation || "Collector en route";
    if (typeof req.body?.latitude === "number") pickup.collectorLatitude = req.body.latitude;
    if (typeof req.body?.longitude === "number") pickup.collectorLongitude = req.body.longitude;
    if (pickup.status === "assigned" || pickup.status === "approved") {
      pickup.status = "picked";
      pickup.pickedDate = pickup.pickedDate || new Date();
    }

    await pickup.save();
    res.json({ success: true, data: pickup });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Location update failed" });
  }
};

/* ======================================================
   HOUSEHOLD CONFIRM COLLECTION (FINAL STEP)
====================================================== */
exports.householdConfirmCollected = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id);
    if (!pickup) {
      return res.status(404).json({ success: false, message: "Pickup not found" });
    }

    if (pickup.status !== "collector_completed") {
      return res.status(400).json({ success: false, message: "Collector has not completed this pickup yet" });
    }

    pickup.status = "completed";
    pickup.completedDate = pickup.completedDate || new Date();
    pickup.householdConfirmedDate = new Date();
    await pickup.save();

    const { pointsAdded } = await applyCompletedPickupReward(pickup, "Household Confirmed");

    await Notification.create({
      title: "Pickup confirmed by household",
      message: "Household confirmed your completed pickup.",
      target: "single_collector",
      userId: pickup.assignedCollector,
      userType: "Collector",
      type: "info"
    });

    res.json({ success: true, data: pickup, pointsAdded, message: "Pickup confirmed successfully" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Household confirmation failed" });
  }
};

exports.householdSubmitReview = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id);
    if (!pickup) return res.status(404).json({ success: false, message: "Pickup not found" });

    const rating = Number(req.body?.rating);
    const comment = (req.body?.comment || "").toString().trim();

    if (!Number.isFinite(rating) || rating < 1 || rating > 5) {
      return res.status(400).json({ success: false, message: "Rating must be between 1 and 5" });
    }

    pickup.householdReviewRating = rating;
    pickup.householdReviewComment = comment;
    await pickup.save();

    if (pickup.assignedCollector) {
      await Notification.create({
        title: "Household submitted review",
        message: `A household review was submitted (${rating}/5).`,
        target: "single_collector",
        userId: pickup.assignedCollector,
        userType: "Collector",
        type: "info"
      });
    }

    res.json({ success: true, data: pickup });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Review submission failed" });
  }
};

exports.householdSubmitComplaint = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id);
    if (!pickup) return res.status(404).json({ success: false, message: "Pickup not found" });

    const category = (req.body?.category || "").toString().trim();
    const detail = (req.body?.detail || "").toString().trim();

    if (!category || !detail) {
      return res.status(400).json({ success: false, message: "Category and detail are required" });
    }

    pickup.householdComplaintCategory = category;
    pickup.householdComplaintDetail = detail;
    await pickup.save();

    if (pickup.assignedCollector) {
      await Notification.create({
        title: "Household raised complaint",
        message: `Complaint: ${category}`,
        target: "single_collector",
        userId: pickup.assignedCollector,
        userType: "Collector",
        type: "alert"
      });
    }

    res.json({ success: true, data: pickup });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Complaint submission failed" });
  }
};

exports.collectorSubmitReview = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id);
    if (!pickup) return res.status(404).json({ success: false, message: "Pickup not found" });

    const rating = Number(req.body?.rating);
    const comment = (req.body?.comment || "").toString().trim();

    if (!Number.isFinite(rating) || rating < 1 || rating > 5) {
      return res.status(400).json({ success: false, message: "Rating must be between 1 and 5" });
    }

    pickup.collectorReviewRating = rating;
    pickup.collectorReviewComment = comment;
    await pickup.save();

    await Notification.create({
      title: "Collector submitted review",
      message: `Your collector submitted a review (${rating}/5).`,
      target: "single_household",
      userId: pickup.household,
      userType: "Household",
      type: "info"
    });

    res.json({ success: true, data: pickup });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Collector review submission failed" });
  }
};

exports.collectorSubmitComplaint = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id);
    if (!pickup) return res.status(404).json({ success: false, message: "Pickup not found" });

    const category = (req.body?.category || "").toString().trim();
    const detail = (req.body?.detail || "").toString().trim();

    if (!category || !detail) {
      return res.status(400).json({ success: false, message: "Category and detail are required" });
    }

    pickup.collectorComplaintCategory = category;
    pickup.collectorComplaintDetail = detail;
    await pickup.save();

    await Notification.create({
      title: "Collector raised complaint",
      message: `Complaint: ${category}`,
      target: "single_household",
      userId: pickup.household,
      userType: "Household",
      type: "alert"
    });

    res.json({ success: true, data: pickup });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Collector complaint submission failed" });
  }
};

/* ======================================================
   COLLECTOR PICKUP (UPLOAD PROOF)
====================================================== */
exports.collectorPickup = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id).populate("household");
    if (!pickup) return res.status(404).json({ message: "Pickup not found" });

    if (!req.file) {
      return res.status(400).json({ message: "Proof image required" });
    }

    if (pickup.status !== "picked") {
      return res.status(400).json({ message: "Start route before uploading proof" });
    }

    if (!req.body.weight || Number(req.body.weight) <= 0) {
      return res.status(400).json({ message: "Weight is required to complete pickup" });
    }

    pickup.collectorImage = req.file.filename;
    // Collector finished physically; household still needs to confirm final completion.
    pickup.status = "collector_completed";
    pickup.pickedDate = new Date(); // They picked it up
    pickup.completedDate = new Date();
    pickup.weight = Number(req.body.weight);

    await pickup.save();

    // Reward is finalized when status moves to completed (household confirm/admin completion).

    // Notify household that collector submitted evidence and awaits final confirmation.
    await Notification.create({
      title: "Collector completed pickup",
      message: "Your collector has uploaded evidence. Please confirm completion from your app.",
      target: "single_household",
      userId: pickup.household._id,
      userType: "Household",
      type: "info"
    });

    res.json({ success: true, message: "Evidence saved. Waiting for household confirmation." });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Pickup failed" });
  }
};








/* ======================================================
   UPDATE STATUS (ADMIN)
   → AUTO CREATE REWARD ON COMPLETED
====================================================== */
exports.updateStatus = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id)
      .populate("household");

    if (!pickup)
      return res.status(404).json({ message: "Pickup not found" });

    const { status, weight } = req.body;
    pickup.status = status;

    if (weight) {
      pickup.weight = Number(weight);
    }

    if (status === "completed") {
      pickup.completedDate = new Date();
      await applyCompletedPickupReward(pickup, "Admin");
    }

    if (status === "cancelled") {
      pickup.cancelledDate = new Date();
    }

    await pickup.save();
    res.json({ success: true });

  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Update failed" });
  }
};













/* ======================================================
   SEND WARNING TO HOUSEHOLD
====================================================== */
exports.sendWarning = async (req, res) => {
  try {
    const household = await Household.findById(req.params.id);
    if (!household)
      return res.status(404).json({ message: "Household not found" });

    // 1. Increment Count
    household.warnings = (household.warnings || 0) + 1;
    await household.save();

    // 2. 🔔 NOTIFY HOUSEHOLD
    await Notification.create({
      title: "Warning Issued ⚠️",
      message: "We detected a false pickup request. Please verify your waste items. Repeated false requests may lead to account suspension.",
      target: "single_household",
      userId: household._id,
      userType: "Household",
      type: "alert"
    });

    res.json({ success: true });
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Warning failed: " + err.message });
  }
};

/* ======================================================
   EXPORT PICKUPS (CSV)
====================================================== */
exports.exportPickups = async (req, res) => {
  try {
    const data = await Pickup.find()
      .populate("household", "name")
      .populate("assignedCollector", "name")
      .sort({ requestDate: -1 });

    let csv =
      "Household,Waste,Address,Collector,Status,Requested,Picked,Completed\n";

    data.forEach(p => {
      csv += `"${p.household?.name || ""}",`;
      csv += `"${p.wasteType}",`;
      csv += `"${p.address}",`;
      csv += `"${p.assignedCollector?.name || ""}",`;
      csv += `"${p.status}",`;
      csv += `"${p.requestDate || ""}",`;
      csv += `"${p.pickedDate || ""}",`;
      csv += `"${p.completedDate || ""}"\n`;
    });

    res.setHeader("Content-Type", "text/csv");
    res.setHeader("Content-Disposition", "attachment; filename=pickups.csv");
    res.send(csv);
  } catch (err) {
    console.error(err);
    res.status(500).send("Export failed");
  }
};

/* ======================================================
   GET HOUSEHOLD PICKUPS (MOBILE)
====================================================== */
exports.getHouseholdRequests = async (req, res) => {
  try {
    const pickups = await Pickup.find({ household: req.params.householdId })
      .populate("assignedCollector", "name phone")
      .sort({ requestDate: -1 });

    res.json({ success: true, data: pickups });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Load failed" });
  }
};

/* ======================================================
   GET INCOMING REQUESTS FOR COLLECTOR (MOBILE)
====================================================== */
exports.getCollectorIncomingRequests = async (req, res) => {
  try {
    const { email, uid, collectorId } = req.query;
    let collector = null;

    if (collectorId) collector = await Collector.findById(collectorId);
    if (!collector && uid) collector = await Collector.findOne({ uid });
    if (!collector && email) collector = await Collector.findOne({ email });

    if (!collector) {
      return res.status(404).json({ success: false, message: "Collector not found" });
    }

    const incoming = await Pickup.find({
      assignedCollector: collector._id,
      status: { $in: ACTIVE_ASSIGNMENT_STATUSES }
    })
      .populate("household", "name phone address")
      .sort({ requestDate: -1 });

    res.json({ success: true, data: incoming });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Load failed" });
  }
};
