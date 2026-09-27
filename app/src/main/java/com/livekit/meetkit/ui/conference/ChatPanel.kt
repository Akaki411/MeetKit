/**
 * Панель текстового чата в конференции: сообщения, эмодзи, вложения файлов, ответы и лайтбокс картинок.
 */
package com.livekit.meetkit.ui.conference

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.livekit.meetkit.data.ApiClient
import com.livekit.meetkit.data.models.Attachment
import com.livekit.meetkit.data.models.ReplySnippet
import com.livekit.meetkit.ui.components.rememberApiImage
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.theme.*
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.DataPublishReliability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val CHAT_TOPIC = "chat"
private const val GROUP_WINDOW_MS = 5 * 60 * 1000L
private const val HIGHLIGHT_MS = 1000L
private const val CHUNK_BYTES = 7 * 1024 * 1024
private const val MAX_FILE_BYTES = 256L * 1024 * 1024

data class ChatMessage(
    val id: String,
    val from: String,
    val message: String,
    val timestamp: Long,
    val local: Boolean,
    val attachment: Attachment? = null,
    val replyTo: ReplySnippet? = null,
)

private data class ChatPacket(
    val message: String = "",
    val timestamp: Long = 0,
    val attachment: Attachment? = null,
    val replyTo: ReplySnippet? = null,
)

class ChatState(private val room: Room, private val apiClient: ApiClient) {
    val messages = mutableStateListOf<ChatMessage>()
    var draft by mutableStateOf("")
    var replyingTo by mutableStateOf<ChatMessage?>(null)
    var uploading by mutableStateOf(false)
        private set
    var uploadProgress by mutableIntStateOf(0)
        private set
    var uploadName by mutableStateOf("")
        private set
    private val gson = Gson()

