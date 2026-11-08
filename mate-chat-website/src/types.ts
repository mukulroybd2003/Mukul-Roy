export interface User {
  id: number;
  username: string;
  email: string;
  full_name: string;
  avatar_url?: string;
  bio?: string;
  is_online?: boolean;
}

export interface Conversation {
  id: number;
  type: 'direct' | 'group';
  title?: string;
  avatar_url?: string;
  last_message_at: string;
  unread_count: number;
  is_pinned: boolean;
  direct_peer_name?: string;
  direct_peer_username?: string;
  direct_peer_avatar?: string;
  direct_peer_online?: boolean;
}

export interface Message {
  id: number;
  conversation_id: number;
  sender_id: number;
  sender_name?: string;
  message_type: 'text' | 'image' | 'voice_note' | 'document';
  encrypted_payload: string;
  decrypted_text?: string;
  iv: string;
  auth_tag: string;
  created_at: string;
  is_me?: boolean;
  reaction_emoji?: string;
}

export interface AddaRoom {
  id: number;
  title: string;
  topic: string;
  host_name: string;
  speaker_count: number;
  listener_count: number;
  is_live: boolean;
}
