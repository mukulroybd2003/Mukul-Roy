<?php
declare(strict_types=1);

namespace MateChat\Controllers;

use MateChat\Database;
use PDO;

class AuthController {
    private PDO $db;

    public function __construct() {
        $this->db = Database::getConnection();
    }

    public function register(): void {
        $data = json_decode(file_get_contents('php://input'), true) ?: [];
        $username = trim($data['username'] ?? '');
        $email    = strtolower(trim($data['email'] ?? ''));
        $password = $data['password'] ?? '';
        $fullName = trim($data['full_name'] ?? $username);
        $deviceName = trim($data['device_name'] ?? 'Android Device');
        $platform = $data['platform'] ?? 'android';

        if (strlen($username) < 3 || !filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($password) < 8) {
            http_response_code(422);
            echo json_encode(['error' => 'Validation error: username (min 3 chars), valid email, and password (min 8 chars) required.']);
            return;
        }

        // Check uniqueness
        $stmt = $this->db->prepare("SELECT id FROM users WHERE username = :u OR email = :e LIMIT 1");
        $stmt->execute([':u' => $username, ':e' => $email]);
        if ($stmt->fetch()) {
            http_response_code(409);
            echo json_encode(['error' => 'Username or email already in use.']);
            return;
        }

        $passwordHash = password_hash($password, PASSWORD_ARGON2ID);
        $this->db->beginTransaction();
        try {
            $userStmt = $this->db->prepare("
                INSERT INTO users (username, email, password_hash, status, email_verified_at)
                VALUES (:u, :e, :p, 'active', NOW())
            ");
            $userStmt->execute([':u' => $username, ':e' => $email, ':p' => $passwordHash]);
            $userId = (int)$this->db->lastInsertId();

            // Create profile
            $profStmt = $this->db->prepare("
                INSERT INTO user_profiles (user_id, full_name, bio, is_online, last_seen)
                VALUES (:uid, :fn, 'Hey there! I am using MATE CHAT.', 1, NOW())
            ");
            $profStmt->execute([':uid' => $userId, ':fn' => $fullName]);

            // Register initial device
            $deviceUuid = bin2hex(random_bytes(16));
            $devStmt = $this->db->prepare("
                INSERT INTO devices (user_id, device_uuid, device_name, platform, is_active)
                VALUES (:uid, :uuid, :dname, :plat, 1)
            ");
            $devStmt->execute([':uid' => $userId, ':uuid' => $deviceUuid, ':dname' => $deviceName, ':plat' => $platform]);
            $deviceId = (int)$this->db->lastInsertId();

            // Issue initial session token
            $sessionToken = bin2hex(random_bytes(32));
            $sessStmt = $this->db->prepare("
                INSERT INTO user_sessions (user_id, device_id, session_token, ip_address, expires_at)
                VALUES (:uid, :did, :token, :ip, DATE_ADD(NOW(), INTERVAL 30 DAY))
            ");
            $sessStmt->execute([
                ':uid' => $userId,
                ':did' => $deviceId,
                ':token' => $sessionToken,
                ':ip' => $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1'
            ]);

            $this->db->commit();

            echo json_encode([
                'success' => true,
                'message' => 'Registration successful',
                'user' => [
                    'id' => $userId,
                    'username' => $username,
                    'email' => $email,
                    'full_name' => $fullName,
                ],
                'device' => [
                    'id' => $deviceId,
                    'uuid' => $deviceUuid,
                    'name' => $deviceName
                ],
                'session_token' => $sessionToken
            ]);
        } catch (\Throwable $e) {
            $this->db->rollBack();
            http_response_code(500);
            echo json_encode(['error' => 'Registration failed: ' . $e->getMessage()]);
        }
    }

    public function login(): void {
        $data = json_decode(file_get_contents('php://input'), true) ?: [];
        $identity = trim($data['username'] ?? $data['email'] ?? '');
        $password = $data['password'] ?? '';
        $deviceName = trim($data['device_name'] ?? 'Android Device');
        $platform = $data['platform'] ?? 'android';

        if (!$identity || !$password) {
            http_response_code(422);
            echo json_encode(['error' => 'Username/email and password required.']);
            return;
        }

        $stmt = $this->db->prepare("
            SELECT u.id, u.username, u.email, u.password_hash, u.status, p.full_name, p.avatar_url, p.bio
            FROM users u
            JOIN user_profiles p ON p.user_id = u.id
            WHERE u.username = :ident OR u.email = :ident
            LIMIT 1
        ");
        $stmt->execute([':ident' => $identity]);
        $user = $stmt->fetch();

        if (!$user || !password_verify($password, $user['password_hash'])) {
            http_response_code(401);
            echo json_encode(['error' => 'Invalid credentials.']);
            return;
        }

        if ($user['status'] !== 'active') {
            http_response_code(403);
            echo json_encode(['error' => "Account is {$user['status']}. Please contact support."]);
            return;
        }

        // Register or find device
        $deviceUuid = $data['device_uuid'] ?? bin2hex(random_bytes(16));
        $devStmt = $this->db->prepare("
            INSERT INTO devices (user_id, device_uuid, device_name, platform, is_active)
            VALUES (:uid, :uuid, :dname, :plat, 1)
            ON DUPLICATE KEY UPDATE device_name = VALUES(device_name), is_active = 1, updated_at = NOW()
        ");
        $devStmt->execute([':uid' => $user['id'], ':uuid' => $deviceUuid, ':dname' => $deviceName, ':plat' => $platform]);

        $fetchDev = $this->db->prepare("SELECT id FROM devices WHERE user_id = :uid AND device_uuid = :uuid LIMIT 1");
        $fetchDev->execute([':uid' => $user['id'], ':uuid' => $deviceUuid]);
        $deviceId = (int)$fetchDev->fetchColumn();

        $sessionToken = bin2hex(random_bytes(32));
        $sessStmt = $this->db->prepare("
            INSERT INTO user_sessions (user_id, device_id, session_token, ip_address, expires_at)
            VALUES (:uid, :did, :token, :ip, DATE_ADD(NOW(), INTERVAL 30 DAY))
        ");
        $sessStmt->execute([
            ':uid' => $user['id'],
            ':did' => $deviceId,
            ':token' => $sessionToken,
            ':ip' => $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1'
        ]);

        echo json_encode([
            'success' => true,
            'user' => [
                'id' => (int)$user['id'],
                'username' => $user['username'],
                'email' => $user['email'],
                'full_name' => $user['full_name'],
                'avatar_url' => $user['avatar_url'],
                'bio' => $user['bio']
            ],
            'device' => [
                'id' => $deviceId,
                'uuid' => $deviceUuid,
                'name' => $deviceName
            ],
            'session_token' => $sessionToken
        ]);
    }

    public function logout(array $session): void {
        $stmt = $this->db->prepare("
            UPDATE user_sessions SET revoked_at = NOW() WHERE id = :id
        ");
        $stmt->execute([':id' => $session['session_id']]);
        echo json_encode(['success' => true, 'message' => 'Logged out successfully.']);
    }
}
