const mongoose = require('mongoose');
const Pickup = require("./src/models/PickupRequest");
const uri = "mongodb+srv://jeyalan_db_user:hoBK1KjwtHTQC4Qq@cluster0.sklseaw.mongodb.net/WasteManagement";

async function run() {
    try {
        await mongoose.connect(uri);

        // The test request we created had this household ID and waste type
        const householdId = "6953bf0e6d8f8ae3567d7520";

        // Find the most recent pending pickup for this household
        const pickup = await Pickup.findOne({
            household: householdId,
            wasteType: "Plastic",
            address: "Test Generated Pickup" // We used this specific address
        });

        if (pickup) {
            console.log(`Found Test Pickup ID: ${pickup._id}`);
            await Pickup.deleteOne({ _id: pickup._id });
            console.log("Successfully deleted the test pickup request.");
        } else {
            console.log("Test pickup not found (maybe already deleted?).");
        }

    } catch (err) {
        console.error(err);
    } finally {
        await mongoose.disconnect();
    }
}
run();
