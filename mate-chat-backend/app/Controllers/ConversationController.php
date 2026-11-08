<?php
declare(strict_types=1);

namespace MateChat\Controllers;

use MateChat\Database;
use PDO;

class ConversationController {
    private PDO $db;

    public function __construct() {
        $this->db = Database::getConnection();
    }

    public function list(array $session): void {
        $userId = $session['user_id'];
        $stmt = $this->db->prepare("
            SELECT c.id, c.type, c.title, c.avatar_url, c.last_message_at, c.disappearing_seconds,
                   cm.role, cm.is_muted, cm.is_pinned, cm.is_archived, cm.unread_count,
                   (
                       SELECT u.username FROM conversation_members cm2
                       JOIN users u ON u.id = cm2.user_id
                       WHERE cm2.conversation_id = c.id AND cm2.user_id != :uid
                       LIMIT 1
                   ) AS direct_peer_username,
                   (
                       SELECT p.full_name FROM conversation_members cm2
                       JOIN user_profiles p ON p.user_id = cm2.user_id
                       WHERE cm2.conversation_id = c.id AND cm2.user_id != :uid
                       LIMIT 1
                   ) AS direct_peer_name,
                   (
                       SELECT p.avatar_url FROM conversation_members cm2
                       JOIN user_profiles p ON p.user_id = cm2.user_id
                       WHERE cm2.conversation_id = c.id AND cm2.user_id != :uid
                       LIMIT 1
                   ) AS direct_peer_avatar,
                   (
                       SELECT p.is_online FROM conversation_members cm2
                       JOIN user_profiles p ON p.user_id = cm2.user_id
                       WHERE cm2.conversation_id = c.id AND cm2.user_id != :uid
                       LIMIT 1
                   ) AS direct_peer_online
            FROM conversations c
            JOIN conversation_members cm ON cm.conversation_id = c.id AND cm.user_id = :uid
            ORDER BY cm.is_pinned DESC, c.last_message_at DESC
        ");
        $stmt->execute([':uid' => $userId]);
        $conversations = $stmt->fetchAll();

        echo json_encode([
            'success' => true,
            'conversations' => $conversations
        ]);
    }

    public function createDirect(array $session): void {
        $data = json_decode(file_get_contents('php://input'), true) ?: [];
        $peerId = (int)($data['peer_user_id'] ?? 0);

        if (!$peerId || $peerId === $session['user_id']) {
            http_response_code(422);
            echo json_encode(['error' => 'Invalid peer user ID.']);
            return;
        }

        // Check if direct conversation already exists
        $stmt = $this->db->prepare("
            SELECT c.id FROM conversations c
            JOIN conversation_members cm1 ON cm1.conversation_id = c.id AND cm1.user_id = :u1
            JOIN conversation_members cm2 ON cm2.conversation_id = c.id AND cm2.user_id = :u2
            WHERE c.type = 'direct'
            LIMIT 1
        ");
        $stmt->execute([':u1' => $session['user_id'], ':u2' => $peerId]);
        $existing = $stmt->fetch();
        if ($existing) {
            echo json_encode(['success' => true, 'conversation_id' => (int)$existing['id'], 'is_new' => false]);
            return;
        }

        $this->db->beginTransaction();
        try {
            $convStmt = $this->db->prepare("
                INSERT INTO conversations (type, created_by) VALUES ('direct', :creator)
            ");
            $convStmt->execute([':creator' => $session['user_id']]);
            $convId = (int)$this->db->lastInsertId();

            $memStmt = $this->db->prepare("
                INSERT INTO conversation_members (conversation_id, user_id, role)
                VALUES (:cid, :uid, 'member')
            ");
            $memStmt->execute([':cid' => $convId, ':uid' => $session['user_id']]);
            $memStmt->execute([':cid' => $convId, ':uid' => $peerId]);

            $this->db->commit();
            echo json_encode(['success' => true, 'conversation_id' => $convId, 'is_new' => true]);
        } catch (\Throwable $e) {
            $this->db->rollBack();
            http_response_code(500);
            echo json_encode(['error' => 'Failed to create conversation: ' . $e->getMessage()]);
        }
    }
}
