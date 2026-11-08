import React, { useState, useEffect } from 'react';
import { 
  Shield, Lock, MessageSquare, Phone, Video, Users, Settings, 
  Search, Send, Mic, Paperclip, MoreVertical, Radio, CheckCheck, 
  UserCheck, Bell, LogOut, ShieldAlert, Sparkles, PhoneOff, MicOff, Camera
} from 'lucide-react';
import { WebCryptoE2EE } from './crypto';
import { User, Conversation, Message, AddaRoom } from './types';

export default function App() {
  const [activeTab, setActiveTab] = useState<'chats' | 'adda' | 'calls' | 'settings' | 'admin'>('chats');
  const [currentUser] = useState<User>({
    id: 1,
    username: 'alex_mate',
    email: 'alex@matechat.app',
    full_name: 'Alex Rivers',
    bio: 'Building next-gen secure communication 🚀',
    is_online: true
  });

  const [conversations] = useState<Conversation[]>([
    {
      id: 1,
      type: 'direct',
      direct_peer_name: 'Sophia Zhang',
      direct_peer_username: 'sophia_code',
      direct_peer_online: true,
      last_message_at: 'Just now',
      unread_count: 0,
      is_pinned: true
    },
    {
      id: 2,
      type: 'direct',
      direct_peer_name: 'Marcus Vance',
      direct_peer_username: 'marcus_design',
      direct_peer_online: false,
      last_message_at: '15m ago',
      unread_count: 2,
      is_pinned: false
    },
    {
      id: 3,
      type: 'group',
      title: 'MATE Core Engineering',
      last_message_at: '1h ago',
      unread_count: 0,
      is_pinned: false
    }
  ]);

  const [selectedConv, setSelectedConv] = useState<Conversation>(conversations[0]);
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 101,
      conversation_id: 1,
      sender_id: 2,
      sender_name: 'Sophia Zhang',
      message_type: 'text',
      encrypted_payload: 'enc_payload_demo_01',
      decrypted_text: 'Hey Alex! Did you review the Curve25519 ratchet rotation?',
      iv: 'iv_demo_01',
      auth_tag: 'tag_demo_01',
      created_at: '10:42 AM',
      is_me: false,
      reaction_emoji: '🔥'
    },
    {
      id: 102,
      conversation_id: 1,
      sender_id: 1,
      sender_name: 'Alex Rivers',
      message_type: 'text',
      encrypted_payload: 'enc_payload_demo_02',
      decrypted_text: 'Yes! The double ratchet is working seamlessly across both Web and Android.',
      iv: 'iv_demo_02',
      auth_tag: 'tag_demo_02',
      created_at: '10:44 AM',
      is_me: true,
      reaction_emoji: '🚀'
    }
  ]);

  const [hoveredMsgId, setHoveredMsgId] = useState<number | null>(null);
  const [activeReactionPopoverId, setActiveReactionPopoverId] = useState<number | null>(null);

  const handleReact = (msgId: number, emoji: string) => {
    setMessages(prev => prev.map(m => {
      if (m.id === msgId) {
        return { ...m, reaction_emoji: m.reaction_emoji === emoji ? undefined : emoji };
      }
      return m;
    }));
    setActiveReactionPopoverId(null);
  };

  const [inputMsg, setInputMsg] = useState('');
  const [inCall, setInCall] = useState<'audio' | 'video' | null>(null);
  const [callDuration, setCallDuration] = useState(0);
  const [isMicMuted, setIsMicMuted] = useState(false);
  const [showSafetyModal, setShowSafetyModal] = useState(false);
  const [safetyNumber, setSafetyNumber] = useState('38942 91048 20194 88392 01928 47291 99201 38491 02938 48192 39019 48291');

  const [addaRooms] = useState<AddaRoom[]>([
    {
      id: 1,
      title: 'Cybersecurity & Cryptography Cafe',
      topic: 'Signal Protocol, X3DH, and MLS group ratchets',
      host_name: 'Alex Rivers',
      speaker_count: 3,
      listener_count: 28,
      is_live: true
    },
    {
      id: 2,
      title: 'Kotlin Multiplatform & Compose Tech Adda',
      topic: 'Jetpack Compose UI patterns, M3 design system, and state hoisting',
      host_name: 'Sophia Zhang',
      speaker_count: 2,
      listener_count: 15,
      is_live: true
    }
  ]);

  // Call timer effect
  useEffect(() => {
    let timer: any;
    if (inCall) {
      timer = setInterval(() => setCallDuration(d => d + 1), 1000);
    } else {
      setCallDuration(0);
    }
    return () => clearInterval(timer);
  }, [inCall]);

  const handleSend = () => {
    if (!inputMsg.trim()) return;
    const newMsg: Message = {
      id: Date.now(),
      conversation_id: selectedConv.id,
      sender_id: currentUser.id,
      sender_name: currentUser.full_name,
      message_type: 'text',
      encrypted_payload: btoa(inputMsg),
      decrypted_text: inputMsg,
      iv: 'auto_iv',
      auth_tag: 'auto_tag',
      created_at: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      is_me: true
    };
    setMessages(prev => [...prev, newMsg]);
    setInputMsg('');
  };

  const formatSeconds = (s: number) => {
    const mins = Math.floor(s / 60);
    const secs = s % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div style={{ display: 'flex', width: '100vw', height: '100vh', background: '#0F172A', color: '#F8FAFC', fontFamily: 'system-ui, -apple-system, sans-serif' }}>
      {/* LEFT NAVIGATION BAR */}
      <div style={{ width: 72, background: '#090D16', borderRight: '1px solid #1E293B', display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '16px 0' }}>
        <div style={{ width: 44, height: 44, borderRadius: 12, background: 'linear-gradient(135deg, #06B6D4, #8B5CF6)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 24 }}>
          <Shield size={24} color="#FFF" />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16, flex: 1 }}>
          <button onClick={() => setActiveTab('chats')} style={{ width: 44, height: 44, borderRadius: 10, background: activeTab === 'chats' ? '#1E293B' : 'transparent', border: 'none', color: activeTab === 'chats' ? '#06B6D4' : '#94A3B8', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <MessageSquare size={20} />
          </button>
          <button onClick={() => setActiveTab('adda')} style={{ width: 44, height: 44, borderRadius: 10, background: activeTab === 'adda' ? '#1E293B' : 'transparent', border: 'none', color: activeTab === 'adda' ? '#06B6D4' : '#94A3B8', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Radio size={20} />
          </button>
          <button onClick={() => setActiveTab('admin')} style={{ width: 44, height: 44, borderRadius: 10, background: activeTab === 'admin' ? '#1E293B' : 'transparent', border: 'none', color: activeTab === 'admin' ? '#06B6D4' : '#94A3B8', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <ShieldAlert size={20} />
          </button>
        </div>
        <button onClick={() => setActiveTab('settings')} style={{ width: 44, height: 44, borderRadius: 10, background: 'transparent', border: 'none', color: '#94A3B8', cursor: 'pointer' }}>
          <Settings size={20} />
        </button>
      </div>

      {/* CONVERSATION LIST / SIDE PANEL */}
      <div style={{ width: 320, background: '#0F172A', borderRight: '1px solid #1E293B', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '20px 16px', borderBottom: '1px solid #1E293B', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 700, letterSpacing: -0.5 }}>MATE CHAT</h2>
          <span style={{ fontSize: 11, background: '#06B6D422', color: '#06B6D4', padding: '3px 8px', borderRadius: 12, fontWeight: 600 }}>E2EE ACTIVE</span>
        </div>

        {activeTab === 'chats' && (
          <div style={{ flex: 1, overflowY: 'auto' }}>
            <div style={{ padding: '12px 16px' }}>
              <div style={{ background: '#1E293B', borderRadius: 8, padding: '8px 12px', display: 'flex', alignItems: 'center', gap: 8 }}>
                <Search size={16} color="#94A3B8" />
                <input placeholder="Search chats and contacts..." style={{ background: 'transparent', border: 'none', color: '#FFF', outline: 'none', fontSize: 13, width: '100%' }} />
              </div>
            </div>
            {conversations.map(conv => (
              <div 
                key={conv.id} 
                onClick={() => setSelectedConv(conv)}
                style={{ 
                  padding: '12px 16px', 
                  display: 'flex', 
                  gap: 12, 
                  alignItems: 'center', 
                  cursor: 'pointer', 
                  background: selectedConv.id === conv.id ? '#1E293B88' : 'transparent',
                  borderLeft: selectedConv.id === conv.id ? '3px solid #06B6D4' : '3px solid transparent'
                }}
              >
                <div style={{ position: 'relative' }}>
                  <div style={{ width: 44, height: 44, borderRadius: 22, background: '#334155', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600 }}>
                    {conv.type === 'direct' ? (conv.direct_peer_name?.[0] || 'U') : 'G'}
                  </div>
                  {conv.direct_peer_online && (
                    <div style={{ position: 'absolute', bottom: 0, right: 0, width: 12, height: 12, borderRadius: 6, background: '#10B981', border: '2px solid #0F172A' }} />
                  )}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div style={{ fontWeight: 600, fontSize: 14 }}>{conv.type === 'direct' ? conv.direct_peer_name : conv.title}</div>
                    <div style={{ fontSize: 11, color: '#64748B' }}>{conv.last_message_at}</div>
                  </div>
                  <div style={{ fontSize: 12, color: '#94A3B8', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    End-to-End Encrypted Session
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {activeTab === 'adda' && (
          <div style={{ flex: 1, padding: 16, overflowY: 'auto' }}>
            <h3 style={{ fontSize: 16, margin: '0 0 12px 0' }}>Live Adda Rooms 🎙️</h3>
            {addaRooms.map(room => (
              <div key={room.id} style={{ background: '#1E293B', borderRadius: 12, padding: 14, marginBottom: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <span style={{ fontSize: 10, background: '#EF444422', color: '#EF4444', padding: '2px 6px', borderRadius: 8, fontWeight: 700 }}>● LIVE</span>
                  <span style={{ fontSize: 11, color: '#94A3B8' }}>{room.listener_count} listening</span>
                </div>
                <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>{room.title}</div>
                <div style={{ fontSize: 12, color: '#94A3B8', marginBottom: 10 }}>{room.topic}</div>
                <button style={{ width: '100%', padding: '8px 0', background: '#06B6D4', border: 'none', borderRadius: 6, color: '#FFF', fontWeight: 600, cursor: 'pointer' }}>
                  Join Adda Stage
                </button>
              </div>
            ))}
          </div>
        )}

        {activeTab === 'admin' && (
          <div style={{ flex: 1, padding: 16, overflowY: 'auto' }}>
            <h3 style={{ fontSize: 16, margin: '0 0 12px 0', color: '#06B6D4' }}>Admin Panel</h3>
            <div style={{ background: '#1E293B', padding: 12, borderRadius: 8, marginBottom: 12 }}>
              <div style={{ fontSize: 11, color: '#94A3B8' }}>Active Users</div>
              <div style={{ fontSize: 20, fontWeight: 700, color: '#FFF' }}>1,482</div>
            </div>
            <div style={{ background: '#1E293B', padding: 12, borderRadius: 8, marginBottom: 12 }}>
              <div style={{ fontSize: 11, color: '#94A3B8' }}>E2EE Encrypted Messages</div>
              <div style={{ fontSize: 20, fontWeight: 700, color: '#10B981' }}>284,910</div>
            </div>
            <div style={{ background: '#1E293B', padding: 12, borderRadius: 8 }}>
              <div style={{ fontSize: 11, color: '#94A3B8' }}>Pending Reports</div>
              <div style={{ fontSize: 20, fontWeight: 700, color: '#F59E0B' }}>0</div>
            </div>
          </div>
        )}
      </div>

      {/* MAIN CHAT AREA */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: '#0B0F19' }}>
        {/* HEADER */}
        <div style={{ height: 68, borderBottom: '1px solid #1E293B', padding: '0 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ width: 40, height: 40, borderRadius: 20, background: '#334155', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 600 }}>
              {selectedConv.type === 'direct' ? (selectedConv.direct_peer_name?.[0] || 'U') : 'G'}
            </div>
            <div>
              <div style={{ fontWeight: 600, fontSize: 15 }}>{selectedConv.type === 'direct' ? selectedConv.direct_peer_name : selectedConv.title}</div>
              <div style={{ fontSize: 12, color: '#10B981', display: 'flex', alignItems: 'center', gap: 4 }}>
                <Lock size={12} /> End-to-End Encrypted
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <button onClick={() => setShowSafetyModal(true)} style={{ background: '#1E293B', border: 'none', padding: '8px 12px', borderRadius: 8, color: '#06B6D4', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, fontWeight: 600 }}>
              <Shield size={14} /> Verify Safety Number
            </button>
            <button onClick={() => setInCall('audio')} style={{ background: '#1E293B', border: 'none', width: 36, height: 36, borderRadius: 8, color: '#FFF', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Phone size={18} />
            </button>
            <button onClick={() => setInCall('video')} style={{ background: '#1E293B', border: 'none', width: 36, height: 36, borderRadius: 8, color: '#FFF', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Video size={18} />
            </button>
          </div>
        </div>

        {/* MESSAGES BODY */}
        <div style={{ flex: 1, padding: 20, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div style={{ alignSelf: 'center', background: '#1E293B66', padding: '6px 14px', borderRadius: 20, fontSize: 11, color: '#94A3B8', display: 'flex', alignItems: 'center', gap: 6 }}>
            <Lock size={12} color="#06B6D4" /> Messages and calls are end-to-end encrypted. No one outside of this chat, not even MATE CHAT, can read or listen to them.
          </div>

          {messages.map(msg => (
            <div 
              key={msg.id} 
              onMouseEnter={() => setHoveredMsgId(msg.id)}
              onMouseLeave={() => setHoveredMsgId(null)}
              style={{ alignSelf: msg.is_me ? 'flex-end' : 'flex-start', maxWidth: '65%', position: 'relative' }}
            >
              {/* Floating Reaction Popover on Hover or Click */}
              {(hoveredMsgId === msg.id || activeReactionPopoverId === msg.id) && (
                <div style={{
                  position: 'absolute',
                  top: -38,
                  [msg.is_me ? 'right' : 'left']: 0,
                  background: '#1E293B',
                  border: '1px solid #334155',
                  borderRadius: 20,
                  padding: '4px 8px',
                  display: 'flex',
                  gap: 6,
                  boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
                  zIndex: 10
                }}>
                  {['👍', '❤️', '🔥', '😂', '😮', '😢', '🙏', '👏', '🎉'].map(emoji => (
                    <button
                      key={emoji}
                      onClick={() => handleReact(msg.id, emoji)}
                      style={{
                        background: msg.reaction_emoji === emoji ? '#06B6D433' : 'transparent',
                        border: 'none',
                        fontSize: 16,
                        cursor: 'pointer',
                        padding: '2px 4px',
                        borderRadius: 8
                      }}
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
              )}

              <div 
                onContextMenu={(e) => { e.preventDefault(); setActiveReactionPopoverId(msg.id); }}
                style={{
                  background: msg.is_me ? 'linear-gradient(135deg, #0284C7, #4F46E5)' : '#1E293B',
                  color: '#FFF',
                  padding: '10px 14px',
                  borderRadius: msg.is_me ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
                  fontSize: 14,
                  boxShadow: '0 2px 4px rgba(0,0,0,0.15)',
                  cursor: 'pointer'
                }}
              >
                {msg.decrypted_text}
              </div>

              {/* Reaction display below message */}
              {msg.reaction_emoji && (
                <div 
                  onClick={() => handleReact(msg.id, msg.reaction_emoji!)}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 4,
                    marginTop: 4,
                    background: '#1E293B',
                    border: '1px solid #06B6D488',
                    borderRadius: 12,
                    padding: '2px 8px',
                    fontSize: 12,
                    cursor: 'pointer',
                    float: msg.is_me ? 'right' : 'left'
                  }}
                >
                  <span>{msg.reaction_emoji}</span>
                  <span style={{ fontSize: 10, fontWeight: 700, color: '#06B6D4' }}>1</span>
                </div>
              )}

              <div style={{ clear: 'both', fontSize: 10, color: '#64748B', marginTop: 4, textAlign: msg.is_me ? 'right' : 'left', display: 'flex', alignItems: 'center', justifyContent: msg.is_me ? 'flex-end' : 'flex-start', gap: 4 }}>
                {msg.created_at} {msg.is_me && <CheckCheck size={12} color="#06B6D4" />}
              </div>
            </div>
          ))}
        </div>

        {/* INPUT BAR */}
        <div style={{ padding: '16px 20px', borderTop: '1px solid #1E293B', background: '#0F172A', display: 'flex', alignItems: 'center', gap: 12 }}>
          <button style={{ background: 'transparent', border: 'none', color: '#94A3B8', cursor: 'pointer' }}>
            <Paperclip size={20} />
          </button>
          <input 
            value={inputMsg}
            onChange={e => setInputMsg(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSend()}
            placeholder="Type an end-to-end encrypted message..." 
            style={{ flex: 1, background: '#1E293B', border: '1px solid #334155', borderRadius: 24, padding: '12px 18px', color: '#FFF', outline: 'none', fontSize: 14 }}
          />
          <button style={{ background: 'transparent', border: 'none', color: '#94A3B8', cursor: 'pointer' }}>
            <Mic size={20} />
          </button>
          <button onClick={handleSend} style={{ width: 44, height: 44, borderRadius: 22, background: '#06B6D4', border: 'none', color: '#FFF', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
            <Send size={18} />
          </button>
        </div>
      </div>

      {/* WEBRTC CALL OVERLAY MODAL */}
      {inCall && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(11, 15, 25, 0.95)', zIndex: 100, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ width: 100, height: 100, borderRadius: 50, background: '#334155', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 36, fontWeight: 700, marginBottom: 16 }}>
            {selectedConv.direct_peer_name?.[0] || 'U'}
          </div>
          <h2 style={{ fontSize: 24, fontWeight: 700, margin: '0 0 6px 0' }}>{selectedConv.direct_peer_name}</h2>
          <div style={{ fontSize: 14, color: '#10B981', display: 'flex', alignItems: 'center', gap: 6, marginBottom: 24 }}>
            <Lock size={14} /> WebRTC E2EE {inCall === 'video' ? 'Video' : 'Voice'} Call • {formatSeconds(callDuration)}
          </div>

          {inCall === 'video' && (
            <div style={{ width: 480, height: 280, background: '#1E293B', borderRadius: 16, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#94A3B8', marginBottom: 24, position: 'relative' }}>
              <Video size={48} />
              <div style={{ position: 'absolute', bottom: 12, right: 12, width: 100, height: 75, background: '#0F172A', borderRadius: 8, border: '1px solid #334155', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Camera size={20} color="#06B6D4" />
              </div>
            </div>
          )}

          <div style={{ display: 'flex', gap: 20 }}>
            <button onClick={() => setIsMicMuted(!isMicMuted)} style={{ width: 56, height: 56, borderRadius: 28, background: isMicMuted ? '#EF4444' : '#1E293B', border: 'none', color: '#FFF', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
              {isMicMuted ? <MicOff size={24} /> : <Mic size={24} />}
            </button>
            <button onClick={() => setInCall(null)} style={{ width: 56, height: 56, borderRadius: 28, background: '#EF4444', border: 'none', color: '#FFF', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
              <PhoneOff size={24} />
            </button>
          </div>
        </div>
      )}

      {/* SAFETY NUMBER VERIFICATION MODAL */}
      {showSafetyModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.8)', zIndex: 90, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: '#1E293B', borderRadius: 16, padding: 24, width: 420, border: '1px solid #334155' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
              <Shield size={24} color="#06B6D4" />
              <h3 style={{ margin: 0, fontSize: 18 }}>Verify Safety Number</h3>
            </div>
            <p style={{ fontSize: 13, color: '#94A3B8', lineHeight: 1.5 }}>
              Compare this 60-digit fingerprint with <strong>{selectedConv.direct_peer_name}</strong> to verify end-to-end encryption authenticity.
            </p>
            <div style={{ background: '#0F172A', padding: 16, borderRadius: 8, fontFamily: 'monospace', fontSize: 13, letterSpacing: 1.5, color: '#38BDF8', textAlign: 'center', margin: '16px 0', lineHeight: 2 }}>
              {safetyNumber}
            </div>
            <button onClick={() => setShowSafetyModal(false)} style={{ width: '100%', padding: '10px 0', background: '#06B6D4', border: 'none', borderRadius: 8, color: '#FFF', fontWeight: 600, cursor: 'pointer' }}>
              Confirm & Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
