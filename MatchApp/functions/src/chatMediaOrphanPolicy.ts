export const CHAT_MEDIA_ORPHAN_TTL_MS = 7 * 24 * 60 * 60 * 1000;

export type ChatMediaExtension = "jpg" | "m4a";

export interface ParsedChatMediaPath {
  threadId: string;
  messageId: string;
  extension: ChatMediaExtension;
}

const CHAT_MEDIA_PATH =
  /^chat-media\/([a-f0-9]{64})\/([A-Za-z0-9_-]{16,128})\.(jpg|m4a)$/;

export function parseChatMediaPath(value: unknown): ParsedChatMediaPath | null {
  const storagePath = typeof value === "string" ? value.trim() : "";
  const match = storagePath.match(CHAT_MEDIA_PATH);
  if (!match) return null;
  return {
    threadId: match[1],
    messageId: match[2],
    extension: match[3] as ChatMediaExtension,
  };
}

export function chatMediaOrphanId(media: ParsedChatMediaPath): string {
  return `${media.threadId}_${media.messageId}_${media.extension}`;
}
