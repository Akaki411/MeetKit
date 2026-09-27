/**
 * Главная Activity приложения: управляет навигацией, авторизацией и отображением экранов.
 */
package com.livekit.meetkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.data.ServerPreferences
import com.livekit.meetkit.data.models.UserMe
import com.livekit.meetkit.ui.components.ParticleBackground
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.screens.*
import com.livekit.meetkit.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeetKitTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val prefs = remember { ServerPreferences(context) }
    val apiClient = remember { ApiClient(context) }

    var serverUrl by remember { mutableStateOf(prefs.serverUrl) }
    var userMe by remember { mutableStateOf<UserMe?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var activeCallRoom by remember { mutableStateOf<Pair<String, String?>?>(null) }
    val scope = rememberCoroutineScope()

    val loadMe = {
        scope.launch {
            val res = apiClient.getMe()
            if (res.isSuccess) {
                userMe = res.getOrNull()
            } else {
                userMe = UserMe()
            }
        }
    }

    LaunchedEffect(serverUrl) {
        if (serverUrl.isNotEmpty()) {
            loadMe()
        }
    }

    ParticleBackground(active = activeCallRoom == null) {
        if (serverUrl.isEmpty()) {
            ServerSetupScreen(
                apiClient = apiClient,
                prefs = prefs,
                onSuccess = {
                    serverUrl = prefs.serverUrl
                }
            )
        } else {
            val currentUser = userMe ?: UserMe()
            val isAdmin = currentUser.isAdmin

            val tabs = if (isAdmin) {
                listOf(
                    NavigationTabItem("Звонки", Tabler.Phone),
                    NavigationTabItem("Админка", Tabler.LayoutDashboard),
                    NavigationTabItem("Аккаунт", Tabler.User)
                )
            } else {
                listOf(
                    NavigationTabItem("Звонки", Tabler.Phone),
                    NavigationTabItem("Аккаунт", Tabler.User)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        Column {
                            HorizontalDivider(thickness = 1.dp, color = DarkCardBorder)
                            NavigationBar(
                                containerColor = DarkBackground,
                                contentColor = TextPrimary,
                                tonalElevation = 0.dp
                            ) {
                                tabs.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        icon = {
                                            Icon(
                                                item.icon,
                                                contentDescription = item.label,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = { Text(item.label, fontSize = 12.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = TextPrimary,
                                            selectedTextColor = TextPrimary,
                                            indicatorColor = Color.Transparent,
                                            unselectedIconColor = TextMuted,
                                            unselectedTextColor = TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isAdmin) {
                            when (selectedTab) {
                                0 -> CallsScreen(
                                    userMe = currentUser,
                                    apiClient = apiClient,
                                    prefs = prefs,
                                    onStartCall = { room, pass -> activeCallRoom = Pair(room, pass) }
                                )
                                1 -> AdminScreen(
                                    userMe = currentUser,
                                    apiClient = apiClient
                                )
                                2 -> AccountScreen(
                                    userMe = currentUser,
                                    apiClient = apiClient,
                                    onRefreshMe = { loadMe() },
                                    onChangeServer = {
                                        prefs.clearServerUrl()
                                        serverUrl = ""
                                        selectedTab = 0
                                    }
                                )
                            }
                        } else {
                            when (selectedTab) {
                                0 -> CallsScreen(
                                    userMe = currentUser,
                                    apiClient = apiClient,
                                    prefs = prefs,
                                    onStartCall = { room, pass -> activeCallRoom = Pair(room, pass) }
                                )
                                1 -> AccountScreen(
                                    userMe = currentUser,
                                    apiClient = apiClient,
                                    onRefreshMe = { loadMe() },
                                    onChangeServer = {
                                        prefs.clearServerUrl()
                                        serverUrl = ""
                                        selectedTab = 0
                                    }
                                )
                            }
                        }
                    }
                }

                if (activeCallRoom != null) {
                    val (roomName, password) = activeCallRoom!!
                    CallOverlayScreen(
                        roomName = roomName,
                        initialPassword = password,
                        userMe = currentUser,
                        apiClient = apiClient,
                        prefs = prefs,
                        onDismiss = { activeCallRoom = null }
                    )
                }
            }
        }
    }
}

data class NavigationTabItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
