const admin = require('firebase-admin');

// Initialize Firebase Admin SDK
// Ideally, use a service account key file for production
// For now, we'll try to use default application credentials or a mock
if (!admin.apps.length) {
    try {
        admin.initializeApp({
            credential: admin.credential.applicationDefault(),
            // Or use: admin.credential.cert(serviceAccount)
        });
        console.log('Firebase Admin Initialized');
    } catch (error) {
        console.error('Firebase Admin Init Error (Expected if no creds provided):', error.message);
    }
}

module.exports = admin;
