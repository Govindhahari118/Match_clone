"use client";

import Image from "next/image";
import dynamic from "next/dynamic";
import Link from "next/link";
import { memo, Suspense, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { toast } from "react-toastify";
import api from "../../../services/api";
import { useAuth } from "../../../context/AuthContext";
import { useGuestScrollGate } from "../../../hooks/useGuestScrollGate";
import { getCachedRequest } from "../../../services/requestCache";
import PageEmptyState from "../../../components/states/PageEmptyState";
import PageLoadingState from "../../../components/states/PageLoadingState";
import PageHero from "../../../components/PageHero";

const LoginPromptModal = dynamic(() => import("../../../components/LoginPromptModal"), {
  ssr: false,
});

const FALLBACK_FILTER_META = {
  religion: ["Any", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist"],
  caste: ["Any", "Brahmin", "Kshatriya", "Vaisya", "Kayastha", "Rajput", "Reddy", "Naidu", "Chettiar", "Jat", "Maratha", "Vanniyar", "Yadav", "Ezhava", "Nair", "SC", "ST", "OBC", "General"],
  income: ["Any", "Below 3L", "3-5L", "5-10L", "10-25L", "25-50L", "50L+"],
  maritalStatus: ["Any", "Never Married", "Divorced", "Widowed", "Awaiting Divorce", "Separated"],
  motherTongue: ["Any", "Hindi", "English", "Marathi", "Tamil", "Telugu", "Kannada", "Malayalam", "Bengali", "Gujarati", "Punjabi", "Urdu"],
  education: ["Any", "MBBS", "MD", "B.Tech", "M.Tech", "MBA", "CA", "B.Arch", "B.Des", "LLB", "PhD", "IAS", "IPS", "B.Com", "M.Com", "B.Sc", "M.Sc", "Other"],
  profession: ["Any", "Doctor", "Engineer", "Software Engineer", "Architect", "Designer", "Lawyer", "Teacher", "Professor", "Govt. Officer", "Business", "Student", "Self Employed", "Other"],
  residentialStatus: ["Any", "Citizen", "Permanent Resident", "Work Permit", "Student Visa", "Other"],
  children: ["Any", "No", "Yes, living with me", "Yes, not living with me"],
};

const FALLBACK_LOCATION_META = {
  countries: [
    {
      code: "IN",
      name: "India",
      states: [
        {
          name: "Maharashtra",
          cities: ["Mumbai", "Pune", "Nagpur", "Nashik"],
          districts: ["Mumbai City", "Mumbai Suburban", "Pune", "Nagpur", "Nashik"],
        },
        {
          name: "Karnataka",
          cities: ["Bangalore", "Mysore", "Mangalore", "Hubli"],
          districts: ["Bangalore Urban", "Bangalore Rural", "Mysore", "Dakshina Kannada"],
        },
        {
          name: "Tamil Nadu",
          cities: ["Chennai", "Coimbatore", "Madurai", "Trichy"],
          districts: ["Chennai", "Coimbatore", "Madurai", "Tiruchirappalli"],
        },
        {
          name: "Telangana",
          cities: ["Hyderabad", "Warangal", "Nizamabad", "Karimnagar"],
          districts: ["Hyderabad", "Warangal", "Nizamabad", "Karimnagar"],
        },
        {
          name: "Delhi",
          cities: ["New Delhi", "Delhi"],
          districts: ["Central Delhi", "South Delhi", "North Delhi", "West Delhi", "East Delhi"],
        },
      ],
    },
  ],
};

const HAS_CHILDREN_OPTIONS = [
  { value: "Any", label: "Any" },
  { value: "no", label: "No" },
  { value: "yes_living_with_me", label: "Yes, living with me" },
  { value: "yes_not_living_with_me", label: "Yes, not living with me" },
  { value: "unknown", label: "Prefer not to say" },
];

const RESIDENTIAL_STATUS_OPTIONS = [
  { value: "Any", label: "Any" },
  { value: "citizen", label: "Citizen" },
  { value: "permanent_resident", label: "Permanent resident" },
  { value: "work_permit", label: "Work permit" },
  { value: "student_visa", label: "Student visa" },
  { value: "not_specified", label: "Not specified" },
  { value: "other", label: "Other" },
];

const SUBCASTE_MAP = {
  Brahmin: ["Iyer", "Iyengar", "Gaur", "Saraswat", "Maithil", "Kanyakubja", "Nambootiri", "Deshastha", "Smartha"],
  Kshatriya: ["Rajput", "Maratha", "Thakur", "Raghuvanshi"],
  Vaisya: ["Gupta", "Agarwal", "Maheshwari", "Khandelwal", "Jain (Vania)"],
  Kayastha: ["Saxena", "Srivastava", "Mathur", "Nigam", "Bose", "Ghosh", "Dutta"],
  Rajput: ["Chauhan", "Rathore", "Sisodia", "Parmar", "Solanki"],
  Reddy: ["Pokanati", "Motati", "Gudati", "Pakanati", "Chowdary"],
  Naidu: ["Kamma", "Kapu", "Balija", "Gavara", "Velama"],
  Chettiar: ["Nattukottai", "Vaniya", "24 Manai"],
  Jat: ["Sidhu", "Gill", "Mann", "Sandhu", "Grewal", "Dahiya"],
  Maratha: ["96 Kuli", "Deshmukh", "Patil", "Pawar"],
  Vanniyar: ["Padayachi", "Gounder"],
  Yadav: ["Ahir", "Gwal", "Krishnauth"],
  Ezhava: ["Thiyya"],
  Nair: ["Menon", "Pillai", "Kurup", "Nambiar"],
  Any: [],
};

const NAKSHATRAS = [
  "Any",
  "Ashwini",
  "Bharani",
  "Krittika",
  "Rohini",
  "Mrigashira",
  "Ardra",
  "Punarvasu",
  "Pushya",
  "Ashlesha",
  "Magha",
  "Purva Phalguni",
  "Uttara Phalguni",
  "Hasta",
  "Chitra",
  "Swati",
  "Vishakha",
  "Anuradha",
  "Jyeshtha",
  "Mula",
  "Purva Ashadha",
  "Uttara Ashadha",
  "Shravana",
  "Dhanishta",
  "Shatabhisha",
  "Purva Bhadrapada",
  "Uttara Bhadrapada",
  "Revati",
];

const RASHIS = ["Any", "Aries (Mesha)", "Taurus (Vrishabha)", "Gemini (Mithuna)", "Cancer (Karka)", "Leo (Simha)", "Virgo (Kanya)", "Libra (Tula)", "Scorpio (Vrishchika)", "Sagittarius (Dhanu)", "Capricorn (Makara)", "Aquarius (Kumbha)", "Pisces (Meena)"];
const GOTRAS = ["Any", "Bharadwaj", "Kashyap", "Shandilya", "Vashishtha", "Vishwamitra", "Garga", "Atri", "Gautam", "Harita", "Jamadagni", "Kaushik", "Srivatsa", "Kaundinya", "Moudgalya", "Parashara", "Agastya", "Bhrigu", "Angirasa"];
const DOSHAS = ["Any", "No", "Manglik", "Sarpa Dosha", "Don't Know"];
const DIETS = ["Any", "Vegetarian", "Non-Vegetarian", "Vegan", "Eggetarian"];
const MOCK_ACTIVITY = {
  recent: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
  active: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
  stale: new Date(Date.now() - 26 * 60 * 60 * 1000).toISOString(),
};

const MOCK_PROFILES = [
  { userId: "u1", firstName: "Priya", age: 26, city: "Mumbai", profession: "Doctor", religion: "Hindu", caste: "Brahmin", subCaste: "Iyer", gothra: "Bharadwaj", nakshatra: "Rohini", rashi: "Taurus (Vrishabha)", zodiacSign: "Taurus (Vrishabha)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/44.jpg", isVerified: true, isPremium: true, match: 94, education: "MBBS", income: "10-25L", height: 165, heightCm: 165, maritalStatus: "Never Married", motherTongue: "Tamil", foodHabit: "Vegetarian", country: "India", state: "Maharashtra", district: "Mumbai City", hasChildren: "no", residentialStatus: "citizen", hasHoroscope: true, photoLocked: false, lastActiveAt: MOCK_ACTIVITY.recent },
  { userId: "u2", firstName: "Ananya", age: 24, city: "Bangalore", profession: "Engineer", religion: "Hindu", caste: "Reddy", subCaste: "Pokanati", gothra: "Kashyap", nakshatra: "Ashwini", rashi: "Aries (Mesha)", zodiacSign: "Aries (Mesha)", dosha: "Manglik", photo: "https://randomuser.me/api/portraits/women/45.jpg", isVerified: true, isPremium: false, match: 89, education: "B.Tech", income: "10-25L", height: 160, heightCm: 160, maritalStatus: "Never Married", motherTongue: "Telugu", foodHabit: "Non-Vegetarian", country: "India", state: "Karnataka", district: "Bangalore Urban", hasChildren: "no", residentialStatus: "work_permit", hasHoroscope: true, photoLocked: false, lastActiveAt: MOCK_ACTIVITY.active },
  { userId: "u3", firstName: "Kavya", age: 28, city: "Hyderabad", profession: "Lawyer", religion: "Hindu", caste: "Kshatriya", subCaste: "Thakur", gothra: "Atri", nakshatra: "Bharani", rashi: "Aries (Mesha)", zodiacSign: "Aries (Mesha)", dosha: "Don't Know", photo: "https://randomuser.me/api/portraits/women/49.jpg", isVerified: false, isPremium: false, match: 75, education: "LLB", income: "3-5L", height: 155, heightCm: 155, maritalStatus: "Never Married", motherTongue: "Hindi", foodHabit: "Vegetarian", country: "India", state: "Telangana", district: "Hyderabad", hasChildren: "yes_not_living_with_me", residentialStatus: "citizen", hasHoroscope: true, photoLocked: false, lastActiveAt: MOCK_ACTIVITY.stale },
  { userId: "u4", firstName: "Meera", age: 29, city: "Jaipur", profession: "Govt. Officer", religion: "Hindu", caste: "Brahmin", subCaste: "Gaur", gothra: "Gautam", nakshatra: "Mula", rashi: "Sagittarius (Dhanu)", zodiacSign: "Sagittarius (Dhanu)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/50.jpg", isVerified: true, isPremium: true, match: 72, education: "IAS", income: "5-10L", height: 163, heightCm: 163, maritalStatus: "Never Married", motherTongue: "Hindi", foodHabit: "Vegetarian", country: "India", state: "Rajasthan", district: "Jaipur", hasChildren: "no", residentialStatus: "citizen", hasHoroscope: true, photoLocked: false, lastActiveAt: MOCK_ACTIVITY.active },
  { userId: "u5", firstName: "Sara", age: 27, city: "Delhi", profession: "Designer", religion: "Muslim", caste: "Any", subCaste: "Any", gothra: "Any", nakshatra: "Any", rashi: "Any", zodiacSign: "Any", dosha: "Any", photo: "https://randomuser.me/api/portraits/women/52.jpg", isVerified: true, isPremium: false, match: 83, education: "B.Des", income: "5-10L", height: 158, heightCm: 158, maritalStatus: "Never Married", motherTongue: "English", foodHabit: "Non-Vegetarian", country: "India", state: "Delhi", district: "South Delhi", hasChildren: "no", residentialStatus: "permanent_resident", hasHoroscope: false, photoLocked: false, lastActiveAt: MOCK_ACTIVITY.recent },
  { userId: "u6", firstName: "Nisha", age: 25, city: "Pune", profession: "Software Engineer", religion: "Hindu", caste: "Kayastha", subCaste: "Saxena", gothra: "Kaushik", nakshatra: "Hasta", rashi: "Virgo (Kanya)", zodiacSign: "Virgo (Kanya)", dosha: "No", photo: "https://randomuser.me/api/portraits/women/31.jpg", isVerified: true, isPremium: true, match: 88, education: "B.Tech", income: "10-25L", height: 162, heightCm: 162, maritalStatus: "Never Married", motherTongue: "Hindi", foodHabit: "Eggetarian", country: "India", state: "Maharashtra", district: "Pune", hasChildren: "yes_living_with_me", residentialStatus: "citizen", hasHoroscope: true, photoLocked: false, lastActiveAt: MOCK_ACTIVITY.active },
];

const INITIAL_FILTERS = {
  query: "",
  minAge: 18,
  maxAge: 40,
  minMatch: 0,
  religion: "Any",
  caste: "Any",
  subCaste: "Any",
  gothra: "Any",
  nakshatra: "Any",
  rashi: "Any",
  dosha: "Any",
  income: "Any",
  country: "Any",
  state: "Any",
  district: "Any",
  city: "Any",
  education: "Any",
  profession: "Any",
  minHeight: 140,
  maxHeight: 200,
  maritalStatus: "Any",
  motherTongue: "Any",
  diet: "Any",
  hasChildren: "Any",
  residentialStatus: "Any",
  withPhotoOnly: false,
  premiumOnly: false,
  onlineNow: false,
  withHoroscopeOnly: false,
  verifiedOnly: false,
  shortlistedOnly: false,
};

const MATCHES_VIEW_KEY = "matches:viewMode";
const MATCHES_SORT_KEY = "matches:sortBy";
const MATCHES_FILTERS_KEY = "matches:filters";
const GRID_BATCH_SIZE = 12;
const LIST_BATCH_SIZE = 8;

function clampNumber(value, min, max) {
  const number = Number(value);
  if (Number.isNaN(number)) return min;
  return Math.min(max, Math.max(min, number));
}

function normalizeOptions(source, fallback, ensureAny = true) {
  if (!Array.isArray(source) || source.length === 0) {
    return fallback;
  }

  const normalized = Array.from(
    new Set(
      source
        .map((item) => (typeof item === "string" ? item.trim() : ""))
        .filter(Boolean)
    )
  );

  if (!ensureAny) {
    return normalized;
  }

  if (!normalized.some((item) => item.toLowerCase() === "any")) {
    normalized.unshift("Any");
  }

  return normalized;
}

function normalizeText(value) {
  return String(value || "").trim().toLowerCase();
}

function normalizeEnum(value) {
  return String(value || "")
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, "_");
}

function uniqueList(items = []) {
  return Array.from(new Set(items.filter(Boolean)));
}

function hasVisiblePhoto(profile) {
  if (!profile) return false;
  if (profile.photoLocked) return false;
  const url = String(profile.photo || "");
  if (!url) return false;
  return !url.includes("placeholder");
}

function isOnlineNow(lastActiveAt) {
  if (!lastActiveAt) return false;
  const timestamp = new Date(lastActiveAt).getTime();
  if (Number.isNaN(timestamp)) return false;
  return timestamp >= Date.now() - 15 * 60 * 1000;
}

function labelForValue(options, value) {
  if (!Array.isArray(options)) return value;
  const found = options.find((item) => item.value === value);
  return found ? found.label : value;
}

function getMatchTier(matchScore) {
  if (matchScore >= 90) return "elite";
  if (matchScore >= 80) return "strong";
  return "rising";
}

function getMatchReason(profile) {
  const reasons = [];
  if (profile.match >= 90) reasons.push("Top compatibility score");
  if (profile.religion && profile.religion !== "Any") reasons.push(`Shared ${profile.religion} values`);
  if (profile.motherTongue && profile.motherTongue !== "Any") reasons.push(`Speaks ${profile.motherTongue}`);
  if (profile.city && profile.city !== "Any") reasons.push(`Based in ${profile.city}`);
  if (profile.profession && profile.profession !== "Any") reasons.push(`Career aligned`);
  if (reasons.length === 0) return "Compatibility based on your preferences.";
  return reasons.slice(0, 2).join(" | ");
}

function MatchActionIcon({ kind, active = false }) {
  if (kind === "star") {
    return (
      <svg
        viewBox="0 0 24 24"
        fill={active ? "currentColor" : "none"}
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="m12 3.8 2.6 5.3 5.9.9-4.3 4.2 1 5.9-5.2-2.8-5.2 2.8 1-5.9-4.3-4.2 5.9-.9L12 3.8Z" />
      </svg>
    );
  }

  if (kind === "open") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M8 16 16 8M10 8h6v6" />
      </svg>
    );
  }

  if (kind === "grid") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M4.5 4.5h6v6h-6zM13.5 4.5h6v6h-6zM4.5 13.5h6v6h-6zM13.5 13.5h6v6h-6z" />
      </svg>
    );
  }

  if (kind === "list") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M9 6h11M9 12h11M9 18h11M4.5 6h.01M4.5 12h.01M4.5 18h.01" />
      </svg>
    );
  }

  if (kind === "chevronDown") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="m6 9 6 6 6-6" />
      </svg>
    );
  }

  if (kind === "chevronUp") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="m6 15 6-6 6 6" />
      </svg>
    );
  }

  if (kind === "refresh") {
    return (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M20 12a8 8 0 1 1-2.3-5.7" />
        <path d="M20 4v6h-6" />
      </svg>
    );
  }

  return null;
}

