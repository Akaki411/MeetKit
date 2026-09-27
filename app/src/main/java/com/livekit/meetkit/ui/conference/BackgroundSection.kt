/**
 * Секция выбора фона камеры в настройках: пресеты, размытие и загрузка своего изображения.
 */
package com.livekit.meetkit.ui.conference

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.theme.*
import kotlinx.coroutines.launch

private val OptionShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackgroundSection(effects: BackgroundEffects, toast: ToastState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val apply: (BackgroundEffects.Mode, String?) -> Unit = { mode, id ->
        scope.launch {
            runCatching { effects.select(mode, id) }.onFailure {
                android.util.Log.e("Background", "failed to apply background", it)
                toast.show("Не удалось применить фон", error = true)
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val bitmap = bytes?.let { effects.decodeUpload(it) }
            if (bitmap == null) {
                toast.show("Не удалось прочитать изображение", error = true)
            } else {
                runCatching { effects.setUploaded(bitmap) }.onFailure { toast.show("Не удалось применить фон", error = true) }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.6.dp)) {
        Text("ФОН", color = IconMuted, fontSize = 12.8.sp, letterSpacing = 0.64.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(9.6.dp), verticalArrangement = Arrangement.spacedBy(9.6.dp)) {
            Option(
                label = "Без фона", active = effects.mode == BackgroundEffects.Mode.NONE, thumbnail = null,
                onClick = { apply(BackgroundEffects.Mode.NONE, null) }
            )
            Option(
                label = "Размытие", active = effects.mode == BackgroundEffects.Mode.BLUR, thumbnail = null,
                fill = Color(0x33FFFFFF),
                onClick = { apply(BackgroundEffects.Mode.BLUR, null) }
            )
            BackgroundPreset.values().forEach { preset ->
                Option(
                    label = preset.label,
                    active = effects.mode == BackgroundEffects.Mode.IMAGE && effects.imageId == preset.id,
                    thumbnail = remember(preset) { effects.preset(preset) },
                    onClick = { apply(BackgroundEffects.Mode.IMAGE, preset.id) }
                )
            }
            effects.uploaded?.let { own ->
                Option(
                    label = "Своё",
                    active = effects.mode == BackgroundEffects.Mode.IMAGE && effects.imageId == BackgroundEffects.UPLOADED,
                    thumbnail = own,
                    onClick = { apply(BackgroundEffects.Mode.IMAGE, BackgroundEffects.UPLOADED) }
                )
            }
            val dash = Color(0x38FFFFFF)
            Column(
                modifier = Modifier
                    .size(84.dp, 60.dp)
                    .clip(OptionShape)
                    .background(Fill05)
                    .drawBehind {
                        drawRoundRect(
                            color = dash, size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                        )
                    }
                    .clickable { picker.launch("image/*") },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                Icon(Tabler.Upload, contentDescription = null, tint = Color(0xB3FFFFFF), modifier = Modifier.size(20.dp))
                Text("Загрузить", color = Color(0xB3FFFFFF), fontSize = 10.9.sp)
            }
        }
    }
}

@Composable
private fun Option(
    label: String,
    active: Boolean,
    thumbnail: Bitmap?,
    onClick: () -> Unit,
    fill: Color = Fill08,
) {
    Box(
        modifier = Modifier
            .size(84.dp, 60.dp)
            .clip(OptionShape)
            .background(fill)
            .border(2.dp, if (active) Green else Color.Transparent, OptionShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = remember(thumbnail) { thumbnail.asImageBitmap() },
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            label, color = TextPrimary, fontSize = 12.sp,
            modifier = Modifier
                .padding(3.2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x99000000))
                .padding(horizontal = 4.8.dp, vertical = 1.6.dp)
        )
    }
}
