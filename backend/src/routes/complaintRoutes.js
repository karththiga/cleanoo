const express = require("express");
const router = express.Router();

const {
  getComplaints,
  updateComplaint
} = require("../controllers/complaintController");

// GET ALL
router.get("/", getComplaints);

// UPDATE STATUS
router.put("/:id", updateComplaint);

module.exports = router;
