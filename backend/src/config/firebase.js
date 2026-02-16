const admin = require("firebase-admin");
const path = require("path");

// Path to Service Account Key
// Adjust path if file is moved: currently at backend root
const serviceAccountPath = path.join(__dirname, "../../serviceAccountKey.json");

try {
    if (!admin.apps.length) {
        admin.initializeApp({
            credential: admin.credential.cert(require(serviceAccountPath))
        });
        console.log("🔥 Firebase Admin Initialized Successfully");
    }
} catch (error) {
    console.error("❌ Firebase Admin Initialization Failed:", error.message);
}

module.exports = admin;
