const express = require("express");
const router = express.Router();

const {
  sendNotification,
  getNotifications,
  markAsRead
} = require("../controllers/notificationController");


// SEND NOTIFICATION
router.post("/send", sendNotification);

// VIEW HISTORY
router.get("/", getNotifications);

// MARK AS READ
router.put("/mark-read", markAsRead); 

module.exports = router;
