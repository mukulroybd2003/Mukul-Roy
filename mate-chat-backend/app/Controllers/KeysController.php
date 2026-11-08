<?php
declare(strict_types=1);

namespace MateChat\Controllers;

use MateChat\Services\CryptoKeyService;

class KeysController {
    private CryptoKeyService $keyService;

    public function __construct() {
        $this->keyService = new CryptoKeyService();
    }

    public function uploadBundle(array $session): void {
        $data = json_decode(file_get_contents('php://input'), true) ?: [];
        $identityKey = $data['identity_key'] ?? '';
        $signedPrekey = $data['signed_prekey'] ?? '';
        $signature = $data['signature'] ?? '';
        $oneTimePrekeys = $data['one_time_prekeys'] ?? [];

        if (!$identityKey || !$signedPrekey || !$signature) {
            http_response_code(422);
            echo json_encode(['error' => 'Missing public identity key, signed prekey or signature']);
            return;
        }

        $ok = $this->keyService->publishDeviceBundle(
            $session['user_id'],
            $session['device_id'],
            $identityKey,
            $signedPrekey,
            $signature,
            $oneTimePrekeys
        );

        if ($ok) {
            echo json_encode(['success' => true, 'message' => 'Key bundle published successfully']);
        } else {
            http_response_code(500);
            echo json_encode(['error' => 'Failed to publish key bundle']);
        }
    }

    public function fetchBundle(array $session, int $targetUserId): void {
        $bundle = $this->keyService->getKeyBundle($targetUserId);
        if (!$bundle) {
            http_response_code(404);
            echo json_encode(['error' => 'No cryptographic key bundle found for this user']);
            return;
        }

        echo json_encode(['success' => true, 'bundle' => $bundle]);
    }
}
