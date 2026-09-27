/**
 * Модификатор и утилиты эффекта матового стекла (Glassmorphism) с размытием фона.
 */
package com.livekit.meetkit.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class GlassBackdrop {
    var image: ImageBitmap? = null
        private set
    var scale = 1f
        private set

    var origin by mutableStateOf(Offset.Zero)

    private var version by mutableIntStateOf(0)

    fun publish(image: ImageBitmap, scale: Float) {
        this.image = image
        this.scale = scale
        version++
    }

    fun readImage(): ImageBitmap? {
        @Suppress("UNUSED_VARIABLE") val v = version
        return image
    }
}

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

private val FallbackScrim = Color(0xD9131313)

fun Modifier.glass(shape: Shape = RectangleShape, tint: Color = Color.Transparent): Modifier = composed {
    val backdrop = LocalGlassBackdrop.current
    var position by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { position = it.positionInWindow() }
        .clip(shape)
        .drawBehind {
            val image = backdrop?.readImage()
            if (image == null) {
                drawRect(FallbackScrim)
            } else {
                val offset = backdrop.origin - position
                val inv = 1f / backdrop.scale
                translate(offset.x, offset.y) {
                    scale(inv, inv, pivot = Offset.Zero) {
                        drawImage(image)
                    }
                }
            }
            if (tint.alpha > 0f) drawRect(tint)
        }
}

internal fun boxRadiusFor(sigma: Float): Int =
    max(1, ((sqrt(4f * sigma * sigma + 1f) - 1f) / 2f).roundToInt())

internal fun blurOpaque(pixels: IntArray, tmp: IntArray, w: Int, h: Int, radius: Int) {
    repeat(3) {
        boxPass(pixels, tmp, w, h, radius, horizontal = true)
        boxPass(tmp, pixels, w, h, radius, horizontal = false)
    }
}

private fun boxPass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
    val lines = if (horizontal) h else w
    val len = if (horizontal) w else h
    val step = if (horizontal) 1 else w
    val window = 2 * r + 1
    for (line in 0 until lines) {
        val start = if (horizontal) line * w else line
        var sr = 0; var sg = 0; var sb = 0
        for (k in -r..r) {
            val c = src[start + min(len - 1, max(0, k)) * step]
            sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF
        }
        for (i in 0 until len) {
            dst[start + i * step] = (0xFF shl 24) or ((sr / window) shl 16) or ((sg / window) shl 8) or (sb / window)
            val out = src[start + max(0, i - r) * step]
            val inn = src[start + min(len - 1, i + r + 1) * step]
            sr += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
            sg += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
            sb += (inn and 0xFF) - (out and 0xFF)
        }
    }
}
