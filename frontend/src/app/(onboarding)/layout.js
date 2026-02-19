"use client";

import { usePathname } from "next/navigation";

export default function OnboardingLayout({ children }) {
    const pathname = usePathname();
    const currentStep = parseInt(pathname.split("/").pop().replace("step-", "")) || 1;
    const totalSteps = 5;

    return (
        <div className="flex min-h-screen flex-col items-center bg-gray-50 pt-10">
            <div className="w-full max-w-2xl bg-white p-8 shadow-md rounded-lg">
                {/* Progress Bar */}
                <div className="mb-8">
                    <div className="relative h-2 w-full bg-gray-200 rounded-full">
                        <div
                            className="absolute top-0 left-0 h-2 bg-blue-600 rounded-full transition-all duration-300"
                            style={{ width: `${(currentStep / totalSteps) * 100}%` }}
                        ></div>
                    </div>
                    <p className="text-right text-sm text-gray-500 mt-2">Step {currentStep} of {totalSteps}</p>
                </div>

                {children}
            </div>
        </div>
    );
}
