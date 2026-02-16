const mongoose = require('mongoose');
const Pickup = require("./src/models/PickupRequest");
const Collector = require("./src/models/Collector");
const Household = require("./src/models/Household");
const uri = "mongodb+srv://jeyalan_db_user:hoBK1KjwtHTQC4Qq@cluster0.sklseaw.mongodb.net/WasteManagement";

async function run() {
    try {
        await mongoose.connect(uri);

        const householdId = "6953bf0e6d8f8ae3567d7520";
        const household = await Household.findById(householdId);

        if (!household) {
            console.log("Household not found");
            return;
        }

        console.log(`Household Zone: ${household.zone || 'None'}`);

        if (household.zone) {
            const collectors = await Collector.find({ zone: household.zone, status: "active" });
            console.log(`Active Collectors in Zone: ${collectors.length}`);

            for (const col of collectors) {
                const activeTasks = await Pickup.countDocuments({
                    assignedCollector: col._id,
                    status: { $in: ["assigned", "approved"] }
                });
                console.log(`- Collector ${col.name}: ${activeTasks} active tasks`);
            }
        }

        // Check the latest pickup for this household
        const latestPickup = await Pickup.findOne({ household: householdId }).sort({ requestDate: -1 });
        console.log("Latest Pickup Status:", latestPickup?.status);
        console.log("Assigned Collector:", latestPickup?.assignedCollector);

    } catch (err) {
        console.error(err);
    } finally {
        await mongoose.disconnect();
    }
}
run();
