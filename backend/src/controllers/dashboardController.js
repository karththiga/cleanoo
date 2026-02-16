const Collector = require("../models/Collector");
const Pickup = require("../models/PickupRequest");
const Household = require("../models/Household");
const Complaint = require("../models/Complaint");
const Reward = require("../models/Reward");
const Notification = require("../models/Notification");

const dashboardStats = async (req, res) => {
  try {
    const households = await Household.countDocuments();
    const collectors = await Collector.countDocuments();
    const pickups = await Pickup.countDocuments();

    const pending = await Pickup.countDocuments({ status: "pending" });
    const assigned = await Pickup.countDocuments({ status: "assigned" });
    const completed = await Pickup.countDocuments({ status: "completed" });
    const cancelled = await Pickup.countDocuments({ status: "cancelled" });

    res.json({
      success: true,
      data: {
        households,
        collectors,
        pickups,
        assigned,
        pending,
        completed,
        cancelled,
        wasteBreakdown: await Pickup.aggregate([
          { $group: { _id: "$wasteType", value: { $sum: "$weight" } } },
          { $project: { _id: 0, name: "$_id", value: 1 } }
        ])
      }
    });

  } catch (error) {
    console.error("Dashboard Stats Error:", error);
    res.status(500).json({
      success: false,
      message: "Dashboard fetch failed"
    });
  }
};

const getSidebarCounts = async (req, res) => {
  try {
    const pickupCount = await Pickup.countDocuments({ status: "pending" });
    const complaintCount = await Complaint.countDocuments({ status: "pending" });
    const rewardCount = await Reward.countDocuments({ status: "pending" });
    const notificationCount = await Notification.countDocuments({ type: "admin_alert", isRead: false });

    // Note for Households: We don't have a "status" field for "new", generally just show total or nothing?
    // User asked for "number counts in the sidebar each topics near like whatsapp".
    // I will return these specific "Attention Needed" counts.

    res.json({
      success: true,
      counts: {
        pickups: pickupCount,
        complaints: complaintCount,
        rewards: rewardCount,
        notifications: notificationCount
      }
    });
  } catch (error) {
    console.error("Sidebar Counts Error:", error);
    res.status(500).json({ success: false, counts: {} });
  }
};

module.exports = { dashboardStats, getSidebarCounts };
