-- Matrimony trust/freshness hardening (additive, backward-compatible)
-- Safe to apply after the current initial schema.

ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "lastActiveAt" TIMESTAMP(3);
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "statusReconfirmedAt" TIMESTAMP(3);
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "searchStatus" TEXT NOT NULL DEFAULT 'active';
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "profileVersion" INTEGER NOT NULL DEFAULT 1;
ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "deletedAt" TIMESTAMP(3);

ALTER TABLE "Profile" ADD COLUMN IF NOT EXISTS "managerRelationship" TEXT;

ALTER TABLE "PartnerPreference" ADD COLUMN IF NOT EXISTS "hardFields" TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];

ALTER TABLE "Photo" ADD COLUMN IF NOT EXISTS "verificationStatus" TEXT NOT NULL DEFAULT 'not_started';
ALTER TABLE "Photo" ADD COLUMN IF NOT EXISTS "moderationStatus" TEXT NOT NULL DEFAULT 'approved';
ALTER TABLE "Photo" ADD COLUMN IF NOT EXISTS "rejectionReason" TEXT;
ALTER TABLE "Photo" ADD COLUMN IF NOT EXISTS "verifiedAt" TIMESTAMP(3);

ALTER TABLE "Message" ADD COLUMN IF NOT EXISTS "clientMessageId" TEXT;
ALTER TABLE "Message" ADD COLUMN IF NOT EXISTS "status" TEXT NOT NULL DEFAULT 'sent';
ALTER TABLE "Message" ADD COLUMN IF NOT EXISTS "deliveredAt" TIMESTAMP(3);
ALTER TABLE "Message" ADD COLUMN IF NOT EXISTS "readAt" TIMESTAMP(3);

ALTER TABLE "Subscription" ADD COLUMN IF NOT EXISTS "planVersion" INTEGER NOT NULL DEFAULT 1;
ALTER TABLE "Subscription" ADD COLUMN IF NOT EXISTS "renewalStatus" TEXT NOT NULL DEFAULT 'none';

ALTER TABLE "Payment" ADD COLUMN IF NOT EXISTS "gateway" TEXT NOT NULL DEFAULT 'razorpay';
ALTER TABLE "Payment" ADD COLUMN IF NOT EXISTS "idempotencyKey" TEXT;
ALTER TABLE "Payment" ADD COLUMN IF NOT EXISTS "failureReason" TEXT;

ALTER TABLE "Report" ADD COLUMN IF NOT EXISTS "severity" TEXT NOT NULL DEFAULT 'normal';
ALTER TABLE "Report" ADD COLUMN IF NOT EXISTS "resolvedAt" TIMESTAMP(3);

CREATE UNIQUE INDEX IF NOT EXISTS "Message_clientMessageId_key" ON "Message"("clientMessageId") WHERE "clientMessageId" IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS "Payment_idempotencyKey_key" ON "Payment"("idempotencyKey") WHERE "idempotencyKey" IS NOT NULL;
CREATE INDEX IF NOT EXISTS "User_searchStatus_idx" ON "User"("searchStatus");
CREATE INDEX IF NOT EXISTS "User_lastActiveAt_idx" ON "User"("lastActiveAt");
CREATE INDEX IF NOT EXISTS "Photo_verificationStatus_idx" ON "Photo"("verificationStatus");
CREATE INDEX IF NOT EXISTS "Photo_moderationStatus_idx" ON "Photo"("moderationStatus");
CREATE INDEX IF NOT EXISTS "Message_status_idx" ON "Message"("status");
CREATE INDEX IF NOT EXISTS "Report_severity_idx" ON "Report"("severity");

