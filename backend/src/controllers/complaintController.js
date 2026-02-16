const Complaint = require("../models/Complaint");
const Household = require("../models/Household");
const Collector = require("../models/Collector");
const Notification = require("../models/Notification");

// GET ALL COMPLAINTS (ADMIN)
const getComplaints = async (req, res) => {
  const complaints = await Complaint.find().sort({ createdAt: -1 });

  const result = [];

  for (let c of complaints) {
    let userData = null;

    if (c.userType === "household") {
      userData = await Household.findById(c.user).select("name email");
    }

    if (c.userType === "collector") {
      userData = await Collector.findById(c.user).select("name email");
    }

    result.push({
      _id: c._id,
      userType: c.userType,
      user: userData,
      message: c.message,
      image: c.image,  // Added Image
      status: c.status,
      adminNote: c.adminNote,
      createdAt: c.createdAt
    });
  }

  res.json({ success: true, data: result });
};

// UPDATE STATUS
const updateComplaint = async (req, res) => {
  const { status, adminNote } = req.body;
  const complaintId = req.params.id;

  const updated = await Complaint.findByIdAndUpdate(
    complaintId,
    { status, adminNote },
    { new: true }
  );

  // NOTIFICATION LOGIC
  const targetUser = updated.userType === 'collector' ? 'single_collector' : 'admin'; // Hack for notification target enum

  if (status === "resolved") {
    if (updated) {
      await Notification.create({
        title: "Complaint Resolved",
        message: `Your complaint has been resolved.`,
        userId: updated.user,
        userType: updated.userType === 'household' ? 'Household' : 'Collector',
        target: targetUser,
        type: "alert",
        isRead: false
      });
    }
  } else if (adminNote) {
    // Acknowledgment / Note Notification
    if (updated) {
      await Notification.create({
        title: "New Admin Message",
        message: adminNote,
        userId: updated.user,
        userType: updated.userType === 'household' ? 'Household' : 'Collector',
        target: targetUser,
        type: "info",
        isRead: false
      });
    }
  }

  res.json({ success: true, data: updated });
};

module.exports = {
  getComplaints,
  updateComplaint
};