const MatchCard = memo(function MatchCard({ profile, isShortlisted, onShortlist, onInterest, onOpenProfile, viewMode }) {
  const matchTier = getMatchTier(Number(profile.match) || 0);
  const heightValue = profile.height ?? profile.heightCm;
  const metaLine = [profile.profession, profile.city, heightValue ? `${heightValue}cm` : null]
    .filter(Boolean)
    .join(" | ");
  const matchReason = getMatchReason(profile);
  const isOnline = isOnlineNow(profile.lastActiveAt);
  const tagItems = [
    isOnline ? { label: "Online", tone: "support" } : null,
    profile.isVerified ? { label: "Verified", tone: "support" } : null,
    isShortlisted ? { label: "Saved", tone: "brand" } : null,
    profile.religion ? { label: profile.religion, tone: "brand" } : null,
    profile.education ? { label: profile.education, tone: "brand" } : null,
    profile.income ? { label: profile.income, tone: "brand" } : null,
  ]
    .filter(Boolean)
    .slice(0, 3);

  if (viewMode === "list") {
    return (
      <article className="panel panel-hover anim-rise listing-stage match-card match-card-list">
        <div className="match-media">
          <Image
            className="match-photo"
            src={profile.photo}
            alt={`${profile.firstName} profile`}
            width={720}
            height={900}
            sizes="(max-width: 760px) 100vw, 208px"
          />
          <div className="match-media-overlay" />
          <span className={`match-badge match-badge-${matchTier}`}>{profile.match}% Match</span>
        </div>
        <div className="match-card-content">
          <div className="match-card-header">
            <div className="match-card-title">
              <h3 className="match-card-name">
                {profile.firstName}, {profile.age}
              </h3>
              <p className="profile-meta match-card-meta">
                {metaLine}
              </p>
              <p className="match-reason">{matchReason}</p>
            </div>
            <button
              type="button"
              onClick={() => onShortlist(profile.userId)}
              className="button button-secondary shortlist-fab icon-only-btn match-card-save"
              aria-label={isShortlisted ? "Unsave profile" : "Save profile"}
              title={isShortlisted ? "Unsave profile" : "Save profile"}
            >
              <MatchActionIcon kind="star" active={isShortlisted} />
            </button>
          </div>

          <div className="match-card-tags">
            {tagItems.map((tag) => (
              <span key={`${tag.label}-${tag.tone}`} className={`chip chip-${tag.tone}`}>
                {tag.label}
              </span>
            ))}
          </div>

          <div className="match-card-actions">
            <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
              Send Interest
            </button>
            <Link href={`/profile/${profile.userId}`} className="button button-secondary icon-only-btn view-fab" onClick={() => onOpenProfile(profile.userId)} aria-label="View profile" title="View profile">
              <MatchActionIcon kind="open" />
            </Link>
          </div>
        </div>
      </article>
    );
  }

  return (
    <article className="panel panel-hover anim-rise listing-stage match-card">
      <div className="match-media">
        <Image
          className="match-photo"
          src={profile.photo}
          alt={`${profile.firstName} profile`}
          width={720}
          height={900}
          sizes="(max-width: 760px) 100vw, (max-width: 1200px) 50vw, 33vw"
        />
        <div className="match-media-overlay" />
        <span className={`match-badge match-badge-${matchTier}`}>{profile.match}% Match</span>
        <button
          type="button"
          onClick={() => onShortlist(profile.userId)}
          className="button button-secondary shortlist-fab icon-only-btn match-card-save"
          aria-label={isShortlisted ? "Unsave profile" : "Save profile"}
          title={isShortlisted ? "Unsave profile" : "Save profile"}
        >
          <MatchActionIcon kind="star" active={isShortlisted} />
        </button>
      </div>

      <div className="match-card-body">
        <div className="match-card-heading">
          <h3 className="match-card-name">
            {profile.firstName}, {profile.age}
          </h3>
          <p className="profile-meta match-card-meta">
            {metaLine}
          </p>
        </div>
        <p className="match-reason">{matchReason}</p>

        <div className="match-card-tags">
          {tagItems.map((tag) => (
            <span key={`${tag.label}-${tag.tone}`} className={`chip chip-${tag.tone}`}>
              {tag.label}
            </span>
          ))}
        </div>

        <div className="match-card-actions">
          <button type="button" className="button button-primary" onClick={() => onInterest(profile.userId)}>
            Send Interest
          </button>
          <Link href={`/profile/${profile.userId}`} className="button button-secondary icon-only-btn view-fab" onClick={() => onOpenProfile(profile.userId)} aria-label="View profile" title="View profile">
            <MatchActionIcon kind="open" />
          </Link>
        </div>
      </div>
    </article>
  );
});

