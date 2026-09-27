/**
 * Главный экран видеоконференции: отображение видеосеток, панелей чата, участников и панели управления.
 */
package com.livekit.meetkit.ui.conference

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.livekit.meetkit.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.ui.screens.DeviceChoice
import com.livekit.meetkit.ui.theme.DarkBackground
import com.livekit.meetkit.ui.theme.DarkCardBorder
import com.twilio.audioswitch.AudioDevice
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalAudioTrack
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.screencapture.ScreenCaptureParams
import kotlinx.coroutines.launch

private val RecordingBorder = Color(0xE6F5333F)
private const val SHARE_NOTIFICATION_ID = 1002
private const val SHARE_CHANNEL_ID = "meetkit_screen_share"

private fun screenShareNotification(context: Context): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(SHARE_CHANNEL_ID, "Демонстрация экрана", NotificationManager.IMPORTANCE_LOW)
        )
    }
    return NotificationCompat.Builder(context, SHARE_CHANNEL_ID)
        .setContentTitle("MeetKit")
        .setContentText("Идёт демонстрация экрана")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setOngoing(true)
        .build()
}

@Composable
fun ConferenceScreen(
    room: Room,
    roomName: String,
    apiClient: ApiClient,
    cameras: List<DeviceChoice>,
    initialCameraId: String?,
    effects: BackgroundEffects,
    onLeave: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tick = rememberRoomTick(room)
    val toast = remember { ToastState() }
    val chat = remember(room) { ChatState(room, apiClient) }

    var chatOpen by remember { mutableStateOf(false) }
    var participantsOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var unread by remember { mutableIntStateOf(0) }
    var pinnedKey by remember { mutableStateOf<String?>(null) }
    var localMuted by remember { mutableStateOf(setOf<String>()) }
    var activeCameraId by remember { mutableStateOf(initialCameraId) }
    var noiseSuppression by remember { mutableStateOf(true) }
    var audioRevision by remember { mutableIntStateOf(0) }
    var micPending by remember { mutableStateOf(false) }
    var cameraPending by remember { mutableStateOf(false) }
    var sharePending by remember { mutableStateOf(false) }

    val chatOpenNow by rememberUpdatedState(chatOpen)

    LaunchedEffect(room) {
        room.events.events.collect { event ->
            when (event) {
                is RoomEvent.DataReceived ->
                    if (event.topic == "chat" && chat.receive(event.participant, event.data) && !chatOpenNow) unread++
                is RoomEvent.Reconnecting -> scope.launch { toast.show("Переподключение…", loading = true, durationMs = null) }
                is RoomEvent.Reconnected -> toast.dismiss()
                is RoomEvent.RecordingStatusChanged ->
                    if (room.isRecording) scope.launch { toast.show("Эта встреча записывается", durationMs = 3000) }
                else -> Unit
            }
        }
    }

    @Suppress("UNUSED_EXPRESSION") tick
    val local = room.localParticipant
    val micEnabled = local.getTrackPublication(Track.Source.MICROPHONE)?.let { !it.muted } ?: false
    val cameraEnabled = local.getTrackPublication(Track.Source.CAMERA)?.let { !it.muted } ?: false
    val screenSharing = local.getTrackPublication(Track.Source.SCREEN_SHARE) != null
    val participants = room.standardParticipants()
    val tiles = room.tiles()
    val focus = tiles.firstOrNull { it.key == pinnedKey } ?: tiles.firstOrNull { it.isScreenShare }
    val myMeta = parseParticipantMeta(local.metadata)
    val microphones = remember(tick) { listMicrophones(context) }

    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            scope.launch {
                sharePending = true
                runCatching {
                    local.setScreenShareEnabled(
                        true,
                        ScreenCaptureParams(data, SHARE_NOTIFICATION_ID, screenShareNotification(context))
                    )
                }
                    .onFailure { toast.show("Не удалось начать демонстрацию экрана", error = true) }
                sharePending = false
            }
        }
    }
    val toggleShare: () -> Unit = {
        if (screenSharing) {
            scope.launch {
                sharePending = true
                runCatching { local.setScreenShareEnabled(false) }
                sharePending = false
            }
        } else {
            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(manager.createScreenCaptureIntent())
        }
    }
    val toggleMic = {
        scope.launch {
            micPending = true
            runCatching { local.setMicrophoneEnabled(!micEnabled) }
            micPending = false
        }
        Unit
    }
    val toggleCamera = {
        scope.launch {
            cameraPending = true
            runCatching { effects.setCameraEnabled(!cameraEnabled) }
            cameraPending = false
        }
        Unit
    }
    val selectCamera: (DeviceChoice) -> Unit = { choice ->
        activeCameraId = choice.id
        local.videoTrackCaptureDefaults = local.videoTrackCaptureDefaults.copy(deviceId = choice.id)
        (local.getTrackPublication(Track.Source.CAMERA)?.track as? LocalVideoTrack)
            ?.let { runCatching { it.switchCamera(deviceId = choice.id) } }
    }
    val selectMic: (DeviceChoice) -> Unit = { choice ->
        val handler = room.audioSwitchHandler
        val type = choice.id?.substringBefore(':')?.toIntOrNull()
        if (handler != null && type != null) {
            val devices = handler.availableAudioDevices
            val target = when (type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> devices.firstOrNull { it is AudioDevice.BluetoothHeadset }
                AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET ->
                    devices.firstOrNull { it is AudioDevice.WiredHeadset }
                else -> devices.firstOrNull { it is AudioDevice.Speakerphone } ?: devices.firstOrNull { it is AudioDevice.Earpiece }
            }
            target?.let { handler.selectDevice(it); audioRevision++ }
        }
    }
    val shareLink = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Ссылка на встречу", apiClient.roomLink(roomName)))
        scope.launch { toast.show("Ссылка на встречу скопирована") }
        Unit
    }
    val applyNoiseSuppression: (Boolean) -> Unit = { value ->
        noiseSuppression = value
        local.audioTrackCaptureDefaults = local.audioTrackCaptureDefaults.copy(noiseSuppression = value)
        (local.getTrackPublication(Track.Source.MICROPHONE)?.track as? LocalAudioTrack)?.let { live ->
            scope.launch {
                val wasEnabled = micEnabled
                runCatching {
                    local.unpublishTrack(live)
                    if (wasEnabled) local.setMicrophoneEnabled(true)
                }
            }
        }
    }
    val toggleLocalMute: (io.livekit.android.room.participant.Participant) -> Unit = { p ->
        val id = p.identity?.value
        if (id != null) {
            val nowMuted = !localMuted.contains(id)
            localMuted = if (nowMuted) localMuted + id else localMuted - id
            (p as? io.livekit.android.room.participant.RemoteParticipant)
                ?.audioTrackPublications?.forEach { (pub, _) ->
                    (pub.track as? io.livekit.android.room.track.RemoteAudioTrack)?.setVolume(if (nowMuted) 0.0 else 1.0)
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground).systemBarsPadding()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= 760.dp
            val panelOpen = chatOpen || participantsOpen

            val stage: @Composable (Modifier) -> Unit = { mod ->
                Box(modifier = mod) {
                    val bottomPadding = if (panelOpen && !wide) 12.dp else 88.dp
                    if (focus != null) {
                        FocusLayout(
                            room = room, focus = focus, tiles = tiles, apiClient = apiClient,
                            bottomPadding = bottomPadding,
                            onSelect = { pinnedKey = if (pinnedKey == it.key) null else it.key },
                            onUnpin = { pinnedKey = null }
                        )
                    } else {
                        GridLayout(
                            room = room, tiles = tiles, apiClient = apiClient, bottomPadding = bottomPadding,
                            onSelect = { pinnedKey = it.key }
                        )
                    }
                    ControlBar(
                        micEnabled = micEnabled, cameraEnabled = cameraEnabled,
                        micPending = micPending, cameraPending = cameraPending,
                        microphones = microphones, cameras = cameras, activeCameraId = activeCameraId,
                        chatOpen = chatOpen, unread = unread,
                        participantsOpen = participantsOpen, participantCount = participants.size,
                        isModerator = myMeta.creator || myMeta.admin,
                        onToggleMic = toggleMic, onToggleCamera = toggleCamera,
                        onSelectMic = selectMic, onSelectCamera = selectCamera,
                        onFlipCamera = {
                            val next = cameras.firstOrNull { it.id != activeCameraId } ?: cameras.firstOrNull()
                            if (next != null) selectCamera(next)
                        },
                        onToggleParticipants = { chatOpen = false; participantsOpen = !participantsOpen },
                        onToggleChat = {
                            participantsOpen = false
                            if (!chatOpen) unread = 0
                            chatOpen = !chatOpen
                        },
                        onShare = shareLink,
                        onSettings = { settingsOpen = true },
                        onLeave = { endForAll ->
                            scope.launch {
                                if (endForAll) runCatching { apiClient.endRoom(roomName) }
                                onLeave()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (panelOpen && !wide) 6.4.dp else 16.dp)
                    )
                }
            }

            val panel: @Composable (Modifier) -> Unit = { mod ->
                if (chatOpen) {
                    ChatPanel(chat = chat, apiClient = apiClient, toast = toast, modifier = mod)
                } else if (participantsOpen) {
                    ParticipantsPanel(
                        room = room, roomName = roomName, apiClient = apiClient, localMuted = localMuted,
                        onToggleMute = toggleLocalMute, toast = toast, modifier = mod
                    )
                }
            }

            when {
                wide -> Row(modifier = Modifier.fillMaxSize()) {
                    stage(Modifier.weight(1f).fillMaxHeight())
                    if (panelOpen) panel(Modifier.width(340.dp).fillMaxHeight().border(1.dp, DarkCardBorder))
                }
                panelOpen -> Column(modifier = Modifier.fillMaxSize()) {
                    stage(Modifier.weight(0.34f).fillMaxWidth())
                    panel(Modifier.weight(0.66f).fillMaxWidth())
                }
                else -> stage(Modifier.fillMaxSize())
            }

            ScreenShareButton(
                active = screenSharing, pending = sharePending, onClick = toggleShare,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.4.dp, bottom = 83.2.dp)
            )
        }

        if (room.isRecording) {
            Box(modifier = Modifier.fillMaxSize().border(3.dp, RecordingBorder))
        }

        ToastHost(toast, modifier = Modifier.align(Alignment.TopCenter))

        if (settingsOpen) {
            @Suppress("UNUSED_EXPRESSION") audioRevision
            val handler = room.audioSwitchHandler
            val outputs = handler?.availableAudioDevices.orEmpty().map { d ->
                DeviceChoice(
                    d.name, when (d) {
                        is AudioDevice.Speakerphone -> "Динамик"
                        is AudioDevice.Earpiece -> "Разговорный динамик"
                        is AudioDevice.BluetoothHeadset -> "Bluetooth: ${d.name}"
                        is AudioDevice.WiredHeadset -> "Проводная гарнитура"
                        else -> d.name
                    }
                )
            }
            SettingsDialog(
                room = room, roomName = roomName, apiClient = apiClient, toast = toast,
                micEnabled = micEnabled, cameraEnabled = cameraEnabled,
                onToggleMic = toggleMic, onToggleCamera = toggleCamera,
                cameras = cameras, activeCameraId = activeCameraId, onSelectCamera = selectCamera,
                noiseSuppression = noiseSuppression, onNoiseSuppression = applyNoiseSuppression,
                microphones = microphones, onSelectMic = selectMic,
                audioOutputs = outputs, activeAudioOutput = handler?.selectedAudioDevice?.name,
                onSelectAudioOutput = { choice ->
                    handler?.availableAudioDevices?.firstOrNull { it.name == choice.id }?.let {
                        handler.selectDevice(it)
                        audioRevision++
                    }
                },
                backgroundSection = { BackgroundSection(effects, toast) },
                onClose = { settingsOpen = false }
            )
        }
    }
}
