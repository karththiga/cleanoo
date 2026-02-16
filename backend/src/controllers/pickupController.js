const Pickup = require("../models/PickupRequest");
const Collector = require("../models/Collector");
const Household = require("../models/Household");
const Reward = require("../models/Reward");
const Setting = require("../models/Setting");
const Notification = require("../models/Notification");

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

    // AUTO-ASSIGN LOGIC
    let status = "pending";
    let assignedCollector = null;

    try {
      const household = await Household.findById(data.household);
      if (household && household.zone) {
        // Find active collectors in the same zone
        const collectors = await Collector.find({
          zone: household.zone,
          status: "active"
        });

        // Find one with 0 active tasks
        for (const col of collectors) {
          const activeTasks = await Pickup.countDocuments({
            assignedCollector: col._id,
            status: { $in: ["assigned", "approved"] }
          });

          if (activeTasks === 0) {
            assignedCollector = col._id;
            status = "assigned";
            break; // Found one!
          }
        }
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

    const collector = await Collector.findOne({
      zone: pickup.household.zone,
      status: "active"
    });

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
   COLLECTOR PICKUP (UPLOAD PROOF)
====================================================== */
exports.collectorPickup = async (req, res) => {
  try {
    const pickup = await Pickup.findById(req.params.id).populate("household");
    if (!pickup) return res.status(404).json({ message: "Pickup not found" });

    if (!req.file) {
      return res.status(400).json({ message: "Proof image required" });
    }

    if (!req.body.weight || Number(req.body.weight) <= 0) {
      return res.status(400).json({ message: "Weight is required to complete pickup" });
    }

    pickup.collectorImage = req.file.filename;
    // USER REQUEST: Auto-complete when collector uploads proof + weight
    pickup.status = "completed";
    pickup.pickedDate = new Date(); // They picked it up
    pickup.completedDate = new Date(); // And finished the job
    pickup.weight = Number(req.body.weight);

    await pickup.save();

    // 🏆 GENERATE PENDING REWARD (Logic Moved/Duplicated from updateStatus)
    const rewardExists = await Reward.findOne({ pickup: pickup._id });

    if (!rewardExists) {
      const settings = await Setting.findOne();
      const ratePerKg = settings?.rewards?.get(pickup.wasteType) || 0;
      const points = Math.round(pickup.weight * ratePerKg);

      await Reward.create({
        household: pickup.household._id,
        pickup: pickup._id,
        wasteType: pickup.wasteType,
        points,
        status: "pending" // Waiting for Admin Approval
      });

      // 🔔 NOTIFY ADMIN
      await Notification.create({
        title: "Reward Approval Needed",
        message: `Collector completed pickup (${pickup.wasteType}, ${pickup.weight}kg). Please review reward.`,
        target: "admin",
        type: "admin_alert"
      });
    }

    res.json({ success: true, message: "Pickup completed & Reward generated (Pending Approval)" });
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

      // 🔒 prevent duplicate rewards
      const rewardExists = await Reward.findOne({
        pickup: pickup._id
      });

      if (!rewardExists) {
        const settings = await Setting.findOne();

        // Check if weight is provided, else default to 0 (or handled by frontend to be required)
        // If weight < 0.1, we might treat it as 0 points or minimum 1? 
        // For now: exact calculation
        const pickupWeight = pickup.weight || 0;
        const ratePerKg = settings?.rewards?.get(pickup.wasteType) || 0;

        const points = Math.round(pickupWeight * ratePerKg);

        await Reward.create({
          household: pickup.household._id,
          pickup: pickup._id,
          wasteType: pickup.wasteType,
          points,
          status: "pending"
        });

        // 🔔 NOTIFY ADMIN (REWARD APPROVAL)
        await Notification.create({
          title: "Reward Approval Needed",
          message: `Reward generated for completed pickup (Type: ${pickup.wasteType}). Please review.`,
          target: "admin",
          type: "admin_alert"
        });
      }
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
