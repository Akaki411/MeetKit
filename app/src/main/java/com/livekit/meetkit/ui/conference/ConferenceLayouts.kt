/**
 * Компоненты раскладки видеоплиток участников: сетка (Grid) и фокусный режим (Focus).
 */
package com.livekit.meetkit.ui.conference

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.ui.components.rememberApiImage
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.theme.*
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.video.CameraCapturerUtils
import livekit.org.webrtc.RendererCommon
import kotlin.math.floor
import kotlin.math.max

private val TileShape = RoundedCornerShape(12.dp)
private val TileGap = 12.dp
private const val TileAspect = 16f / 9f

@Composable
fun ConferenceTile(
    room: Room,
    tile: TileRef,
    apiClient: ApiClient,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val participant = tile.participant
    val meta = parseParticipantMeta(participant.metadata)
    val name = participant.displayName
    val isLocal = participant is LocalParticipant
    val speaking = participant.isSpeaking && !tile.isScreenShare
    val micMuted = participant.getTrackPublication(Track.Source.MICROPHONE)?.muted ?: true
    val avatar = rememberApiImage(apiClient, meta.avatar?.let { "/api/avatars/$it" })

    val isFrontCamera = remember(participant, tile.track) {
        if (isLocal && !tile.isScreenShare) {
            val deviceId = (participant as LocalParticipant).videoTrackCaptureDefaults.deviceId
            runCatching {
                val enumerator = CameraCapturerUtils.createCameraEnumerator(context)
                if (deviceId != null) {
                    enumerator.isFrontFacing(deviceId)
                } else {
                    true
                }
            }.getOrDefault(true)
        } else {
            false
        }
    }

    Box(
        modifier = modifier
            .border(2.dp, if (speaking) Green else Color.Transparent, TileShape)
            .clip(TileShape)
            .background(if (tile.isScreenShare) Color.Black else DarkSurface)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val track = tile.track
        if (track != null) {
            key(track) {
                AndroidView(
                    factory = { ctx ->
                        TextureViewRenderer(ctx).apply {
                            room.initVideoRenderer(this)
                            setScalingType(
                                if (tile.isScreenShare) RendererCommon.ScalingType.SCALE_ASPECT_FIT
                                else RendererCommon.ScalingType.SCALE_ASPECT_FILL
                            )
                            setMirror(isLocal && !tile.isScreenShare && isFrontCamera)
                            track.addRenderer(this)
                        }
                    },
                    onRelease = { renderer ->
                        track.removeRenderer(renderer)
                        renderer.release()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val size = (maxWidth * 0.42f).coerceIn(72.dp, 160.dp).coerceAtMost(maxHeight * 0.8f)
                if (avatar != null) {
                    Image(
                        bitmap = avatar,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .border(2.dp, Color(0x26FFFFFF), CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(size).clip(CircleShape).background(Fill10),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initialsOf(name),
                            color = TextPrimary,
                            fontSize = 35.2.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(6.4.dp))
                .background(Color(0x8C000000))
                .padding(horizontal = 8.dp, vertical = 3.2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.8.dp)
        ) {
            if (micMuted && !tile.isScreenShare) {
                Icon(Tabler.MicrophoneOff, contentDescription = null, tint = Coral, modifier = Modifier.size(16.dp))
            }
            Text(
                text = if (tile.isScreenShare) "$name · экран" else name,
                color = TextPrimary,
                fontSize = 12.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun bestTileSize(width: Float, height: Float, count: Int, gap: Float): Triple<Int, Float, Float> {
    if (count <= 0 || width <= 0 || height <= 0) return Triple(1, 0f, 0f)
    var best = Triple(1, 0f, 0f)
    var bestArea = 0f
    for (cols in 1..count) {
        val rows = (count + cols - 1) / cols
        var w = (width - gap * (cols - 1)) / cols
        var h = w / TileAspect
        if (h * rows + gap * (rows - 1) > height) {
            h = (height - gap * (rows - 1)) / rows
            w = h * TileAspect
        }
        if (w <= 0 || h <= 0) continue
        if (w * h > bestArea) {
            bestArea = w * h
            best = Triple(cols, w, h)
        }
    }
    return Triple(best.first, floor(best.second), floor(best.third))
}

@Composable
fun GridLayout(
    room: Room,
    tiles: List<TileRef>,
    apiClient: ApiClient,
    bottomPadding: Dp,
    onSelect: (TileRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = bottomPadding)
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val (cols, w, h) = with(density) {
            bestTileSize(maxWidth.toPx(), maxHeight.toPx(), tiles.size, TileGap.toPx())
        }
        val tileW = with(density) { w.toDp() }
        val tileH = with(density) { h.toDp() }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TileGap, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(0.dp))
            tiles.chunked(max(1, cols)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                    row.forEach { tile ->
                        key(tile.key) {
                            ConferenceTile(
                                room = room, tile = tile, apiClient = apiClient,
                                modifier = Modifier.size(tileW, tileH),
                                onClick = { onSelect(tile) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusLayout(
    room: Room,
    focus: TileRef,
    tiles: List<TileRef>,
    apiClient: ApiClient,
    bottomPadding: Dp,
    onSelect: (TileRef) -> Unit,
    onUnpin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val others = tiles.filter { it.key != focus.key }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val carouselHeight = (maxHeight * 0.2f).coerceIn(96.dp, 144.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(TileGap)
        ) {
            ConferenceTile(
                room = room, tile = focus, apiClient = apiClient,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onClick = onUnpin
            )
            if (others.isNotEmpty()) {
                Row(
                    modifier = Modifier.height(carouselHeight).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    others.forEach { tile ->
                        key(tile.key) {
                            ConferenceTile(
                                room = room, tile = tile, apiClient = apiClient,
                                modifier = Modifier.fillMaxHeight().aspectRatio(TileAspect),
                                onClick = { onSelect(tile) }
                            )
                        }
                    }
                }
            }
        }
    }
}
