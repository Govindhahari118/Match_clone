import { CallKind } from "./callSessionPolicy";

export type ProviderSessionRequest = {
  sessionId: string;
  callerUid: string;
  targetUid: string;
  kind: CallKind;
};

export type ProviderSessionAllocation = {
  providerSessionId: string;
  clientToken: string;
  expiresAtMs: number;
  numberMasking: boolean;
};

export interface CommunicationProvider {
  readonly ready: boolean;
  readonly voiceAvailable: boolean;
  readonly videoAvailable: boolean;
  readonly numberMaskingAvailable: boolean;

  allocateSession(request: ProviderSessionRequest): Promise<ProviderSessionAllocation>;
}

/**
 * Production-safe default. Replacing this adapter requires provider-specific secret management,
 * callback verification, fraud controls and physical-device acceptance.
 */
class DisabledCommunicationProvider implements CommunicationProvider {
  readonly ready = false;
  readonly voiceAvailable = false;
  readonly videoAvailable = false;
  readonly numberMaskingAvailable = false;

  async allocateSession(): Promise<ProviderSessionAllocation> {
    throw new Error("communication-provider-not-configured");
  }
}

export const communicationProvider: CommunicationProvider =
  new DisabledCommunicationProvider();
