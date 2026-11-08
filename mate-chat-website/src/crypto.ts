// MATE CHAT - WEB CRYPTO E2EE ENGINE
// Implements browser-based ECDH (P-256) key agreement + AES-GCM-256 message encryption

export interface E2EEKeyPair {
  publicKey: CryptoKey;
  privateKey: CryptoKey;
  publicKeyBase64: string;
}

export class WebCryptoE2EE {
  private static keyPair: E2EEKeyPair | null = null;

  public static async generateOrLoadKeyPair(): Promise<E2EEKeyPair> {
    if (this.keyPair) return this.keyPair;

    const kp = await window.crypto.subtle.generateKey(
      { name: 'ECDH', namedCurve: 'P-256' },
      false, // private key non-extractable from memory
      ['deriveKey', 'deriveBits']
    );

    const exported = await window.crypto.subtle.exportKey('raw', kp.publicKey);
    const base64 = btoa(String.fromCharCode(...new Uint8Array(exported)));

    this.keyPair = {
      publicKey: kp.publicKey,
      privateKey: kp.privateKey,
      publicKeyBase64: base64
    };
    return this.keyPair;
  }

  public static async deriveSharedKey(peerPublicKeyBase64: string): Promise<CryptoKey> {
    const local = await this.generateOrLoadKeyPair();
    const binary = Uint8Array.from(atob(peerPublicKeyBase64), c => c.charCodeAt(0));
    const peerKey = await window.crypto.subtle.importKey(
      'raw',
      binary,
      { name: 'ECDH', namedCurve: 'P-256' },
      false,
      []
    );

    return await window.crypto.subtle.deriveKey(
      { name: 'ECDH', public: peerKey },
      local.privateKey,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt']
    );
  }

  public static async encrypt(plaintext: string, sharedKey: CryptoKey): Promise<{ ciphertext: string; iv: string; tag: string }> {
    const iv = window.crypto.getRandomValues(new Uint8Array(12));
    const encoded = new TextEncoder().encode(plaintext);

    const encrypted = await window.crypto.subtle.encrypt(
      { name: 'AES-GCM', iv },
      sharedKey,
      encoded
    );

    const encBytes = new Uint8Array(encrypted);
    // In AES-GCM the last 16 bytes are the auth tag
    const ciphertextBytes = encBytes.slice(0, -16);
    const tagBytes = encBytes.slice(-16);

    return {
      ciphertext: btoa(String.fromCharCode(...ciphertextBytes)),
      iv: btoa(String.fromCharCode(...iv)),
      tag: btoa(String.fromCharCode(...tagBytes))
    };
  }

  public static async decrypt(ciphertextBase64: string, ivBase64: string, tagBase64: string, sharedKey: CryptoKey): Promise<string> {
    const ct = Uint8Array.from(atob(ciphertextBase64), c => c.charCodeAt(0));
    const iv = Uint8Array.from(atob(ivBase64), c => c.charCodeAt(0));
    const tag = Uint8Array.from(atob(tagBase64), c => c.charCodeAt(0));

    // Combine ct + tag for SubtleCrypto decrypt
    const combined = new Uint8Array(ct.length + tag.length);
    combined.set(ct);
    combined.set(tag, ct.length);

    const decrypted = await window.crypto.subtle.decrypt(
      { name: 'AES-GCM', iv },
      sharedKey,
      combined
    );

    return new TextDecoder().decode(decrypted);
  }

  public static async calculateSafetyNumber(keyA: string, keyB: string): Promise<string> {
    const combined = [keyA, keyB].sort().join(':');
    const hashBuffer = await window.crypto.subtle.digest('SHA-256', new TextEncoder().encode(combined));
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    // Format into 12 blocks of 5 digits (total 60 digits) for standard Signal-like safety number
    let digits = '';
    for (let i = 0; i < hashArray.length; i++) {
      digits += (hashArray[i] % 10).toString();
    }
    while (digits.length < 60) digits += '7';
    return digits.slice(0, 60).match(/.{1,5}/g)?.join(' ') || digits;
  }
}
