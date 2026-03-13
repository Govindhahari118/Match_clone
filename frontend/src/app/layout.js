import { Fraunces, Sora } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";
import { ThemeProvider } from "@/context/ThemeContext";
import GuestBannerGate from "@/components/GuestBannerGate";
import RuntimeClientBootstrap from "@/components/RuntimeClientBootstrap";
import SiteFooter from "@/components/SiteFooter";

const sora = Sora({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-sans",
  weight: ["400", "500", "600", "700"],
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
    "Find your life partner with privacy-first profiles, rule-based matching, and secure conversations.",
  keywords:
    "matrimony, shaadi, marriage, match, find bride, find groom, matrimonial site India",
  manifest: "/manifest.json",
  openGraph: {
    title: "MatrimonyConnect - Find Your Perfect Partner",
    description:
      "Trusted matrimony platform with privacy-first profiles and transparent discovery.",
    type: "website",
  },
};

export const viewport = {
  themeColor: "#0F766E",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={`${sora.variable} ${fraunces.variable}`}>
        <ThemeProvider>
          <AuthProvider>
            <RuntimeClientBootstrap />
            <GuestBannerGate />
            {children}
            <SiteFooter />
          </AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}

