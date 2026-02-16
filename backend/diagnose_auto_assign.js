const mongoose = require('mongoose');
const Collector = require("./src/models/Collector");
const Pickup = require("./src/models/PickupRequest");
const uri = "mongodb+srv://jeyalan_db_user:hoBK1KjwtHTQC4Qq@cluster0.sklseaw.mongodb.net/WasteManagement";

async function run() {
    try {
        await mongoose.connect(uri);
        console.log("Connected to DB.");

        // 1. List all active collectors and their zones
        const collectors = await Collector.find({ status: 'active' });
        console.log(`\nFound ${collectors.length} active collectors:`);

        for (const col of collectors) {
            // 2. Count active tasks for each
            const activeTasks = await Pickup.countDocuments({
                assignedCollector: col._id,
                status: { $in: ["assigned", "approved"] }
            });

            console.log(`- ${col.name} (Zone: ${col.zone}): ${activeTasks} active tasks.`);

            if (activeTasks > 0) {
                console.log(`  -> WOULD SKIP (Requires 0 tasks)`);
            } else {
                console.log(`  -> ELIGIBLE for Auto-Assign`);
            }
        }

    } catch (err) {
        console.error(err);
    } finally {
        await mongoose.disconnect();
    }
}
run();
