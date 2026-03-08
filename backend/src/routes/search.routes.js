const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/auth.middleware');
const searchController = require('../controllers/search.controller');

router.use(authMiddleware);

router.get('/', searchController.executeSearch);
router.get('/execute', searchController.executeSearch);
router.get('/rails', searchController.getDiscoveryRails);
router.get('/suggestions', searchController.getSuggestions);
router.get('/compare', searchController.compareProfiles);
router.get('/saved', searchController.listSavedSearches);
router.post('/saved', searchController.createSavedSearch);
router.patch('/saved/:id', searchController.updateSavedSearch);
router.delete('/saved/:id', searchController.deleteSavedSearch);

module.exports = router;
