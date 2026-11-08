<?php
declare(strict_types=1);

namespace MateChat\Middleware;

use MateChat\Database;
use PDO;

class AuthMiddleware {
    public static function authenticate(): array {
        $headers = getallheaders();
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;

        if (!$authHeader || !preg_match('/Bearer\s+(\S+)/', $authHeader, $matches)) {
            http_response_code(401);
            echo json_encode(['error' => 'Unauthorized: Missing or invalid token']);
            exit;
        }

        $token = $matches[1];
        $db = Database::getConnection();

        $stmt = $db->prepare("
            SELECT s.id AS session_id, s.user_id, s.device_id, u.username, u.email, u.status
            FROM user_sessions s
            JOIN users u ON u.id = s.user_id
            WHERE s.session_token = :token
              AND s.expires_at > NOW()
              AND s.revoked_at IS NULL
              AND u.status = 'active'
            LIMIT 1
        ");
        $stmt->execute([':token' => $token]);
        $session = $stmt->fetch();

        if (!$session) {
            http_response_code(401);
            echo json_encode(['error' => 'Unauthorized: Session expired or revoked']);
            exit;
        }

        return $session;
    }
}
