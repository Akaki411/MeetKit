/**
 * Экран аккаунта: авторизация пользователя, смена логина/пароля, загрузка аватара и выбор сервера.
 */
package com.livekit.meetkit.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.data.models.UserMe
import com.livekit.meetkit.ui.components.*
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    userMe: UserMe,
    apiClient: ApiClient,
    onRefreshMe: () -> Unit,
    onChangeServer: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (userMe.login == null) {
        var loginInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        val doLogin = {
            if (loginInput.isNotBlank() && passwordInput.isNotBlank()) {
                isLoading = true
                errorMsg = null
                scope.launch {
                    val result = apiClient.login(loginInput.trim(), passwordInput)
                    isLoading = false
                    if (result.isSuccess) {
                        onRefreshMe()
                    } else {
                        errorMsg = "Неверный логин или пароль"
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            WebCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(horizontal = 24.dp, vertical = 36.dp),
                spacing = 16.dp,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GradientAvatar(initials = "", size = 64.dp, fontSize = 20.sp) {
                    Icon(
                        imageVector = Tabler.User,
                        contentDescription = "Аккаунт",
                        modifier = Modifier.size(32.dp),
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "Авторизация",
                    fontSize = 22.sp,
                    color = TextPrimary
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = ErrorRed,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Пожалуйста, авторизуйтесь для доступа к настройкам аккаунта",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(0.dp))

                WebField(
                    value = loginInput,
                    onValueChange = { loginInput = it },
                    placeholder = "Логин",
                    modifier = Modifier.fillMaxWidth()
                )

                WebField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    placeholder = "Пароль",
                    password = true,
                    modifier = Modifier.fillMaxWidth()
                )

                WebButton(
                    onClick = { doLogin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.6.dp),
                    enabled = !isLoading && loginInput.isNotBlank() && passwordInput.isNotBlank()
                ) {
                    WebButtonText(if (isLoading) "Вход…" else "Войти")
                }

                WebButton(
                    onClick = onChangeServer,
                    modifier = Modifier.fillMaxWidth(),
                    fill = Fill06,
                    height = 40.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Icon(Tabler.Server, contentDescription = null, tint = Color85, modifier = Modifier.size(16.dp))
                    Text(
                        "Сменить сервер (${apiClient.baseUrl})",
                        fontSize = 13.6.sp,
                        color = Color85,
                        maxLines = 1
                    )
                }
            }
        }
    } else {
        var newLoginInput by remember { mutableStateOf(userMe.login) }
        var currentPassInput by remember { mutableStateOf("") }
        var newPassInput by remember { mutableStateOf("") }

        var feedbackText by remember { mutableStateOf<String?>(null) }
        var feedbackIsError by remember { mutableStateOf(false) }

        var isAvatarUploading by remember { mutableStateOf(false) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                isAvatarUploading = true
                scope.launch {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                        if (bytes != null) {
                            val res = apiClient.uploadAvatar(bytes, type)
                            if (res.isSuccess) {
                                feedbackText = "Аватар сохранен"
                                feedbackIsError = false
                                onRefreshMe()
                            } else {
                                feedbackText = "Ошибка загрузки аватара"
                                feedbackIsError = true
                            }
                        }
                    } catch (_: Exception) {
                        feedbackText = "Ошибка чтения файла"
                        feedbackIsError = true
                    }
                    isAvatarUploading = false
                }
            }
        }

        val saveLogin = {
            if (newLoginInput.isNotBlank()) {
                scope.launch {
                    val res = apiClient.updateLogin(newLoginInput.trim())
                    if (res.isSuccess) {
                        feedbackText = "Логин успешно изменен"
                        feedbackIsError = false
                        onRefreshMe()
                    } else {
                        feedbackText = "Логин уже занят"
                        feedbackIsError = true
                    }
                }
            }
        }

        val savePassword = {
            if (currentPassInput.isNotBlank() && newPassInput.isNotBlank()) {
                scope.launch {
                    val res = apiClient.updatePassword(currentPassInput, newPassInput)
                    if (res.isSuccess) {
                        feedbackText = "Пароль успешно изменен"
                        feedbackIsError = false
                        currentPassInput = ""
                        newPassInput = ""
                    } else {
                        feedbackText = "Неверный текущий пароль"
                        feedbackIsError = true
                    }
                }
            }
        }

        val doLogout = {
            scope.launch {
                apiClient.logout()
                onRefreshMe()
            }
        }

        val displayName = userMe.nickname ?: userMe.login
        val initials = displayName.take(2).uppercase()
        val avatar = rememberApiImage(apiClient, userMe.avatar?.let { "/api/avatars/$it" })

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(96.dp)
                ) {
                    GradientAvatar(initials = initials, size = 96.dp, fontSize = 32.sp) {
                        if (avatar != null) {
                            Image(
                                bitmap = avatar,
                                contentDescription = "Аватар",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(initials, color = TextPrimary, fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(1.6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF252525))
                            .clickable(enabled = !isAvatarUploading) { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Tabler.CameraFilled,
                            contentDescription = "Загрузить аватар",
                            tint = IconMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = displayName,
                    fontSize = 22.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                RoleBadge(role = userMe.role ?: "user")
            }

            if (feedbackText != null) {
                item {
                    Text(
                        text = feedbackText!!,
                        color = if (feedbackIsError) StatusError else SuccessGreen,
                        fontSize = 13.1.sp
                    )
                }
            }

            item {
                WebCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(17.6.dp), spacing = 8.dp) {
                    Text("Изменение логина", fontSize = 12.5.sp, color = TextLabel)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        WebField(
                            value = newLoginInput,
                            onValueChange = { newLoginInput = it },
                            placeholder = "Новый логин",
                            fontSize = 14.4.sp,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        SmallSaveButton(onClick = { saveLogin() }, label = null)
                    }
                }
            }

            item {
                WebCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(17.6.dp), spacing = 8.dp) {
                    Text("Изменение пароля", fontSize = 12.5.sp, color = TextLabel)
                    WebField(
                        value = currentPassInput,
                        onValueChange = { currentPassInput = it },
                        placeholder = "Текущий пароль",
                        password = true,
                        fontSize = 14.4.sp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    WebField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it },
                        placeholder = "Новый пароль",
                        password = true,
                        fontSize = 14.4.sp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    SmallSaveButton(onClick = { savePassword() }, label = "Сохранить", modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                WebCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(17.6.dp), spacing = 8.dp) {
                    Text("Сервер", fontSize = 12.5.sp, color = TextLabel)
                    val serverInteraction = remember { MutableInteractionSource() }
                    val serverPressed by serverInteraction.collectIsPressedAsState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(androidx.compose.foundation.shape.RoundedCornerShape(8.dp), if (serverPressed) Fill10 else Fill06)
                            .clickable(interactionSource = serverInteraction, indication = null, onClick = onChangeServer)
                            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            apiClient.baseUrl,
                            fontSize = 14.4.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Tabler.Pencil,
                            contentDescription = "Сменить сервер",
                            tint = TextLabel,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item {
                WebButton(
                    onClick = { doLogout() },
                    fill = Fill03,
                    pressedFill = Fill08,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Tabler.Logout, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    WebButtonText("Выйти из аккаунта", fontSize = 14.4.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SmallSaveButton(onClick: () -> Unit, label: String?, modifier: Modifier = Modifier) {
    WebButton(
        onClick = onClick,
        modifier = modifier,
        fill = Fill10,
        pressedFill = Color(0x29FFFFFF),
        height = 40.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(Tabler.DeviceFloppy, contentDescription = "Сохранить", tint = TextPrimary, modifier = Modifier.size(16.dp))
        if (label != null) Text(label, fontSize = 13.6.sp, color = TextPrimary)
    }
}
