package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoEngine
import com.example.data.local.AddaRoomEntity
import com.example.data.local.CallHistoryEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.repository.MateChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    SPLASH,
    ONBOARDING,
    LOGIN,
    REGISTER,
    HOME,
    CHAT_LIST,
    PRIVATE_CHAT,
    GROUP_CHAT,
    FRIENDS,
    USER_PROFILE,
    EDIT_PROFILE,
    CALL_HISTORY,
    ADDA_ROOMS,
    SETTINGS,
    PRIVACY_SETTINGS,
    BLOCKED_USERS,
    SECURITY_DEVICES
}

data class ActiveCallState(
    val peerName: String,
    val callType: String, // "audio" or "video"
    val isIncoming: Boolean,
    val isConnected: Boolean,
    val durationSeconds: Int,
    val isMuted: Boolean,
    val isSpeakerOn: Boolean,
    val isCameraOn: Boolean
)

data class FriendItem(
    val id: Long,
    val name: String,
    val username: String,
    val isOnline: Boolean,
    val status: String
)

class MateChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MateChatRepository(application)

    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenBackStack = mutableListOf<Screen>()

    // Current logged-in user
    val currentUserId = 1L
    val currentUserName = "Alex Rivers"
    val currentUserHandle = "@alex_mate"

    // Conversations & Messages
    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callHistory: StateFlow<List<CallHistoryEntity>> = repository.callHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val addaRooms: StateFlow<List<AddaRoomEntity>> = repository.liveAddaRooms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedConversation = MutableStateFlow<ConversationEntity?>(null)
    val selectedConversation: StateFlow<ConversationEntity?> = _selectedConversation.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<MessageEntity>> = _currentMessages.asStateFlow()

    private var messagesJob: Job? = null

    // Call state
    private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
    val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()
    private var callTimerJob: Job? = null

    // Voice message recording simulation
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0)
    val recordingDurationSeconds: StateFlow<Int> = _recordingDurationSeconds.asStateFlow()
    private var recordTimerJob: Job? = null

    // Safety Number Verification
    private val _safetyNumber = MutableStateFlow("")
    val safetyNumber: StateFlow<String> = _safetyNumber.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Friends list
    val friendsList = listOf(
        FriendItem(2L, "Sophia Zhang", "@sophia_code", true, "Security & Crypto Engineer 🔐"),
        FriendItem(3L, "Marcus Vance", "@marcus_design", false, "Visual & Motion Design ✨"),
        FriendItem(4L, "Emma Watson", "@emma_sound", true, "Tech Talk Podcaster 🎙️"),
        FriendItem(5L, "Liam Chen", "@liam_ops", false, "Cloud Infrastructure ☁️")
    )

    fun navigateTo(screen: Screen) {
        _screenBackStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun handleBack(): Boolean {
        if (_activeCall.value != null) {
            endCall()
            return true
        }
        if (_screenBackStack.isNotEmpty()) {
            _currentScreen.value = _screenBackStack.removeAt(_screenBackStack.size - 1)
            return true
        }
        if (_currentScreen.value != Screen.HOME) {
            _currentScreen.value = Screen.HOME
            return true
        }
        return false
    }

    fun openConversation(conversation: ConversationEntity) {
        _selectedConversation.value = conversation
        viewModelScope.launch {
            repository.markConversationRead(conversation.id)
        }
        // Load messages for conversation
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessagesForConversation(conversation.id).collect { msgs ->
                _currentMessages.value = msgs
            }
        }
        // Compute E2EE Safety Number
        val myPub = CryptoEngine.getLocalPublicKeyBase64()
        val peerPub = conversation.peerPublicKey ?: myPub
        _safetyNumber.value = CryptoEngine.generateSafetyNumber(myPub, peerPub)

        if (conversation.type == "group") {
            navigateTo(Screen.GROUP_CHAT)
        } else {
            navigateTo(Screen.PRIVATE_CHAT)
        }
    }

    fun sendTextMessage(text: String) {
        val conv = _selectedConversation.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = conv.id,
                senderId = currentUserId,
                senderName = currentUserName,
                plainText = text.trim(),
                peerPublicKey = conv.peerPublicKey,
                messageType = "text"
            )
        }
    }

    fun startVoiceRecording() {
        _isRecordingVoice.value = true
        _recordingDurationSeconds.value = 0
        recordTimerJob?.cancel()
        recordTimerJob = viewModelScope.launch {
            while (_isRecordingVoice.value) {
                delay(1000)
                _recordingDurationSeconds.value += 1
            }
        }
    }

    fun stopAndSendVoiceRecording() {
        val conv = _selectedConversation.value ?: return
        val duration = _recordingDurationSeconds.value
        _isRecordingVoice.value = false
        recordTimerJob?.cancel()
        if (duration < 1) return

        viewModelScope.launch {
            repository.sendMessage(
                conversationId = conv.id,
                senderId = currentUserId,
                senderName = currentUserName,
                plainText = "Voice message (${duration}s)",
                peerPublicKey = conv.peerPublicKey,
                messageType = "voice_note",
                voiceDuration = duration
            )
        }
    }

    fun cancelVoiceRecording() {
        _isRecordingVoice.value = false
        recordTimerJob?.cancel()
        _recordingDurationSeconds.value = 0
    }

    fun sendPhotoMessage() {
        val conv = _selectedConversation.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = conv.id,
                senderId = currentUserId,
                senderName = currentUserName,
                plainText = "Attached encrypted photo",
                peerPublicKey = conv.peerPublicKey,
                messageType = "image"
            )
        }
    }

    fun reactToMessage(messageId: Long, emoji: String) {
        val currentMsg = _currentMessages.value.find { it.id == messageId }
        val newEmoji = if (currentMsg?.reactionEmoji == emoji) null else emoji
        viewModelScope.launch {
            repository.reactToMessage(messageId, newEmoji)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Call management
    fun startCall(peerName: String, callType: String) {
        _activeCall.value = ActiveCallState(
            peerName = peerName,
            callType = callType,
            isIncoming = false,
            isConnected = false,
            durationSeconds = 0,
            isMuted = false,
            isSpeakerOn = callType == "video",
            isCameraOn = callType == "video"
        )
        // Simulate connecting after 1.5 seconds
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            delay(1500)
            _activeCall.value = _activeCall.value?.copy(isConnected = true)
            while (_activeCall.value != null) {
                delay(1000)
                _activeCall.value = _activeCall.value?.let { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    fun toggleCallMute() {
        _activeCall.value = _activeCall.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleCallSpeaker() {
        _activeCall.value = _activeCall.value?.let { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun toggleCallCamera() {
        _activeCall.value = _activeCall.value?.let { it.copy(isCameraOn = !it.isCameraOn) }
    }

    fun endCall() {
        val currentCall = _activeCall.value ?: return
        viewModelScope.launch {
            repository.logCall(
                peerName = currentCall.peerName,
                callType = currentCall.callType,
                isIncoming = currentCall.isIncoming,
                status = "completed",
                durationSeconds = currentCall.durationSeconds
            )
        }
        callTimerJob?.cancel()
        _activeCall.value = null
    }
}
