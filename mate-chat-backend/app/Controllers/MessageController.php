<?php
declare(strict_types=1);

namespace MateChat\Controllers;

use MateChat\Database;
use MateChat\Services\FCMService;
use PDO;

class MessageController {
    private PDO $db;
    private FCMService $fcm;

    public function __construct() {
        $this->db = Database::getConnection();
        $this->fcm = new FCMService();
    }

    public function getMessages(array $session, int $conversationId): void {
        // Verify membership
        $memberStmt = $this->db->prepare("
            SELECT id FROM conversation_members
            WHERE conversation_id = :cid AND user_id = :uid
            LIMIT 1
        ");
        $memberStmt->execute([':cid' => $conversationId, ':uid' => $session['user_id']]);
        if (!$memberStmt->fetch()) {
            http_response_code(403);
            echo json_encode(['error' => 'Not a member of this conversation']);
            return;
        }

        $limit = min(100, max(1, (int)($_GET['limit'] ?? 50)));
        $beforeId = isset($_GET['before_id']) ? (int)$_GET['before_id'] : null;

        $sql = "
            SELECT m.id, m.conversation_id, m.sender_id, m.sender_device_id,
                   m.message_type, m.encrypted_payload, m.iv, m.auth_tag, m.key_fingerprint,
                   m.reply_to_id, m.is_edited, m.is_deleted_everyone, m.created_at,
                   u.username AS sender_username, p.full_name AS sender_full_name, p.avatar_url AS sender_avatar
            FROM messages m
            JOIN users u ON u.id = m.sender_id
            JOIN user_profiles p ON p.user_id = u.id
            WHERE m.conversation_id = :cid
        ";
        if ($beforeId) {
            $sql .= " AND m.id < :before_id";
        }
        $sql .= " ORDER BY m.id DESC LIMIT :limit";

        $stmt = $this->db->prepare($sql);
        $stmt->bindValue(':cid', $conversationId, PDO::PARAM_INT);
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        if ($beforeId) {
            $stmt->bindValue(':before_id', $beforeId, PDO::PARAM_INT);
        }
        $stmt->execute();
        $messages = array_reverse($stmt->fetchAll());

        echo json_encode([
            'success' => true,
            'conversation_id' => $conversationId,
            'messages' => $messages
        ]);
    }

    public function sendMessage(array $session): void {
        $data = json_decode(file_get_contents('php://input'), true) ?: [];
        $conversationId = (int)($data['conversation_id'] ?? 0);
        $encryptedPayload = $data['encrypted_payload'] ?? '';
        $iv = $data['iv'] ?? '';
        $authTag = $data['auth_tag'] ?? '';
        $keyFingerprint = $data['key_fingerprint'] ?? '';
        $messageType = $data['message_type'] ?? 'text';
        $replyToId = !empty($data['reply_to_id']) ? (int)$data['reply_to_id'] : null;

        if (!$conversationId || empty($encryptedPayload)) {
            http_response_code(422);
            echo json_encode(['error' => 'Missing conversation_id or encrypted_payload.']);
            return;
        }

        // Verify membership
        $memberStmt = $this->db->prepare("
            SELECT id FROM conversation_members
            WHERE conversation_id = :cid AND user_id = :uid
            LIMIT 1
        ");
        $memberStmt->execute([':cid' => $conversationId, ':uid' => $session['user_id']]);
        if (!$memberStmt->fetch()) {
            http_response_code(403);
            echo json_encode(['error' => 'Not authorized to send messages in this conversation']);
            return;
        }

        $this->db->beginTransaction();
        try {
            $stmt = $this->db->prepare("
                INSERT INTO messages (conversation_id, sender_id, sender_device_id, message_type,
                                      encrypted_payload, iv, auth_tag, key_fingerprint, reply_to_id)
                VALUES (:cid, :sid, :sdid, :mtype, :payload, :iv, :tag, :kfp, :reply)
            ");
            $stmt->execute([
                ':cid' => $conversationId,
                ':sid' => $session['user_id'],
                ':sdid' => $session['device_id'],
                ':mtype' => $messageType,
                ':payload' => $encryptedPayload,
                ':iv' => $iv,
                ':tag' => $authTag,
                ':kfp' => $keyFingerprint,
                ':reply' => $replyToId
            ]);
            $messageId = (int)$this->db->lastInsertId();

            // Update conversation last_message_at
            $updateConv = $this->db->prepare("
                UPDATE conversations
                SET last_message_id = :mid, last_message_at = NOW()
                WHERE id = :cid
            ");
            $updateConv->execute([':mid' => $messageId, ':cid' => $conversationId]);

            // Increment unread count for other members
            $incStmt = $this->db->prepare("
                UPDATE conversation_members
                SET unread_count = unread_count + 1
                WHERE conversation_id = :cid AND user_id != :uid
            ");
            $incStmt->execute([':cid' => $conversationId, ':uid' => $session['user_id']]);

            $this->db->commit();

            // Dispatch privacy-compliant FCM push to recipients
            $recipientsStmt = $this->db->prepare("
                SELECT user_id FROM conversation_members
                WHERE conversation_id = :cid AND user_id != :uid
            ");
            $recipientsStmt->execute([':cid' => $conversationId, ':uid' => $session['user_id']]);
            $recipients = $recipientsStmt->fetchAll(PDO::FETCH_COLUMN);

            foreach ($recipients as $recipientId) {
                $this->fcm->sendToUser(
                    (int)$recipientId,
                    'new_message',
                    'MATE CHAT',
                    'New end-to-end encrypted message received',
                    ['conversation_id' => (string)$conversationId, 'message_id' => (string)$messageId]
                );
            }

            echo json_encode([
                'success' => true,
                'message_id' => $messageId,
                'created_at' => date('Y-m-d H:i:s')
            ]);
        } catch (\Throwable $e) {
            $this->db->rollBack();
            http_response_code(500);
            echo json_encode(['error' => 'Failed to persist message: ' . $e->getMessage()]);
        }
    }
}
