package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.Formatters
import com.example.domain.RoomGeometryRect
import com.example.domain.WallMeasurement
import com.example.ui.theme.BlackBorderSubtle
import com.example.ui.theme.BrightDoorGreen
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.PtSerifFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarningRed
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun AnimatedScreenEntrance(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(200)) +
            slideInVertically(
                animationSpec = tween(220),
                initialOffsetY = { it / 24 }
            )
    ) {
        content()
    }
}

@Composable
fun ZamerTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureWhite)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (onBackClick != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .heightIn(min = 50.dp)
                        .testTag("back_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, PureBlack),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PureWhite,
                        contentColor = PureBlack
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Назад",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (trailing != null) {
                    trailing()
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = PureBlack,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = PureBlack
            )
        }
    }
}

@Composable
fun ZamerPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_scale"
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .scale(scale)
            .let { if (testTag != null) it.testTag(testTag) else it },
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepGreen,
            contentColor = PureWhite,
            disabledContainerColor = BlackBorderSubtle,
            disabledContentColor = PureBlack
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ZamerSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = PureBlack,
    textColor: Color = PureBlack,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sec_btn_scale"
    )
    OutlinedButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .scale(scale)
            .let { if (testTag != null) it.testTag(testTag) else it },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = PureWhite,
            contentColor = textColor
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ZamerCard(
    modifier: Modifier = Modifier,
    borderColor: Color = PureBlack,
    containerColor: Color = PureWhite,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun ZamerInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    isGiantNumeric: Boolean = false,
    singleLine: Boolean = true,
    testTag: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = PureBlack,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .let { if (testTag != null) it.testTag(testTag) else it },
            singleLine = singleLine,
            placeholder = if (placeholder.isNotEmpty()) {
                {
                    Text(
                        text = placeholder,
                        style = if (isGiantNumeric) {
                            TextStyle(fontFamily = PtSerifFontFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = BlackBorderSubtle
                    )
                }
            } else null,
            textStyle = if (isGiantNumeric) {
                TextStyle(
                    fontFamily = PtSerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = PureBlack
                )
            } else {
                TextStyle(
                    fontFamily = PtSerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = PureBlack
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone?.invoke() },
                onNext = { onDone?.invoke() }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeepGreen,
                unfocusedBorderColor = PureBlack,
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                cursorColor = DeepGreen,
                focusedTextColor = PureBlack,
                unfocusedTextColor = PureBlack
            )
        )
    }
}

/**
 * Realistic schematic floorplan renderer:
 * - Room labels (Кухня, С/У, Комната...) inside rooms, NO dimension numbers
 * - Bright green ajar entry door ALWAYS at the bottom for instant orientation
 */
