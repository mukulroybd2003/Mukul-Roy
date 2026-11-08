package com.example

import com.example.crypto.CryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleRobolectricTest {

  @Test
  fun testE2EEMessageEncryptionAndDecryption() {
    // Generate simulated Alice and Bob keys
    val alicePub = CryptoEngine.getLocalPublicKeyBase64()
    assertNotNull(alicePub)

    val plaintext = "MATE CHAT: Test end-to-end encrypted packet"
    val encrypted = CryptoEngine.encryptText(plaintext, alicePub)

    assertNotNull(encrypted.ciphertextBase64)
    assertNotNull(encrypted.ivBase64)
    assertNotNull(encrypted.authTagBase64)

    val decrypted = CryptoEngine.decryptText(
      encrypted.ciphertextBase64,
      encrypted.ivBase64,
      encrypted.authTagBase64,
      alicePub
    )
    assertEquals(plaintext, decrypted)
  }
}
