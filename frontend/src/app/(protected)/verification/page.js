"use client";

import IdentityVerification from "../../../components/IdentityVerification";

export default function VerificationPage() {
    return (
        <div className="max-w-2xl mx-auto py-8 px-4">
            <h1 className="text-2xl font-bold mb-6">Trust & Safety Center</h1>
            <p className="mb-6 text-gray-600">
                Verify your identity to unlock premium features and increase your trust score.
                Your document is securely stored and only used for verification.
            </p>

            <IdentityVerification />

            <div className="mt-8 bg-blue-50 p-4 rounded-md text-sm text-blue-800">
                <h3 className="font-semibold mb-2">Why Verify?</h3>
                <ul className="list-disc list-inside space-y-1">
                    <li>Get the &quot;Verified&quot; Blue Tick badge</li>
                    <li>Appear higher in search results</li>
                    <li>Unlock unlimited connection requests</li>
                    <li>Build trust with potential matches</li>
                </ul>
            </div>
        </div>
    );
}
