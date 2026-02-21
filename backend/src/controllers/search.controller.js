const searchService = require('../services/search.service');

const searchController = {
    async executeSearch(req, res) {
        try {
            const userId = req.user.sub;
            const result = await searchService.executeSearch(userId, req.query);
            res.status(200).json(result);
        } catch (error) {
            console.error('Execute search error:', error);
            res.status(500).json({ error: 'Failed to execute search' });
        }
    },

    async createSavedSearch(req, res) {
        try {
            const userId = req.user.sub;
            const created = await searchService.createSavedSearch(userId, req.body);
            res.status(201).json(created);
        } catch (error) {
            console.error('Create saved search error:', error);
            res.status(500).json({ error: 'Failed to save search' });
        }
    },

    async listSavedSearches(req, res) {
        try {
            const userId = req.user.sub;
            const list = await searchService.listSavedSearches(userId);
            res.status(200).json(list);
        } catch (error) {
            console.error('List saved searches error:', error);
            res.status(500).json({ error: 'Failed to list saved searches' });
        }
    },

    async updateSavedSearch(req, res) {
        try {
            const userId = req.user.sub;
            const { id } = req.params;
            const updated = await searchService.updateSavedSearch(userId, id, req.body);
            if (!updated) {
                return res.status(404).json({ error: 'Saved search not found' });
            }
            res.status(200).json(updated);
        } catch (error) {
            console.error('Update saved search error:', error);
            res.status(500).json({ error: 'Failed to update saved search' });
        }
    },

    async deleteSavedSearch(req, res) {
        try {
            const userId = req.user.sub;
            const { id } = req.params;
            const deleted = await searchService.deleteSavedSearch(userId, id);
            if (!deleted) {
                return res.status(404).json({ error: 'Saved search not found' });
            }
            res.status(200).json({ success: true });
        } catch (error) {
            console.error('Delete saved search error:', error);
            res.status(500).json({ error: 'Failed to delete saved search' });
        }
    },
};

module.exports = searchController;
