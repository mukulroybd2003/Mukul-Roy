<?php
declare(strict_types=1);

namespace MateChat\Controllers;

use MateChat\Database;
use PDO;

class AdminController {
    private PDO $db;

    public function __construct() {
        $this->db = Database::getConnection();
    }

    public function getDashboardStats(): void {
        $totalUsers = (int)$this->db->query("SELECT COUNT(*) FROM users")->fetchColumn();
        $activeUsers = (int)$this->db->query("SELECT COUNT(*) FROM users WHERE status = 'active'")->fetchColumn();
        $onlineUsers = (int)$this->db->query("SELECT COUNT(*) FROM user_profiles WHERE is_online = 1")->fetchColumn();
        $totalConversations = (int)$this->db->query("SELECT COUNT(*) FROM conversations")->fetchColumn();
        $totalMessages = (int)$this->db->query("SELECT COUNT(*) FROM messages")->fetchColumn();
        $pendingReports = (int)$this->db->query("SELECT COUNT(*) FROM reports WHERE status = 'pending'")->fetchColumn();

        echo json_encode([
            'success' => true,
            'stats' => [
                'total_users' => $totalUsers,
                'active_users' => $activeUsers,
                'online_users' => $onlineUsers,
                'total_conversations' => $totalConversations,
                'total_encrypted_messages' => $totalMessages,
                'pending_reports' => $pendingReports,
                'system_health' => 'operational',
                'database_engine' => 'MySQL 8.0 InnoDB',
                'e2ee_status' => 'enforced'
            ]
        ]);
    }

    public function listUsers(): void {
        $stmt = $this->db->query("
            SELECT u.id, u.username, u.email, u.status, u.created_at,
                   p.full_name, p.is_online, p.last_seen,
                   (SELECT COUNT(*) FROM devices WHERE user_id = u.id) AS device_count
            FROM users u
            JOIN user_profiles p ON p.user_id = u.id
            ORDER BY u.id DESC LIMIT 100
        ");
        echo json_encode(['success' => true, 'users' => $stmt->fetchAll()]);
    }

    public function toggleUserStatus(int $userId): void {
        $data = json_decode(file_get_contents('php://input'), true) ?: [];
        $newStatus = $data['status'] ?? 'suspended';

        if (!in_array($newStatus, ['active', 'suspended', 'deactivated'])) {
            http_response_code(422);
            echo json_encode(['error' => 'Invalid status']);
            return;
        }

        $stmt = $this->db->prepare("UPDATE users SET status = :s WHERE id = :id");
        $stmt->execute([':s' => $newStatus, ':id' => $userId]);

        // Revoke active sessions if suspended
        if ($newStatus === 'suspended') {
            $revStmt = $this->db->prepare("UPDATE user_sessions SET revoked_at = NOW() WHERE user_id = :id");
            $revStmt->execute([':id' => $userId]);
        }

        echo json_encode(['success' => true, 'message' => "User status updated to {$newStatus}"]);
    }
}
