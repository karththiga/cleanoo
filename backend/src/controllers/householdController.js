const Household = require("../models/Household");
const Pickup = require("../models/PickupRequest");
const Reward = require("../models/Reward");

/* ==================================================
   MOBILE SIGNUP: CREATE HOUSEHOLD PROFILE
================================================== */
const createHouseholdProfile = async (req, res) => {
  try {
    const firebaseUid = req.user?.uid;
    const { name, email, phone, address, zone } = req.body;

    if (!firebaseUid) {
      return res.status(401).json({ success: false, message: "Unauthorized" });
    }

    if (!name || !email || !phone || !address) {
      return res.status(400).json({
        success: false,
        message: "name, email, phone and address are required"
      });
    }

    const existing = await Household.findOne({ firebaseUid });
    if (existing) {
      return res.json({ success: true, message: "Profile already exists", data: existing });
    }

    const household = await Household.create({
      firebaseUid,
      name,
      email,
      phone,
      address,
      zone: zone || "Unassigned"
    });

    return res.status(201).json({ success: true, message: "Household created", data: household });
  } catch (err) {
    console.error("Create household profile error:", err);
    if (err.code === 11000) {
      return res.status(409).json({ success: false, message: "Profile already exists" });
    }
    return res.status(500).json({ success: false, message: "Failed to create profile" });
  }
};

/* ==================================================
   MOBILE SIGNIN: FETCH HOUSEHOLD PROFILE BY FIREBASE UID
================================================== */
const getMyHouseholdProfile = async (req, res) => {
  try {
    const firebaseUid = req.user?.uid;
    if (!firebaseUid) {
      return res.status(401).json({ success: false, message: "Unauthorized" });
    }

    const household = await Household.findOne({ firebaseUid });

    if (!household) {
      return res.status(404).json({
        success: false,
        message: "Household profile not found for this account"
      });
    }

    return res.json({ success: true, data: household });
  } catch (err) {
    console.error("Get household profile error:", err);
    return res.status(500).json({ success: false, message: "Failed to fetch profile" });
  }
};

/* ==================================================
   MOBILE PROFILE UPDATE: UPDATE HOUSEHOLD BY FIREBASE UID
================================================== */
async function updateMyHouseholdProfile(req, res) {
  try {
    const firebaseUid = req.user?.uid;
    if (!firebaseUid) {
      return res.status(401).json({ success: false, message: "Unauthorized" });
    }

    const allowedUpdates = ["name", "phone", "address", "zone"];
    const payload = {};

    allowedUpdates.forEach((key) => {
      if (typeof req.body[key] === "string") {
        payload[key] = req.body[key].trim();
      }
    });

    if (!Object.keys(payload).length) {
      return res.status(400).json({
        success: false,
        message: "No valid fields provided"
      });
    }

    const updated = await Household.findOneAndUpdate(
      { firebaseUid },
      payload,
      { new: true }
    );

    if (!updated) {
      return res.status(404).json({
        success: false,
        message: "Household profile not found for this account"
      });
    }

    return res.json({ success: true, message: "Profile updated", data: updated });
  } catch (err) {
    console.error("Update household profile error:", err);
    return res.status(500).json({ success: false, message: "Failed to update profile" });
  }
}

/* ==================================================
   GET ALL HOUSEHOLDS (ADMIN)
================================================== */
const getHouseholds = async (req, res) => {
  try {
    const households = await Household.find()
      .sort({ createdAt: -1 })
      .select("-password"); // safety

    res.json({
      success: true,
      total: households.length,
      data: households
    });

  } catch (err) {
    console.error("Household load error:", err);
    res.status(500).json({ success: false, message: "Fetch failed" });
  }
};

/* ==================================================
   GET HOUSEHOLD PICKUP HISTORY
================================================== */
const getHouseholdHistory = async (req, res) => {
  try {
    const householdId = req.params.id;

    // 1. Get Pickups
    const pickups = await Pickup.find({ household: householdId })
      .sort({ requestDate: -1 })
      .populate("assignedCollector", "name phone");

    // 2. Get Rewards for this household
    const rewards = await Reward.find({ household: householdId });

    // 3. Map rewards to pickups for easy lookup
    const rewardMap = {};
    rewards.forEach(r => {
      rewardMap[r.pickup.toString()] = {
        points: r.points,
        status: r.status,
        isExpired: r.isExpired,
        isRedeemed: r.isRedeemed
      };
    });

    // 4. Attach points to pickup objects
    const historyWithPoints = pickups.map(p => {
      const reward = rewardMap[p._id.toString()];
      return {
        ...p.toObject(), // Convert mongoose doc to plain object
        points: reward ? reward.points : 0,
        rewardStatus: reward ? reward.status : null,
        isExpired: reward ? reward.isExpired : false,
        isRedeemed: reward ? reward.isRedeemed : false
      };
    });

    res.json({
      success: true,
      total: historyWithPoints.length,
      data: historyWithPoints
    });

  } catch (err) {
    console.error("History load error:", err);
    res.status(500).json({ success: false, message: "History load failed" });
  }
};

/* ==================================================
   GET HOUSEHOLD REWARD HISTORY
   (ADMIN + MOBILE APP)
================================================== */
const getHouseholdRewards = async (req, res) => {
  try {
    const rewards = await Reward.find({ household: req.params.id })
      .populate("pickup", "wasteType requestDate")
      .sort({ createdAt: -1 });

    res.json({
      success: true,
      total: rewards.length,
      data: rewards
    });

  } catch (err) {
    console.error("Reward history error:", err);
    res.status(500).json({ success: false, message: "Reward history failed" });
  }
};

/* ==================================================
   SEND WARNING TO HOUSEHOLD (ADMIN)
================================================== */
const sendWarning = async (req, res) => {
  try {
    const household = await Household.findById(req.params.id);

    if (!household) {
      return res.status(404).json({
        success: false,
        message: "Household not found"
      });
    }

    household.warnings = (household.warnings || 0) + 1;

    household.warningHistory = household.warningHistory || [];
    household.warningHistory.push({
      reason: req.body.reason || "Invalid waste upload",
      date: new Date()
    });

    await household.save();

    res.json({
      success: true,
      message: "Warning sent successfully",
      warnings: household.warnings
    });

  } catch (err) {
    console.error("Warning error:", err);
    res.status(500).json({ success: false, message: "Warning failed" });
  }
};

/* ==================================================
   UPDATE HOUSEHOLD (ADMIN)
================================================== */
const updateHousehold = async (req, res) => {
  try {
    const updated = await Household.findByIdAndUpdate(
      req.params.id,
      req.body,
      { new: true }
    );

    res.json({ success: true, data: updated });

  } catch (err) {
    console.error("Update error:", err);
    res.status(400).json({ success: false, message: "Update failed" });
  }
};

/* ==================================================
   DELETE HOUSEHOLD
================================================== */
const deleteHousehold = async (req, res) => {
  try {
    await Household.findByIdAndDelete(req.params.id);

    // optional cleanup (safe)
    await Pickup.deleteMany({ household: req.params.id });
    await Reward.deleteMany({ household: req.params.id });

    res.json({ success: true, message: "Deleted successfully" });

  } catch (err) {
    console.error("Delete error:", err);
    res.status(500).json({ success: false, message: "Delete failed" });
  }
};

module.exports = {
  createHouseholdProfile,
  getMyHouseholdProfile,
  updateMyHouseholdProfile,
  getHouseholds,
  deleteHousehold,
  updateHousehold,
  getHouseholdHistory,
  getHouseholdRewards, // ✅ NEW (important)
  sendWarning
};
