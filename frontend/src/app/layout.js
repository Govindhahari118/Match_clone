import { Plus_Jakarta_Sans, Fraunces } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";
import { ThemeProvider } from "@/context/ThemeContext";
import GuestBannerGate from "@/components/GuestBannerGate";

const plusJakarta = Plus_Jakarta_Sans({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-sans",
});

const fraunces = Fraunces({
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
  themeColor: "#1d4ed8",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={`${plusJakarta.variable} ${fraunces.variable}`}>
        <ThemeProvider>
          <AuthProvider>
            <GuestBannerGate />
            {children}
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
