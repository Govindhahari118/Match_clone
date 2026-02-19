import { Inter } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";

const inter = Inter({ subsets: ["latin"], display: "swap" });

import GuestBanner from "@/components/GuestBanner";

export const metadata = {
  title: "MatrimonyConnect — India's #1 Trusted Matrimony Site",
  description: "Find your perfect life partner on MatrimonyConnect. 5M+ verified profiles. AI-powered matches. Free registration.",
  keywords: "matrimony, shaadi, marriage, match, find bride, find groom, matrimonial site India",
  manifest: "/manifest.json",
  openGraph: {
    title: "MatrimonyConnect — Find Your Perfect Partner",
    description: "India's most trusted matrimony platform. 5M+ members. AI-powered matches.",
    type: "website",
  },
};

export const viewport = {
  themeColor: "#e11d48",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <AuthProvider>
          <GuestBanner />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