    fun receive(participant: Participant?, data: ByteArray): Boolean {
        return try {
            val packet = gson.fromJson(String(data, Charsets.UTF_8), ChatPacket::class.java) ?: return false
            messages += ChatMessage(
                id = "${participant?.identity?.value ?: "unknown"}-${packet.timestamp}",
                from = participant?.name?.ifBlank { null } ?: participant?.identity?.value ?: "",
                message = packet.message,
                timestamp = packet.timestamp,
                local = false,
                attachment = packet.attachment?.takeIf { it.id.isNotEmpty() },
                replyTo = packet.replyTo?.takeIf { it.id.isNotEmpty() },
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun snippet(msg: ChatMessage): ReplySnippet {
        var text = msg.message
        if (text.isEmpty() && msg.attachment != null) {
            text = if (msg.attachment.type.startsWith("image/")) "📷 Фото" else "📎 ${msg.attachment.name}"
        }
        return ReplySnippet(msg.id, msg.from, text.take(140))
    }

    private suspend fun publish(message: String, attachment: Attachment?, replyTo: ReplySnippet?) {
        val timestamp = System.currentTimeMillis()
        val payload = gson.toJson(ChatPacket(message, timestamp, attachment, replyTo)).toByteArray(Charsets.UTF_8)
        room.localParticipant.publishData(payload, DataPublishReliability.RELIABLE, CHAT_TOPIC)
        messages += ChatMessage(
            id = "local-$timestamp",
            from = room.localParticipant.name?.ifBlank { null } ?: room.localParticipant.identity?.value ?: "",
            message = message, timestamp = timestamp, local = true, attachment = attachment, replyTo = replyTo,
        )
    }

    suspend fun send() {
        val text = draft.trim()
        if (text.isEmpty()) return
        val reply = replyingTo?.let { snippet(it) }
        publish(text, null, reply)
        draft = ""
        replyingTo = null
    }

    suspend fun sendFile(context: Context, uri: Uri, toast: ToastState) {
        val resolver = context.contentResolver
        var name = "file"
        var size = -1L
        resolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
            }
        }
        val type = resolver.getType(uri) ?: "application/octet-stream"
        if (size > MAX_FILE_BYTES) {
            toast.show("Файл слишком большой (макс. ${MAX_FILE_BYTES / (1024 * 1024)} МБ)", error = true)
            return
        }
        val reply = replyingTo?.let { snippet(it) }
        uploading = true; uploadProgress = 0; uploadName = name
        try {
            val attachment = withContext(Dispatchers.IO) { upload(context, uri, name, type, size) }
            publish("", attachment, reply)
            replyingTo = null
        } catch (e: Exception) {
            android.util.Log.e("Chat", "attachment upload failed", e)
            toast.show(
                if (e.message == "too_large") "Файл слишком большой (макс. ${MAX_FILE_BYTES / (1024 * 1024)} МБ)"
                else "Не удалось загрузить файл",
                error = true
            )
        } finally {
            uploading = false
        }
    }

    private suspend fun upload(context: Context, uri: Uri, name: String, type: String, size: Long): Attachment {
        require(size >= 0) { "unknown size" }
        val uploadId = UUID.randomUUID().toString()
        val total = max(1, ((size + CHUNK_BYTES - 1) / CHUNK_BYTES).toInt())
        var stored: Attachment? = null
        context.contentResolver.openInputStream(uri)!!.use { input ->
            for (index in 0 until total) {
                val buffer = ByteArray(min(CHUNK_BYTES.toLong(), size - index.toLong() * CHUNK_BYTES).toInt().coerceAtLeast(0))
                var read = 0
                while (read < buffer.size) {
                    val n = input.read(buffer, read, buffer.size - read)
                    if (n < 0) break
                    read += n
                }
                val result = apiClient.uploadRoomFileChunk(uploadId, index, total, name, type, buffer.copyOf(read))
                stored = result.getOrThrow() ?: stored
                uploadProgress = (((index + 1).toFloat() / total) * 100).roundToInt()
            }
        }
        return stored ?: error("upload failed")
    }

    fun startReply(msg: ChatMessage) { replyingTo = msg }
}

private val EMOJI_GROUPS = listOf(
    listOf("😀", "😂", "🤣", "😭", "😅", "😊", "😍", "🥰", "😘", "😜", "🤪", "🤔", "😎", "🥳", "😴", "🙄", "😇", "🤗", "😳", "🫠", "🤯", "🥺", "😤", "😡", "🤡", "💀", "☠️", "👻", "👽", "🤖"),
    listOf("👍", "👎", "👏", "🙏", "🤝", "👌", "✌️", "🤞", "💪", "👋", "🤙", "🖐️", "🫡", "🫵", "🤌", "🤷", "🤦", "👀", "🖕"),
    listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔", "💕", "💖", "💗", "💯"),
    listOf("🐶", "🐱", "🐭", "🐹", "🦊", "🐻", "🐼", "🐨", "🐸", "🦁", "🐷", "🐵", "🙈", "🙉", "🙊", "🦄", "🗿", "🐔", "🦆", "🐍"),
    listOf("🍎", "🍕", "🍔", "🍟", "🌭", "🍩", "🍪", "🍫", "🍿", "☕", "🍺", "🍷", "🍆", "🍑", "🥑", "🍌"),
    listOf("⚽", "🏀", "🎮", "🎉", "🎂", "🎁", "🔥", "✨", "⭐", "📷", "🎵", "💡", "⏰", "🚀", "💰", "💸", "🧠", "👁️", "🐐", "💅", "🤑", "🎯", "📈", "📉", "🔔"),
)
private val ALL_EMOJIS = EMOJI_GROUPS.flatten()

private val UrlRegex = Regex("(https?://[^\\s]+)")
private val TimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(Locale.US, "%.1f MB", bytes / 1024f / 1024f)
}

private suspend fun saveToDownloads(context: Context, apiClient: ApiClient, attachment: Attachment): Boolean =
    withContext(Dispatchers.IO) {
        val bytes = apiClient.fetchApiBlob(attachment.url).getOrNull() ?: return@withContext false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, attachment.name)
                    put(MediaStore.Downloads.MIME_TYPE, attachment.type.ifEmpty { "application/octet-stream" })
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@withContext false
                java.io.File(dir, attachment.name).writeBytes(bytes)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

