const Notification = require("../models/Notification");
const Household = require("../models/Household");
const Collector = require("../models/Collector");

/* =========================================
   SEND NOTIFICATION (NEW LOGIC)
========================================= */
const sendNotification = async (req, res) => {
  try {
    const { title, message, target, targetValue } = req.body;

    let receivers = [];
    let userType = null;

    // 1️⃣ ALL
    if (target === "all") {
      const households = await Household.find();
      const collectors = await Collector.find();
      receivers = [...households, ...collectors];
    }

    // 2️⃣ ALL HOUSEHOLDS
    if (target === "all_households") {
      receivers = await Household.find();
      userType = "Household";
    }

    // 3️⃣ ALL COLLECTORS
    if (target === "all_collectors") {
      receivers = await Collector.find();
      userType = "Collector";
    }

    // 4️⃣ ZONE (HOUSEHOLDS ONLY)
    if (target === "zone") {
      receivers = await Household.find({ zone: targetValue });
      userType = "Household";
    }

    // 5️⃣ SINGLE HOUSEHOLD
    if (target === "single_household") {
      receivers = await Household.find({ _id: targetValue });
      userType = "Household";
    }

    // 6️⃣ SINGLE COLLECTOR
    if (target === "single_collector") {
      receivers = await Collector.find({ _id: targetValue });
      userType = "Collector";
    }

    // Check if Broadcast
    const isBroadcast = ["all", "all_households", "all_collectors", "zone"].includes(target);

    // SAVE NOTIFICATIONS (Individual Delivery)
    for (let r of receivers) {
      await Notification.create({
        title,
        message,
        target,
        targetValue: targetValue || null,
        userId: r._id,
        userType: userType || r.constructor.modelName,
        isHidden: isBroadcast // Hide from admin log if broadcast
      });
    }

    // CREATE ONE LOG ENTRY FOR BROADCAST
    if (isBroadcast) {
      await Notification.create({
        title,
        message,
        target,
        targetValue: targetValue || null,
        userId: null, // No specific user
        isHidden: false // Show in admin log
      });
    }

    res.json({ success: true, message: "Notification sent successfully" });

  } catch (err) {
    console.error("Notification Error:", err);
    res.status(500).json({ success: false, message: "Send failed" });
  }
};

/* =========================================
   GET NOTIFICATION HISTORY
========================================= */
const getNotifications = async (req, res) => {
  try {
    const list = await Notification.find({ isHidden: { $ne: true } })
      .sort({ createdAt: -1 });

    res.json({ success: true, data: list });

  } catch (err) {
    res.status(500).json({ success: false, message: "Fetch failed" });
  }
};

const markAsRead = async (req, res) => {
  try {
    await Notification.updateMany(
      { type: "admin_alert", isRead: false },
      { isRead: true }
    );
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false });
  }
};

module.exports = {
  sendNotification,
  getNotifications,
  markAsRead // ✅ NEW
};
