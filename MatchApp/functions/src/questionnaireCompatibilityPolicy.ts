export const QUESTIONNAIRE_VECTOR_LENGTH = 50;
export const QUESTIONNAIRE_FORMULA_VERSION = "questionnaire-v1-bilateral-cosine";

function finiteVector(value: unknown): number[] | null {
  if (!Array.isArray(value) || value.length !== QUESTIONNAIRE_VECTOR_LENGTH) return null;
  const result: number[] = [];
  for (const item of value) {
    const n = Number(item);
    if (!Number.isFinite(n) || n < -1 || n > 1) return null;
    result.push(n);
  }
  return result;
}

function cosine(a: number[], b: number[]): number {
  if (a.length !== b.length || a.length === 0) return 0;
  let dot = 0;
  let normA = 0;
  let normB = 0;
  for (let index = 0; index < a.length; index += 1) {
    dot += a[index] * b[index];
    normA += a[index] * a[index];
    normB += b[index] * b[index];
  }
  if (normA <= 0 || normB <= 0) return 0;
  return Math.max(-1, Math.min(1, dot / (Math.sqrt(normA) * Math.sqrt(normB))));
}

export type QuestionnaireCompatibility = {
  score: number;
  formulaVersion: typeof QUESTIONNAIRE_FORMULA_VERSION;
};

export function sanitizeQuestionnaireVectors(
  selfVector: unknown,
  partnerVector: unknown
): { selfVector: number[]; partnerVector: number[] } | null {
  const self = finiteVector(selfVector);
  const partner = finiteVector(partnerVector);
  if (!self || !partner) return null;
  return { selfVector: self, partnerVector: partner };
}

/**
 * Bilateral values compatibility:
 * - A's desired-partner vector vs B's self vector
 * - B's desired-partner vector vs A's self vector
 * Each cosine is mapped from [-1,1] to [0,1], then averaged.
 */
export function questionnaireCompatibility(
  aSelfValue: unknown,
  aPartnerValue: unknown,
  bSelfValue: unknown,
  bPartnerValue: unknown
): QuestionnaireCompatibility | null {
  const a = sanitizeQuestionnaireVectors(aSelfValue, aPartnerValue);
  const b = sanitizeQuestionnaireVectors(bSelfValue, bPartnerValue);
  if (!a || !b) return null;

  const aToB = (cosine(a.partnerVector, b.selfVector) + 1) / 2;
  const bToA = (cosine(b.partnerVector, a.selfVector) + 1) / 2;
  return {
    score: Math.max(0, Math.min(1, (aToB + bToA) / 2)),
    formulaVersion: QUESTIONNAIRE_FORMULA_VERSION,
  };
}
