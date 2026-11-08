package com.example

import com.example.crypto.CryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testDeviceKeyPairGeneration() {
    val keyPair = CryptoEngine.getOrCreateDeviceKeyPair()
    assertNotNull(keyPair.public)
    assertNotNull(keyPair.private)

    val pubBase64 = CryptoEngine.getLocalPublicKeyBase64()
    assertTrue(pubBase64.isNotBlank())
  }

  @Test
  fun testSafetyNumberGenerationFormat() {
    val keyA = CryptoEngine.getLocalPublicKeyBase64()
    val keyB = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEfakePublicKeyForTestingSafetyNumbersOnly1234567890abcdefghijklmnopqrstuvwxyz="

    val safetyNumber = CryptoEngine.generateSafetyNumber(keyA, keyB)
    assertNotNull(safetyNumber)

    // Should have 12 chunks of 5 digits separated by spaces
    val chunks = safetyNumber.split(" ")
    assertEquals(12, chunks.size)
    for (chunk in chunks) {
      assertEquals(5, chunk.length)
      assertTrue(chunk.all { it.isDigit() })
    }

    // Must be symmetric: safetyNumber(A, B) == safetyNumber(B, A)
    val symmetric = CryptoEngine.generateSafetyNumber(keyB, keyA)
    assertEquals(safetyNumber, symmetric)
  }

  @Test
  fun testFingerprintGeneration() {
    val pub = CryptoEngine.getLocalPublicKeyBase64()
    val fp = CryptoEngine.computeFingerprint(pub)
    assertEquals(16, fp.length) // 8 bytes in hex = 16 chars
  }
}