function MatchesContent() {
  const searchParams = useSearchParams();
  const { user } = useAuth();
  const { showLoginModal, setShowLoginModal } = useGuestScrollGate();

  const [loading, setLoading] = useState(true);
  const [rawProfiles, setRawProfiles] = useState([]);
  const [isPreview, setIsPreview] = useState(false);
  const [shortlisted, setShortlisted] = useState(new Set());
  const [viewMode, setViewMode] = useState("grid");
  const [sortBy, setSortBy] = useState("compatibility");
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState(INITIAL_FILTERS);
  const [filterMeta, setFilterMeta] = useState(FALLBACK_FILTER_META);
  const [hasLoadedFilterMeta, setHasLoadedFilterMeta] = useState(false);
  const [locationMeta, setLocationMeta] = useState(FALLBACK_LOCATION_META);
  const [hasLoadedLocationMeta, setHasLoadedLocationMeta] = useState(false);
  const [savedSearches, setSavedSearches] = useState([]);
  const [savedSearchName, setSavedSearchName] = useState("");
  const [savingSearch, setSavingSearch] = useState(false);
  const [loadingSavedSearches, setLoadingSavedSearches] = useState(false);
  const [savedSearchError, setSavedSearchError] = useState("");
  const [refreshIndex, setRefreshIndex] = useState(0);
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  useEffect(() => {
    const queryFromParams = searchParams.get("q");
    const religionFromParams = searchParams.get("religion");
    const countryFromParams = searchParams.get("country");
    const stateFromParams = searchParams.get("state");
    const districtFromParams = searchParams.get("district");
    const cityFromParams = searchParams.get("city");
    const motherTongueFromParams = searchParams.get("motherTongue");
    const hasChildrenFromParams = searchParams.get("hasChildren");
    const residentialFromParams = searchParams.get("residentialStatus");
    const minAgeFromParams = searchParams.get("minAge");
    const maxAgeFromParams = searchParams.get("maxAge");
    const minMatchFromParams = searchParams.get("minMatch");
    const verifiedFromParams = searchParams.get("verified");
    const premiumFromParams = searchParams.get("premium");
    const onlineFromParams = searchParams.get("online");
    const withPhotoFromParams = searchParams.get("withPhoto");
    const horoscopeFromParams = searchParams.get("withHoroscope");
    const savedFromParams = searchParams.get("saved");

    const nextFilters = {};
    if (typeof queryFromParams === "string") {
      nextFilters.query = queryFromParams;
    }
    if (religionFromParams) {
      nextFilters.religion = religionFromParams;
    }
    if (countryFromParams) {
      nextFilters.country = countryFromParams;
    }
    if (stateFromParams) {
      nextFilters.state = stateFromParams;
    }
    if (districtFromParams) {
      nextFilters.district = districtFromParams;
    }
    if (cityFromParams) {
      nextFilters.city = cityFromParams;
    }
    if (motherTongueFromParams) {
      nextFilters.motherTongue = motherTongueFromParams;
    }
    if (hasChildrenFromParams) {
      nextFilters.hasChildren = hasChildrenFromParams;
    }
    if (residentialFromParams) {
      nextFilters.residentialStatus = residentialFromParams;
    }
    if (minAgeFromParams !== null) {
      nextFilters.minAge = clampNumber(minAgeFromParams, 18, 70);
    }
    if (maxAgeFromParams !== null) {
      nextFilters.maxAge = clampNumber(maxAgeFromParams, 18, 70);
    }
    if (minMatchFromParams !== null) {
      nextFilters.minMatch = clampNumber(minMatchFromParams, 0, 100);
    }
    if (verifiedFromParams === "1") {
      nextFilters.verifiedOnly = true;
    }
    if (premiumFromParams === "1") {
      nextFilters.premiumOnly = true;
    }
    if (onlineFromParams === "1") {
      nextFilters.onlineNow = true;
    }
    if (withPhotoFromParams === "1") {
      nextFilters.withPhotoOnly = true;
    }
    if (horoscopeFromParams === "1") {
      nextFilters.withHoroscopeOnly = true;
    }
    if (savedFromParams === "1") {
      nextFilters.shortlistedOnly = true;
    }

    if (Object.keys(nextFilters).length > 0) {
      setFilters((previous) => {
        const merged = { ...previous, ...nextFilters };
        if (merged.maxAge < merged.minAge) {
          merged.maxAge = merged.minAge;
        }
        return merged;
      });
    }

    const shouldOpenFilters =
      searchParams.get("filters") === "1" ||
      Boolean(queryFromParams) ||
      Boolean(religionFromParams) ||
      Boolean(countryFromParams) ||
      Boolean(stateFromParams) ||
      Boolean(districtFromParams) ||
      Boolean(cityFromParams) ||
      Boolean(motherTongueFromParams) ||
      Boolean(hasChildrenFromParams) ||
      Boolean(residentialFromParams) ||
      minAgeFromParams !== null ||
      maxAgeFromParams !== null ||
      minMatchFromParams !== null ||
      verifiedFromParams === "1" ||
      premiumFromParams === "1" ||
      onlineFromParams === "1" ||
      withPhotoFromParams === "1" ||
      horoscopeFromParams === "1" ||
      savedFromParams === "1";

    if (shouldOpenFilters) {
      setShowFilters(true);
    }
  }, [searchParams]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (searchParams.toString()) return;
    try {
      const stored = JSON.parse(window.localStorage.getItem(MATCHES_FILTERS_KEY) || "null");
      if (!stored || typeof stored !== "object") return;
      setFilters((previous) => {
        const merged = { ...previous, ...stored };
        if (Number(merged.maxAge) < Number(merged.minAge)) merged.maxAge = merged.minAge;
        if (Number(merged.maxHeight) < Number(merged.minHeight)) merged.maxHeight = merged.minHeight;
        return merged;
      });
      const hasActiveStoredFilters = Object.entries(stored).some(([key, value]) => value !== INITIAL_FILTERS[key]);
      if (hasActiveStoredFilters) setShowFilters(true);
    } catch {
      // Ignore persisted filters if malformed.
    }
  }, [searchParams]);

  useEffect(() => {
    if (!showFilters || hasLoadedFilterMeta) return;

    let cancelled = false;

    const loadFilterMeta = async () => {
      try {
        const response = await getCachedRequest("meta:filters", () => api.get("/meta/filters"), 5 * 60_000);
        const payload = response?.data?.data || response?.data || {};
        if (cancelled) return;

        setFilterMeta({
          religion: normalizeOptions(payload.religion, FALLBACK_FILTER_META.religion),
          caste: normalizeOptions(payload.caste, FALLBACK_FILTER_META.caste),
          income: normalizeOptions(payload.income, FALLBACK_FILTER_META.income),
          maritalStatus: normalizeOptions(payload.maritalStatus, FALLBACK_FILTER_META.maritalStatus),
          motherTongue: normalizeOptions(payload.motherTongue, FALLBACK_FILTER_META.motherTongue),
          education: normalizeOptions(payload.education, FALLBACK_FILTER_META.education),
          profession: normalizeOptions(payload.profession, FALLBACK_FILTER_META.profession),
          residentialStatus: normalizeOptions(payload.residentialStatus, FALLBACK_FILTER_META.residentialStatus),
          children: normalizeOptions(payload.children, FALLBACK_FILTER_META.children),
        });
        setHasLoadedFilterMeta(true);
      } catch {
        if (!cancelled) {
          setFilterMeta(FALLBACK_FILTER_META);
          setHasLoadedFilterMeta(true);
        }
      }
    };

    loadFilterMeta();
    return () => {
      cancelled = true;
    };
  }, [showFilters, hasLoadedFilterMeta]);

  useEffect(() => {
    if (!showFilters || hasLoadedLocationMeta) return;

    let cancelled = false;

    const loadLocations = async () => {
      try {
        const response = await getCachedRequest("meta:locations", () => api.get("/meta/locations"), 5 * 60_000);
        const payload = response?.data?.data || response?.data || {};
        if (cancelled) return;

        if (Array.isArray(payload.countries)) {
          setLocationMeta(payload);
        } else {
          setLocationMeta(FALLBACK_LOCATION_META);
        }
        setHasLoadedLocationMeta(true);
      } catch {
        if (!cancelled) {
          setLocationMeta(FALLBACK_LOCATION_META);
          setHasLoadedLocationMeta(true);
        }
      }
    };

    loadLocations();
    return () => {
      cancelled = true;
    };
  }, [showFilters, hasLoadedLocationMeta]);

  useEffect(() => {
    if (!user) {
      setSavedSearches([]);
      return;
    }

    let cancelled = false;
    setLoadingSavedSearches(true);

    const loadSaved = async () => {
      try {
        const response = await api.get("/search/saved");
        const list = Array.isArray(response?.data) ? response.data : response?.data?.items;
        if (!cancelled) {
          setSavedSearches(Array.isArray(list) ? list : []);
        }
      } catch {
        if (!cancelled) {
          setSavedSearches([]);
        }
      } finally {
        if (!cancelled) {
          setLoadingSavedSearches(false);
        }
      }
    };

    loadSaved();
    return () => {
      cancelled = true;
    };
  }, [user]);

  useEffect(() => {
    if (!user) {
      setShortlisted(new Set());
      return;
    }

    let cancelled = false;
    const loadShortlisted = async () => {
      try {
        const response = await api.get("/shortlist");
        const list = Array.isArray(response?.data) ? response.data : response?.data?.items;
        if (cancelled) return;
        const ids = (Array.isArray(list) ? list : [])
          .map((item) => item?.userId || item?.profileId || item?.id)
          .filter(Boolean);
        setShortlisted(new Set(ids));
      } catch {
        if (!cancelled) {
          setShortlisted(new Set());
        }
      }
    };

    loadShortlisted();
    return () => {
      cancelled = true;
    };
  }, [user]);

  useEffect(() => {
    if (!user && filters.shortlistedOnly) {
      setFilters((previous) => ({ ...previous, shortlistedOnly: false }));
    }
  }, [user, filters.shortlistedOnly]);

  const userId = user?.id || user?.sub || null;

  const genderParam = useMemo(() => {
    const rawGender = searchParams.get("gender");
    if (!rawGender) return null;
    const normalized = rawGender.toLowerCase();
    return ["male", "female"].includes(normalized) ? normalized : null;
  }, [searchParams]);

  const requestFilters = useMemo(() => {
    const payload = {};
    const addIf = (key, value) => {
      if (value === undefined || value === null) return;
      if (typeof value === "string") {
        const trimmed = value.trim();
        if (!trimmed || trimmed === "Any") return;
        payload[key] = trimmed;
        return;
      }
      payload[key] = value;
    };

    addIf("query", filters.query);
    addIf("religion", filters.religion);
    addIf("caste", filters.caste);
    addIf("subCaste", filters.subCaste);
    addIf("gothra", filters.gothra);
    addIf("nakshatra", filters.nakshatra);
    addIf("rashi", filters.rashi);
    addIf("dosha", filters.dosha);
    addIf("income", filters.income);
    addIf("country", filters.country);
    addIf("state", filters.state);
    addIf("district", filters.district);
    addIf("city", filters.city);
    addIf("education", filters.education);
    addIf("profession", filters.profession);
    addIf("maritalStatus", filters.maritalStatus);
    addIf("motherTongue", filters.motherTongue);
    addIf("diet", filters.diet);
    if (filters.hasChildren && filters.hasChildren !== "Any") payload.hasChildren = filters.hasChildren;
    if (filters.residentialStatus && filters.residentialStatus !== "Any") payload.residentialStatus = filters.residentialStatus;

    const minAge = Number(filters.minAge);
    const maxAge = Number(filters.maxAge);
    const minHeight = Number(filters.minHeight);
    const maxHeight = Number(filters.maxHeight);
    if (!Number.isNaN(minAge)) payload.minAge = minAge;
    if (!Number.isNaN(maxAge)) payload.maxAge = maxAge;
    if (!Number.isNaN(minHeight)) payload.minHeight = minHeight;
    if (!Number.isNaN(maxHeight)) payload.maxHeight = maxHeight;

    const minMatch = Number(filters.minMatch);
    if (!Number.isNaN(minMatch) && minMatch > 0) payload.minMatch = minMatch;

    if (filters.verifiedOnly) payload.verifiedOnly = true;
    if (filters.withPhotoOnly) payload.withPhotoOnly = true;
    if (filters.premiumOnly) payload.premiumOnly = true;
    if (filters.onlineNow) payload.onlineNow = true;
    if (filters.withHoroscopeOnly) payload.withHoroscopeOnly = true;
    if (filters.shortlistedOnly) payload.shortlistedOnly = true;

    if (genderParam) payload.gender = genderParam;
    return payload;
  }, [filters, genderParam]);

  const requestKey = useMemo(() => JSON.stringify(requestFilters), [requestFilters]);

  useEffect(() => {
    if (!user) {
      setRawProfiles(MOCK_PROFILES);
      setIsPreview(true);
      setLoading(false);
      setLastUpdatedAt(new Date());
      return;
    }

    let cancelled = false;
    const cacheKey = userId ? `matches:${userId}:${requestKey}:${refreshIndex}` : `matches:${requestKey}:${refreshIndex}`;
    const debounce = window.setTimeout(async () => {
      setLoading(true);
      try {
        const response = await getCachedRequest(
          cacheKey,
          () => api.get("/matches", { params: requestFilters }),
          45_000
        );
        if (cancelled) return;
        const list = Array.isArray(response.data) ? response.data : [];
        setRawProfiles(list);
        setIsPreview(false);
        setLastUpdatedAt(new Date());
      } catch {
        if (!cancelled) {
          setRawProfiles(MOCK_PROFILES);
          setIsPreview(true);
          setLastUpdatedAt(new Date());
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }, 250);

    return () => {
      cancelled = true;
      window.clearTimeout(debounce);
    };
  }, [user, userId, requestKey, requestFilters, refreshIndex]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const frame = window.requestAnimationFrame(() => {
      try {
        const storedViewMode = window.localStorage.getItem(MATCHES_VIEW_KEY);
        if (storedViewMode === "grid" || storedViewMode === "list") {
          setViewMode(storedViewMode);
        }

        const storedSortBy = window.localStorage.getItem(MATCHES_SORT_KEY);
        if (storedSortBy && ["compatibility", "verified", "ageAsc", "ageDesc", "matchAsc", "recent"].includes(storedSortBy)) {
          setSortBy(storedSortBy);
        }
      } catch {}
    });

    return () => window.cancelAnimationFrame(frame);
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.setItem(MATCHES_VIEW_KEY, viewMode);
      window.localStorage.setItem(MATCHES_SORT_KEY, sortBy);
    } catch {}
  }, [viewMode, sortBy]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.setItem(MATCHES_FILTERS_KEY, JSON.stringify(filters));
    } catch {}
  }, [filters]);

  const onFilterChange = (key, value) => {
    setFilters((previous) => {
      if (key === "shortlistedOnly" && value && !user) {
        setShowLoginModal(true);
        return previous;
      }
      const next = { ...previous, [key]: value };
      if (key === "caste") next.subCaste = "Any";
      if (key === "country") {
        next.state = "Any";
        next.city = "Any";
        next.district = "Any";
      }
      if (key === "state") {
        next.city = "Any";
        next.district = "Any";
      }
      if (key === "minAge" && Number(next.minAge) > Number(next.maxAge)) {
        next.maxAge = next.minAge;
      }
      if (key === "maxAge" && Number(next.maxAge) < Number(next.minAge)) {
        next.minAge = next.maxAge;
      }
      if (key === "minHeight" && Number(next.minHeight) > Number(next.maxHeight)) {
        next.maxHeight = next.minHeight;
      }
      if (key === "maxHeight" && Number(next.maxHeight) < Number(next.minHeight)) {
        next.minHeight = next.maxHeight;
      }
      return next;
    });
  };

  const resetFilters = () => setFilters({ ...INITIAL_FILTERS });

  const toggleQuickFilter = (key) => {
    onFilterChange(key, !filters[key]);
  };

  const handleRefresh = () => {
    if (!user) {
      setLastUpdatedAt(new Date());
      return;
    }
    setRefreshIndex((previous) => previous + 1);
  };

  const clearFilter = (key) => {
    setFilters((previous) => {
      if (key === "age") {
        return { ...previous, minAge: INITIAL_FILTERS.minAge, maxAge: INITIAL_FILTERS.maxAge };
      }
      if (key === "height") {
        return { ...previous, minHeight: INITIAL_FILTERS.minHeight, maxHeight: INITIAL_FILTERS.maxHeight };
      }
      if (key === "country") {
        return {
          ...previous,
          country: INITIAL_FILTERS.country,
          state: INITIAL_FILTERS.state,
          city: INITIAL_FILTERS.city,
          district: INITIAL_FILTERS.district,
        };
      }
      if (key === "state") {
        return {
          ...previous,
          state: INITIAL_FILTERS.state,
          city: INITIAL_FILTERS.city,
          district: INITIAL_FILTERS.district,
        };
      }
      if (key === "city") {
        return { ...previous, city: INITIAL_FILTERS.city };
      }
      if (key === "district") {
        return { ...previous, district: INITIAL_FILTERS.district };
      }
      if (key === "caste") {
        return { ...previous, caste: INITIAL_FILTERS.caste, subCaste: INITIAL_FILTERS.subCaste };
      }
      return { ...previous, [key]: INITIAL_FILTERS[key] };
    });
  };

  const applySavedSearch = (saved) => {
    if (!saved || typeof saved !== "object") return;
    const savedFilters = saved.filters && typeof saved.filters === "object" ? saved.filters : {};
    setFilters({ ...INITIAL_FILTERS, ...savedFilters });
    setShowFilters(true);
  };

  const handleSaveSearch = async () => {
    if (!user) {
      setShowLoginModal(true);
      return;
    }

    const name = savedSearchName.trim() || `Search ${new Date().toLocaleDateString()}`;
    setSavingSearch(true);
    setSavedSearchError("");

    try {
      const response = await api.post("/search/saved", { name, filters });
      const created = response?.data;
      if (created?.id) {
        setSavedSearches((prev) => [created, ...prev.filter((item) => item.id !== created.id)]);
      } else {
        setSavedSearches((prev) => [{ id: `tmp-${Date.now()}`, name, filters }, ...prev]);
      }
      setSavedSearchName("");
      toast.success("Search saved.");
    } catch (error) {
      setSavedSearchError(error.response?.data?.error || "Unable to save search.");
    } finally {
      setSavingSearch(false);
    }
  };

  const handleDeleteSavedSearch = async (id) => {
    if (!user || !id) return;
    setSavedSearches((prev) => prev.filter((item) => item.id !== id));
    try {
      await api.delete(`/search/saved/${id}`);
      toast.info("Saved search removed.");
    } catch {
      toast.error("Unable to remove saved search.");
    }
  };

  const subcasteOptions = useMemo(() => ["Any", ...(SUBCASTE_MAP[filters.caste] || [])], [filters.caste]);
  const locationCountries = useMemo(
    () => (Array.isArray(locationMeta?.countries) && locationMeta.countries.length > 0 ? locationMeta.countries : FALLBACK_LOCATION_META.countries),
    [locationMeta]
  );
  const countryOptions = useMemo(
    () => ["Any", ...uniqueList(locationCountries.map((country) => country.name))],
    [locationCountries]
  );
  const selectedCountry = useMemo(
    () => locationCountries.find((country) => country.name === filters.country) || null,
    [locationCountries, filters.country]
  );
  const availableStates = useMemo(() => {
    if (filters.country !== "Any" && selectedCountry) {
      return Array.isArray(selectedCountry.states) ? selectedCountry.states : [];
    }
    return locationCountries.flatMap((country) => (Array.isArray(country.states) ? country.states : []));
  }, [filters.country, selectedCountry, locationCountries]);
  const stateOptions = useMemo(
    () => ["Any", ...uniqueList(availableStates.map((state) => state.name))],
    [availableStates]
  );
  const selectedState = useMemo(
    () => availableStates.find((state) => state.name === filters.state) || null,
    [availableStates, filters.state]
  );
  const cityOptions = useMemo(() => {
    const sourceStates = filters.state !== "Any" && selectedState ? [selectedState] : availableStates;
    return ["Any", ...uniqueList(sourceStates.flatMap((state) => state.cities || []))];
  }, [filters.state, selectedState, availableStates]);
  const districtOptions = useMemo(() => {
    const sourceStates = filters.state !== "Any" && selectedState ? [selectedState] : availableStates;
    return ["Any", ...uniqueList(sourceStates.flatMap((state) => state.districts || []))];
  }, [filters.state, selectedState, availableStates]);

  const visibleProfiles = useMemo(() => {
    const query = filters.query.trim().toLowerCase();
    const baseProfiles = isPreview
      ? rawProfiles.filter((item) => {
        const heightValue = Number(item.height ?? item.heightCm ?? 0);
        const gothraValue = item.gothra || "Any";
        const nakshatraValue = item.nakshatra || "Any";
        const rashiValue = item.rashi || item.zodiacSign || "Any";
        const hasHoroscope = Boolean(item.hasHoroscope ?? item.gothra ?? item.nakshatra ?? item.zodiacSign);

        if (item.age < Number(filters.minAge) || item.age > Number(filters.maxAge)) return false;
        if (Number(item.match || 0) < Number(filters.minMatch || 0)) return false;
        if (filters.religion !== "Any" && normalizeText(item.religion) !== normalizeText(filters.religion)) return false;
        if (filters.caste !== "Any" && normalizeText(item.caste) !== normalizeText(filters.caste)) return false;
        if (filters.subCaste !== "Any" && normalizeText(item.subCaste || "Any") !== normalizeText(filters.subCaste)) return false;
        if (filters.gothra !== "Any" && normalizeText(gothraValue) !== normalizeText(filters.gothra)) return false;
        if (filters.nakshatra !== "Any" && normalizeText(nakshatraValue) !== normalizeText(filters.nakshatra)) return false;
        if (filters.rashi !== "Any" && normalizeText(rashiValue) !== normalizeText(filters.rashi)) return false;
        if (filters.dosha !== "Any" && item.dosha !== filters.dosha) return false;
        if (filters.income !== "Any" && item.income !== filters.income) return false;
        if (filters.country !== "Any" && normalizeText(item.country) !== normalizeText(filters.country)) return false;
        if (filters.state !== "Any" && normalizeText(item.state) !== normalizeText(filters.state)) return false;
        if (filters.district !== "Any" && normalizeText(item.district) !== normalizeText(filters.district)) return false;
        if (filters.city !== "Any" && normalizeText(item.city) !== normalizeText(filters.city)) return false;
        if (filters.education !== "Any" && !normalizeText(item.education || "").includes(normalizeText(filters.education))) return false;
        if (filters.profession !== "Any" && !normalizeText(item.profession || "").includes(normalizeText(filters.profession))) return false;
        if (heightValue && (heightValue < Number(filters.minHeight) || heightValue > Number(filters.maxHeight))) return false;
        if (filters.maritalStatus !== "Any" && normalizeEnum(item.maritalStatus) !== normalizeEnum(filters.maritalStatus)) return false;
        if (filters.motherTongue !== "Any" && normalizeText(item.motherTongue) !== normalizeText(filters.motherTongue)) return false;
        if (filters.diet !== "Any" && normalizeEnum(item.foodHabit) !== normalizeEnum(filters.diet)) return false;
        if (filters.hasChildren !== "Any" && normalizeEnum(item.hasChildren) !== normalizeEnum(filters.hasChildren)) return false;
        if (filters.residentialStatus !== "Any" && normalizeEnum(item.residentialStatus) !== normalizeEnum(filters.residentialStatus)) return false;
        if (filters.withPhotoOnly && !hasVisiblePhoto(item)) return false;
        if (filters.premiumOnly && !item.isPremium) return false;
        if (filters.onlineNow && !isOnlineNow(item.lastActiveAt)) return false;
        if (filters.withHoroscopeOnly && !hasHoroscope) return false;
        if (filters.verifiedOnly && !item.isVerified) return false;
        if (filters.shortlistedOnly && !shortlisted.has(item.userId)) return false;
        if (!query) return true;
        return [item.firstName, item.city, item.profession, item.religion, item.caste, item.subCaste, item.education].filter(Boolean).join(" ").toLowerCase().includes(query);
      })
      : rawProfiles;

    const sorted = [...baseProfiles];
    if (sortBy === "compatibility") {
      sorted.sort((a, b) => Number(b.match || 0) - Number(a.match || 0));
    } else if (sortBy === "verified") {
      sorted.sort((a, b) => Number(Boolean(b.isVerified)) - Number(Boolean(a.isVerified)) || Number(b.match || 0) - Number(a.match || 0));
    } else if (sortBy === "ageAsc") {
      sorted.sort((a, b) => Number(a.age || 0) - Number(b.age || 0));
    } else if (sortBy === "ageDesc") {
      sorted.sort((a, b) => Number(b.age || 0) - Number(a.age || 0));
    } else if (sortBy === "matchAsc") {
      sorted.sort((a, b) => Number(a.match || 0) - Number(b.match || 0));
    } else if (sortBy === "recent") {
      sorted.sort((a, b) => new Date(b.lastActiveAt || 0).getTime() - new Date(a.lastActiveAt || 0).getTime());
    }

    return sorted;
  }, [rawProfiles, filters, sortBy, shortlisted, isPreview]);

  const dataUpdatedLabel = useMemo(() => {
    if (!lastUpdatedAt) return null;
    return lastUpdatedAt.toLocaleString(undefined, {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  }, [lastUpdatedAt]);

  const profileInsights = useMemo(() => {
    const total = visibleProfiles.length;
    const verified = visibleProfiles.filter((item) => item.isVerified).length;
    const highCompatibility = visibleProfiles.filter((item) => Number(item.match) >= 85).length;
    const averageCompatibility = total > 0
      ? Math.round(visibleProfiles.reduce((sum, item) => sum + Number(item.match || 0), 0) / total)
      : 0;
    return { total, verified, highCompatibility, averageCompatibility };
  }, [visibleProfiles]);

  const quickFilters = useMemo(() => {
    const items = [
      { key: "verifiedOnly", label: "Verified", active: filters.verifiedOnly },
      { key: "onlineNow", label: "Online now", active: filters.onlineNow },
      { key: "withPhotoOnly", label: "With photo", active: filters.withPhotoOnly },
      { key: "premiumOnly", label: "Premium", active: filters.premiumOnly },
      { key: "withHoroscopeOnly", label: "Horoscope", active: filters.withHoroscopeOnly },
    ];
    if (user) {
      items.push({ key: "shortlistedOnly", label: "Saved", active: filters.shortlistedOnly });
    }
    return items;
  }, [filters, user]);

  const activeFilterTags = useMemo(() => {
    const tags = [];
    if (filters.query.trim()) tags.push({ key: "query", label: `Query: ${filters.query.trim()}` });
    if (Number(filters.minMatch) > 0) tags.push({ key: "minMatch", label: `Min match ${filters.minMatch}%` });
    if (Number(filters.minAge) !== INITIAL_FILTERS.minAge || Number(filters.maxAge) !== INITIAL_FILTERS.maxAge) {
      tags.push({ key: "age", label: `Age ${filters.minAge}-${filters.maxAge}` });
    }
    if (filters.religion !== "Any") tags.push({ key: "religion", label: filters.religion });
    if (filters.caste !== "Any") tags.push({ key: "caste", label: filters.caste });
    if (filters.subCaste !== "Any") tags.push({ key: "subCaste", label: `Sub-caste: ${filters.subCaste}` });
    if (filters.gothra !== "Any") tags.push({ key: "gothra", label: `Gothra: ${filters.gothra}` });
    if (filters.nakshatra !== "Any") tags.push({ key: "nakshatra", label: `Nakshatra: ${filters.nakshatra}` });
    if (filters.rashi !== "Any") tags.push({ key: "rashi", label: `Rashi: ${filters.rashi}` });
    if (filters.dosha !== "Any") tags.push({ key: "dosha", label: `Dosha: ${filters.dosha}` });
    if (filters.income !== "Any") tags.push({ key: "income", label: `Income: ${filters.income}` });
    if (filters.country !== "Any") tags.push({ key: "country", label: `Country: ${filters.country}` });
    if (filters.state !== "Any") tags.push({ key: "state", label: `State: ${filters.state}` });
    if (filters.district !== "Any") tags.push({ key: "district", label: `District: ${filters.district}` });
    if (filters.city !== "Any") tags.push({ key: "city", label: filters.city });
    if (filters.education !== "Any") tags.push({ key: "education", label: `Education: ${filters.education}` });
    if (filters.profession !== "Any") tags.push({ key: "profession", label: filters.profession });
    if (Number(filters.minHeight) !== INITIAL_FILTERS.minHeight || Number(filters.maxHeight) !== INITIAL_FILTERS.maxHeight) {
      tags.push({ key: "height", label: `Height ${filters.minHeight}-${filters.maxHeight}cm` });
    }
    if (filters.maritalStatus !== "Any") tags.push({ key: "maritalStatus", label: filters.maritalStatus });
    if (filters.motherTongue !== "Any") tags.push({ key: "motherTongue", label: `Language: ${filters.motherTongue}` });
    if (filters.diet !== "Any") tags.push({ key: "diet", label: `Diet: ${filters.diet}` });
    if (filters.hasChildren !== "Any") {
      tags.push({ key: "hasChildren", label: `Children: ${labelForValue(HAS_CHILDREN_OPTIONS, filters.hasChildren)}` });
    }
    if (filters.residentialStatus !== "Any") {
      tags.push({ key: "residentialStatus", label: `Status: ${labelForValue(RESIDENTIAL_STATUS_OPTIONS, filters.residentialStatus)}` });
    }
    if (filters.withPhotoOnly) tags.push({ key: "withPhotoOnly", label: "With photo" });
    if (filters.premiumOnly) tags.push({ key: "premiumOnly", label: "Premium only" });
    if (filters.onlineNow) tags.push({ key: "onlineNow", label: "Online now" });
    if (filters.withHoroscopeOnly) tags.push({ key: "withHoroscopeOnly", label: "Horoscope ready" });
    if (filters.verifiedOnly) tags.push({ key: "verifiedOnly", label: "Verified only" });
    if (filters.shortlistedOnly) tags.push({ key: "shortlistedOnly", label: "Saved only" });
    return tags;
  }, [filters]);

  const activeFilterCount = useMemo(() => {
    const checks = [
      filters.query.trim() !== "",
      Number(filters.minMatch) !== INITIAL_FILTERS.minMatch,
      Number(filters.minAge) !== INITIAL_FILTERS.minAge,
      Number(filters.maxAge) !== INITIAL_FILTERS.maxAge,
      filters.religion !== INITIAL_FILTERS.religion,
      filters.caste !== INITIAL_FILTERS.caste,
      filters.subCaste !== INITIAL_FILTERS.subCaste,
      filters.gothra !== INITIAL_FILTERS.gothra,
      filters.nakshatra !== INITIAL_FILTERS.nakshatra,
      filters.rashi !== INITIAL_FILTERS.rashi,
      filters.dosha !== INITIAL_FILTERS.dosha,
      filters.income !== INITIAL_FILTERS.income,
      filters.country !== INITIAL_FILTERS.country,
      filters.state !== INITIAL_FILTERS.state,
      filters.district !== INITIAL_FILTERS.district,
      filters.city !== INITIAL_FILTERS.city,
      filters.education !== INITIAL_FILTERS.education,
      filters.profession !== INITIAL_FILTERS.profession,
      Number(filters.minHeight) !== INITIAL_FILTERS.minHeight,
      Number(filters.maxHeight) !== INITIAL_FILTERS.maxHeight,
      filters.maritalStatus !== INITIAL_FILTERS.maritalStatus,
      filters.motherTongue !== INITIAL_FILTERS.motherTongue,
      filters.diet !== INITIAL_FILTERS.diet,
      filters.hasChildren !== INITIAL_FILTERS.hasChildren,
      filters.residentialStatus !== INITIAL_FILTERS.residentialStatus,
      filters.withPhotoOnly !== INITIAL_FILTERS.withPhotoOnly,
      filters.premiumOnly !== INITIAL_FILTERS.premiumOnly,
      filters.onlineNow !== INITIAL_FILTERS.onlineNow,
      filters.withHoroscopeOnly !== INITIAL_FILTERS.withHoroscopeOnly,
      filters.verifiedOnly !== INITIAL_FILTERS.verifiedOnly,
      filters.shortlistedOnly !== INITIAL_FILTERS.shortlistedOnly,
    ];
    return checks.filter(Boolean).length;
  }, [filters]);

  const relaxSuggestions = useMemo(() => {
    const suggestions = [];
    const push = (key, label) => suggestions.push({ key, label });

    if (filters.shortlistedOnly) push("shortlistedOnly", "Show all profiles");
    if (filters.onlineNow) push("onlineNow", "Include offline");
    if (filters.verifiedOnly) push("verifiedOnly", "Include unverified");
    if (filters.withHoroscopeOnly) push("withHoroscopeOnly", "Include profiles without horoscope");
    if (filters.premiumOnly) push("premiumOnly", "Include free members");
    if (filters.withPhotoOnly) push("withPhotoOnly", "Include profiles without photo");
    if (Number(filters.minMatch) > 0) push("minMatch", "Remove match threshold");
    if (filters.caste !== "Any") push("caste", "Clear caste filter");
    if (filters.profession !== "Any") push("profession", "Clear profession filter");
    if (filters.city !== "Any" || filters.district !== "Any" || filters.state !== "Any" || filters.country !== "Any") {
      push("location", "Broaden location");
    }

    return suggestions.slice(0, 4);
  }, [filters]);

  const handleRelaxSuggestion = (key) => {
    if (key === "location") {
      if (filters.city !== "Any") {
        clearFilter("city");
        return;
      }
      if (filters.district !== "Any") {
        clearFilter("district");
        return;
      }
      if (filters.state !== "Any") {
        clearFilter("state");
        return;
      }
      if (filters.country !== "Any") {
        clearFilter("country");
      }
      return;
    }

    if (key === "minMatch") {
      onFilterChange("minMatch", 0);
      return;
    }

    if (key === "caste") {
      clearFilter("caste");
      return;
    }

    if (key === "profession") {
      clearFilter("profession");
      return;
    }

    if (["shortlistedOnly", "onlineNow", "verifiedOnly", "withHoroscopeOnly", "premiumOnly", "withPhotoOnly"].includes(key)) {
      onFilterChange(key, false);
    }
  };

  const batchSize = viewMode === "grid" ? GRID_BATCH_SIZE : LIST_BATCH_SIZE;
  const [visibleCount, setVisibleCount] = useState(batchSize);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => {
      setVisibleCount(batchSize);
    });
    return () => window.cancelAnimationFrame(frame);
  }, [batchSize, filters, sortBy, rawProfiles]);

  const renderedProfiles = useMemo(() => visibleProfiles.slice(0, visibleCount), [visibleProfiles, visibleCount]);
  const hasMoreProfiles = visibleCount < visibleProfiles.length;

  const onInterest = async (userId) => {
    if (!user) {
      setShowLoginModal(true);
      return;
    }
    try {
      const response = await api.post("/interactions/like", { receiverId: userId });
      await api.post("/matches/feedback", { targetUserId: userId, action: "like", source: "matches_feed" });
      if (response.data?.matchId) toast.success("It is a match. Start chatting now.");
      else toast.success("Interest sent successfully.");
    } catch {
      toast.info("Interest flow is in demo mode.");
    }
  };

  const onOpenProfile = async (userId) => {
    if (!user) return;
    try {
      await api.post("/matches/feedback", { targetUserId: userId, action: "open", source: "matches_feed" });
    } catch {
      // Non-blocking feedback event.
    }
  };

  const onShortlist = async (userId) => {
    if (!user) {
      setShowLoginModal(true);
      return;
    }

    const alreadyShortlisted = shortlisted.has(userId);
    setShortlisted((previous) => {
      const next = new Set(previous);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });
    if (filters.shortlistedOnly && alreadyShortlisted) {
      setRawProfiles((previous) => previous.filter((profile) => profile.userId !== userId));
    }

    try {
      if (alreadyShortlisted) {
        await api.post("/shortlist/remove", { shortlistedUserId: userId });
        toast.info("Removed from shortlist.");
      } else {
        await api.post("/shortlist/add", { shortlistedUserId: userId });
        toast.success("Added to shortlist.");
      }
    } catch {
      toast.info("Shortlist update saved locally.");
    }
  };

  return (
    <div className={`matches-shell-grid ${showFilters ? "filters-open" : "filters-collapsed"}`}>
      {showFilters && (
        <aside className="panel filter-panel matches-filter-panel">
          <div className="matches-filter-header">
            <div className="matches-filter-title-row">
              <p className="section-label">Filters</p>
              <span className="filter-count-chip">{activeFilterCount > 0 ? `${activeFilterCount} active` : "No filters"}</span>
            </div>
            <div className="matches-filter-actions">
              <button type="button" className="button button-secondary" onClick={resetFilters}>
                Reset
              </button>
            </div>
          </div>

          <div className="filter-stack">
          <div className="filter-saved-block">
            <div className="filter-saved-head">
              <strong>Saved searches</strong>
              {user && <span className="filter-count-chip">{savedSearches.length}</span>}
            </div>
            {!user ? (
              <p className="form-note">Log in to save and reuse your favorite filter sets.</p>
            ) : (
              <>
                {loadingSavedSearches ? (
                  <p className="form-note">Loading saved searches...</p>
                ) : savedSearches.length === 0 ? (
                  <p className="form-note">No saved searches yet.</p>
                ) : (
                  <div className="saved-search-list">
                    {savedSearches.map((item) => (
                      <div key={item.id} className="saved-search-item">
                        <button type="button" className="saved-search-apply" onClick={() => applySavedSearch(item)}>
                          {item.name}
                        </button>
                        <button type="button" className="saved-search-delete" onClick={() => handleDeleteSavedSearch(item.id)}>
                          Remove
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                <div className="saved-search-form">
                  <input
                    className="form-input"
                    value={savedSearchName}
                    onChange={(event) => setSavedSearchName(event.target.value)}
                    placeholder="Name this search"
                  />
                  <button type="button" className="button button-primary" onClick={handleSaveSearch} disabled={savingSearch}>
                    {savingSearch ? "Saving..." : "Save"}
                  </button>
                </div>
                {savedSearchError && <p className="form-error" role="alert">{savedSearchError}</p>}
              </>
            )}
          </div>

          <div>
            <label className="form-label">Search keyword</label>
            <input className="form-input" value={filters.query} onChange={(event) => onFilterChange("query", event.target.value)} placeholder="Name, city, caste, profession" />
          </div>

          <div className="filter-grid-2">
            <div>
              <label className="form-label">Min age</label>
              <input className="form-input" type="number" min={18} max={70} value={filters.minAge} onChange={(event) => onFilterChange("minAge", clampNumber(event.target.value, 18, 70))} />
            </div>
            <div>
              <label className="form-label">Max age</label>
              <input className="form-input" type="number" min={18} max={70} value={filters.maxAge} onChange={(event) => onFilterChange("maxAge", clampNumber(event.target.value, 18, 70))} />
            </div>
          </div>

          <div>
            <label className="form-label">Minimum compatibility</label>
            <select className="form-input" value={filters.minMatch} onChange={(event) => onFilterChange("minMatch", clampNumber(event.target.value, 0, 100))}>
              <option value={0}>Any score</option>
              <option value={70}>70% and above</option>
              <option value={80}>80% and above</option>
              <option value={90}>90% and above</option>
            </select>
          </div>

          <div className="filter-toggle-row">
            <label className="filter-toggle">
              <input type="checkbox" checked={filters.verifiedOnly} onChange={(event) => onFilterChange("verifiedOnly", event.target.checked)} />
              <span>Verified only</span>
            </label>
            <label className="filter-toggle">
              <input type="checkbox" checked={filters.shortlistedOnly} onChange={(event) => onFilterChange("shortlistedOnly", event.target.checked)} />
              <span>Saved only</span>
            </label>
          </div>

          <div>
            <label className="form-label">Religion</label>
            <select className="form-input" value={filters.religion} onChange={(event) => onFilterChange("religion", event.target.value)}>
              {filterMeta.religion.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <div className="filter-grid-2">
            <div>
              <label className="form-label">Caste</label>
              <select className="form-input" value={filters.caste} onChange={(event) => onFilterChange("caste", event.target.value)}>
                {filterMeta.caste.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="form-label">Sub-caste</label>
              <select className="form-input" value={filters.subCaste} onChange={(event) => onFilterChange("subCaste", event.target.value)}>
                {subcasteOptions.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="filter-grid-2">
            <div>
              <label className="form-label">Country</label>
              <select className="form-input" value={filters.country} onChange={(event) => onFilterChange("country", event.target.value)}>
                {countryOptions.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="form-label">State</label>
              <select className="form-input" value={filters.state} onChange={(event) => onFilterChange("state", event.target.value)}>
                {stateOptions.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="filter-grid-2">
            <div>
              <label className="form-label">District</label>
              <select className="form-input" value={filters.district} onChange={(event) => onFilterChange("district", event.target.value)}>
                {districtOptions.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="form-label">City</label>
              <select className="form-input" value={filters.city} onChange={(event) => onFilterChange("city", event.target.value)}>
                {cityOptions.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="form-label">Profession</label>
            <select className="form-input" value={filters.profession} onChange={(event) => onFilterChange("profession", event.target.value)}>
              {filterMeta.profession.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>

          <details className="filter-disclosure">
            <summary className="filter-disclosure-summary">Advanced filters</summary>
            <div className="filter-disclosure-body">
              <div>
                <label className="form-label">Education</label>
                <select className="form-input" value={filters.education} onChange={(event) => onFilterChange("education", event.target.value)}>
                  {filterMeta.education.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="form-label">Income</label>
                <select className="form-input" value={filters.income} onChange={(event) => onFilterChange("income", event.target.value)}>
                  {filterMeta.income.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>
              <div className="filter-grid-2">
                <div>
                  <label className="form-label">Min height (cm)</label>
                  <input className="form-input" type="number" min={120} max={250} value={filters.minHeight} onChange={(event) => onFilterChange("minHeight", clampNumber(event.target.value, 120, 250))} />
                </div>
                <div>
                  <label className="form-label">Max height (cm)</label>
                  <input className="form-input" type="number" min={120} max={250} value={filters.maxHeight} onChange={(event) => onFilterChange("maxHeight", clampNumber(event.target.value, 120, 250))} />
                </div>
              </div>
              <div>
                <label className="form-label">Marital status</label>
                <select className="form-input" value={filters.maritalStatus} onChange={(event) => onFilterChange("maritalStatus", event.target.value)}>
                  {filterMeta.maritalStatus.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="form-label">Mother tongue</label>
                <select className="form-input" value={filters.motherTongue} onChange={(event) => onFilterChange("motherTongue", event.target.value)}>
                  {filterMeta.motherTongue.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="form-label">Diet</label>
                <select className="form-input" value={filters.diet} onChange={(event) => onFilterChange("diet", event.target.value)}>
                  {DIETS.map((value) => (
                    <option key={value}>{value}</option>
                  ))}
                </select>
              </div>

              <div className="filter-grid-2">
                <div>
                  <label className="form-label">Have children</label>
                  <select className="form-input" value={filters.hasChildren} onChange={(event) => onFilterChange("hasChildren", event.target.value)}>
                    {HAS_CHILDREN_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="form-label">Residential status</label>
                  <select className="form-input" value={filters.residentialStatus} onChange={(event) => onFilterChange("residentialStatus", event.target.value)}>
                    {RESIDENTIAL_STATUS_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="filter-toggle-row">
                <label className="filter-toggle">
                  <input type="checkbox" checked={filters.withPhotoOnly} onChange={(event) => onFilterChange("withPhotoOnly", event.target.checked)} />
                  <span>With photo</span>
                </label>
                <label className="filter-toggle">
                  <input type="checkbox" checked={filters.premiumOnly} onChange={(event) => onFilterChange("premiumOnly", event.target.checked)} />
                  <span>Premium only</span>
                </label>
                <label className="filter-toggle">
                  <input type="checkbox" checked={filters.onlineNow} onChange={(event) => onFilterChange("onlineNow", event.target.checked)} />
                  <span>Online now</span>
                </label>
                <label className="filter-toggle">
                  <input type="checkbox" checked={filters.withHoroscopeOnly} onChange={(event) => onFilterChange("withHoroscopeOnly", event.target.checked)} />
                  <span>Horoscope ready</span>
                </label>
              </div>

              <details className="filter-disclosure filter-disclosure-nested">
                <summary className="filter-disclosure-summary">Horoscope filters</summary>
                <div className="filter-disclosure-body">
                  <div>
                    <label className="form-label">Gothra</label>
                    <select className="form-input" value={filters.gothra} onChange={(event) => onFilterChange("gothra", event.target.value)}>
                      {GOTRAS.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Nakshatra</label>
                    <select className="form-input" value={filters.nakshatra} onChange={(event) => onFilterChange("nakshatra", event.target.value)}>
                      {NAKSHATRAS.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Rashi</label>
                    <select className="form-input" value={filters.rashi} onChange={(event) => onFilterChange("rashi", event.target.value)}>
                      {RASHIS.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="form-label">Dosha</label>
                    <select className="form-input" value={filters.dosha} onChange={(event) => onFilterChange("dosha", event.target.value)}>
                      {DOSHAS.map((value) => (
                        <option key={value}>{value}</option>
                      ))}
                    </select>
                  </div>
                </div>
              </details>
            </div>
          </details>
        </div>
        </aside>
      )}

      <section>
        <PageHero
          eyebrow="Personalized Feed"
          title="Matches for you"
          copy={loading ? "Finding your best matches..." : `${visibleProfiles.length} compatible profiles ready`}
          className="listing-hero matches-hero"
          actions={(
            <div className="hero-actions matches-hero-actions">
              <button
                type="button"
                className={`button toolbar-filter-btn ${showFilters ? "button-primary" : "button-secondary"}`}
                onClick={() => setShowFilters((previous) => !previous)}
                aria-label={showFilters ? "Hide filters" : "Show filters"}
                title={showFilters ? "Hide filters" : "Show filters"}
              >
                <span>Filters</span>
                <span className="toolbar-arrow" aria-hidden="true">
                  <MatchActionIcon kind={showFilters ? "chevronUp" : "chevronDown"} />
                </span>
                {activeFilterCount > 0 && <span className="toolbar-count">{activeFilterCount}</span>}
              </button>
              <div className="sort-control">
                <label className="form-label form-label-tight" htmlFor="sortBy">
                  Sort by
                </label>
                <select id="sortBy" className="form-input" value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
                  <option value="compatibility">Top Compatibility</option>
                  <option value="verified">Verified First</option>
                  <option value="recent">Recently Active</option>
                  <option value="ageAsc">Age: Low to High</option>
                  <option value="ageDesc">Age: High to Low</option>
                  <option value="matchAsc">Compatibility: Low to High</option>
                </select>
              </div>
              <button type="button" className={`button view-switch-btn icon-only-btn ${viewMode === "grid" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("grid")} aria-label="Grid view" title="Grid view">
                <MatchActionIcon kind="grid" />
              </button>
              <button type="button" className={`button view-switch-btn icon-only-btn ${viewMode === "list" ? "button-primary" : "button-secondary"}`} onClick={() => setViewMode("list")} aria-label="List view" title="List view">
                <MatchActionIcon kind="list" />
              </button>
              <button type="button" className="button button-secondary view-switch-btn icon-only-btn" onClick={handleRefresh} disabled={loading} aria-label="Refresh results" title="Refresh results">
                <MatchActionIcon kind="refresh" />
              </button>
            </div>
          )}
        >
          {!loading && (
            <div className="result-metrics">
              <span className="metric-chip metric-chip-highlight">{profileInsights.total} curated</span>
              <span className="metric-chip">{profileInsights.verified} verified</span>
              <span className="metric-chip">{profileInsights.highCompatibility} high compatibility</span>
              <span className="metric-chip">{profileInsights.averageCompatibility}% avg match</span>
              <span className="metric-chip">{shortlisted.size} saved</span>
              {isPreview && <span className="metric-chip">Preview mode</span>}
            </div>
          )}
          {!loading && dataUpdatedLabel && <p className="data-freshness">Updated {dataUpdatedLabel}</p>}
          {!loading && (
            <div className="quick-filter-row" aria-label="Quick filters">
              {quickFilters.map((item) => (
                <button
                  key={item.key}
                  type="button"
                  className={`quick-filter-chip ${item.active ? "active" : ""}`}
                  aria-pressed={item.active}
                  onClick={() => toggleQuickFilter(item.key)}
                >
                  {item.label}
                </button>
              ))}
              {activeFilterCount > 0 && (
                <button type="button" className="quick-filter-chip clear" onClick={resetFilters}>
                  Reset all
                </button>
              )}
            </div>
          )}
          {!loading && activeFilterTags.length > 0 && (
            <div className="active-filter-row">
              {activeFilterTags.map((tag) => (
                <button key={tag.key} type="button" className="active-filter-pill" onClick={() => clearFilter(tag.key)}>
                  <span>{tag.label}</span>
                  <span aria-hidden="true">x</span>
                </button>
              ))}
              <button type="button" className="active-filter-pill active-filter-pill-clear" onClick={resetFilters}>
                Clear all
              </button>
            </div>
          )}
        </PageHero>

        {isPreview && !loading && (
          <div className="status-banner warning matches-preview-banner">
            <span>Preview mode: log in to see live matches, save searches, and view verified contact access.</span>
            {!user && (
              <Link href="/login" className="button button-secondary">
                Login
              </Link>
            )}
          </div>
        )}

        {loading ? (
          <div className="matches-skeleton-grid">
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={`skeleton-${index}`} className="panel listing-stage skeleton-tile matches-skeleton-tile" />
            ))}
          </div>
        ) : visibleProfiles.length === 0 ? (
          <>
            <PageEmptyState
              title="No matches found with current filters"
              description="Try broadening caste or city filters, or disable horoscope filters."
              primaryActionLabel="Reset Filters"
              onPrimaryAction={resetFilters}
              secondaryActionLabel={!showFilters ? "Open Filters" : ""}
              onSecondaryAction={() => setShowFilters(true)}
            />
            {relaxSuggestions.length > 0 && (
              <div className="panel listing-stage matches-empty-suggestions matches-empty-panel">
                <p className="section-label">Quick adjustments</p>
                <p className="section-copy">
                  Loosen one of these filters to broaden your results.
                </p>
                <div className="quick-filter-row">
                  {relaxSuggestions.map((item) => (
                    <button
                      key={item.key}
                      type="button"
                      className="quick-filter-chip"
                      onClick={() => handleRelaxSuggestion(item.key)}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </>
        ) : (
          <div className={`results-grid matches-results-grid ${viewMode === "grid" ? "is-grid" : "is-list"}`}>
            {renderedProfiles.map((profile) => (
              <MatchCard key={profile.userId} profile={profile} isShortlisted={shortlisted.has(profile.userId)} onShortlist={onShortlist} onInterest={onInterest} onOpenProfile={onOpenProfile} viewMode={viewMode} />
            ))}
          </div>
        )}
        {!loading && hasMoreProfiles && (
          <div className="matches-load-more-row">
            <button type="button" className="button button-secondary" onClick={() => setVisibleCount((previous) => previous + batchSize)}>
              Load more profiles
            </button>
            <span>
              Showing {renderedProfiles.length} of {visibleProfiles.length}
            </span>
          </div>
        )}
      </section>

      <LoginPromptModal isOpen={showLoginModal} onClose={() => setShowLoginModal(false)} />

    </div>
  );
}

export default function MatchesPage() {
  return (
    <Suspense
      fallback={
        <PageLoadingState
          title="Loading matches..."
          description="Preparing curated matches and compatibility insights."
          compact
        />
      }
    >
      <MatchesContent />
    </Suspense>
  );
}
