const express = require("express");
const cors = require("cors");
const dotenv = require("dotenv");
const connectDB = require("./src/config/db");
const adminRoutes = require("./src/routes/adminRoutes");
const expirationJob = require("./src/jobs/pointExpirationJob"); // Cron Job

dotenv.config();

const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// DB
connectDB();

// Health Check
app.get("/", (req, res) => {
  res.send("Waste Management Admin Backend Running");
});


//  COLLECTORS
app.use("/api/collectors", require("./src/routes/collectorRoutes"));
app.use("/api/admin/collectors", require("./src/routes/collectorRoutes"));


//  PICKUPS  (FIXED PATH)
app.use("/api/pickups", require("./src/routes/pickupRoutes"));
app.use("/api/admin/pickups", require("./src/routes/pickupRoutes"));


//  COMPLAINTS
app.use("/api/admin/complaints", require("./src/routes/complaintRoutes"));


//  ADMIN AUTH
app.use("/api/admin", adminRoutes);


//  HOUSEHOLDS
app.use("/api/admin/households", require("./src/routes/householdRoutes"));

//  MOBILE HOUSEHOLDS (FIREBASE UID BASED)
app.use("/api/households", require("./src/routes/mobileHouseholdRoutes"));


//  NOTIFICATIONS
app.use("/api/admin/notifications", require("./src/routes/notificationRoutes"));


//  REWARDS
app.use("/api/admin/rewards", require("./src/routes/rewardRoutes"));


//  SETTINGS
app.use("/api/admin/settings", require("./src/routes/settingsRoutes"));


//  IMAGE SERVING
app.use("/uploads", express.static("uploads"));


//  REMOVE THIS LINE (WRONG PATH)
// app.use("/api/pickuprequests", require("./routes/pickupRoutes"));


// ERROR HANDLER
app.use((err, req, res, next) => {
  console.error("GLOBAL ERROR:", err);
  res.status(500).json({ success: false, message: "Internal Server Error" });
});

// START
// Start Cron Job
expirationJob.start();

const PORT = process.env.PORT || 7777;
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
