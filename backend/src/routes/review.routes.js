const express = require("express");
const router = express.Router();
const { authenticate } = require("../middleware/auth.middleware");
const { createReview, getReviews, deleteReview } = require("../controllers/review.controller");

router.post("/", authenticate, createReview);
router.get("/user/:userId", getReviews);
router.delete("/:id", authenticate, deleteReview);

module.exports = router;
