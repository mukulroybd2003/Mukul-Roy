package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ActiveCallOverlay
import com.example.ui.screens.AddaRoomsScreen
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.MateChatTheme
import com.example.ui.theme.MatePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.MateChatViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: MateChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MateChatTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: MateChatViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()

    // Handle back button across screens and active call
    BackHandler(enabled = true) {
        viewModel.handleBack()
    }

    val showBottomNav = currentScreen in listOf(
        Screen.HOME,
        Screen.CHAT_LIST,
        Screen.ADDA_ROOMS,
        Screen.CALL_HISTORY,
        Screen.FRIENDS,
        Screen.USER_PROFILE
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            if (showBottomNav) {
                BottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    Screen.HOME, Screen.CHAT_LIST -> HomeScreen(viewModel = viewModel)
                    Screen.PRIVATE_CHAT, Screen.GROUP_CHAT -> ChatDetailScreen(viewModel = viewModel)
                    Screen.CALL_HISTORY -> CallsScreen(viewModel = viewModel)
                    Screen.ADDA_ROOMS -> AddaRoomsScreen(viewModel = viewModel)
                    Screen.FRIENDS -> FriendsScreen(viewModel = viewModel)
                    Screen.USER_PROFILE -> ProfileScreen(viewModel = viewModel)
                    Screen.SETTINGS, Screen.PRIVACY_SETTINGS, Screen.BLOCKED_USERS, Screen.SECURITY_DEVICES -> SettingsScreen(viewModel = viewModel)
                    Screen.LOGIN -> LoginScreen(viewModel = viewModel)
                    Screen.REGISTER -> RegisterScreen(viewModel = viewModel)
                    else -> HomeScreen(viewModel = viewModel)
                }
            }

            // WebRTC Call Overlay (rendered over any screen when in active call)
            activeCall?.let { callState ->
                ActiveCallOverlay(callState = callState, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .testTag("bottom_nav_bar")
    ) {
        val navItems = listOf(
            Triple(Screen.HOME, Icons.Default.ChatBubble, "Chats"),
            Triple(Screen.ADDA_ROOMS, Icons.Default.Radio, "Adda"),
            Triple(Screen.CALL_HISTORY, Icons.Default.Phone, "Calls"),
            Triple(Screen.FRIENDS, Icons.Default.Group, "Friends"),
            Triple(Screen.USER_PROFILE, Icons.Default.Person, "Profile")
        )

        navItems.forEach { (screen, icon, label) ->
            val isSelected = currentScreen == screen || (screen == Screen.HOME && currentScreen == Screen.CHAT_LIST)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(screen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MatePrimary,
                    selectedTextColor = MatePrimary,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = MatePrimary.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_item_${label.lowercase()}")
            )
        }
    }
}
