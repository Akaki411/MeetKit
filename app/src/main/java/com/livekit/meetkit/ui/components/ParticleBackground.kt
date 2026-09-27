/**
 * Анимированный фон из движущихся узлов и связей для фонового оформления экранов.
 */
package com.livekit.meetkit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val DENSITY_DIVISOR = 13000f
private const val MIN_PARTICLES = 24
private const val MAX_PARTICLES_MOBILE = 52
private const val FRAME_INTERVAL_NS = 1_000_000_000L / 30

private const val NODE_SPEED_MIN = 3f
private const val NODE_SPEED_MAX = 8f
private const val BOKEH_SPEED_MIN = 1f
private const val BOKEH_SPEED_MAX = 3f

private const val NODE_SIZE = 5f
private const val PARTICLE_SIZE_SPREAD = 1f
private const val BOKEH_SIZE_MIN = 16f
private const val BOKEH_SIZE_MAX = 46f

private const val BOKEH_FRACTION = 0.2f
private const val NODE_BLUR_MAX = 0.4f
private const val BOKEH_BLUR_MIN = 0.75f
private const val BOKEH_BLUR_MAX = 1.0f
private const val BLURRED_LINK_FRACTION = 0.5f
private const val LINK_BLUR_INTENSITY = 0.6f
private const val LINK_BLUR_SPREAD = 0.2f

private const val LINK_SPACING_FACTOR = 1.8f
private const val LINE_OPACITY = 0.8f
private const val LINE_WIDTH = 1.3f
private const val LINE_BLUR_WIDTH = 4f

private const val NODE_ALPHA_MIN = 0.5f
private const val NODE_ALPHA_MAX = 1.0f
private const val BOKEH_ALPHA_MIN = 0.1f
private const val BOKEH_ALPHA_MAX = 0.3f

private const val PARTICLE_GRAY = 0.9f

private fun webGlColor(a: Float): Color {
    val alpha = a.coerceIn(0f, 1f)
    val gray = PARTICLE_GRAY * alpha
    return Color(gray, gray, gray, alpha * alpha)
}

private fun rand(min: Float, max: Float) = min + Random.nextFloat() * (max - min)

private fun pairRand(i: Int, j: Int, salt: Int): Float {
    var h = ((i + 1) * 73856093) xor ((j + 1) * 19349663) xor (salt * 83492791)
    h = (h xor (h ushr 15)) * -2048144777
    h = h xor (h ushr 13)
    return (h.toLong() and 0xFFFFFFFFL).toFloat() / 4294967295f
}

private class Particles(val w: Float, val h: Float, densityScale: Float) {
    val n: Int
    val b: Int
    val px: FloatArray
    val py: FloatArray
    val vx: FloatArray
    val vy: FloatArray
    val size: FloatArray
    val blur: FloatArray
    val alpha: FloatArray
    val visible: BooleanArray

    init {
        val cssArea = (w / densityScale) * (h / densityScale)
        n = max(MIN_PARTICLES, min(MAX_PARTICLES_MOBILE, (cssArea / DENSITY_DIVISOR).roundToInt()))
        b = (n * BOKEH_FRACTION).roundToInt()
        px = FloatArray(n) { Random.nextFloat() * w }
        py = FloatArray(n) { Random.nextFloat() * h }
        vx = FloatArray(n)
        vy = FloatArray(n)
        size = FloatArray(n)
        blur = FloatArray(n)
        alpha = FloatArray(n)
        visible = BooleanArray(n)
        for (i in 0 until n) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            if (i < b) {
                val sp = rand(BOKEH_SPEED_MIN, BOKEH_SPEED_MAX) * densityScale
                vx[i] = cos(ang) * sp; vy[i] = sin(ang) * sp
                size[i] = rand(BOKEH_SIZE_MIN, BOKEH_SIZE_MAX) * densityScale
                blur[i] = rand(BOKEH_BLUR_MIN, BOKEH_BLUR_MAX)
                alpha[i] = rand(BOKEH_ALPHA_MIN, BOKEH_ALPHA_MAX)
            } else {
                val sp = rand(NODE_SPEED_MIN, NODE_SPEED_MAX) * densityScale
                vx[i] = cos(ang) * sp; vy[i] = sin(ang) * sp
                size[i] = NODE_SIZE * (1 + (Random.nextFloat() * 2 - 1) * PARTICLE_SIZE_SPREAD) * densityScale
                blur[i] = Random.nextFloat() * NODE_BLUR_MAX
                alpha[i] = rand(NODE_ALPHA_MIN, NODE_ALPHA_MAX)
            }
        }
    }

    fun step(dt: Float) {
        for (i in 0 until n) {
            var x = px[i] + vx[i] * dt
            var y = py[i] + vy[i] * dt
            if (x < 0) { x = 0f; vx[i] = -vx[i] } else if (x > w) { x = w; vx[i] = -vx[i] }
            if (y < 0) { y = 0f; vy[i] = -vy[i] } else if (y > h) { y = h; vy[i] = -vy[i] }
            px[i] = x; py[i] = y
        }
    }
}

