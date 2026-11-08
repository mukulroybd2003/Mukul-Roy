package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MessageEntity
import com.example.ui.theme.BubbleIncoming
import com.example.ui.theme.BubbleOutgoing
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.MatePrimary
import com.example.ui.theme.MateSecondary
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MateChatViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun ChatDetailScreen(
    viewModel: MateChatViewModel,
    modifier: Modifier = Modifier
) {
    val conversation by viewModel.selectedConversation.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val safetyNumber by viewModel.safetyNumber.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showSafetyDialog by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val peerName = conversation?.let {
        if (it.type == "group") it.title ?: "Group"
        else it.peerFullName ?: it.peerUsername ?: "Chat"
    } ?: "Chat"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.handleBack() },
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                // Peer Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = peerName.take(1),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title + Encryption state
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showSafetyDialog = true }
                ) {
                    Text(
                        text = peerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "E2EE",
                            tint = StatusOnline,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "End-to-End Encrypted",
                            fontSize = 11.sp,
                            color = StatusOnline
                        )
                    }
                }

                // WebRTC Audio Call Action
                IconButton(
                    onClick = { viewModel.startCall(peerName, "audio") },
                    modifier = Modifier.testTag("audio_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Voice Call",
                        tint = TextPrimary
                    )
                }

                // WebRTC Video Call Action
                IconButton(
                    onClick = { viewModel.startCall(peerName, "video") },
                    modifier = Modifier.testTag("video_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = TextPrimary
                    )
                }

                // Verify Safety Number Action
                IconButton(
                    onClick = { showSafetyDialog = true },
                    modifier = Modifier.testTag("verify_key_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Verify Safety Number",
                        tint = MatePrimary
                    )
                }
            }

            // ENCRYPTION NOTICE BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MatePrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Messages are secured with audited E2EE. Tap to verify.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // MESSAGES LAZY LIST
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                reverseLayout = false
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        onReact = { emoji -> viewModel.reactToMessage(msg.id, emoji) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // BOTTOM BAR / INPUT ROW
            if (isRecordingVoice) {
                VoiceRecordingBar(
                    durationSeconds = recordingDuration,
                    onCancel = { viewModel.cancelVoiceRecording() },
                    onSend = { viewModel.stopAndSendVoiceRecording() }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentMenu = true },
                        modifier = Modifier.testTag("attachment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attachment",
                            tint = TextSecondary
                        )
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                "End-to-End Encrypted Message",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MatePrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = false,
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .testTag("message_input")
                    )

                    if (textInput.isBlank()) {
                        IconButton(
                            onClick = { viewModel.startVoiceRecording() },
                            modifier = Modifier.testTag("voice_record_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Voice Note",
                                tint = MatePrimary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.sendTextMessage(textInput)
                                textInput = ""
                            },
                            modifier = Modifier.testTag("send_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MatePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SAFETY NUMBER VERIFICATION DIALOG
        if (showSafetyDialog) {
            AlertDialog(
                onDismissRequest = { showSafetyDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MatePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Verify Safety Number",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "To verify that your end-to-end encrypted messages with $peerName are secure, compare the 60-digit number below with their device:",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = safetyNumber,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MatePrimary,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showSafetyDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = MatePrimary)
                    ) {
                        Text("Verified & Close", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DarkSurfaceCard
            )
        }

        // ATTACHMENT MENU DIALOG
        if (showAttachmentMenu) {
            AlertDialog(
                onDismissRequest = { showAttachmentMenu = false },
                title = { Text("Encrypted Attachment", color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.sendPhotoMessage()
                                showAttachmentMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📷 Send Encrypted Photo", color = MatePrimary)
                        }
                        TextButton(
                            onClick = {
                                viewModel.sendTextMessage("Shared file: Audit_Report_2026.pdf (Encrypted)")
                                showAttachmentMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📄 Send Encrypted Document", color = MateSecondary)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAttachmentMenu = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurfaceCard
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    onReact: (String) -> Unit
) {
    val isOutgoing = message.isOutgoing
    val bubbleColor = if (isOutgoing) BubbleOutgoing else BubbleIncoming
    var showReactionPopover by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    val reactionEmojis = listOf("👍", "❤️", "🔥", "😂", "😮", "😢", "🙏", "👏", "🎉")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            },
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .pointerInput(message.id) {
                    detectTapGestures(
                        onLongPress = {
                            showReactionPopover = true
                        },
                        onTap = {
                            // On hover or double tap, also toggle reaction popover
                            if (isHovered) {
                                showReactionPopover = !showReactionPopover
                            }
                        }
                    )
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("message_bubble_${message.id}")
        ) {
            Column {
                if (message.messageType == "voice_note") {
                    VoiceNotePlayer(duration = message.voiceDurationSeconds ?: 5)
                } else if (message.messageType == "image") {
                    Box(
                        modifier = Modifier
                            .size(width = 240.dp, height = 140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔐 Encrypted Photo (Preview)",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    Text(
                        text = message.decryptedText ?: "Encrypted ciphertext",
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // FLOATING REACTION POPOVER (triggered by hover or long-press)
        if (showReactionPopover || isHovered) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkSurfaceCard)
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(24.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("reaction_popover_${message.id}")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    reactionEmojis.forEach { emoji ->
                        val isSelected = message.reactionEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MatePrimary.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    onReact(emoji)
                                    showReactionPopover = false
                                }
                                .testTag("reaction_emoji_$emoji"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        // POPUP REACTION POPOVER FALLBACK (guaranteed overlay above bubble on long-press)
        if (showReactionPopover && !isHovered) {
            Popup(
                alignment = if (isOutgoing) Alignment.TopEnd else Alignment.TopStart,
                offset = IntOffset(x = if (isOutgoing) -12 else 12, y = -100),
                onDismissRequest = { showReactionPopover = false },
                properties = PopupProperties(focusable = true)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, MatePrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(4.dp).testTag("reaction_floating_popup_${message.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        reactionEmojis.forEach { emoji ->
                            val isSelected = message.reactionEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MatePrimary.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable {
                                        onReact(emoji)
                                        showReactionPopover = false
                                    }
                                    .testTag("reaction_popup_emoji_$emoji"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }

        // REACTIONS DISPLAY BELOW MESSAGE
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!message.reactionEmoji.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceCard)
                        .border(1.dp, MatePrimary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .clickable {
                            // Clicking toggles reaction or opens popover
                            showReactionPopover = !showReactionPopover
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("reaction_badge_${message.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.reactionEmoji,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "1",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatePrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Quick add reaction button pill
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceCard.copy(alpha = 0.8f))
                    .border(1.dp, DarkBorder, CircleShape)
                    .clickable {
                        showReactionPopover = !showReactionPopover
                    }
                    .testTag("add_reaction_trigger_${message.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (message.reactionEmoji.isNullOrBlank()) "＋" else "✏️",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VoiceNotePlayer(duration: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play voice note",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Simulated Waveform Bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(14, 22, 10, 28, 18, 30, 24, 12, 26, 16, 20, 8, 22, 14).forEach { heightDp ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(heightDp.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "0:${duration.toString().padStart(2, '0')}",
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VoiceRecordingBar(
    durationSeconds: Int,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Recording 0:${durationSeconds.toString().padStart(2, '0')}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFEF4444)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MatePrimary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send voice note",
                    tint = Color.White
                )
            }
        }
    }
}
