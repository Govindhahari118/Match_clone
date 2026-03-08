import { Manrope, Cormorant_Garamond } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";
import { ThemeProvider } from "@/context/ThemeContext";
import GuestBannerGate from "@/components/GuestBannerGate";
import RuntimeClientBootstrap from "@/components/RuntimeClientBootstrap";

const manrope = Manrope({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-sans",
});

const cormorantGaramond = Cormorant_Garamond({
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
  themeColor: "#1d8eb3",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={`${manrope.variable} ${cormorantGaramond.variable}`}>
        <ThemeProvider>
          <AuthProvider>
            <RuntimeClientBootstrap />
            <GuestBannerGate />
            {children}
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}

