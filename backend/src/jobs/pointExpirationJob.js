const cron = require("node-cron");
const Reward = require("../models/Reward");
const Household = require("../models/Household");

// Schedule: Run every day at midnight (00:00)
const expirationJob = cron.schedule("0 0 * * *", async () => {
    console.log("⏰ Running Point Expiration Job...");

    try {
        // 1. Calculate cutoff date (6 months ago)
        const cutoffDate = new Date();
        cutoffDate.setMonth(cutoffDate.getMonth() - 6);

        // 2. Find eligible rewards (Approved, Not Expired, Not Used)
        const expiredRewards = await Reward.find({
            status: "approved",
            isExpired: false,
            isRedeemed: false, // 🚫 Don't expire if already used
            createdAt: { $lt: cutoffDate }
        });

        if (expiredRewards.length === 0) {
            console.log("✅ No rewards to expire.");
            return;
        }

        console.log(`⚠️ Found ${expiredRewards.length} rewards to expire.`);

        // 3. Process each reward
        for (const reward of expiredRewards) {
            // Deduct points from household
            await Household.findByIdAndUpdate(reward.household, {
                $inc: { points: -reward.points }
            });

            // Mark reward as expired
            reward.isExpired = true;
            await reward.save();

            console.log(`🔻 Expired ${reward.points} pts for Household ${reward.household}`);
        }

        console.log("✅ Point Expiration Job Completed.");

    } catch (error) {
        console.error("❌ Point Expiration Job Failed:", error);
    }
}, {
    scheduled: false // Don't start immediately, wait for manual start in server.js
});

module.exports = expirationJob;