CREATE TABLE IF NOT EXISTS "UserBlock" (
  "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "blockerId" UUID NOT NULL,
  "blockedUserId" UUID NOT NULL,
  "reason" TEXT,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "UserBlock_blockerId_fkey" FOREIGN KEY ("blockerId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT "UserBlock_blockedUserId_fkey" FOREIGN KEY ("blockedUserId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT "UserBlock_not_self" CHECK ("blockerId" <> "blockedUserId")
);
CREATE UNIQUE INDEX IF NOT EXISTS "UserBlock_blockerId_blockedUserId_key" ON "UserBlock"("blockerId", "blockedUserId");
CREATE INDEX IF NOT EXISTS "UserBlock_blockerId_idx" ON "UserBlock"("blockerId");
CREATE INDEX IF NOT EXISTS "UserBlock_blockedUserId_idx" ON "UserBlock"("blockedUserId");

CREATE TABLE IF NOT EXISTS "ProfileExposure" (
  "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "viewerId" UUID NOT NULL,
  "candidateId" UUID NOT NULL,
  "firstSeenAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "lastSeenAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "impressionCount" INTEGER NOT NULL DEFAULT 1,
  "interactionState" TEXT NOT NULL DEFAULT 'seen',
  "cooldownUntil" TIMESTAMP(3),
  CONSTRAINT "ProfileExposure_viewerId_fkey" FOREIGN KEY ("viewerId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT "ProfileExposure_candidateId_fkey" FOREIGN KEY ("candidateId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT "ProfileExposure_not_self" CHECK ("viewerId" <> "candidateId")
);
CREATE UNIQUE INDEX IF NOT EXISTS "ProfileExposure_viewerId_candidateId_key" ON "ProfileExposure"("viewerId", "candidateId");
CREATE INDEX IF NOT EXISTS "ProfileExposure_viewerId_interactionState_idx" ON "ProfileExposure"("viewerId", "interactionState");
CREATE INDEX IF NOT EXISTS "ProfileExposure_viewerId_cooldownUntil_idx" ON "ProfileExposure"("viewerId", "cooldownUntil");

CREATE TABLE IF NOT EXISTS "ContactRequest" (
  "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "requesterId" UUID NOT NULL,
  "targetId" UUID NOT NULL,
  "status" TEXT NOT NULL DEFAULT 'pending',
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "ContactRequest_requesterId_fkey" FOREIGN KEY ("requesterId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT "ContactRequest_targetId_fkey" FOREIGN KEY ("targetId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT "ContactRequest_not_self" CHECK ("requesterId" <> "targetId")
);
CREATE UNIQUE INDEX IF NOT EXISTS "ContactRequest_requesterId_targetId_key" ON "ContactRequest"("requesterId", "targetId");
CREATE INDEX IF NOT EXISTS "ContactRequest_targetId_status_idx" ON "ContactRequest"("targetId", "status");

CREATE TABLE IF NOT EXISTS "PhotoAccessRequest" (
  "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "requesterId" UUID NOT NULL,
  "targetId" UUID NOT NULL,
  "status" TEXT NOT NULL DEFAULT 'pending',
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "PhotoAccessRequest_requesterId_fkey" FOREIGN KEY ("requesterId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT "PhotoAccessRequest_targetId_fkey" FOREIGN KEY ("targetId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT "PhotoAccessRequest_not_self" CHECK ("requesterId" <> "targetId")
);
CREATE UNIQUE INDEX IF NOT EXISTS "PhotoAccessRequest_requesterId_targetId_key" ON "PhotoAccessRequest"("requesterId", "targetId");
CREATE INDEX IF NOT EXISTS "PhotoAccessRequest_targetId_status_idx" ON "PhotoAccessRequest"("targetId", "status");

CREATE TABLE IF NOT EXISTS "SupportTicket" (
  "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "userId" UUID NOT NULL,
  "category" TEXT NOT NULL,
  "subject" TEXT NOT NULL,
  "description" TEXT NOT NULL,
  "priority" TEXT NOT NULL DEFAULT 'normal',
  "status" TEXT NOT NULL DEFAULT 'open',
  "assignedTeam" TEXT,
  "resolution" TEXT,
  "resolvedAt" TIMESTAMP(3),
  "closedAt" TIMESTAMP(3),
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "SupportTicket_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS "SupportTicket_userId_status_idx" ON "SupportTicket"("userId", "status");
CREATE INDEX IF NOT EXISTS "SupportTicket_priority_status_idx" ON "SupportTicket"("priority", "status");
CREATE INDEX IF NOT EXISTS "SupportTicket_createdAt_idx" ON "SupportTicket"("createdAt" DESC);

CREATE TABLE IF NOT EXISTS "SupportEvent" (
  "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "ticketId" UUID NOT NULL,
  "actorType" TEXT NOT NULL,
  "actorId" TEXT,
  "eventType" TEXT NOT NULL,
  "message" TEXT,
  "metadata" JSONB,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "SupportEvent_ticketId_fkey" FOREIGN KEY ("ticketId") REFERENCES "SupportTicket"("id") ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS "SupportEvent_ticketId_createdAt_idx" ON "SupportEvent"("ticketId", "createdAt");

CREATE TABLE IF NOT EXISTS "RiskSignal" (
  "id" UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  "userId" UUID NOT NULL,
  "signalType" TEXT NOT NULL,
  "severity" TEXT NOT NULL DEFAULT 'low',
  "source" TEXT NOT NULL,
  "metadata" JSONB,
  "resolvedAt" TIMESTAMP(3),
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "RiskSignal_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX IF NOT EXISTS "RiskSignal_userId_createdAt_idx" ON "RiskSignal"("userId", "createdAt" DESC);
CREATE INDEX IF NOT EXISTS "RiskSignal_signalType_idx" ON "RiskSignal"("signalType");
CREATE INDEX IF NOT EXISTS "RiskSignal_severity_idx" ON "RiskSignal"("severity");
