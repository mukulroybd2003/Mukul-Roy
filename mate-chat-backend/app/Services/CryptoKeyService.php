<?php
declare(strict_types=1);

namespace MateChat\Services;

use MateChat\Database;
use PDO;

/**
 * Service managing E2EE public cryptographic materials.
 * CRITICAL RULE: Server stores ONLY public identity keys, signed prekeys, and one-time prekeys.
 * Private keys are NEVER uploaded to or stored on the server.
 */
class CryptoKeyService {
    private PDO $db;

    public function __construct() {
        $this->db = Database::getConnection();
    }

    public function publishDeviceBundle(int $userId, int $deviceId, string $identityKey, string $signedPrekey, string $signature, array $oneTimePrekeys = []): bool {
        $this->db->beginTransaction();
        try {
            // Upsert device keys
            $stmt = $this->db->prepare("
                INSERT INTO device_keys (user_id, device_id, public_identity_key, registration_id)
                VALUES (:user_id, :device_id, :identity_key, :reg_id)
                ON DUPLICATE KEY UPDATE public_identity_key = VALUES(public_identity_key)
            ");
            $stmt->execute([
                ':user_id' => $userId,
                ':device_id' => $deviceId,
                ':identity_key' => $identityKey,
                ':reg_id' => random_int(10000, 99999)
            ]);

            // Save Key Bundle
            $stmt = $this->db->prepare("
                INSERT INTO key_bundles (device_id, identity_key, signed_prekey, signed_prekey_signature, last_rotated_at)
                VALUES (:device_id, :identity_key, :signed_prekey, :sig, NOW())
                ON DUPLICATE KEY UPDATE
                    identity_key = VALUES(identity_key),
                    signed_prekey = VALUES(signed_prekey),
                    signed_prekey_signature = VALUES(signed_prekey_signature),
                    last_rotated_at = NOW()
            ");
            $stmt->execute([
                ':device_id' => $deviceId,
                ':identity_key' => $identityKey,
                ':signed_prekey' => $signedPrekey,
                ':sig' => $signature
            ]);

            // Insert one-time prekeys if provided
            if (!empty($oneTimePrekeys)) {
                $otpStmt = $this->db->prepare("
                    INSERT INTO one_time_prekeys (device_id, key_id, public_key, is_consumed)
                    VALUES (:device_id, :key_id, :pub_key, 0)
                ");
                foreach ($oneTimePrekeys as $otp) {
                    $otpStmt->execute([
                        ':device_id' => $deviceId,
                        ':key_id' => $otp['key_id'],
                        ':pub_key' => $otp['public_key']
                    ]);
                }
            }

            $this->db->commit();
            return true;
        } catch (\Throwable $e) {
            $this->db->rollBack();
            error_log("Failed to publish key bundle: " . $e->getMessage());
            return false;
        }
    }

    public function getKeyBundle(int $userId, ?int $deviceId = null): ?array {
        $sql = "
            SELECT d.id AS device_id, d.user_id, kb.identity_key, kb.signed_prekey, kb.signed_prekey_signature
            FROM devices d
            JOIN key_bundles kb ON kb.device_id = d.id
            WHERE d.user_id = :user_id AND d.is_active = 1
        ";
        if ($deviceId !== null) {
            $sql .= " AND d.id = :device_id";
        }
        $sql .= " LIMIT 1";

        $stmt = $this->db->prepare($sql);
        $params = [':user_id' => $userId];
        if ($deviceId !== null) {
            $params[':device_id'] = $deviceId;
        }
        $stmt->execute($params);
        $bundle = $stmt->fetch();

        if (!$bundle) {
            return null;
        }

        // Fetch one unused one-time prekey and mark as consumed
        $otpStmt = $this->db->prepare("
            SELECT id, key_id, public_key
            FROM one_time_prekeys
            WHERE device_id = :device_id AND is_consumed = 0
            ORDER BY id ASC LIMIT 1
            FOR UPDATE
        ");
        $otpStmt->execute([':device_id' => $bundle['device_id']]);
        $otp = $otpStmt->fetch();

        if ($otp) {
            $updateStmt = $this->db->prepare("
                UPDATE one_time_prekeys SET is_consumed = 1, consumed_at = NOW() WHERE id = :id
            ");
            $updateStmt->execute([':id' => $otp['id']]);
            $bundle['one_time_prekey'] = [
                'key_id' => (int)$otp['key_id'],
                'public_key' => $otp['public_key']
            ];
        } else {
            $bundle['one_time_prekey'] = null;
        }

        return $bundle;
    }
}
