const express = require("express");
const router = express.Router();
const metaController = require("../controllers/meta.controller");

router.get("/filters", metaController.getFilters);
router.get("/locations", metaController.getLocations);
router.get("/communities", metaController.getCommunities);

module.exports = router;
