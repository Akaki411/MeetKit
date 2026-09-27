/**
 * Переиспользуемые UI-компоненты: поля ввода, кнопки, карточки, бейджи и переключатели.
 */
package com.livekit.meetkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livekit.meetkit.ui.icons.Tabler
import com.livekit.meetkit.ui.theme.*

val FieldShape = RoundedCornerShape(9.6.dp)
val CardShape = RoundedCornerShape(16.dp)

@Composable
fun WebField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    enabled: Boolean = true,
    fontSize: TextUnit = 16.sp,
    shape: RoundedCornerShape = FieldShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 13.6.dp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(color = TextPrimary, fontSize = fontSize),
        cursorBrush = SolidColor(TextPrimary),
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (password) keyboardOptions.copy(keyboardType = KeyboardType.Password) else keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .glass(shape, Fill06)
            .alpha(if (enabled) 1f else 0.6f),
        decorationBox = { inner ->
            Row(
                modifier = Modifier.padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.4.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = TextMuted,
                            fontSize = fontSize,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
                if (password) {
                    Icon(
                        imageVector = if (visible) Tabler.EyeOff else Tabler.Eye,
                        contentDescription = "Показать пароль",
                        tint = TextLabel,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { visible = !visible },
                    )
                }
            }
        },
    )
}

@Composable
fun WebButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fill: Color = Fill08,
    pressedFill: Color = Fill10,
    shape: RoundedCornerShape = FieldShape,
    height: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier
            .height(height)
            .glass(shape, if (pressed && enabled) pressedFill else fill)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.6f)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(6.4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun WebButtonText(text: String, fontSize: TextUnit = 16.sp, color: Color = TextPrimary) {
    Text(text, color = color, fontSize = fontSize, fontWeight = FontWeight.Normal, maxLines = 1)
}

@Composable
fun WebCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(24.dp),
    spacing: Dp = 14.4.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .glass(CardShape, Fill03)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

@Composable
fun WebSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = TextPrimary, fontSize = 17.6.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

@Composable
fun WebCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = TextLabel,
        fontSize = 12.8.sp,
        letterSpacing = 0.64.sp,
        modifier = modifier,
    )
}

@Composable
fun WebPill(
    text: String,
    container: Color,
    content: Color,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(horizontal = 9.6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
        Text(text, color = content, fontSize = 11.5.sp, maxLines = 1)
    }
}

@Composable
fun WebIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    size: Dp = 32.dp,
    iconSize: Dp = 16.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill = when {
        danger && pressed -> Color(0x3DFF5050)
        danger -> DangerContainer
        pressed -> Color(0x24FFFFFF)
        else -> Fill08
    }
    Box(
        modifier = modifier
            .size(size)
            .glass(shape, fill)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (danger) DangerText else if (pressed) TextPrimary else Color(0xB3FFFFFF),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun GradientAvatar(
    initials: String,
    size: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(BrandAccent, BrandAccentDark))),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) content() else {
            Text(initials, color = TextPrimary, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun BrandTitle(fontSize: TextUnit = 40.sp, modifier: Modifier = Modifier) {
    Text(
        text = androidx.compose.ui.text.buildAnnotatedString {
            pushStyle(androidx.compose.ui.text.SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold))
            append("Meet")
            pop()
            pushStyle(androidx.compose.ui.text.SpanStyle(color = Coral, fontWeight = FontWeight.ExtraLight))
            append("Kit")
            pop()
        },
        fontSize = fontSize,
        letterSpacing = (-0.5).sp,
        modifier = modifier,
    )
}

@Composable
fun WebCheck(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.2.dp),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (checked) BrandAccent else Fill10),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Tabler.Check,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Text(label, color = Color(0xBFFFFFFF), fontSize = 14.4.sp)
    }
}
