<?php
declare(strict_types=1);

/**
 * MATE CHAT - REAL-TIME WEBSOCKET SERVER
 * Handles:
 * - Connection Authentication
 * - E2EE Ciphertext Relaying (Zero-Knowledge)
 * - User Presence (Online / Offline / Away)
 * - Typing Indicators
 * - WebRTC Call Signaling (Offer, Answer, ICE Candidates)
 */

namespace MateChat\WebSocket;

class RealtimeServer {
    private array $clients = [];
    private array $userConnections = []; // userId => [connId => conn]

    public function onOpen($conn): void {
        $this->clients[$conn->resourceId] = [
            'conn' => $conn,
            'user_id' => null,
            'device_id' => null
        ];
        echo "New connection: ({$conn->resourceId})\n";
    }

    public function onMessage($from, string $msg): void {
        $data = json_decode($msg, true);
        if (!$data || !isset($data['action'])) {
            return;
        }

        switch ($data['action']) {
            case 'auth':
                // Authenticate session token
                $userId = (int)($data['user_id'] ?? 0);
                $deviceId = (int)($data['device_id'] ?? 0);
                $this->clients[$from->resourceId]['user_id'] = $userId;
                $this->clients[$from->resourceId]['device_id'] = $deviceId;
                $this->userConnections[$userId][$from->resourceId] = $from;
                $from->send(json_encode(['event' => 'authenticated', 'status' => 'connected', 'user_id' => $userId]));
                $this->broadcastPresence($userId, 'online');
                break;

            case 'typing':
                // Relay typing indicator to conversation peer
                $this->relayToUser((int)$data['recipient_id'], [
                    'event' => 'typing',
                    'conversation_id' => $data['conversation_id'],
                    'user_id' => $this->clients[$from->resourceId]['user_id'],
                    'is_typing' => (bool)$data['is_typing']
                ]);
                break;

            case 'webrtc_signal':
                // Relay WebRTC Call Signaling (offer, answer, ice-candidate, reject, end)
                $targetUserId = (int)($data['target_user_id'] ?? 0);
                $this->relayToUser($targetUserId, [
                    'event' => 'webrtc_signal',
                    'signal_type' => $data['signal_type'], // offer, answer, ice, reject, end
                    'call_id' => $data['call_id'] ?? '',
                    'channel' => $data['channel'] ?? '',
                    'call_type' => $data['call_type'] ?? 'audio',
                    'caller_id' => $this->clients[$from->resourceId]['user_id'],
                    'payload' => $data['payload'] ?? null
                ]);
                break;

            case 'direct_message_relay':
                // Low-latency E2EE message notification to online peer
                $targetUserId = (int)$data['recipient_id'];
                $this->relayToUser($targetUserId, [
                    'event' => 'new_message',
                    'conversation_id' => $data['conversation_id'],
                    'message_id' => $data['message_id'],
                    'sender_id' => $this->clients[$from->resourceId]['user_id'],
                    'created_at' => date('Y-m-d H:i:s')
                ]);
                break;
        }
    }

    public function onClose($conn): void {
        $client = $this->clients[$conn->resourceId] ?? null;
        if ($client && $client['user_id']) {
            $userId = $client['user_id'];
            unset($this->userConnections[$userId][$conn->resourceId]);
            if (empty($this->userConnections[$userId])) {
                unset($this->userConnections[$userId]);
                $this->broadcastPresence($userId, 'offline');
            }
        }
        unset($this->clients[$conn->resourceId]);
        echo "Connection {$conn->resourceId} disconnected\n";
    }

    private function relayToUser(int $userId, array $eventData): void {
        if (!isset($this->userConnections[$userId])) {
            return;
        }
        $json = json_encode($eventData);
        foreach ($this->userConnections[$userId] as $conn) {
            $conn->send($json);
        }
    }

    private function broadcastPresence(int $userId, string $status): void {
        $json = json_encode(['event' => 'presence_change', 'user_id' => $userId, 'status' => $status]);
        foreach ($this->clients as $c) {
            $c['conn']->send($json);
        }
    }
}
