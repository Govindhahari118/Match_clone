"use client";

import { useState } from "react";
import api from "../../services/api";

export default function PhotoUpload({ onUploadComplete, disabled = false }) {
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState("");

    const handleFileChange = async (e) => {
        if (disabled) return;
        const file = e.target.files[0];
        if (!file) return;

        // Validate type and size
        if (!file.type.startsWith("image/")) {
            setError("Please select an image file.");
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            setError("File size must be less than 5MB.");
            return;
        }

        setUploading(true);
        setError("");

        const formData = new FormData();
        formData.append("file", file);
        formData.append("is_primary", "false"); // Can be toggled later

        try {
            const response = await api.post("/photos/upload", formData, {
                headers: { "Content-Type": "multipart/form-data" },
            });
            if (onUploadComplete) {
                onUploadComplete(response.data);
            }
        } catch (err) {
            setError(err.response?.data?.error || "Upload failed");
        } finally {
            setUploading(false);
        }
    };

    return (
        <div className="flex flex-col items-center justify-center border-2 border-dashed border-gray-300 rounded-lg p-6 bg-gray-50 hover:bg-gray-100 transition duration-300">
            <input
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                className="hidden"
                id="photo-upload"
                disabled={uploading || disabled}
            />
            <label
                htmlFor="photo-upload"
                className={`flex flex-col items-center ${disabled ? "cursor-not-allowed opacity-60" : "cursor-pointer"}`}
            >
                <svg
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                    strokeWidth={1.5}
                    stroke="currentColor"
                    className="w-10 h-10 text-gray-400 mb-2"
                >
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"
                    />
                </svg>
                <span className="text-gray-600 font-medium">
                    {disabled ? "Login to upload a photo" : (uploading ? "Uploading..." : "Click to Upload Photo")}
                </span>
            </label>
            {error && <p className="text-red-500 text-sm mt-2">{error}</p>}
        </div>
    );
}
