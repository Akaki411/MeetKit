/**
 * Главный экран звонков: быстрое создание и вход в комнату, список недавних вызовов.
 */
package com.livekit.meetkit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.data.ServerPreferences
import com.livekit.meetkit.data.models.UserMe
import com.livekit.meetkit.ui.components.*
import com.livekit.meetkit.ui.icons.Logo
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun CallsScreen(
    userMe: UserMe,
    apiClient: ApiClient,
    prefs: ServerPreferences,
    onStartCall: (roomName: String, password: String?) -> Unit
) {
    var roomNameInput by remember { mutableStateOf("") }
    var withPassword by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }
    var recentRooms by remember { mutableStateOf(prefs.getRecentRooms()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val isAuthed = userMe.login != null && userMe.role != "guest"

    fun formatRelativeTime(ts: Long): String {
        val min = (System.currentTimeMillis() - ts) / 60000
        if (min < 1) return "только что"
        if (min < 60) return "$min мин назад"
        val hours = min / 60
        if (hours < 24) return "$hours ч назад"
        val days = hours / 24
        if (days < 7) return "$days д назад"
        return java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(ts)
    }

    val submitStart = {
        createError = null
        val trimmed = roomNameInput.trim().replace("\\s+".toRegex(), "-")
        val target = if (trimmed.isNotEmpty()) trimmed else UUID.randomUUID().toString().substring(0, 8)

        if (isAuthed && withPassword && passwordInput.trim().isNotEmpty()) {
            isLoading = true
            scope.launch {
                val result = apiClient.createRoom(target, passwordInput)
                isLoading = false
                if (result.isSuccess) {
                    onStartCall(target, passwordInput)
                } else {
                    val err = result.exceptionOrNull()?.message
                    if (err == "room_exists") {
                        createError = "Комната с таким названием уже существует"
                    } else {
                        createError = "Ошибка создания комнаты с паролем"
                    }
                }
            }
        } else {
            onStartCall(target, if (withPassword) passwordInput else null)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(32.dp))

            Image(
                imageVector = Logo.LiveKitMeet,
                contentDescription = "LiveKit Meet",
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .aspectRatio(Logo.ASPECT_RATIO)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Введите название комнаты, чтобы создать новую встречу или присоединиться к существующей.",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            WebCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(16.dp),
                spacing = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WebField(
                        value = roomNameInput,
                        onValueChange = { roomNameInput = it },
                        placeholder = "Название комнаты",
                        modifier = Modifier.weight(1f)
                    )
                    if (isAuthed) {
                        WebButton(
                            onClick = { withPassword = !withPassword },
                            fill = if (withPassword) Fill10 else Fill08,
                            height = 51.dp,
                            contentPadding = PaddingValues(horizontal = 15.dp)
                        ) {
                            Icon(
                                imageVector = if (withPassword) Tabler.Lock else Tabler.LockOpen2,
                                contentDescription = "Установить пароль",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.8.dp)
                            )
                        }
                    }
                }

                if (isAuthed) {
                    if (withPassword) {
                        WebField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            placeholder = "Пароль комнаты",
                            password = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (createError != null) {
                    Text(
                        text = createError!!,
                        color = ErrorRed,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                WebButton(
                    onClick = { submitStart() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 32.dp)
                ) {
                    WebButtonText(if (isLoading) "Создание…" else "Начать")
                }
            }
        }

        if (recentRooms.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                WebCaption(
                    text = "Недавние комнаты",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                )
            }

            items(recentRooms, key = { it.name }) { recent ->
                Row(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .glass(FieldShape, Fill05)
                        .clickable { onStartCall(recent.name, null) }
                        .padding(start = 14.4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f).padding(vertical = 7.dp)) {
                        Text(
                            text = recent.name,
                            fontSize = 15.2.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatRelativeTime(recent.ts),
                            fontSize = 12.8.sp,
                            color = TextMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(4.8.dp))
                            .clickable {
                                prefs.removeRecentRoom(recent.name)
                                recentRooms = prefs.getRecentRooms()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Tabler.X,
                            contentDescription = "Удалить из недавних",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