@Composable
fun ChatPanel(
    chat: ChatState,
    apiClient: ApiClient,
    toast: ToastState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var emojiOpen by remember { mutableStateOf(false) }
    var highlightId by remember { mutableStateOf<String?>(null) }
    var lightbox by remember { mutableStateOf<Pair<ImageBitmap, String>?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { chat.sendFile(context, uri, toast) }
    }

    LaunchedEffect(chat.messages.size) {
        if (chat.messages.isNotEmpty()) listState.animateScrollToItem(chat.messages.lastIndex)
    }

    Column(modifier = modifier.background(PanelBackground)) {
        HorizontalDivider(thickness = 1.dp, color = DarkCardBorder)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)
        ) {
            itemsIndexed(chat.messages, key = { _, m -> m.id }) { index, msg ->
                val prev = chat.messages.getOrNull(index - 1)
                val grouped = prev != null && prev.local == msg.local && prev.from == msg.from &&
                    msg.timestamp - prev.timestamp < GROUP_WINDOW_MS
                MessageRow(
                    msg = msg,
                    grouped = grouped,
                    first = index == 0,
                    highlighted = highlightId == msg.id,
                    apiClient = apiClient,
                    onOpenImage = { bmp, name -> lightbox = bmp to name },
                    onReply = { chat.startReply(msg) },
                    onDownload = { attachment ->
                        scope.launch {
                            val ok = saveToDownloads(context, apiClient, attachment)
                            toast.show(if (ok) "Сохранено в Загрузки: ${attachment.name}" else "Не удалось загрузить файл", error = !ok)
                        }
                    },
                    onJumpTo = { id ->
                        val target = chat.messages.indexOfFirst { it.id == id }
                        if (target >= 0) scope.launch {
                            listState.animateScrollToItem(target)
                            highlightId = id
                            delay(HIGHLIGHT_MS)
                            highlightId = null
                        }
                    }
                )
            }
        }

        if (chat.uploading) {
            HorizontalDivider(thickness = 1.dp, color = DarkCardBorder)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.6.dp)
            ) {
                Text(chat.uploadName, color = Color(0xA6FFFFFF), fontSize = 12.5.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.width(80.dp).height(4.8.dp).clip(RoundedCornerShape(50)).background(Color(0x1FFFFFFF))) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(chat.uploadProgress / 100f).background(Coral))
                }
                Text("${chat.uploadProgress}%", color = Color(0xA6FFFFFF), fontSize = 12.5.sp)
            }
        }

        chat.replyingTo?.let { reply ->
            HorizontalDivider(thickness = 1.dp, color = DarkCardBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Fill03)
                    .padding(end = 14.4.dp)
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(Coral))
                Column(modifier = Modifier.weight(1f).padding(vertical = 6.4.dp)) {
                    Text(reply.from, color = CoralSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = reply.message.ifEmpty {
                            if (reply.attachment?.type?.startsWith("image/") == true) "📷 Фото" else "📎 ${reply.attachment?.name ?: ""}"
                        },
                        color = Color(0x99FFFFFF), fontSize = 12.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier.size(28.8.dp).clip(CircleShape).background(Fill08)
                        .clickable { chat.replyingTo = null },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Tabler.X, contentDescription = "Отменить ответ", tint = Color(0xB3FFFFFF), modifier = Modifier.size(16.dp))
                }
            }
        }

        if (emojiOpen) {
            EmojiPicker(onPick = { emoji -> chat.draft += emoji })
        }

        HorizontalDivider(thickness = 1.dp, color = DarkCardBorder)
        ChatInputBar(
            chat = chat,
            emojiOpen = emojiOpen,
            onToggleEmoji = { emojiOpen = !emojiOpen },
            onAttach = { picker.launch("*/*") },
            onSend = { scope.launch { runCatching { chat.send() } } },
        )
    }

    lightbox?.let { (bitmap, name) -> Lightbox(bitmap, name) { lightbox = null } }
}

