export const RASIS = [
  "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
  "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces",
] as const;

export const NAKSHATRAS = [
  "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra", "Punarvasu",
  "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta",
  "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha",
  "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
  "Uttara Bhadrapada", "Revati",
] as const;

type Element = "FIRE" | "EARTH" | "AIR" | "WATER";

const RASI_ELEMENT: Record<string, Element> = {
  Aries: "FIRE", Taurus: "EARTH", Gemini: "AIR", Cancer: "WATER",
  Leo: "FIRE", Virgo: "EARTH", Libra: "AIR", Scorpio: "WATER",
  Sagittarius: "FIRE", Capricorn: "EARTH", Aquarius: "AIR", Pisces: "WATER",
};

const GANA: Record<string, "Deva" | "Manushya" | "Rakshasa"> = {
  Ashwini: "Deva", Mrigashira: "Deva", Punarvasu: "Deva", Pushya: "Deva",
  Hasta: "Deva", Swati: "Deva", Anuradha: "Deva", Shravana: "Deva", Revati: "Deva",
  Bharani: "Manushya", Rohini: "Manushya", Ardra: "Manushya",
  "Purva Phalguni": "Manushya", "Uttara Phalguni": "Manushya",
  "Purva Ashadha": "Manushya", "Uttara Ashadha": "Manushya",
  "Purva Bhadrapada": "Manushya", "Uttara Bhadrapada": "Manushya",
  Krittika: "Rakshasa", Ashlesha: "Rakshasa", Magha: "Rakshasa",
  Chitra: "Rakshasa", Vishakha: "Rakshasa", Jyeshtha: "Rakshasa",
  Mula: "Rakshasa", Dhanishta: "Rakshasa", Shatabhisha: "Rakshasa",
};

function elementScore(a: Element, b: Element): number {
  if (a === b) return 1;
  if ((a === "FIRE" && b === "AIR") || (a === "AIR" && b === "FIRE")) return 0.85;
  if ((a === "EARTH" && b === "WATER") || (a === "WATER" && b === "EARTH")) return 0.85;
  if ((a === "FIRE" && b === "WATER") || (a === "WATER" && b === "FIRE")) return 0.25;
  if ((a === "EARTH" && b === "AIR") || (a === "AIR" && b === "EARTH")) return 0.35;
  return 0.55;
}

export type HoroscopeCompatibility = {
  score: number;
  formulaVersion: "RASI_NAKSHATRA_2026_09_V1";
};

export function horoscopeCompatibility(
  rasiA: unknown,
  nakshatraA: unknown,
  rasiB: unknown,
  nakshatraB: unknown
): HoroscopeCompatibility | null {
  if (
    typeof rasiA !== "string" ||
    typeof nakshatraA !== "string" ||
    typeof rasiB !== "string" ||
    typeof nakshatraB !== "string"
  ) return null;

  const rA = RASIS.indexOf(rasiA as typeof RASIS[number]);
  const rB = RASIS.indexOf(rasiB as typeof RASIS[number]);
  const nA = NAKSHATRAS.indexOf(nakshatraA as typeof NAKSHATRAS[number]);
  const nB = NAKSHATRAS.indexOf(nakshatraB as typeof NAKSHATRAS[number]);
  if (rA < 0 || rB < 0 || nA < 0 || nB < 0) return null;

  if (rA === rB && nA === nB) {
    return { score: 1, formulaVersion: "RASI_NAKSHATRA_2026_09_V1" };
  }

  let points = 0;
  const diff9 = (nB - nA + 27) % 9;
  if ([2, 4, 6, 8, 0].includes(diff9)) points += 3;

  const ganaA = GANA[nakshatraA];
  const ganaB = GANA[nakshatraB];
  if (ganaA === ganaB) points += 6;
  else if (
    (ganaA === "Deva" && ganaB === "Manushya") ||
    (ganaB === "Deva" && ganaA === "Manushya")
  ) points += 4;

  const nakshatraDistance = (nB - nA + 27) % 27;
  if ([4, 7, 10, 13, 16, 19, 22, 25].includes(nakshatraDistance)) points += 1;
  if (nakshatraDistance > 13) points += 1;
  points += nA % 4 === nB % 4 ? 4 : 2;

  const rasiDistance = (rB - rA + 12) % 12;
  if ([0, 1, 6, 7, 8, 9, 10, 11].includes(rasiDistance)) points += 7;
  points += elementScore(RASI_ELEMENT[rasiA], RASI_ELEMENT[rasiB]) * 5;

  if (rasiDistance === 6) points += 2;
  if (nA % 5 !== nB % 5) points += 5;
  if (nA % 3 !== nB % 3) points += 2;

  return {
    score: Math.max(0, Math.min(1, points / 36)),
    formulaVersion: "RASI_NAKSHATRA_2026_09_V1",
  };
}
