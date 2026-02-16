const admin = require("../config/firebase");
const Admin = require("../models/Admin");

// Hardcoded Super Admin Email (Best practice: move to environment variable in real prod)
const SUPER_ADMIN_EMAIL = "jeyalan555@gmail.com";

const superAdminMiddleware = async (req, res, next) => {
    try {
        const { email } = req.user; // req.user populated by authMiddleware

        if (!email) {
            return res.status(401).json({ message: "Unauthorized: No email found in token" });
        }

        const normalizedEmail = email.toLowerCase().trim();
        const normalizedSuperAdmin = SUPER_ADMIN_EMAIL.toLowerCase().trim();

        // 1. Direct Hardcoded Check (for immediate access)
        if (normalizedEmail === normalizedSuperAdmin) {
            return next();
        }

        // 2. Database Role Check
        const adminUser = await Admin.findOne({ email: normalizedEmail });

        if (adminUser && adminUser.role === 'superadmin') {
            return next();
        }

        // If neither, deny access
        return res.status(403).json({ message: "Access Denied: Super Admin privileges required." });

    } catch (error) {
        console.error("Super Admin Auth Error:", error);
        return res.status(500).json({ message: "Internal Server Error during authorization" });
    }
};

module.exports = superAdminMiddleware;
