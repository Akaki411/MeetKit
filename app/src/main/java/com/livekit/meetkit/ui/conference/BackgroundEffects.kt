/**
 * Сервис эффектов камеры: размытие фона и виртуальные фоновые изображения (Virtual Background).
 */
package com.livekit.meetkit.ui.conference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.video.CameraCapturerUtils
import io.livekit.android.track.processing.video.VirtualBackgroundVideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import livekit.org.webrtc.CameraXHelper
import livekit.org.webrtc.CameraXProvider
import livekit.org.webrtc.EglBase

enum class BackgroundPreset(val id: String, val label: String, val asset: String) {
    WALL("wall", "Стена", "background-images/wall_texture_fullhd.webp"),
    ROOM("room", "Комната", "background-images/room_bg_fullhd.webp"),
}

@OptIn(ExperimentalCamera2Interop::class)
class BackgroundEffects(
    private val context: Context,
    private val room: Room,
    private val eglBase: EglBase,
    private val lifecycleOwner: LifecycleOwner,
) {
    enum class Mode { NONE, BLUR, IMAGE }

    var mode by mutableStateOf(Mode.NONE)
        private set
    var imageId by mutableStateOf<String?>(null)
        private set
    var uploaded by mutableStateOf<Bitmap?>(null)
        private set

    private var processor: VirtualBackgroundVideoProcessor? = null
    private var provider: CameraXProvider? = null

    private val presetCache = mutableMapOf<String, Bitmap>()

    fun preset(preset: BackgroundPreset): Bitmap? = presetCache.getOrPut(preset.id) {
        runCatching {
            context.assets.open(preset.asset).use { input ->
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeStream(input, null, options)
            }
        }.getOrNull() ?: return null
    }

    suspend fun decodeUpload(bytes: ByteArray): Bitmap? = withContext(Dispatchers.Default) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 1920) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    suspend fun setUploaded(bitmap: Bitmap) {
        uploaded = bitmap
        select(Mode.IMAGE, UPLOADED)
    }

    private fun ensurePipeline(): Boolean {
        if (processor != null) return false
        val p = VirtualBackgroundVideoProcessor(eglBase, Dispatchers.IO)
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                    )
                    .build()
            )
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also { it.setAnalyzer(Dispatchers.IO.asExecutor(), p.imageAnalyzer) }
        val cameraX = CameraXHelper.createCameraProvider(lifecycleOwner, arrayOf(analysis))
        CameraCapturerUtils.registerCameraProvider(cameraX)
        processor = p
        provider = cameraX
        return true
    }

    suspend fun select(newMode: Mode, imageId: String? = null) {
        if (newMode == Mode.NONE) {
            processor?.enabled = false
            processor?.backgroundImage = null
            mode = Mode.NONE
            this.imageId = null
            return
        }
        val fresh = ensurePipeline()
        val p = processor ?: return
        p.backgroundImage = when {
            newMode != Mode.IMAGE -> null
            imageId == UPLOADED -> uploaded
            else -> BackgroundPreset.values().firstOrNull { it.id == imageId }?.let { preset(it) }
        }
        p.enabled = true
        mode = newMode
        this.imageId = if (newMode == Mode.IMAGE) imageId else null
        if (fresh) restartCameraWithProcessor()
    }

    private suspend fun restartCameraWithProcessor() {
        val local = room.localParticipant
        val pub = local.getTrackPublication(Track.Source.CAMERA)
        val wasOn = pub != null && !pub.muted
        (pub?.track as? LocalVideoTrack)?.let { local.unpublishTrack(it) }
        if (wasOn) {
            delay(200)
            publishProcessedCamera()
        }
    }

    private suspend fun publishProcessedCamera() {
        val local = room.localParticipant
        val track = local.createVideoTrack(
            options = local.videoTrackCaptureDefaults.copy(),
            videoProcessor = processor,
        )
        track.startCapture()
        local.publishVideoTrack(track)
    }

    suspend fun setCameraEnabled(enabled: Boolean) {
        val local = room.localParticipant
        if (processor == null || !enabled) {
            local.setCameraEnabled(enabled)
            return
        }
        val pub = local.getTrackPublication(Track.Source.CAMERA)
        if (pub?.track != null) local.setCameraEnabled(true) else publishProcessedCamera()
    }

    fun release() {
        provider?.let { CameraCapturerUtils.unregisterCameraProvider(it) }
        provider = null
        processor?.dispose()
        processor = null
    }

    companion object {
        const val UPLOADED = "uploaded"
    }
}
