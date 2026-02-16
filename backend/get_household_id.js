const mongoose = require('mongoose');
const uri = "mongodb+srv://jeyalan_db_user:hoBK1KjwtHTQC4Qq@cluster0.sklseaw.mongodb.net/WasteManagement";

async function run() {
    try {
        await mongoose.connect(uri);
        // Access collection directly to avoid Schema issues
        const household = await mongoose.connection.db.collection('households').findOne({});
        if (household) {
            console.log("HOUSEHOLD_ID:" + household._id.toString());
        } else {
            console.log("NO_HOUSEHOLD_FOUND");
        }
    } catch (err) {
        console.error(err);
    } finally {
        await mongoose.disconnect();
    }
}
run();