@Composable
private fun ChatInputBar(
    chat: ChatState,
    emojiOpen: Boolean,
    onToggleEmoji: () -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 10.4.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Fill06)
                .padding(start = 4.8.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarButton(Tabler.MoodSmile, "Эмодзи", onToggleEmoji, tint = if (emojiOpen) TextPrimary else Color(0xB3FFFFFF))
            Box(modifier = Modifier.weight(1f).padding(horizontal = 4.8.dp)) {
                if (chat.draft.isEmpty()) Text("Введите сообщение…", color = TextMuted, fontSize = 14.7.sp)
                BasicTextField(
                    value = chat.draft,
                    onValueChange = { chat.draft = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.7.sp),
                    cursorBrush = SolidColor(TextPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BarButton(
                if (chat.uploading) Tabler.Loader2 else Tabler.Paperclip, "Прикрепить файл", onAttach,
                enabled = !chat.uploading, iconSize = 18.dp
            )
            val canSend = chat.draft.isNotBlank()
            Box(
                modifier = Modifier
                    .size(34.4.dp)
                    .clip(CircleShape)
                    .background(if (canSend) Coral else Fill08)
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Tabler.Send, contentDescription = "Отправить",
                    tint = if (canSend) TextPrimary else Color(0x59FFFFFF),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun BarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = Color(0xB3FFFFFF),
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Box(
        modifier = Modifier.size(34.4.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick).alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun EmojiPicker(onPick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .heightIn(max = 256.dp)
            .clip(RoundedCornerShape(12.8.dp))
            .background(Color(0xFF1B1B1F))
            .padding(9.6.dp)
    ) {
        LazyVerticalGrid(columns = GridCells.Fixed(7), horizontalArrangement = Arrangement.spacedBy(1.6.dp)) {
            items(ALL_EMOJIS) { emoji ->
                Box(
                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(6.4.dp)).clickable { onPick(emoji) },
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun MessageRow(
    msg: ChatMessage,
    grouped: Boolean,
    first: Boolean,
    highlighted: Boolean,
    apiClient: ApiClient,
    onOpenImage: (ImageBitmap, String) -> Unit,
    onReply: () -> Unit,
    onDownload: (Attachment) -> Unit,
    onJumpTo: (String) -> Unit,
) {
    val local = msg.local
    val bare = msg.attachment != null && msg.message.isEmpty() && msg.replyTo == null
    val density = LocalDensity.current
    val triggerPx = with(density) { 46.dp.toPx() }
    val maxPx = with(density) { 68.dp.toPx() }
    var dragX by remember { mutableFloatStateOf(0f) }
    var triggered by remember { mutableStateOf(false) }

    val shape = if (bare) RoundedCornerShape(8.dp) else RoundedCornerShape(
        topStart = 16.8.dp, topEnd = 16.8.dp,
        bottomStart = if (local) 16.8.dp else 3.2.dp,
        bottomEnd = if (local) 3.2.dp else 16.8.dp
    )
    val background = when {
        bare -> Modifier
        highlighted -> Modifier.background(Color(0x47FFC400), shape)
        local -> Modifier.background(Brush.linearGradient(listOf(Color(0xFFFF6B5A), Color(0xFFE0503F))), shape)
        else -> Modifier.background(Color(0xFF202024), shape)
    }
    val metaColor = if (local) Color(0xBFFFFFFF) else Color(0x8CFFFFFF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (first) 0.dp else if (grouped) 1.9.dp else 8.8.dp),
        horizontalArrangement = if (local) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (local) ReplyButton(onReply)
        BoxWithConstraints(modifier = Modifier.weight(1f, fill = false)) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth * 0.78f, min = if (bare) 0.dp else 56.dp)
                    .offset { IntOffset(dragX.roundToInt(), 0) }
                    .pointerInput(msg.id) {
                        detectHorizontalDragGestures(
                            onDragStart = { triggered = false },
                            onDragEnd = { dragX = 0f },
                            onDragCancel = { dragX = 0f },
                        ) { _, delta ->
                            dragX = (dragX + delta).coerceIn(-maxPx, maxPx)
                            if (!triggered && abs(dragX) >= triggerPx) {
                                triggered = true
                                onReply()
                            }
                        }
                    }
                    .then(background)
                    .clip(shape)
                    .then(if (bare) Modifier else Modifier.padding(start = 16.dp, end = 16.dp, top = 7.2.dp, bottom = 5.6.dp)),
                verticalArrangement = Arrangement.spacedBy(2.4.dp)
            ) {
                if (!local && !grouped) {
                    Text(msg.from, color = Color(0xFFFF9A83), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                msg.replyTo?.let { quote ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.8.dp))
                            .background(if (local) Color(0x26000000) else Fill06)
                            .clickable { onJumpTo(quote.id) }
                            .height(IntrinsicSize.Min)
                    ) {
                        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                            Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(if (local) TextPrimary else Color(0xFF8AB4FF)))
                            Column(modifier = Modifier.padding(start = 8.dp, top = 3.2.dp, bottom = 3.2.dp)) {
                                Text(quote.from, color = if (local) TextPrimary else Color(0xFF8AB4FF), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Text(quote.text, color = if (local) Color(0xD9FFFFFF) else Color(0xB3FFFFFF), fontSize = 12.5.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                msg.attachment?.let { AttachmentView(it, local, apiClient, onOpenImage, onDownload) }
                if (msg.message.isNotEmpty()) {
                    val linkColor = if (local) TextPrimary else Color(0xFF8AB4FF)
                    Text(
                        text = buildAnnotatedString {
                            var last = 0
                            for (m in UrlRegex.findAll(msg.message)) {
                                append(msg.message.substring(last, m.range.first))
                                withLink(
                                    LinkAnnotation.Url(
                                        m.value,
                                        TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                                    )
                                ) { append(m.value) }
                                last = m.range.last + 1
                            }
                            append(msg.message.substring(last))
                        },
                        color = TextPrimary, fontSize = 14.4.sp, lineHeight = 19.sp
                    )
                }
                if (!bare) {
                    Text(
                        TimeFormat.format(Date(msg.timestamp)),
                        color = metaColor, fontSize = 10.5.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
        if (!local) ReplyButton(onReply)
    }
}

@Composable
private fun ReplyButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.4.dp)
            .size(27.2.dp)
            .clip(CircleShape)
            .background(Fill08)
            .clickable(onClick = onClick)
            .alpha(0.5f),
        contentAlignment = Alignment.Center
    ) {
        Icon(Tabler.ArrowBackUp, contentDescription = "Ответить", tint = Color(0xA6FFFFFF), modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun AttachmentView(
    attachment: Attachment,
    local: Boolean,
    apiClient: ApiClient,
    onOpenImage: (ImageBitmap, String) -> Unit,
    onDownload: (Attachment) -> Unit,
) {
    if (attachment.type.startsWith("image/")) {
        val image = rememberApiImage(apiClient, attachment.url)
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = attachment.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenImage(image, attachment.name) }
            )
        } else {
            Box(modifier = Modifier.size(120.dp, 80.dp).clip(RoundedCornerShape(8.dp)).background(Fill06))
        }
        return
    }
    val ext = attachment.name.substringAfterLast('.', "").let { if (it.length >= 4) "bin" else it }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (local) Color(0x26000000) else Fill06)
            .clickable { onDownload(attachment) }
            .padding(horizontal = 9.6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Tabler.File, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(28.dp))
            Text(ext, color = TextPrimary, fontSize = 12.8.sp, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(attachment.name, color = TextPrimary, fontSize = 13.6.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp))
            Text(
                "${formatSize(attachment.size)} · Скачать",
                color = if (local) Color(0xBFFFFFFF) else TextLabel, fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun Lightbox(image: ImageBitmap, name: String, onClose: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xE6000000))) {
            Image(
                bitmap = image,
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            offset = if (scale == 1f) androidx.compose.ui.geometry.Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
            )
            Box(
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
                    .size(41.6.dp).clip(CircleShape).background(Color(0x1FFFFFFF)).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Icon(Tabler.X, contentDescription = "Закрыть", tint = TextPrimary, modifier = Modifier.size(20.dp)) }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 17.6.dp)
                    .clip(RoundedCornerShape(32.dp)).background(Color(0xB3141416)).padding(5.6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.4.dp)
            ) {
                listOf(Tabler.ZoomOut to 0.8f, Tabler.ZoomIn to 1.25f).forEach { (icon, factor) ->
                    Box(
                        modifier = Modifier.size(38.4.dp).clip(CircleShape).background(Fill10)
                            .clickable { scale = (scale * factor).coerceIn(1f, 6f); if (scale == 1f) offset = androidx.compose.ui.geometry.Offset.Zero },
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}
