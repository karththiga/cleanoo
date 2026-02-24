const Collector = require("../models/Collector");
const PickupRequest = require("../models/PickupRequest");
const bcrypt = require("bcryptjs");
const admin = require("../config/firebase"); // Import Firebase Admin

// GET ALL COLLECTORS
const getCollectors = async (req, res) => {
  try {
    const collectors = await Collector.find().sort({ createdAt: -1 });
    res.json({
      success: true,
      total: collectors.length,
      data: collectors
    });
  } catch (error) {
    console.error("Fetch collectors error:", error);
    res.status(500).json({ success: false, message: "Failed to fetch collectors" });
  }
};

// ADD COLLECTOR
const addCollector = async (req, res) => {
  try {
    let { name, email, phone, zone, password } = req.body;
    console.log("--> ADD COLLECTOR REQUEST:", email);

    if (!name || !email || !phone) {
      return res.status(400).json({ success: false, message: "Name, email, and phone are required" });
    }

    const exists = await Collector.findOne({ email });
    if (exists) {
      return res.status(409).json({ success: false, message: "Collector already exists" });
    }

    // Auto-generate password if not provided
    let generatedPassword = null;
    if (!password) {
      generatedPassword = Math.random().toString(36).slice(-8); // Random 8 char string
      password = generatedPassword;
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    // 1. Create User in Firebase Auth
    let firebaseUser;
    try {
      firebaseUser = await admin.auth().createUser({
        email,
        password,
        displayName: name,
        phoneNumber: phone.startsWith("+") ? phone : undefined // Firebase requires E.164 format. If not provided, skip or format it.
      });
      console.log("🔥 Firebase User Created (Object):", firebaseUser.uid);
      if (!firebaseUser.uid) {
        console.error("❌ CRITICAL: Firebase user created but has no UID");
      }
    } catch (fbError) {
      console.error("Firebase Create Error:", fbError);
      return res.status(400).json({ success: false, message: "Firebase Auth Error: " + fbError.message });
    }

    // 2. Create Collector in MongoDB
    const newCollector = await Collector.create({
      name,
      email,
      phone,
      zone,
      password: hashedPassword,
      uid: firebaseUser.uid, // Link UID
      status: "active"
    });

    res.json({
      success: true,
      message: "Collector added successfully",
      data: newCollector,
      generatedPassword: generatedPassword || null // Return password if generated
    });

  } catch (error) {
    console.error("Add collector error:", error);
    res.status(500).json({ success: false, message: "Create failed" });
  }
};

// UPDATE COLLECTOR
const updateCollector = async (req, res) => {
  try {
    const updated = await Collector.findByIdAndUpdate(
      req.params.id,
      { $set: req.body },
      { new: true, runValidators: true }
    );

    if (!updated) {
      return res.status(404).json({ success: false, message: "Collector not found" });
    }

    res.json({
      success: true,
      message: "Collector updated successfully",
      data: updated
    });

  } catch (error) {
    console.error("Update collector error:", error);
    res.status(500).json({ success: false, message: "Update failed" });
  }
};

// DELETE COLLECTOR
const deleteCollector = async (req, res) => {
  try {
    const collector = await Collector.findById(req.params.id);

    if (!collector) {
      return res.status(404).json({ success: false, message: "Collector not found" });
    }

    // 1. Delete from Firebase
    if (collector.uid) {
      try {
        await admin.auth().deleteUser(collector.uid);
        console.log(`🔥 Firebase User Deleted: ${collector.uid}`);
      } catch (fbError) {
        console.error("Firebase Delete Warning:", fbError.message);
        // Continue to delete from DB even if Firebase fails (e.g. user not found)
      }
    }

    // 2. Delete from MongoDB
    await Collector.findByIdAndDelete(req.params.id);

    res.json({ success: true, message: "Collector deleted successfully" });

  } catch (error) {
    console.error("Delete collector error:", error);
    res.status(500).json({ success: false, message: "Delete failed" });
  }
};

// TOGGLE STATUS
const toggleCollectorStatus = async (req, res) => {
  try {
    const collector = await Collector.findById(req.params.id);

    if (!collector) {
      return res.status(404).json({ success: false, message: "Collector not found" });
    }

    collector.status = collector.status === "active" ? "blocked" : "active";
    await collector.save();

    res.json({
      success: true,
      message: "Status updated successfully",
      data: collector
    });

  } catch (error) {
    console.error("Toggle error:", error);
    res.status(500).json({ success: false, message: "Status update failed" });
  }
};

//  GET COLLECTOR JOB HISTORY
const getCollectorHistory = async (req, res) => {
  try {
    const history = await PickupRequest.find({
      assignedCollector: req.params.id
    })
      .sort({ requestDate: -1 })
      .select("requestDate wasteType address status image household")
      .populate("household", "name");

    res.json({
      success: true,
      total: history.length,
      data: history
    });

  } catch (error) {
    console.error("Collector history error:", error);
    res.status(500).json({ success: false, message: "History fetch failed" });
  }
};


// GET MY COLLECTOR PROFILE (MOBILE)
const getMyCollectorProfile = async (req, res) => {
  try {
    const uid = req.user?.uid;
    if (!uid) {
      return res.status(401).json({ success: false, message: "Unauthorized" });
    }

    const collector = await Collector.findOne({ uid }).select("-password");
    if (!collector) {
      return res.status(404).json({ success: false, message: "Collector profile not found for this account" });
    }

    return res.json({ success: true, data: collector });
  } catch (error) {
    console.error("Get collector profile error:", error);
    return res.status(500).json({ success: false, message: "Failed to fetch profile" });
  }
};

// UPDATE MY COLLECTOR PROFILE (MOBILE)
const updateMyCollectorProfile = async (req, res) => {
  try {
    const uid = req.user?.uid;
    if (!uid) {
      return res.status(401).json({ success: false, message: "Unauthorized" });
    }

    const allowedUpdates = ["name", "phone", "zone"];
    const payload = {};

    allowedUpdates.forEach((key) => {
      if (typeof req.body[key] === "string") {
        payload[key] = req.body[key].trim();
      }
    });

    if (!Object.keys(payload).length) {
      return res.status(400).json({ success: false, message: "No valid fields provided" });
    }

    const updated = await Collector.findOneAndUpdate(
      { uid },
      payload,
      { new: true, runValidators: true }
    ).select("-password");

    if (!updated) {
      return res.status(404).json({ success: false, message: "Collector profile not found for this account" });
    }

    return res.json({ success: true, message: "Profile updated", data: updated });
  } catch (error) {
    console.error("Update collector profile error:", error);
    return res.status(500).json({ success: false, message: "Failed to update profile" });
  }
};


module.exports = {
  getCollectors,
  addCollector,
  updateCollector,
  deleteCollector,
  toggleCollectorStatus,
  getCollectorHistory,
  getMyCollectorProfile,
  updateMyCollectorProfile
};
