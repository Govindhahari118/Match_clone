const metaService = require("../services/meta.service");

const metaController = {
    getFilters(req, res) {
        try {
            res.status(200).json({
                success: true,
                data: metaService.getFilterMeta(),
            });
        } catch (error) {
            console.error("Get filter meta error:", error);
            res.status(500).json({ success: false, error: "Failed to fetch filter metadata" });
        }
    },

    getLocations(req, res) {
        try {
            res.status(200).json({
                success: true,
                data: metaService.getLocationMeta(),
            });
        } catch (error) {
            console.error("Get location meta error:", error);
            res.status(500).json({ success: false, error: "Failed to fetch location metadata" });
        }
    },

    getCommunities(req, res) {
        try {
            res.status(200).json({
                success: true,
                data: metaService.getCommunityMeta(),
            });
        } catch (error) {
            console.error("Get community meta error:", error);
            res.status(500).json({ success: false, error: "Failed to fetch community metadata" });
        }
    },
};

module.exports = metaController;