@Composable
fun ParticleBackground(
    active: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val density = LocalDensity.current
    val densityScale = density.density
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val particles = remember(layoutSize) {
        if (layoutSize.width > 0 && layoutSize.height > 0) {
            Particles(layoutSize.width.toFloat(), layoutSize.height.toFloat(), densityScale)
        } else {
            null
        }
    }
    var frame by remember { mutableLongStateOf(0L) }

    val backdrop = remember { GlassBackdrop() }
    val glass = remember(particles) { particles?.let { GlassRenderer(it, density) } }

    LaunchedEffect(glass) { glass?.renderInto(backdrop) }

    LaunchedEffect(active, particles) {
        if (!active || particles == null) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                val delta = now - last
                if (delta >= FRAME_INTERVAL_NS) {
                    particles.step(min(0.05f, delta / 1e9f))
                    if (frame % GLASS_FRAME_DIVISOR == 0L) glass?.renderInto(backdrop)
                    last = now
                    frame++
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { layoutSize = it }
            .onGloballyPositioned { backdrop.origin = it.positionInWindow() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBackdropGradient(size.width, size.height)
            if (frame < 0) return@Canvas
            particles?.let { drawParticles(it, densityScale) }
        }
        CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
            content()
        }
    }
}

private fun DrawScope.drawBackdropGradient(w: Float, h: Float) {
    val center = Offset(w * 0.4f, h * 0.4f)
    val radius = sqrt((w * 0.6f).let { it * it } + (h * 0.6f).let { it * it })
    drawRect(
        Brush.radialGradient(
            0f to Color(0xFF161616), 0.6f to Color(0xFF131313), 1f to Color(0xFF111111),
            center = center, radius = radius,
        ),
        size = Size(w, h),
    )
}

private const val GLASS_SCALE = 0.125f
private const val GLASS_FRAME_DIVISOR = 3L
private const val GLASS_BLUR_DP = 6f

private class GlassRenderer(private val particles: Particles, private val density: Density) {
    private val w = max(1, (particles.w * GLASS_SCALE).roundToInt())
    private val h = max(1, (particles.h * GLASS_SCALE).roundToInt())
    private val bitmap = ImageBitmap(w, h)
    private val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
    private val drawScope = CanvasDrawScope()
    private val pixels = IntArray(w * h)
    private val tmp = IntArray(w * h)
    private val dpr = density.density
    private val radius = boxRadiusFor(GLASS_BLUR_DP * dpr * GLASS_SCALE)

    fun renderInto(backdrop: GlassBackdrop) {
        drawScope.draw(density, LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
            scale(GLASS_SCALE, GLASS_SCALE, pivot = Offset.Zero) {
                drawBackdropGradient(particles.w, particles.h)
                drawParticles(particles, dpr)
            }
        }
        val android = bitmap.asAndroidBitmap()
        android.getPixels(pixels, 0, w, 0, 0, w, h)
        blurOpaque(pixels, tmp, w, h, radius)
        android.setPixels(pixels, 0, w, 0, 0, w, h)
        backdrop.publish(bitmap, GLASS_SCALE)
    }
}

private fun DrawScope.softDot(center: Offset, diameter: Float, blur: Float, alpha: Float) {
    val r = diameter / 2f
    if (r <= 0f || alpha <= 0f) return
    val soft = 0.22f + (1f - 0.22f) * blur
    val c = webGlColor(alpha)
    val edge = (1f - soft).coerceIn(0f, 0.999f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to c,
            edge to c,
            (edge + 1f) / 2f to webGlColor(alpha * 0.5f),
            1f to Color.Transparent,
            center = center, radius = r,
        ),
        radius = r,
        center = center,
    )
}

private fun DrawScope.drawParticles(p: Particles, densityScale: Float) {
    for (i in 0 until p.n) p.visible[i] = i < p.b

    for (i in 0 until p.b) softDot(Offset(p.px[i], p.py[i]), p.size[i], p.blur[i], p.alpha[i])

    val focusCount = p.n - p.b
    val link = sqrt(p.w * p.h / max(1, focusCount)) * LINK_SPACING_FACTOR
    val link2 = link * link
    for (i in p.b until p.n) {
        for (j in i + 1 until p.n) {
            val dx = p.px[i] - p.px[j]
            val dy = p.py[i] - p.py[j]
            val d2 = dx * dx + dy * dy
            if (d2 >= link2) continue
            val dist = sqrt(d2)
            val a = (1 - dist / link) * LINE_OPACITY
            p.visible[i] = true
            p.visible[j] = true

            var lblur = 0f
            if (pairRand(i, j, 1) < BLURRED_LINK_FRACTION) {
                lblur = (LINK_BLUR_INTENSITY + (pairRand(i, j, 2) * 2 - 1) * LINK_BLUR_SPREAD).coerceIn(0f, 1f)
            }
            val width = (LINE_WIDTH + lblur * LINE_BLUR_WIDTH) * densityScale
            val soft = 0.15f + 0.85f * lblur
            val start = Offset(p.px[i], p.py[i])
            val end = Offset(p.px[j], p.py[j])
            val core = width * (1f - soft)
            if (core > 0.5f) {
                drawLine(webGlColor(a), start, end, strokeWidth = core, cap = StrokeCap.Butt)
            }
            drawLine(webGlColor(a * 0.5f), start, end, strokeWidth = width, cap = StrokeCap.Butt)
        }
    }

    for (i in p.b until p.n) {
        if (p.visible[i]) softDot(Offset(p.px[i], p.py[i]), p.size[i], p.blur[i], p.alpha[i])
    }
}
