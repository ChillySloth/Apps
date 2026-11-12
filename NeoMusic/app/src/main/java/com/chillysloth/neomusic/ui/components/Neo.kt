package com.chillysloth.neomusic.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chillysloth.neomusic.ui.theme.Neo

/**
 * The core neubrutalist surface: flat fill, thick outline and a hard offset shadow.
 * When clickable it "presses" into its shadow.
 */
@Composable
fun NeoBox(
    modifier: Modifier = Modifier,
    color: Color = Neo.colors.surface,
    shape: Shape = RoundedCornerShape(Neo.dimens.radius),
    shadow: Dp = Neo.dimens.shadow,
    borderWidth: Dp = Neo.dimens.border,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val ink = Neo.colors.ink
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateDpAsState(if (pressed && onClick != null) shadow else 0.dp, spring(stiffness = 1800f), label = "press")
    Box(
        modifier
            .graphicsLayer {
                translationX = press.toPx(); translationY = press.toPx()
            }
            .drawBehind {
                val s = (shadow - press).toPx()
                if (s > 0f) {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    translate(s, s) { drawOutline(outline, ink) }
                }
            }
            .clip(shape)
            .background(color)
            .border(borderWidth, ink, shape)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = onClickLabel,
                    onClick = onClick,
                ) else Modifier
            ),
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = Neo.colors.yellow,
    textColor: Color = Neo.colors.onAccent,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    shape: Shape = RoundedCornerShape(50),
    style: TextStyle = Neo.type.label,
) {
    NeoBox(modifier, color = color, shape = shape, shadow = Neo.dimens.shadowSmall, onClick = onClick, contentAlignment = Alignment.Center) {
        Row(
            Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = style, color = textColor, maxLines = 1)
        }
    }
}

@Composable
fun NeoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Neo.colors.surface,
    tint: Color = if (color == Neo.colors.surface || color == Neo.colors.paper) Neo.colors.text else Neo.colors.onAccent,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    shape: Shape = CircleShape,
    shadow: Dp = Neo.dimens.shadowSmall,
) {
    NeoBox(
        modifier.size(size), color = color, shape = shape, shadow = shadow,
        onClick = onClick, onClickLabel = contentDescription, contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Borderless icon tap target (for row overflow buttons etc). */
@Composable
fun PlainIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Neo.colors.text,
    size: Dp = 40.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = contentDescription),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun NeoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Neo.colors.yellow,
) {
    NeoBox(
        modifier,
        color = if (selected) selectedColor else Neo.colors.surface,
        shape = RoundedCornerShape(50),
        shadow = if (selected) Neo.dimens.shadowSmall else 0.dp,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = Neo.type.label,
            color = if (selected) Neo.colors.onAccent else Neo.colors.text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            maxLines = 1,
        )
    }
}

/** Neubrutal dropdown built on Material's popup (handles positioning + dismiss). */
@Composable
fun NeoMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = offset,
        shape = RoundedCornerShape(Neo.dimens.radius),
        containerColor = Neo.colors.surface,
        border = BorderStroke(Neo.dimens.border, Neo.colors.ink),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        content = content,
    )
}

@Composable
fun NeoMenuItem(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    color: Color = Neo.colors.text,
) {
    DropdownMenuItem(
        text = { Text(text, style = Neo.type.title.copy(fontSize = Neo.type.body.fontSize), color = color) },
        leadingIcon = icon?.let { { Icon(it, null, tint = color) } },
        trailingIcon = trailing,
        onClick = onClick,
        colors = MenuDefaults.itemColors(),
        contentPadding = PaddingValues(horizontal = 16.dp),
    )
}

/** Dialog chrome: a big neubrutal card with a colored title band. */
@Composable
fun NeoDialog(
    title: String,
    onDismiss: () -> Unit,
    accent: Color = Neo.colors.yellow,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val ink = Neo.colors.ink
    Dialog(onDismissRequest = onDismiss) {
        NeoBox(Modifier.fillMaxWidth().padding(end = Neo.dimens.shadow, bottom = Neo.dimens.shadow), shadow = 6.dp) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(accent)
                        .drawBehind {
                            val w = 2.5.dp.toPx()
                            drawLine(
                                ink,
                                start = androidx.compose.ui.geometry.Offset(0f, size.height - w / 2),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height - w / 2),
                                strokeWidth = w,
                            )
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(title, style = Neo.type.h2, color = Neo.colors.onAccent, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Column(Modifier.padding(20.dp), content = content)
            }
        }
    }
}

@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    shadow: Dp = 0.dp,
    color: Color = Neo.colors.surface,
    textFieldModifier: Modifier = Modifier,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
) {
    NeoBox(modifier, color = color, shape = RoundedCornerShape(Neo.dimens.radiusSmall), shadow = shadow) {
        Row(Modifier.padding(horizontal = 14.dp).height(50.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                Icon(leading, null, tint = Neo.colors.text, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = Neo.type.body, color = Neo.colors.textMuted, maxLines = 1)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    textStyle = Neo.type.body.copy(color = Neo.colors.text, fontSize = Neo.type.title.fontSize),
                    cursorBrush = SolidColor(Neo.colors.text),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = textFieldModifier.fillMaxWidth(),
                )
            }
            trailing?.invoke()
        }
    }
}

/** Small sticker-style tag, like the "NEW" badges in the reference designs. */
@Composable
fun NeoTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .border(2.dp, Neo.colors.ink, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = Neo.type.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = Neo.colors.onAccent, maxLines = 1)
    }
}
