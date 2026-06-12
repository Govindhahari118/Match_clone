# IMPL_04 — Communication System (Voice + Video + Chat Enhancements)
## Pin-to-Pin Implementation Plan

> **Gap:** Plans require in-app voice calling, video calling (both without revealing phone numbers), typing indicators, read receipts, message reactions, and scheduled video dates. Currently only basic text chat exists.  
> **Impact:** Communication is the core conversion funnel step (Match → Chat → Call → Meeting). Without calls, users leave the app to use WhatsApp and never convert to paid.  
> **Source Docs:** PLAN_01 CF-01→08, PLAN_03 Section 7, PLAN_05 Section 1.3

---

## DELIVERABLES

### New Files to Create

| # | File | Purpose |
|---|------|---------|
| 1 | `ui/call/VoiceCallScreen.kt` | Full-screen voice call UI (Agora/WebRTC) |
| 2 | `ui/call/VideoCallScreen.kt` | Full-screen video call UI with local/remote feeds |
| 3 | `ui/call/IncomingCallScreen.kt` | Incoming call notification overlay (accept/decline) |
| 4 | `ui/call/CallViewModel.kt` | Manages call state, RTC engine, token fetching |
| 5 | `data/call/AgoraTokenService.kt` | Fetches Agora RTC token from Cloud Function |
| 6 | `data/call/CallRepository.kt` | Firestore call signaling (create/accept/decline/end) |
| 7 | `domain/model/CallState.kt` | Sealed class: Idle/Ringing/Connecting/Connected/Ended |
| 8 | `ui/chat/TypingIndicator.kt` | Animated "..." dots composable |
| 9 | `ui/chat/ReadReceiptIcon.kt` | Single/double tick composable (sent/delivered/read) |
| 10 | `ui/chat/MessageReactionBar.kt` | Emoji reaction picker (long-press on message) |
| 11 | `ui/call/ScheduleCallSheet.kt` | Bottom sheet to pick date/time for video date |
| 12 | Cloud Function: `generateAgoraToken` | HTTPS callable → returns RTC token for channel |
| 13 | Cloud Function: `onCallCreated` | Sends push to recipient with call data |

### Files to Modify

| File | Change |
|------|--------|
| `ui/chat/ChatScreen.kt` | Add typing indicator, read receipts, reaction bar, call button |
| `ui/chat/ChatViewModel.kt` | Add typing state broadcast, read receipt tracking |
| `data/repository/ChatRepository.kt` | Add typing presence + read receipt write + reactions |
| `MatchDetailScreen.kt` | Add "Voice Call" and "Video Call" action buttons |
| `functions/src/index.ts` | Add `generateAgoraToken` + `onCallCreated` |
| `app/build.gradle.kts` | Add Agora SDK dependency |
| `AndroidManifest.xml` | Add CAMERA, RECORD_AUDIO permissions for calls |

### Voice/Video Call Architecture

```
User A taps "Call" → Creates Firestore doc in `calls/{callId}`:
  { fromUid, toUid, type: "voice"|"video", status: "ringing", channelId, createdAt }

Cloud Function `onCallCreated` → Sends FCM high-priority push to User B

User B sees IncomingCallScreen → Taps "Accept":
  Updates `calls/{callId}.status = "connected"`
  Both users call `generateAgoraToken` Cloud Function
  Both join Agora channel with their RTC tokens

Either user taps "End" → Updates status = "ended", leaves channel
30-second timeout → Auto-decline if not answered
```

### Agora Integration

| Config | Value |
|--------|-------|
| SDK | `io.agora.rtc:full-sdk:4.3.0` |
| Channel naming | `call_{callId}` |
| Token generation | Cloud Function using Agora App Certificate |
| Roles | Both users join as BROADCASTER |
| Video resolution | 720p for video calls |
| Audio codec | Opus |

### Cloud Function: generateAgoraToken

```typescript
export const generateAgoraToken = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "");
  
  const { channelId, role } = data;
  const appId = functions.config().agora.app_id;
  const appCertificate = functions.config().agora.app_certificate;
  const uid = 0; // Use 0 for string UID mode
  const expireTime = 3600; // 1 hour token validity
  
  // Use agora-access-token package
  const { RtcTokenBuilder, RtcRole } = require("agora-access-token");
  const token = RtcTokenBuilder.buildTokenWithUid(
    appId, appCertificate, channelId, uid,
    role === "publisher" ? RtcRole.PUBLISHER : RtcRole.SUBSCRIBER,
    Math.floor(Date.now() / 1000) + expireTime
  );
  
  return { token, appId, channelId };
});
```

### Typing Indicator

```
Firestore path: chats/{threadId}/presence/{uid}
Schema: { isTyping: boolean, updatedAt: timestamp }

Rules:
- Set isTyping = true when user starts typing (debounced 500ms)
- Set isTyping = false when user stops typing for 3 seconds
- Recipient observes this doc in real-time
- Auto-clear after 10 seconds (stale detection)
```

### Read Receipts

```
Message doc enhancement:
  sentAt: Timestamp (sender creates)
  deliveredAt: Timestamp (recipient app opens chat)
  readAt: Timestamp (message scrolled into viewport)

Display:
  sentAt only → Single grey tick ✓
  deliveredAt set → Double grey ticks ✓✓
  readAt set → Double blue ticks ✓✓
```

### Subscription Gating

| Feature | Minimum Plan |
|---------|-------------|
| Text chat (post-accept) | FREE |
| Typing indicator | FREE |
| Read receipts | STANDARD |
| Voice call | STANDARD |
| Video call | PREMIUM |
| Scheduled video date | PREMIUM |
| Message reactions | FREE |

### Permissions

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

---

## DEFINITION OF DONE

- [ ] Voice call connects between 2 users (Agora channel join works)
- [ ] Video call shows local + remote camera feeds
- [ ] Incoming call shows overlay with accept/decline
- [ ] 30-second auto-decline timeout
- [ ] Typing indicator shows in real-time
- [ ] Read receipts show correct tick state
- [ ] Call button gated by subscription (STANDARD+ for voice, PREMIUM+ for video)
- [ ] Call history stored in Firestore `calls` collection
- [ ] Push notification for incoming call is high-priority with sound
- [ ] BUILD SUCCESSFUL
