const mongoose = require("mongoose");

const connectDB = async () => {
  const tryConnect = async () => {
    try {
      console.log("→ Trying MongoDB connection...");

      await mongoose.connect(process.env.MONGO_URI);

      console.log("MongoDB Connected");
    } catch (error) {
      console.error("MongoDB Connection Error:", error.message);
      console.log("Retrying in 5 seconds...");
      setTimeout(tryConnect, 5000);
    }
  };

  await tryConnect();
};

module.exports = connectDB;
