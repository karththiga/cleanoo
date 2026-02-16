const express = require("express");
const router = express.Router();

const dashboardController = require("../controllers/dashboardController");
// DASHBOARD STATS
router.get("/dashboard/stats", dashboardController.dashboardStats);
router.get("/sidebar/stats", dashboardController.getSidebarCounts);

// AUTH & SUPER ADMIN MIDDLEWARE
const authMiddleware = require("../middleware/authMiddleware");
const superAdminMiddleware = require("../middleware/superAdminMiddleware");

const adminController = require("../controllers/adminController");

// Protect these routes
router.post("/create", authMiddleware, superAdminMiddleware, adminController.createAdmin);
router.get("/list", authMiddleware, superAdminMiddleware, adminController.getAllAdmins);
router.delete("/delete/:id", authMiddleware, superAdminMiddleware, adminController.deleteAdmin);

router.post("/check-email", adminController.checkAdminEmail);

module.exports = router;
