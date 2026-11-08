# MATE CHAT – END-TO-END ENCRYPTION (E2EE) SPECIFICATION

## Cryptographic Design & Guarantees

MATE CHAT implements strict Zero-Knowledge End-to-End Encryption (E2EE). The server functions solely as an encrypted blind router and ciphertext repository.

### Core Cryptographic Invariants
1. **Zero Server Knowledge:** Plaintext messages, private identity keys, and media decryption keys are NEVER transmitted to or stored on the server.
2. **Hardware Protection:** On Android, private keys are generated with `SecureRandom` and protected via the Android Keystore hardware-backed security module (StrongBox / TEE).
3. **Cross-Platform Interoperability:** Android (Kotlin) and Website (React/WebCrypto) use identical NIST P-256 (secp256r1) Elliptic Curve Diffie-Hellman (ECDH) key agreements and AES-256-GCM symmetric authenticated encryption.
4. **Authenticity & Integrity:** Every message includes a 12-byte cryptographically secure initialization vector (IV) and a 128-bit authentication tag. Tampering with even 1 bit of ciphertext causes immediate decryption failure.
5. **Safety Numbers:** Both peers can verify out-of-band authenticity through a 60-digit numerical fingerprint formatted in 12 blocks of 5 digits:
   ```
   38942 91048 20194 88392 01928 47291 99201 38491 02938 48192 39019 48291
   ```

---

## Message Encryption Flow

```
[Sender Device (Alice)]
    │
    ├─ 1. Retrieve Bob's Public Key Bundle (Identity Key + Signed Prekey)
    ├─ 2. Compute Shared Secret via ECDH: S = ECDH(Alice_Private, Bob_Public)
    ├─ 3. Derive 256-bit symmetric AES key via SHA-256 KDF: K = SHA256(S)
    ├─ 4. Generate random 12-byte IV
    ├─ 5. Encrypt with AES-256-GCM: (Ciphertext, AuthTag) = AES_GCM_Encrypt(Plaintext, K, IV)
    │
    ▼ Transmit (Ciphertext, IV, AuthTag, Alice_Public_Key)
[MATE CHAT Server / WebSocket Relay] (Zero-Knowledge, blind forwarding)
    │
    ▼ Deliver (Ciphertext, IV, AuthTag, Alice_Public_Key)
[Recipient Device (Bob)]
    │
    ├─ 1. Compute Shared Secret via ECDH: S = ECDH(Bob_Private, Alice_Public)
    ├─ 2. Derive 256-bit symmetric AES key: K = SHA256(S)
    ├─ 3. Decrypt with AES-256-GCM: Plaintext = AES_GCM_Decrypt(Ciphertext, K, IV, AuthTag)
    └─ 4. Render Plaintext in UI
```

---

## Media & Voice Message Encryption

Attachments (photos, videos, audio voice notes, documents) are encrypted locally using an ephemeral 256-bit AES key prior to upload:
- The encrypted binary is uploaded to secure storage.
- The decryption key and IV are encrypted inside the primary E2EE message payload.
- Only the conversation recipients can decrypt the media.

---

## Administrator Privacy Rule
Admin panel administrators and moderators CANNOT decrypt private chats under any circumstances. If a user voluntarily reports abuse or harassment, the user's client voluntarily decrypts only the specific message snippet and submits it as human-readable report evidence with explicit user consent.
