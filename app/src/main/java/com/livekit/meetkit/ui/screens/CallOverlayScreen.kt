/**
 * Экран подключения и звонка: предпросмотр камеры/микрофона, ввод данных и переход к конференции.
 */
package com.livekit.meetkit.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.livekit.meetkit.ui.conference.BackgroundEffects
import livekit.org.webrtc.EglBase
import androidx.core.content.ContextCompat
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.data.ServerPreferences
import com.livekit.meetkit.data.models.UserMe
import com.livekit.meetkit.service.CallService
import com.livekit.meetkit.ui.components.ParticleBackground
import com.livekit.meetkit.ui.conference.ConferenceScreen
import io.livekit.android.LiveKit
import io.livekit.android.LiveKitOverrides
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.video.CameraCapturerUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CallOverlayScreen(
    roomName: String,
    initialPassword: String?,
    userMe: UserMe,
    apiClient: ApiClient,
    prefs: ServerPreferences,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nickname by remember { mutableStateOf(userMe.nickname ?: userMe.login ?: "") }
    var password by remember { mutableStateOf(initialPassword ?: "") }
    var requiresPassword by remember { mutableStateOf(false) }

    var isJoined by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var micEnabled by remember { mutableStateOf(true) }
    var cameraEnabled by remember { mutableStateOf(true) }

    val eglBase = remember(roomName) { EglBase.create() }
    val livekitRoom: Room = remember(roomName) {
        LiveKit.create(
            appContext = context,
            overrides = LiveKitOverrides(okHttpClient = apiClient.client, eglBase = eglBase)
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val effects = remember(livekitRoom) { BackgroundEffects(context, livekitRoom, eglBase, lifecycleOwner) }

    fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var cameraGranted by remember { mutableStateOf(hasCameraPermission()) }

    val cameraList = remember {
        runCatching {
            val enumerator = CameraCapturerUtils.createCameraEnumerator(context)
            enumerator.deviceNames.map { id ->
                val label = when {
                    enumerator.isFrontFacing(id) -> "Фронтальная камера"
                    enumerator.isBackFacing(id) -> "Основная камера"
                    else -> "Камера $id"
                }
                DeviceChoice(id, label) to enumerator.isFrontFacing(id)
            }
        }.getOrDefault(emptyList())
    }
    var selectedCameraId by remember {
        mutableStateOf(cameraList.firstOrNull { it.second }?.first?.id ?: cameraList.firstOrNull()?.first?.id)
    }
    val mirrorPreview = cameraList.firstOrNull { it.first.id == selectedCameraId }?.second ?: true

    var previewTrack by remember { mutableStateOf<LocalVideoTrack?>(null) }
    LaunchedEffect(cameraEnabled, cameraGranted, selectedCameraId, isConnecting, isJoined) {
        previewTrack = null
        if (isJoined || isConnecting || !cameraEnabled || !cameraGranted) return@LaunchedEffect
        delay(150)
        try {
            val local = livekitRoom.localParticipant
            val track = local.createVideoTrack(
                options = local.videoTrackCaptureDefaults.copy(deviceId = selectedCameraId)
            )
            track.startCapture()
            previewTrack = track
        } catch (e: Exception) {
            android.util.Log.w("CallOverlayScreen", "Camera preview unavailable", e)
        }
    }
    DisposableEffect(previewTrack) {
        val track = previewTrack
        onDispose {
            if (track != null) {
                try { track.stopCapture(); track.stop() } catch (_: Exception) {}
            }
        }
    }

    DisposableEffect(livekitRoom) {
        onDispose {
            livekitRoom.disconnect()
            effects.release()
            livekitRoom.release()
            eglBase.release()
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.CAMERA] == true) cameraGranted = true
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())

        val infoRes = apiClient.getRoomInfo(roomName)
        if (infoRes.isSuccess) {
            requiresPassword = infoRes.getOrNull()?.requiresPassword == true
        }
    }

    val joinCall = {
        if (nickname.isNotBlank()) {
            isConnecting = true
            errorMessage = null
            scope.launch {
                val connRes = apiClient.getConnectionDetails(
                    roomName = roomName,
                    participantName = nickname,
                    password = password.ifBlank { null }
                )

                if (connRes.isSuccess) {
                    val details = connRes.getOrNull()
                    val token = details?.participantToken
                    val serverUrl = details?.serverUrl

                    if (!token.isNullOrEmpty() && !serverUrl.isNullOrEmpty()) {
                        try {
                            val targetServerUrl = when {
                                serverUrl.startsWith("https://") -> serverUrl.replaceFirst("https://", "wss://")
                                serverUrl.startsWith("http://") -> serverUrl.replaceFirst("http://", "wss://")
                                serverUrl.startsWith("ws://") -> serverUrl.replaceFirst("ws://", "wss://")
                                else -> serverUrl
                            }
                            livekitRoom.connect(targetServerUrl, token)

                            livekitRoom.localParticipant.videoTrackCaptureDefaults =
                                livekitRoom.localParticipant.videoTrackCaptureDefaults.copy(deviceId = selectedCameraId)

                            livekitRoom.localParticipant.setMicrophoneEnabled(micEnabled)
                            livekitRoom.localParticipant.setCameraEnabled(cameraEnabled)

                            CallService.startCall(context, roomName)

                            prefs.addRecentRoom(roomName)
                            isJoined = true
                            isConnecting = false

                            launch {
                                livekitRoom.events.events.collect { event ->
                                    if (event is RoomEvent.Disconnected) {
                                        CallService.stopCall(context)
                                        isJoined = false
                                        onDismiss()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CallOverlayScreen", "Connection error", e)
                            isConnecting = false
                            errorMessage = "Ошибка подключения к медиасерверу: ${e.message ?: e.javaClass.simpleName}"
                        }
                    } else {
                        isConnecting = false
                        errorMessage = "Не удалось получить токен подключения"
                    }
                } else {
                    isConnecting = false
                    val err = connRes.exceptionOrNull()?.message ?: ""
                    errorMessage = when (err) {
                        "wrong_password" -> "Неверный пароль комнаты"
                        "password_required" -> "Требуется пароль"
                        "admin_only" -> "Доступ только для администраторов"
                        "guest_cannot_create" -> "Гости не могут создавать комнаты"
                        "banned" -> "Вы заблокированы в этой комнате"
                        else -> "Ошибка подключения: $err"
                    }
                }
            }
        }
    }

    val leaveCall = {
        CallService.stopCall(context)
        try { livekitRoom.disconnect() } catch (_: Exception) {}
        isJoined = false
        onDismiss()
    }

    BackHandler {
        leaveCall()
    }

    if (!isJoined) {
        ParticleBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                PreJoinCard(
                    room = livekitRoom,
                    previewTrack = previewTrack,
                    micEnabled = micEnabled,
                    cameraEnabled = cameraEnabled,
                    onToggleMic = { micEnabled = !micEnabled },
                    onToggleCamera = { cameraEnabled = !cameraEnabled },
                    microphones = listOf(DeviceChoice(null, "По умолчанию")),
                    cameras = cameraList.map { it.first },
                    selectedCameraId = selectedCameraId,
                    onSelectCamera = { selectedCameraId = it.id },
                    mirrorPreview = mirrorPreview,
                    nickname = nickname,
                    onNicknameChange = { nickname = it },
                    requiresPassword = requiresPassword,
                    password = password,
                    onPasswordChange = { password = it },
                    error = errorMessage,
                    connecting = isConnecting,
                    onJoin = { joinCall() }
                )
            }
        }
    } else {
        ConferenceScreen(
            room = livekitRoom,
            roomName = roomName,
            apiClient = apiClient,
            cameras = cameraList.map { it.first },
            initialCameraId = selectedCameraId,
            effects = effects,
            onLeave = { leaveCall() }
        )
    }
}
