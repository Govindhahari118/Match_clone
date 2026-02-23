export const APP_NAV_SECTIONS = [
  {
    title: "Discover",
    items: [
      { href: "/", label: "Home", short: "HM", blurb: "Overview and activity snapshot" },
      { href: "/matches", label: "Matches", short: "MT", blurb: "Daily compatibility recommendations" },
      { href: "/search", label: "Search", short: "SR", blurb: "Advanced profile filters" },
      { href: "/interests", label: "Interests", short: "IN", blurb: "Sent and received interests" },
      { href: "/shortlists", label: "Shortlists", short: "SL", blurb: "Saved favorites and comparisons" },
      { href: "/who-viewed", label: "Who Viewed", short: "VW", blurb: "Recent profile visitors" },
    ],
  },
  {
    title: "Connect",
    items: [
      { href: "/chat", label: "Messages", short: "CH", blurb: "Conversations with matches" },
      { href: "/notifications", label: "Notifications", short: "NT", blurb: "Realtime alerts and updates" },
      { href: "/success-stories", label: "Success Stories", short: "SS", blurb: "Community wins and inspiration" },
    ],
  },
  {
    title: "Account",
    items: [
      { href: "/profile", label: "Profile", short: "PF", blurb: "Personal and partner preferences" },
      { href: "/settings", label: "Settings", short: "ST", blurb: "Privacy and account controls" },
      { href: "/verification", label: "Verification", short: "VR", blurb: "Trust and identity checks" },
      { href: "/pricing", label: "Pricing", short: "PR", blurb: "Membership and plans" },
      { href: "/help", label: "Help", short: "HP", blurb: "Support and guidance" },
    ],
  },
  {
    title: "Tools",
    items: [
      { href: "/kundli", label: "Kundli", short: "KD", blurb: "Astrology compatibility insights" },
      { href: "/admin", label: "Admin", short: "AD", blurb: "Moderation and platform controls" },
    ],
  },
];

export const APP_NAV_ITEMS = APP_NAV_SECTIONS.flatMap((section) =>
  section.items.map((item) => ({
    ...item,
    section: section.title,
  }))
);

export const MOBILE_PRIMARY_NAV = ["/", "/matches", "/search", "/chat", "/profile"];

export const PUBLIC_SHORTCUTS = [
  { href: "/", label: "Home" },
  { href: "/success-stories", label: "Stories" },
  { href: "/pricing", label: "Pricing" },
  { href: "/help", label: "Help" },
];

export function isRouteActive(pathname, href) {
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(`${href}/`);
}