@Composable
fun SchematicPlanCanvas(
    rooms: List<RoomGeometryRect>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(PureWhite, RoundedCornerShape(12.dp))
            .border(1.5.dp, DeepGreen, RoundedCornerShape(12.dp))
            .padding(6.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val wallStroke = Stroke(width = 4f, cap = StrokeCap.Square)
            val labelPaint = Paint().apply {
                color = PureBlack.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = (minOf(w, h) * 0.088f).coerceIn(18f, 28f)
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            rooms.forEach { r ->
                val x1 = r.left * w
                val y1 = r.top * h
                val x2 = r.right * w
                val y2 = r.bottom * h
                val rw = (x2 - x1).coerceAtLeast(1f)
                val rh = (y2 - y1).coerceAtLeast(1f)

                drawRect(
                    color = DeepGreenSoft,
                    topLeft = Offset(x1, y1),
                    size = Size(rw, rh)
                )
                drawRect(
                    color = PureBlack,
                    topLeft = Offset(x1, y1),
                    size = Size(rw, rh),
                    style = wallStroke
                )

                if (r.hasWindowTop) {
                    val wx1 = x1 + rw * 0.25f
                    val wx2 = x1 + rw * 0.75f
                    drawLine(
                        color = PureWhite,
                        start = Offset(wx1, y1),
                        end = Offset(wx2, y1),
                        strokeWidth = 7f
                    )
                    drawLine(
                        color = DeepGreen,
                        start = Offset(wx1, y1),
                        end = Offset(wx2, y1),
                        strokeWidth = 3.5f
                    )
                }

                if (r.hasDoorBottom) {
                    val dx1 = x1 + rw * 0.36f
                    val dx2 = x1 + rw * 0.62f
                    drawLine(
                        color = PureWhite,
                        start = Offset(dx1, y2),
                        end = Offset(dx2, y2),
                        strokeWidth = 7f
                    )
                    // Subtle interior door leaf
                    drawLine(
                        color = DeepGreen,
                        start = Offset(dx1, y2),
                        end = Offset(dx1 + (dx2 - dx1) * 0.75f, y2 - (dx2 - dx1) * 0.65f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                if (r.hasDoorRight) {
                    val dy1 = y1 + rh * 0.36f
                    val dy2 = y1 + rh * 0.62f
                    drawLine(
                        color = PureWhite,
                        start = Offset(x2, dy1),
                        end = Offset(x2, dy2),
                        strokeWidth = 7f
                    )
                    drawLine(
                        color = DeepGreen,
                        start = Offset(x2, dy1),
                        end = Offset(x2 - (dy2 - dy1) * 0.65f, dy1 + (dy2 - dy1) * 0.75f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                // Bright green ajar entry door ALWAYS at the bottom
                if (r.hasEntryDoorBottom) {
                    val doorW = (rw * 0.34f).coerceIn(18f, 36f)
                    val dx1 = x1 + rw * 0.5f - doorW / 2f
                    val dx2 = dx1 + doorW
                    // Clear gap in outer wall
                    drawLine(
                        color = PureWhite,
                        start = Offset(dx1, y2),
                        end = Offset(dx2, y2),
                        strokeWidth = 8f
                    )
                    // Bright green ajar door leaf opening inward at ~40 degrees
                    drawLine(
                        color = BrightDoorGreen,
                        start = Offset(dx1, y2),
                        end = Offset(dx1 + doorW * 0.78f, y2 - doorW * 0.65f),
                        strokeWidth = 6.5f,
                        cap = StrokeCap.Round
                    )
                    // Subtle door swing arc
                    drawArc(
                        color = BrightDoorGreen,
                        startAngle = 0f,
                        sweepAngle = -45f,
                        useCenter = false,
                        topLeft = Offset(dx1 - doorW, y2 - doorW),
                        size = Size(doorW * 2f, doorW * 2f),
                        style = Stroke(width = 2.5f)
                    )
                }

                if (r.label.isNotBlank()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        r.label,
                        x1 + rw / 2f,
                        y1 + rh / 2f + labelPaint.textSize * 0.35f,
                        labelPaint
                    )
                }
            }
        }
    }
}

/**
 * Live proportional room drawing that morphs dynamically as the user enters wall lengths,
 * highlights the active wall, and flags geometric mismatches in real time.
 */
@Composable
fun ProportionalRoomCanvas(
    walls: List<WallMeasurement>,
    activeWallIndex: Int,
    hasWarning: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val padX = 48f
        val padY = 34f
        val availW = (size.width - padX * 2).coerceAtLeast(40f)
        val availH = (size.height - padY * 2).coerceAtLeast(40f)

        val labelPaint = Paint().apply {
            color = PureBlack.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val pts: List<Offset> = if (walls.size == 4) {
            // Live proportional quadrilateral based on entered lengths
            val w1 = walls[0].lengthMeters.takeIf { it > 0.0 } ?: 4.0
            val w2 = walls[1].lengthMeters.takeIf { it > 0.0 } ?: 3.0
            val w3 = walls[2].lengthMeters.takeIf { it > 0.0 } ?: w1
            val w4 = walls[3].lengthMeters.takeIf { it > 0.0 } ?: w2

            val maxHoriz = max(w1, w3).coerceAtLeast(0.5)
            val maxVert = max(w2, w4).coerceAtLeast(0.5)
            val scale = minOf(availW / maxHoriz.toFloat(), availH / maxVert.toFloat())

            val topLen = w1.toFloat() * scale
            val bottomLen = w3.toFloat() * scale
            val rightLen = w2.toFloat() * scale
            val leftLen = w4.toFloat() * scale

            val cx = size.width / 2f
            val cy = size.height / 2f
            val halfH = max(leftLen, rightLen) / 2f

            val tl = Offset(cx - topLen / 2f, cy - halfH)
            val tr = Offset(cx + topLen / 2f, cy - halfH)
            val br = Offset(cx + bottomLen / 2f, cy - halfH + rightLen)
            val bl = Offset(cx - bottomLen / 2f, cy - halfH + leftLen)
            listOf(tl, tr, br, bl)
        } else {
            val n = walls.size.coerceAtLeast(3)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = (minOf(availW, availH) / 2f).coerceAtLeast(24f)
            List(n) { i ->
                val angle = -Math.PI / 2.0 + (2.0 * Math.PI * i / n)
                Offset(
                    x = cx + (radius * cos(angle)).toFloat(),
                    y = cy + (radius * sin(angle)).toFloat()
                )
            }
        }

        for (i in pts.indices) {
            val p1 = pts[i]
            val p2 = pts[(i + 1) % pts.size]
            val wall = walls.getOrNull(i)
            val isActive = wall?.index == activeWallIndex
            val lineColor = when {
                isActive && hasWarning -> WarningRed
                isActive -> DeepGreen
                wall != null && wall.lengthMeters > 0.0 -> PureBlack
                else -> BlackBorderSubtle
            }
            drawLine(
                color = lineColor,
                start = p1,
                end = p2,
                strokeWidth = if (isActive) 11f else 5f,
                cap = StrokeCap.Round
            )
            if (wall != null && wall.lengthMeters > 0.0) {
                val midX = (p1.x + p2.x) / 2f
                val midY = (p1.y + p2.y) / 2f
                labelPaint.color = if (isActive) DeepGreen.toArgb() else PureBlack.toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    "${Formatters.formatCleanNumber(wall.lengthMeters)} м",
                    midX,
                    midY - 8f,
                    labelPaint
                )
            }
        }
    }
}
