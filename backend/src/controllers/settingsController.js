const Setting = require("../models/Setting");
const admin = require("../config/firebase");


//  GET SETTINGS
const getSettings = async (req, res) => {
  let settings = await Setting.findOne();

  if (!settings) {
    settings = await Setting.create({
      categories: [],
      rewards: new Map(),
      partners: []
    });
  }

  // Use Authenticated User's Profile (Fetch Fresh Data)
  let user = req.user || {};
  try {
    if (user.uid) {
      const userRecord = await admin.auth().getUser(user.uid);
      user = {
        ...user,
        name: userRecord.displayName,
        email: userRecord.email,
        picture: userRecord.photoURL
      };
    }
  } catch (e) {
    console.error("Error fetching fresh user data:", e);
  }

  // Clean up image URL if it's a local upload
  let image = user.picture;

  res.json({
    categories: settings.categories || [],
    rewards: Object.fromEntries(settings.rewards || []),
    partners: settings.partners || [],
    profile: {
      name: user.name || user.email?.split('@')[0] || "Admin",
      email: user.email,
      image: image // Firebase 'picture' field
    }
  });
};


//  ADD CATEGORY + CREATE REWARD FIELD
const addCategory = async (req, res) => {
  try {
    const { name } = req.body;

    const settings = await Setting.findOne();

    // Case-insensitive duplicate check
    const exists = settings.categories
      .map(c => c.toLowerCase())
      .includes(name.toLowerCase());

    if (exists) {
      return res.status(400).json({ message: "Already exists" });
    }

    //  ADD CATEGORY
    settings.categories.push(name);

    //  ADD DEFAULT REWARD (MAP SAFE)
    if (!settings.rewards.has(name)) {
      settings.rewards.set(name, 0);
    }

    await settings.save();

    res.json({
      success: true,
      categories: settings.categories,
      rewards: Object.fromEntries(settings.rewards)
    });

  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false });
  }
};


//  DELETE CATEGORY + DELETE REWARD
const deleteCategory = async (req, res) => {
  try {
    const { name } = req.params;

    const settings = await Setting.findOne();

    settings.categories = settings.categories.filter(c => c !== name);

    //  DELETE FROM MAP
    settings.rewards.delete(name);

    await settings.save();

    res.json({ success: true });

  } catch {
    res.status(500).json({ success: false });
  }
};


//  UPDATE REWARD VALUE
const updateReward = async (req, res) => {
  const { type, val } = req.body;

  const settings = await Setting.findOne();

  settings.rewards.set(type, Number(val));

  await settings.save();

  res.json({ success: true });
};


//  UPDATE PROFILE
const updateProfile = async (req, res) => {
  const { name, password } = req.body;

  try {
    console.log("updateProfile called. req.user:", req.user); // Debug log
    if (!req.user || !req.user.uid) {
      return res.status(401).json({ success: false, message: "Unauthorized: User not identified" });
    }
    const uid = req.user.uid;

    const updateData = {};

    if (name) updateData.displayName = name;

    // Password Update (Admin SDK overrides without needing old password)
    if (password) {
      console.log("Updating password to:", password); // DEBUG
      if (password.length < 6) {
        return res.status(400).json({ success: false, message: "Password must be at least 6 characters" });
      }
      updateData.password = password;
    }

    // Image Update
    if (req.file) {
      // Create a public URL for the uploaded file
      const imageUrl = `http://localhost:5000/uploads/${req.file.filename}`;
      updateData.photoURL = imageUrl;
    }

    // Update Firebase Auth User
    console.log("Attempting to update user in Firebase Admin SDK:", uid, updateData); // DEBUG
    await admin.auth().updateUser(uid, updateData);
    console.log("Firebase Admin update success."); // DEBUG

    // Get updated user record
    const user = await admin.auth().getUser(uid);
    console.log("Fetched fresh user record:", user.email); // DEBUG

    res.json({
      success: true,
      profile: {
        name: user.displayName,
        image: user.photoURL
      },
      message: "Profile updated successfully"
    });

  } catch (error) {
    console.error("Error updating profile in controller:", error);
    // Standardize error message
    let msg = error.message;
    if (error.code === 'auth/weak-password') msg = "Password is too weak.";
    if (error.code === 'auth/requires-recent-login') msg = "Please re-login and try again.";

    res.status(500).json({ success: false, message: msg });
  }
};


//  ADD PARTNER
const addPartner = async (req, res) => {
  try {
    const { name, type } = req.body;
    const settings = await Setting.findOne();
    settings.partners.push({ name, type });
    await settings.save();
    res.json({ success: true, partners: settings.partners });
  } catch {
    res.status(500).json({ success: false });
  }
};

//  DELETE PARTNER
const deletePartner = async (req, res) => {
  try {
    const { id } = req.params;
    const settings = await Setting.findOne();
    settings.partners = settings.partners.filter(p => p._id.toString() !== id);
    await settings.save();
    res.json({ success: true, partners: settings.partners });
  } catch {
    res.status(500).json({ success: false });
  }
};

module.exports = {
  getSettings,
  addCategory,
  deleteCategory,
  updateReward,
  updateProfile,
  addPartner,
  deletePartner
};
