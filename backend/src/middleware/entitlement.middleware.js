const subscriptionService = require('../services/subscription.service');

function deny(res, code, message, details = {}) {
  return res.status(403).json({
    error: message,
    code,
    ...details,
  });
}

function requireEntitlement(entitlementKey, options = {}) {
  const {
    message = 'This feature requires a higher subscription tier.',
    attachToRequest = true,
  } = options;

  return async (req, res, next) => {
    try {
      const userId = req.user?.sub;
      if (!userId) {
        return res.status(401).json({ error: 'Unauthorized' });
      }

      const entitlementState = await subscriptionService.getEntitlements(userId);
      const entitled = Boolean(entitlementState?.entitlements?.[entitlementKey]);

      if (!entitled) {
        return deny(res, 'SUBSCRIPTION_REQUIRED', message, {
          requiredEntitlement: entitlementKey,
          plan: entitlementState?.plan || 'free',
        });
      }

      if (attachToRequest) {
        req.entitlements = entitlementState;
      }

      return next();
    } catch (error) {
      console.error('Entitlement middleware error:', error);
      return res.status(500).json({ error: 'Failed to validate subscription entitlement' });
    }
  };
}

function requirePremiumSearchFilters(req, res, next) {
  const premiumFilterKeys = [
    'premiumOnly',
    'verificationLevel',
    'withHoroscopeOnly',
    'onlineNow',
    'lastActiveDays',
  ];

  const usesPremiumFilter = premiumFilterKeys.some((key) => {
    const value = req.query?.[key];
    if (value === undefined || value === null) return false;
    if (typeof value === 'string') return value.trim() !== '';
    return true;
  });

  if (!usesPremiumFilter) {
    return next();
  }

  return requireEntitlement('canUseAdvancedSearch', {
    message: 'Advanced and premium search filters require an active paid plan.',
    attachToRequest: true,
  })(req, res, next);
}

module.exports = {
  requireEntitlement,
  requirePremiumSearchFilters,
};
