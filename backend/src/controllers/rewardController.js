const Reward = require("../models/Reward");
const Household = require("../models/Household");

/* =========================================
   GET ALL REWARDS
========================================= */
const getRewards = async (req, res) => {
  try {
    const rewards = await Reward.find()
      .populate("household", "name points")
      .populate("pickup", "wasteType requestDate weight");

    res.json({ success: true, data: rewards });
  } catch (err) {
    res.status(500).json({ success: false, message: "Failed to load rewards" });
  }
};









const approveReward = async (req, res) => {
  try {
    const reward = await Reward.findById(req.params.id);

    if (!reward)
      return res.status(404).json({ message: "Reward not found" });

    if (reward.status === "approved")
      return res.json({ success: true, message: "Already approved" });

    if (reward.status === "rejected")
      return res.status(400).json({
        success: false,
        message: "Rejected reward cannot be approved"
      });

    await Household.findByIdAndUpdate(
      reward.household,
      { $inc: { points: reward.points } }
    );

    reward.status = "approved";
    reward.approvedBy = "Admin";
    await reward.save();

    res.json({ success: true, data: reward });

  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Approval failed" });
  }
};








/* =========================================
   REJECT REWARD
========================================= */
const rejectReward = async (req, res) => {
  try {
    const reward = await Reward.findById(req.params.id);

    if (!reward)
      return res.status(404).json({ message: "Reward not found" });

    reward.status = "rejected";
    await reward.save();

    res.json({ success: true, data: reward });

  } catch (err) {
    res.status(500).json({ success: false, message: "Rejection failed" });
  }
};

/* =========================================
   REDEEM REWARD (USE POINTS)
========================================= */
const redeemReward = async (req, res) => {
  try {
    const reward = await Reward.findById(req.params.id);

    if (!reward)
      return res.status(404).json({ message: "Reward not found" });

    if (reward.status !== "approved")
      return res.status(400).json({ message: "Only approved rewards can be used" });

    if (reward.isExpired)
      return res.status(400).json({ message: "Reward is expired" });

    if (reward.isRedeemed)
      return res.status(400).json({ message: "Reward already used" });

    // Deduct points
    await Household.findByIdAndUpdate(
      reward.household,
      { $inc: { points: -reward.points } }
    );

    reward.isRedeemed = true;
    await reward.save();

    res.json({ success: true, data: reward, message: "Points redeemed successfully" });

  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: "Redemption failed" });
  }
};

module.exports = {
  getRewards,
  approveReward,
  rejectReward,
  redeemReward
};
