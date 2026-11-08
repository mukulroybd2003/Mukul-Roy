# MATE CHAT – REST API & WEBSOCKET SPECIFICATION (v1)

## Overview
Base URL: `https://api.matechat.app/api/v1`  
WebSocket URL: `wss://api.matechat.app/ws`  
Authentication: HTTP Bearer Token (`Authorization: Bearer <session_token>`)  
Security Model: Zero-Knowledge End-to-End Encryption for all message payloads.

---

## 1. Authentication Endpoints

### POST `/auth/register`
Creates user, registers primary device, and returns active session.
**Request:**
```json
{
  "username": "sophia_code",
  "email": "sophia@matechat.app",
  "password": "StrongPassword123!",
  "full_name": "Sophia Zhang",
  "device_name": "Pixel 9 Pro",
  "platform": "android"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "user": {
    "id": 2,
    "username": "sophia_code",
    "email": "sophia@matechat.app",
    "full_name": "Sophia Zhang"
  },
  "device": {
    "id": 14,
    "uuid": "4f9d2a1b-...",
    "name": "Pixel 9 Pro"
  },
  "session_token": "a1b2c3d4..."
}
```

### POST `/auth/login`
Authenticates user, registers or updates device session.
**Request:**
```json
{
  "username": "sophia_code",
  "password": "StrongPassword123!",
  "device_name": "Pixel 9 Pro",
  "platform": "android"
}
```

### POST `/auth/logout`
Revokes active session token.

---

## 2. E2EE Cryptographic Key Management

### POST `/keys/bundle`
Publishes user's device public identity key, signed prekey, signature, and one-time prekeys.
*(Private keys are never sent to or accepted by the server).*
**Request:**
```json
{
  "identity_key": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...",
  "signed_prekey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...",
  "signature": "3045022100...",
  "one_time_prekeys": [
    { "key_id": 1, "public_key": "MFkwEwYHKoZIzj0CAQY..." }
  ]
}
```

### GET `/keys/bundle/{userId}`
Retrieves a target user's public key bundle for establishing a new encrypted session.

---

## 3. Conversations & Messaging

### GET `/conversations`
Returns list of direct and group conversations with peer profiles and unread counts.

### POST `/conversations/direct`
Creates or retrieves existing direct conversation with a peer.
```json
{ "peer_user_id": 2 }
```

### GET `/conversations/{id}/messages`
Retrieves encrypted messages with pagination (`?before_id={id}&limit=50`).

### POST `/messages`
Transmits an end-to-end encrypted message ciphertext.
```json
{
  "conversation_id": 1,
  "encrypted_payload": "q8f8z7x...",
  "iv": "dGVzdF9pdg==",
  "auth_tag": "YXV0aF90YWc=",
  "key_fingerprint": "A1B2C3D4E5F67890",
  "message_type": "text"
}
```

---

## 4. Real-Time WebSocket Protocol (`/ws`)

### Connection Authentication
```json
{
  "action": "auth",
  "user_id": 1,
  "device_id": 14,
  "session_token": "a1b2c3d4..."
}
```

### WebRTC Call Signaling (Audio & Video)
```json
{
  "action": "webrtc_signal",
  "target_user_id": 2,
  "signal_type": "offer",
  "channel": "call_channel_109",
  "call_type": "video",
  "payload": { "sdp": "..." }
}
```

### Typing Indicators
```json
{
  "action": "typing",
  "conversation_id": 1,
  "recipient_id": 2,
  "is_typing": true
}
```

---

## 5. Administration Endpoints

### GET `/admin/stats`
Returns system health, total active users, message counts, and pending moderation items.

### GET `/admin/users`
Lists system users with status, last login, and linked device counts.

### POST `/admin/users/{id}/status`
Suspends or restores user accounts.
