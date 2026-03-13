const baseUrl = process.env.NEXT_PUBLIC_SITE_URL || "http://localhost:8000";

const ROUTES = [
  "/",
  "/matches",
  "/shortlists",
  "/interests",
  "/chat",
  "/notifications",
  "/profile",
  "/biodata",
  "/quiz",
  "/pricing",
  "/help",
  "/success-stories",
  "/kundli",
  "/privacy",
  "/terms",
  "/refunds",
  "/community-guidelines",
  "/security",
];

export default function sitemap() {
  const now = new Date();
  return ROUTES.map((route) => ({
    url: `${baseUrl}${route}`,
    lastModified: now,
  }));
}
