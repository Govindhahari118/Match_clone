import { Manrope, Cormorant_Garamond } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";
import GuestBanner from "@/components/GuestBanner";

const manrope = Manrope({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-sans",
});

const cormorant = Cormorant_Garamond({
  subsets: ["latin"],
  display: "swap",
  weight: ["500", "600", "700"],
  variable: "--font-display",
});

export const metadata = {
  title: "MatrimonyConnect - Trusted Global Matrimony Platform",
  description:
    "Find your life partner with verified profiles, compatibility matching, and secure conversations.",
  keywords:
    "matrimony, shaadi, marriage, match, find bride, find groom, matrimonial site India",
  manifest: "/manifest.json",
  openGraph: {
    title: "MatrimonyConnect - Find Your Perfect Partner",
    description:
      "Trusted matrimony platform with verified profiles and compatibility insights.",
    type: "website",
  },
};

export const viewport = {
  themeColor: "#f06b4e",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={`${manrope.variable} ${cormorant.variable}`}>
        <AuthProvider>
          <GuestBanner />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
