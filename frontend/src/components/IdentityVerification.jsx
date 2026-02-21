"use client";

import { useState, useEffect } from 'react';
import api from '../services/api';

export default function IdentityVerification() {
    const [file, setFile] = useState(null);
    const [status, setStatus] = useState('loading');
    const [rejectionReason, setRejectionReason] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [badges, setBadges] = useState(null);

    useEffect(() => {
        fetchStatus();
    }, []);

    const fetchStatus = async () => {
        try {
            const res = await api.get('/verification/status');
            if (res.data.success) {
                setStatus(res.data.status);
                setRejectionReason(res.data.rejectionReason);
            }
            try {
                const badgeRes = await api.get('/verification/badges');
                setBadges(badgeRes?.data?.badges || null);
            } catch {
                setBadges(null);
            }
        } catch (err) {
            console.error(err);
            // If 404 or verify route fails, assume not started
            setStatus('not_started');
        }
    };

    const handleFileChange = (e) => {
        if (e.target.files[0]) {
            setFile(e.target.files[0]);
        }
    };

    const handleUpload = async () => {
        if (!file) return;
        setUploading(true);
        try {
            // 1. Get Presigned URL
            // Note: Backend endpoint /api/media/presigned-url must be active
            const { data } = await api.post('/media/presigned-url', {
                fileName: file.name,
                fileType: file.type,
                resourceType: 'identity'
            });

            const { uploadUrl, publicUrl } = data;

            // 2. Upload to S3 directly
            const uploadRes = await fetch(uploadUrl, {
                method: 'PUT',
                body: file,
                headers: {
                    'Content-Type': file.type
                }
            });

            if (!uploadRes.ok) {
                throw new Error('Failed to upload to S3');
            }

            // 3. Submit to Backend
            await api.post('/verification/submit-id', { docUrl: publicUrl });

            setStatus('pending');
            setFile(null);
            alert('Document submitted successfully! We will review it shortly.');
        } catch (err) {
            console.error("Verification Upload Error:", err);
            alert('Upload failed. Please try again.');
        } finally {
            setUploading(false);
        }
    };

    if (status === 'loading') {
        return <div className="p-4 text-center">Loading verification status...</div>;
    }

    return (
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
            <h2 className="text-xl font-semibold mb-4 flex items-center">
                <span className="mr-2">🛡️</span> Identity Verification
            </h2>

            {badges && (
                <div className="grid grid-cols-2 gap-2 mb-4 text-xs">
                    <div className={`p-2 rounded border ${badges.phoneVerified ? 'bg-green-50 border-green-200 text-green-700' : 'bg-gray-50 border-gray-200 text-gray-600'}`}>
                        Phone {badges.phoneVerified ? 'Verified' : 'Pending'}
                    </div>
                    <div className={`p-2 rounded border ${badges.emailVerified ? 'bg-green-50 border-green-200 text-green-700' : 'bg-gray-50 border-gray-200 text-gray-600'}`}>
                        Email {badges.emailVerified ? 'Verified' : 'Pending'}
                    </div>
                    <div className={`p-2 rounded border ${badges.idVerified ? 'bg-green-50 border-green-200 text-green-700' : 'bg-gray-50 border-gray-200 text-gray-600'}`}>
                        ID {badges.idVerified ? 'Verified' : 'Pending'}
                    </div>
                    <div className={`p-2 rounded border ${badges.blueTick ? 'bg-blue-50 border-blue-200 text-blue-700' : 'bg-gray-50 border-gray-200 text-gray-600'}`}>
                        Blue Tick {badges.blueTick ? 'Active' : 'Locked'}
                    </div>
                </div>
            )}

            {status === 'verified' && (
                <div className="bg-green-50 border border-green-200 text-green-700 p-4 rounded-md flex items-center">
                    <svg className="w-6 h-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path></svg>
                    <div>
                        <p className="font-bold">Verified</p>
                        <p className="text-sm">Your identity has been verified. You have the "Verified Badge" on your profile.</p>
                    </div>
                </div>
            )}

            {status === 'pending' && (
                <div className="bg-yellow-50 border border-yellow-200 text-yellow-700 p-4 rounded-md">
                    <p className="font-bold">Under Review</p>
                    <p className="text-sm">We are currently reviewing your document. This usually takes 24 hours.</p>
                </div>
            )}

            {(status === 'not_started' || status === 'rejected') && (
                <div>
                    {status === 'rejected' && (
                        <div className="bg-red-50 border border-red-200 text-red-700 p-3 rounded-md mb-4">
                            <p className="font-bold">Verification Rejected</p>
                            <p className="text-sm">{rejectionReason || "Common reasons: Blurry image, mismatched name."}</p>
                        </div>
                    )}

                    <p className="text-gray-600 mb-4">
                        Upload a clear photo of your Government ID (Aadhaar, PAN, or Passport) to get verified and boost your matches by 3x.
                    </p>

                    <div className="space-y-4">
                        <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:bg-gray-50 transition-colors">
                            <input
                                type="file"
                                id="id-doc"
                                className="hidden"
                                accept="image/*,.pdf"
                                onChange={handleFileChange}
                            />
                            <label htmlFor="id-doc" className="cursor-pointer block">
                                {file ? (
                                    <div className="text-blue-600 font-medium">
                                        📄 {file.name}
                                    </div>
                                ) : (
                                    <div className="text-gray-500">
                                        <span className="text-2xl block mb-2">📁</span>
                                        <span>Click to upload secure document</span>
                                    </div>
                                )}
                            </label>
                        </div>

                        {file && (
                            <button
                                onClick={handleUpload}
                                disabled={uploading}
                                className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:bg-blue-300 transition-colors"
                            >
                                {uploading ? 'Uploading Securely...' : 'Submit Verification'}
                            </button>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
