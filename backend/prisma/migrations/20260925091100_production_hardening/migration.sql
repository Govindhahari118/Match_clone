-- MATREE production hardening: OTP, authoritative blocks, reliable message delivery.
-- Additive/backfill-first migration to preserve existing rows.

ALTER TABLE "User"
  ADD COLUMN IF NOT EXISTS "otpCodeHash" TEXT,
  ADD COLUMN IF NOT EXISTS "otpExpiresAt" TIMESTAMP(3),
  ADD COLUMN IF NOT EXISTS "otpAttempts" INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS "otpLastSentAt" TIMESTAMP(3),
  ADD COLUMN IF NOT EXISTS "otpPurpose" TEXT;

ALTER TABLE "Message"
  ADD COLUMN IF NOT EXISTS "receiverId" UUID,
  ADD COLUMN IF NOT EXISTS "clientMessageId" TEXT,
  ADD COLUMN IF NOT EXISTS "status" TEXT NOT NULL DEFAULT 'sent',
  ADD COLUMN IF NOT EXISTS "deliveredAt" TIMESTAMP(3),
  ADD COLUMN IF NOT EXISTS "readAt" TIMESTAMP(3);

-- Existing messages can be backfilled from their canonical Match participants.
UPDATE "Message" AS msg
SET "receiverId" = CASE
  WHEN msg."senderId" = mat."userAId" THEN mat."userBId"
  ELSE mat."userAId"
END
FROM "Match" AS mat
WHERE msg."matchId" = mat."id"
  AND msg."receiverId" IS NULL;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM "Message" WHERE "receiverId" IS NULL) THEN
    RAISE EXCEPTION 'Cannot make Message.receiverId required: legacy rows could not be backfilled';
  END IF;
END $$;

ALTER TABLE "Message" ALTER COLUMN "receiverId" SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS "Message_senderId_clientMessageId_key"
  ON "Message"("senderId", "clientMessageId");
CREATE INDEX IF NOT EXISTS "Message_receiverId_idx" ON "Message"("receiverId");
CREATE INDEX IF NOT EXISTS "Message_status_idx" ON "Message"("status");

CREATE TABLE IF NOT EXISTS "Block" (
  "id" UUID NOT NULL DEFAULT gen_random_uuid(),
  "blockerId" UUID NOT NULL,
  "blockedId" UUID NOT NULL,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "Block_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS "Block_blockerId_blockedId_key"
  ON "Block"("blockerId", "blockedId");
CREATE INDEX IF NOT EXISTS "Block_blockerId_idx" ON "Block"("blockerId");
CREATE INDEX IF NOT EXISTS "Block_blockedId_idx" ON "Block"("blockedId");

-- Self-blocks are never valid.
ALTER TABLE "Block"
  DROP CONSTRAINT IF EXISTS "Block_no_self_block";
ALTER TABLE "Block"
  ADD CONSTRAINT "Block_no_self_block" CHECK ("blockerId" <> "blockedId");

-- Clear legacy plaintext OTP material. The application no longer reads it.
UPDATE "User" SET "verificationCode" = NULL WHERE "verificationCode" IS NOT NULL;
