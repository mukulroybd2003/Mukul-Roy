package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CallHistoryEntity
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.MatePrimary
import com.example.ui.theme.StatusBusy
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ActiveCallState
import com.example.ui.viewmodel.MateChatViewModel

@Composable
fun CallsScreen(
    viewModel: MateChatViewModel,
    modifier: Modifier = Modifier
) {
    val callHistory by viewModel.callHistory.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.handleBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Call History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Encrypted WebRTC Audio & Video",
                    fontSize = 11.sp,
                    color = MatePrimary
                )
            }
        }

        // CALLS LIST
        if (callHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No recent calls", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                items(callHistory, key = { it.id }) { call ->
                    CallHistoryItem(
                        call = call,
                        onCallBack = {
                            viewModel.startCall(call.peerName, call.callType)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun CallHistoryItem(
    call: CallHistoryEntity,
    onCallBack: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = call.peerName.take(1),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.peerName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, color) = when {
                        call.status == "missed" -> Pair(Icons.Default.PhoneMissed, StatusBusy)
                        call.isIncoming -> Pair(Icons.AutoMirrored.Filled.CallReceived, StatusOnline)
                        else -> Pair(Icons.AutoMirrored.Filled.CallMade, MatePrimary)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = call.status,
                        tint = color,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${call.timestamp} • ${if (call.callType == "video") "Video" else "Audio"}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            IconButton(
                onClick = onCallBack,
                modifier = Modifier.testTag("call_back_button")
            ) {
                Icon(
                    imageVector = if (call.callType == "video") Icons.Default.Videocam else Icons.Default.Phone,
                    contentDescription = "Call Back",
                    tint = MatePrimary
                )
            }
        }
    }
}

@Composable
fun ActiveCallOverlay(
    callState: ActiveCallState,
    viewModel: MateChatViewModel
) {
    val durationText = "${callState.durationSeconds / 60}:${(callState.durationSeconds % 60).toString().padStart(2, '0')}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP INFO
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = callState.peerName.take(1),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = callState.peerName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = StatusOnline,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (callState.isConnected) "E2EE Connected • $durationText" else "Connecting WebRTC peer...",
                        fontSize = 13.sp,
                        color = if (callState.isConnected) StatusOnline else MatePrimary
                    )
                }
            }

            // VIDEO SIMULATION / PIP
            if (callState.callType == "video") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Feed",
                            tint = TextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "WebRTC Video Stream (Active)",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    // PIP Local Video
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .size(width = 80.dp, height = 110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .border(1.5.dp, MatePrimary, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You",
                            fontSize = 11.sp,
                            color = MatePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // CONTROLS ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute Button
                IconButton(
                    onClick = { viewModel.toggleCallMute() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (callState.isMuted) Color(0xFFEF4444) else DarkSurfaceCard)
                        .testTag("mute_call_button")
                ) {
                    Icon(
                        imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = Color.White
                    )
                }

                // Speaker Button
                IconButton(
                    onClick = { viewModel.toggleCallSpeaker() },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (callState.isSpeakerOn) MatePrimary else DarkSurfaceCard)
                        .testTag("speaker_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speaker",
                        tint = Color.White
                    )
                }

                // Camera Toggle (for video)
                if (callState.callType == "video") {
                    IconButton(
                        onClick = { viewModel.toggleCallCamera() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (!callState.isCameraOn) Color(0xFFEF4444) else DarkSurfaceCard)
                    ) {
                        Icon(
                            imageVector = if (callState.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Camera",
                            tint = Color.White
                        )
                    }
                }

                // End Call Button
                IconButton(
                    onClick = { viewModel.endCall() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
