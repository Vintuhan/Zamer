package com.example.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
import com.example.domain.BlueprintFixtureKind
import com.example.domain.BlueprintOpening
import com.example.domain.BlueprintOpeningKind
import com.example.domain.EntryDoorSide
import com.example.domain.Formatters
import com.example.domain.MinimapPlan
import com.example.domain.RoomGeometryRect
import com.example.domain.SnappedSegment
import com.example.domain.WallMeasurement
import com.example.ui.theme.BlackBorderSubtle
import com.example.ui.theme.BrightDoorGreen
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.PtSerifFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarningRed
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
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
    trailing: (@Composable () -> Unit)? = null,
    sideTrailing: (@Composable () -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureWhite)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (sideTrailing != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (onBackClick != null) {
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
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(
                        modifier = if (onTitleClick != null) {
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(onClick = onTitleClick)
                                .testTag("topbar_rename_title_button")
                        } else {
                            Modifier.fillMaxWidth()
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = PureBlack,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (onTitleClick != null) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Переименовать комнату",
                                tint = DeepGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureBlack
                        )
                    }
                }
                sideTrailing()
            }
        } else {
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
 * Architectural CAD / BTI blueprint renderer:
 * - Subtle drafting millimeter grid background
 * - Distinct exterior load-bearing walls vs crisp interior partitions
 * - Authentic CAD fixtures (kitchen stove/sink, bath/toilet, wardrobe niches)
 * - Position-accurate double-glazed windows, balcony door blocks, and hinged interior doors with 90° swing arcs
 * - Bright green main entry door with entrance arrow
 * - Room labels + typical BTI floor areas (м²)
 */
@Composable
fun SchematicPlanCanvas(
    rooms: List<RoomGeometryRect>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFFAFCFB), RoundedCornerShape(14.dp))
            .border(1.5.dp, DeepGreen, RoundedCornerShape(14.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (w <= 4f || h <= 4f) return@Canvas

            // 0. Subtle CAD blueprint background grid
            val gridStep = (min(w, h) / 16f).coerceIn(14f, 28f)
            var gx = gridStep
            while (gx < w) {
                drawLine(Color(0xFFEFF4F1), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                gx += gridStep
            }
            var gy = gridStep
            while (gy < h) {
                drawLine(Color(0xFFEFF4F1), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                gy += gridStep
            }

            val baseTextSize = (min(w, h) * 0.062f).coerceIn(17f, 29f)
            val labelPaint = Paint().apply {
                color = PureBlack.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = baseTextSize
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
            val areaPaint = Paint().apply {
                color = DeepGreen.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = (baseTextSize * 0.78f).coerceIn(13f, 21f)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            fun buildPoly(r: RoomGeometryRect): List<Offset> =
                r.resolvedPolygon().map { Offset(it.x * w, it.y * h) }

            val roomPolys = rooms.map { r -> r to buildPoly(r) }.filter { it.second.size >= 3 }

            fun signedArea(poly: List<Offset>): Float {
                var sum = 0f
                for (i in poly.indices) {
                    val a = poly[i]
                    val b = poly[(i + 1) % poly.size]
                    sum += a.x * b.y - b.x * a.y
                }
                return sum * 0.5f
            }

            fun inwardNormalForEdge(p1: Offset, p2: Offset, polyAreaSign: Float): Offset {
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                return if (polyAreaSign >= 0f) {
                    Offset(-dy / len, dx / len)
                } else {
                    Offset(dy / len, -dx / len)
                }
            }

            fun pointToSegDistance(p: Offset, a: Offset, b: Offset): Float {
                val dx = b.x - a.x
                val dy = b.y - a.y
                val lenSq = dx * dx + dy * dy
                if (lenSq < 1e-4f) return hypot((p.x - a.x).toDouble(), (p.y - a.y).toDouble()).toFloat()
                val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
                return hypot((p.x - (a.x + t * dx)).toDouble(), (p.y - (a.y + t * dy)).toDouble()).toFloat()
            }

            fun isSharedInteriorEdge(roomIdx: Int, p1: Offset, p2: Offset): Boolean {
                val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                val tol = (min(w, h) * 0.022f).coerceIn(5f, 11f)
                return roomPolys.indices.any { otherIdx ->
                    if (otherIdx == roomIdx) return@any false
                    val oPoly = roomPolys[otherIdx].second
                    oPoly.indices.any { oi ->
                        pointToSegDistance(mid, oPoly[oi], oPoly[(oi + 1) % oPoly.size]) <= tol
                    }
                }
            }

            fun drawQuarterArc(hinge: Offset, ux: Float, uy: Float, nx: Float, ny: Float, radius: Float, color: Color, strokeWidth: Float) {
                val arcPath = Path()
                val steps = 10
                for (s in 0..steps) {
                    val rad = (Math.PI / 2.0) * (s.toFloat() / steps)
                    val ca = cos(rad).toFloat()
                    val sa = sin(rad).toFloat()
                    val px = hinge.x + (ux * ca + nx * sa) * radius
                    val py = hinge.y + (uy * ca + ny * sa) * radius
                    if (s == 0) arcPath.moveTo(px, py) else arcPath.lineTo(px, py)
                }
                drawPath(arcPath, color = color, style = Stroke(width = strokeWidth))
            }

            fun drawWindowSegment(p1: Offset, p2: Offset, centerFraction: Float, widthFraction: Float) {
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                val ux = dx / len
                val uy = dy / len
                val nx = -uy
                val ny = ux
                val span = (len * widthFraction).coerceIn(min(22f, len * 0.65f), len * 0.84f)
                val startT = (len * centerFraction - span / 2f).coerceIn(2f, max(2f, len - span - 2f))
                val wx1 = Offset(p1.x + ux * startT, p1.y + uy * startT)
                val wx2 = Offset(wx1.x + ux * span, wx1.y + uy * span)

                drawLine(Color(0xFFFAFCFB), wx1, wx2, strokeWidth = 9.5f, cap = StrokeCap.Butt)
                // Window frame end ticks + outer sill frame + double glazing
                drawLine(PureBlack, Offset(wx1.x + nx * 3.5f, wx1.y + ny * 3.5f), Offset(wx1.x - nx * 3.5f, wx1.y - ny * 3.5f), strokeWidth = 2.4f)
                drawLine(PureBlack, Offset(wx2.x + nx * 3.5f, wx2.y + ny * 3.5f), Offset(wx2.x - nx * 3.5f, wx2.y - ny * 3.5f), strokeWidth = 2.4f)
                drawLine(DeepGreen, Offset(wx1.x + nx * 2.2f, wx1.y + ny * 2.2f), Offset(wx2.x + nx * 2.2f, wx2.y + ny * 2.2f), strokeWidth = 2.4f)
                drawLine(DeepGreen, Offset(wx1.x - nx * 2.2f, wx1.y - ny * 2.2f), Offset(wx2.x - nx * 2.2f, wx2.y - ny * 2.2f), strokeWidth = 2.4f)
            }

            fun drawBalconyDoorBlock(p1: Offset, p2: Offset, centerFraction: Float, inN: Offset) {
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                val ux = dx / len
                val uy = dy / len
                val nx = -uy
                val ny = ux
                val totalSpan = (len * 0.56f).coerceIn(min(30f, len * 0.72f), len * 0.84f)
                val startT = (len * centerFraction - totalSpan / 2f).coerceIn(2f, max(2f, len - totalSpan - 2f))
                val s0 = Offset(p1.x + ux * startT, p1.y + uy * startT)
                val mid = Offset(s0.x + ux * (totalSpan * 0.45f), s0.y + uy * (totalSpan * 0.45f))
                val s2 = Offset(s0.x + ux * totalSpan, s0.y + uy * totalSpan)

                drawLine(Color(0xFFFAFCFB), s0, s2, strokeWidth = 9.5f, cap = StrokeCap.Butt)
                // Window half
                drawLine(DeepGreen, Offset(mid.x + nx * 2f, mid.y + ny * 2f), Offset(s2.x + nx * 2f, s2.y + ny * 2f), strokeWidth = 2.2f)
                drawLine(DeepGreen, Offset(mid.x - nx * 2f, mid.y - ny * 2f), Offset(s2.x - nx * 2f, s2.y - ny * 2f), strokeWidth = 2.2f)
                // Hinged balcony door half
                val leaf = hypot((mid.x - s0.x).toDouble(), (mid.y - s0.y).toDouble()).toFloat().coerceIn(14f, 30f)
                drawQuarterArc(s0, ux, uy, inN.x, inN.y, leaf, DeepGreen.copy(alpha = 0.50f), 1.5f)
                drawLine(
                    color = DeepGreen,
                    start = s0,
                    end = Offset(s0.x + inN.x * leaf, s0.y + inN.y * leaf),
                    strokeWidth = 2.8f,
                    cap = StrokeCap.Round
                )
            }

            fun drawInteriorDoor(p1: Offset, p2: Offset, centerFraction: Float, widthFraction: Float, inN: Offset) {
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                val ux = dx / len
                val uy = dy / len
                val leaf = (len * widthFraction).coerceIn(min(15f, len * 0.55f), 28f)
                val startT = (len * centerFraction - leaf / 2f).coerceIn(2f, max(2f, len - leaf - 2f))
                val d1 = Offset(p1.x + ux * startT, p1.y + uy * startT)
                val d2 = Offset(d1.x + ux * leaf, d1.y + uy * leaf)

                drawLine(Color(0xFFFAFCFB), d1, d2, strokeWidth = 9f, cap = StrokeCap.Butt)
                drawQuarterArc(d1, ux, uy, inN.x, inN.y, leaf, DeepGreen.copy(alpha = 0.55f), 1.5f)
                drawLine(
                    color = DeepGreen,
                    start = d1,
                    end = Offset(d1.x + inN.x * leaf, d1.y + inN.y * leaf),
                    strokeWidth = 3.0f,
                    cap = StrokeCap.Round
                )
            }

            fun drawMainEntryDoor(p1: Offset, p2: Offset, inN: Offset) {
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                val ux = dx / len
                val uy = dy / len
                val leaf = (len * 0.36f).coerceIn(20f, 38f)
                val startT = ((len - leaf) / 2f).coerceAtLeast(2f)
                val d1 = Offset(p1.x + ux * startT, p1.y + uy * startT)
                val d2 = Offset(d1.x + ux * leaf, d1.y + uy * leaf)

                drawLine(Color(0xFFFAFCFB), d1, d2, strokeWidth = 10.5f, cap = StrokeCap.Butt)
                drawQuarterArc(d1, ux, uy, inN.x, inN.y, leaf, BrightDoorGreen.copy(alpha = 0.75f), 2.0f)
                drawLine(
                    color = BrightDoorGreen,
                    start = d1,
                    end = Offset(d1.x + ux * (leaf * 0.25f) + inN.x * (leaf * 0.92f), d1.y + uy * (leaf * 0.25f) + inN.y * (leaf * 0.92f)),
                    strokeWidth = 6.5f,
                    cap = StrokeCap.Round
                )
                // Exterior arrow showing entry direction
                val midDoor = Offset((d1.x + d2.x) / 2f, (d1.y + d2.y) / 2f)
                val tip = Offset(midDoor.x - inN.x * 3f, midDoor.y - inN.y * 3f)
                val base1 = Offset(midDoor.x - inN.x * 11f - ux * 5f, midDoor.y - inN.y * 11f - uy * 5f)
                val base2 = Offset(midDoor.x - inN.x * 11f + ux * 5f, midDoor.y - inN.y * 11f + uy * 5f)
                val arrow = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(base1.x, base1.y)
                    lineTo(base2.x, base2.y)
                    close()
                }
                drawPath(arrow, color = BrightDoorGreen)
            }

            fun resolveFixtureKind(r: RoomGeometryRect): BlueprintFixtureKind {
                if (r.fixtureKind != BlueprintFixtureKind.NONE) return r.fixtureKind
                val low = r.label.lowercase()
                return when {
                    "кухн" in low -> BlueprintFixtureKind.KITCHEN
                    "ван" in low -> BlueprintFixtureKind.BATH
                    "с/у" in low || "туал" in low || "сануз" in low -> {
                        if (r.right - r.left >= 0.20f && r.bottom - r.top >= 0.20f) {
                            BlueprintFixtureKind.COMBINED_BATH
                        } else {
                            BlueprintFixtureKind.TOILET
                        }
                    }
                    "гард" in low || "клад" in low -> BlueprintFixtureKind.WARDROBE
                    else -> BlueprintFixtureKind.NONE
                }
            }

            // PASS 1: Room floor fills, balcony glazing hatch, wet-zone tiles, and subtle CAD fixtures
            roomPolys.forEach { (r, poly) ->
                val polyPath = Path().apply {
                    moveTo(poly.first().x, poly.first().y)
                    for (i in 1 until poly.size) lineTo(poly[i].x, poly[i].y)
                    close()
                }
                val x1 = poly.minOf { it.x }
                val x2 = poly.maxOf { it.x }
                val y1 = poly.minOf { it.y }
                val y2 = poly.maxOf { it.y }
                val rw = (x2 - x1).coerceAtLeast(1f)
                val rh = (y2 - y1).coerceAtLeast(1f)

                val fxKind = resolveFixtureKind(r)
                val isWet = fxKind == BlueprintFixtureKind.BATH ||
                    fxKind == BlueprintFixtureKind.TOILET ||
                    fxKind == BlueprintFixtureKind.COMBINED_BATH

                drawPath(
                    path = polyPath,
                    color = when {
                        r.isBalcony -> Color(0xFFE6F1EB)
                        isWet -> Color(0xFFF3F9F6)
                        else -> Color(0xFFEDF5F0)
                    }
                )

                if (r.isBalcony) {
                    val step = 14f
                    val span = rw + rh
                    clipPath(polyPath) {
                        var d = -span
                        while (d <= span * 1.5f) {
                            drawLine(
                                color = Color(0xFFC9DFD3),
                                start = Offset(x1 + d, y2 + 4f),
                                end = Offset(x1 + d + rh + 8f, y1 - 4f),
                                strokeWidth = 1.5f
                            )
                            d += step
                        }
                    }
                } else if (isWet) {
                    clipPath(polyPath) {
                        val tStep = 14f
                        var tx = x1 + tStep
                        while (tx < x2) {
                            drawLine(Color(0xFFE0ECE5), Offset(tx, y1), Offset(tx, y2), strokeWidth = 1f)
                            tx += tStep
                        }
                        var ty = y1 + tStep
                        while (ty < y2) {
                            drawLine(Color(0xFFE0ECE5), Offset(x1, ty), Offset(x2, ty), strokeWidth = 1f)
                            ty += tStep
                        }
                    }
                }

                // Subtle BTI CAD symbols clipped inside the room
                val symColor = Color(0xFF8AA596)
                clipPath(polyPath) {
                    when (fxKind) {
                        BlueprintFixtureKind.KITCHEN -> if (rw >= 34f && rh >= 34f) {
                            val padW = min(rw * 0.24f, 18f)
                            drawRect(
                                color = symColor,
                                topLeft = Offset(x1 + 3f, y1 + 3f),
                                size = Size(rw * 0.62f, padW),
                                style = Stroke(width = 1.5f)
                            )
                            // 4-burner stove circle grid
                            val sx = x1 + rw * 0.14f
                            val sy = y1 + 3f + padW * 0.5f
                            val br = min(padW * 0.19f, 3.2f)
                            drawCircle(symColor, br, Offset(sx - br * 1.4f, sy - br * 1.2f), style = Stroke(1.2f))
                            drawCircle(symColor, br, Offset(sx + br * 1.4f, sy - br * 1.2f), style = Stroke(1.2f))
                            drawCircle(symColor, br, Offset(sx - br * 1.4f, sy + br * 1.2f), style = Stroke(1.2f))
                            drawCircle(symColor, br, Offset(sx + br * 1.4f, sy + br * 1.2f), style = Stroke(1.2f))
                            // Sink basin
                            drawRect(
                                color = symColor,
                                topLeft = Offset(x1 + rw * 0.42f, y1 + 5f),
                                size = Size(min(rw * 0.16f, 14f), max(4f, padW - 4f)),
                                style = Stroke(width = 1.3f)
                            )
                        }
                        BlueprintFixtureKind.BATH -> if (rw >= 24f && rh >= 24f) {
                            val bathH = min(rh * 0.36f, 18f)
                            drawRoundRect(
                                color = symColor,
                                topLeft = Offset(x1 + 4f, y1 + 4f),
                                size = Size(max(12f, rw - 8f), bathH),
                                cornerRadius = CornerRadius(6f, 6f),
                                style = Stroke(width = 1.5f)
                            )
                            drawCircle(symColor, 2.2f, Offset(x1 + rw * 0.2f, y1 + 4f + bathH / 2f), style = Stroke(1.2f))
                        }
                        BlueprintFixtureKind.TOILET -> if (rw >= 18f && rh >= 22f) {
                            val tw = min(rw * 0.44f, 14f)
                            val th = min(rh * 0.26f, 12f)
                            drawRect(
                                color = symColor,
                                topLeft = Offset(x1 + (rw - tw) / 2f, y1 + 4f),
                                size = Size(tw, max(4f, th * 0.45f)),
                                style = Stroke(1.3f)
                            )
                            drawOval(
                                color = symColor,
                                topLeft = Offset(x1 + (rw - tw * 0.85f) / 2f, y1 + 4f + th * 0.55f),
                                size = Size(tw * 0.85f, max(6f, th * 0.9f)),
                                style = Stroke(1.3f)
                            )
                        }
                        BlueprintFixtureKind.COMBINED_BATH -> if (rw >= 32f && rh >= 32f) {
                            val bathH = min(rh * 0.32f, 16f)
                            drawRoundRect(
                                color = symColor,
                                topLeft = Offset(x1 + 4f, y1 + 4f),
                                size = Size(max(14f, rw - 8f), bathH),
                                cornerRadius = CornerRadius(5f, 5f),
                                style = Stroke(width = 1.5f)
                            )
                            val tw = min(rw * 0.28f, 11f)
                            drawOval(
                                color = symColor,
                                topLeft = Offset(x2 - tw - 6f, y2 - tw * 1.25f - 4f),
                                size = Size(tw, tw * 1.15f),
                                style = Stroke(1.3f)
                            )
                        }
                        BlueprintFixtureKind.WARDROBE -> if (rw >= 20f && rh >= 20f) {
                            drawLine(
                                color = symColor,
                                start = Offset(x1 + 4f, y1 + rh * 0.25f),
                                end = Offset(x2 - 4f, y1 + rh * 0.25f),
                                strokeWidth = 1.3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                            )
                        }
                        BlueprintFixtureKind.NONE -> Unit
                    }
                }
            }

            // PASS 2: Draw structural exterior walls (thick) & interior partition walls (crisp medium)
            roomPolys.forEachIndexed { rIdx, (_, poly) ->
                for (i in poly.indices) {
                    val p1 = poly[i]
                    val p2 = poly[(i + 1) % poly.size]
                    val isInterior = isSharedInteriorEdge(rIdx, p1, p2)
                    drawLine(
                        color = PureBlack,
                        start = p1,
                        end = p2,
                        strokeWidth = if (isInterior) 3.8f else 7.0f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // PASS 3: Draw all windows, balcony blocks, interior doors, and green main entry door
            roomPolys.forEach { (r, poly) ->
                val areaSign = signedArea(poly)

                // New exact positional openings
                r.openings.forEach { op ->
                    if (op.edgeIndex in poly.indices) {
                        val p1 = poly[op.edgeIndex]
                        val p2 = poly[(op.edgeIndex + 1) % poly.size]
                        val inN = inwardNormalForEdge(p1, p2, areaSign)
                        when (op.kind) {
                            BlueprintOpeningKind.WINDOW ->
                                drawWindowSegment(p1, p2, op.centerFraction, op.widthFraction)
                            BlueprintOpeningKind.BALCONY_DOOR ->
                                drawBalconyDoorBlock(p1, p2, op.centerFraction, inN)
                            BlueprintOpeningKind.DOOR ->
                                drawInteriorDoor(p1, p2, op.centerFraction, op.widthFraction, inN)
                        }
                    }
                }

                // Polygon edge lists (used in slanted/chamfered rooms)
                r.windowEdgeIndices.forEach { eIdx ->
                    if (eIdx in poly.indices && r.openings.none { it.edgeIndex == eIdx && it.kind == BlueprintOpeningKind.WINDOW }) {
                        drawWindowSegment(poly[eIdx], poly[(eIdx + 1) % poly.size], 0.5f, 0.54f)
                    }
                }
                r.doorEdgeIndices.forEach { eIdx ->
                    if (eIdx in poly.indices && r.openings.none { it.edgeIndex == eIdx && (it.kind == BlueprintOpeningKind.DOOR || it.kind == BlueprintOpeningKind.BALCONY_DOOR) }) {
                        val p1 = poly[eIdx]
                        val p2 = poly[(eIdx + 1) % poly.size]
                        drawInteriorDoor(p1, p2, 0.5f, 0.28f, inwardNormalForEdge(p1, p2, areaSign))
                    }
                }

                // Legacy rectangular side flags (when edge-based openings not set on that edge)
                if (r.polygon.isEmpty() && poly.size == 4) {
                    val hasOpOn = { idx: Int -> r.openings.any { it.edgeIndex == idx } }
                    if (r.hasWindowTop && !hasOpOn(0)) drawWindowSegment(poly[0], poly[1], 0.5f, 0.54f)
                    if (r.hasWindowRight && !hasOpOn(1)) drawWindowSegment(poly[1], poly[2], 0.5f, 0.54f)
                    if (r.hasWindowBottom && !hasOpOn(2)) drawWindowSegment(poly[2], poly[3], 0.5f, 0.54f)
                    if (r.hasWindowLeft && !hasOpOn(3)) drawWindowSegment(poly[3], poly[0], 0.5f, 0.54f)

                    if (r.hasDoorTop && !hasOpOn(0)) drawInteriorDoor(poly[0], poly[1], 0.5f, 0.28f, inwardNormalForEdge(poly[0], poly[1], areaSign))
                    if (r.hasDoorRight && !hasOpOn(1)) drawInteriorDoor(poly[1], poly[2], 0.5f, 0.28f, inwardNormalForEdge(poly[1], poly[2], areaSign))
                    if (r.hasDoorBottom && !hasOpOn(2)) drawInteriorDoor(poly[2], poly[3], 0.5f, 0.28f, inwardNormalForEdge(poly[2], poly[3], areaSign))
                    if (r.hasDoorLeft && !hasOpOn(3)) drawInteriorDoor(poly[3], poly[0], 0.5f, 0.28f, inwardNormalForEdge(poly[3], poly[0], areaSign))
                }

                // Main bright-green entrance door
                val entryEdge = when {
                    r.entryDoorEdgeIndex in poly.indices -> r.entryDoorEdgeIndex
                    r.polygon.isEmpty() && poly.size == 4 -> when (r.entryDoorSide) {
                        EntryDoorSide.TOP -> 0
                        EntryDoorSide.RIGHT -> 1
                        EntryDoorSide.BOTTOM -> 2
                        EntryDoorSide.LEFT -> 3
                        EntryDoorSide.NONE -> -1
                    }
                    else -> -1
                }
                if (entryEdge in poly.indices) {
                    val p1 = poly[entryEdge]
                    val p2 = poly[(entryEdge + 1) % poly.size]
                    drawMainEntryDoor(p1, p2, inwardNormalForEdge(p1, p2, areaSign))
                }
            }

            // PASS 4: Room labels + typical BTI area annotation inside each room
            roomPolys.forEach { (r, poly) ->
                if (r.label.isBlank()) return@forEach
                val x1 = poly.minOf { it.x }
                val x2 = poly.maxOf { it.x }
                val y1 = poly.minOf { it.y }
                val y2 = poly.maxOf { it.y }
                val rw = (x2 - x1).coerceAtLeast(1f)
                val rh = (y2 - y1).coerceAtLeast(1f)
                val labelPt = r.resolvedLabelCenter().let { Offset(it.x * w, it.y * h) }

                var sz = baseTextSize.coerceAtMost(rh * 0.42f)
                labelPaint.textSize = sz
                val maxTextW = rw * 0.86f
                val measured = labelPaint.measureText(r.label)
                if (measured > maxTextW && measured > 0f) {
                    sz = (sz * (maxTextW / measured)).coerceAtLeast(11f)
                    labelPaint.textSize = sz
                }

                val areaText = r.typicalAreaSqm?.let { "${Formatters.formatCleanNumber(it)} м²" }
                val showArea = areaText != null && rh >= 38f && rw >= 34f
                val nameOffsetY = if (showArea) -labelPaint.textSize * 0.14f else labelPaint.textSize * 0.34f

                drawContext.canvas.nativeCanvas.drawText(
                    r.label,
                    labelPt.x,
                    labelPt.y + nameOffsetY,
                    labelPaint
                )

                if (showArea && areaText != null) {
                    var aSz = (sz * 0.80f).coerceIn(10f, 19f)
                    areaPaint.textSize = aSz
                    val aMeasured = areaPaint.measureText(areaText)
                    if (aMeasured > maxTextW && aMeasured > 0f) {
                        aSz = (aSz * (maxTextW / aMeasured)).coerceAtLeast(9.5f)
                        areaPaint.textSize = aSz
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        areaText,
                        labelPt.x,
                        labelPt.y + labelPaint.textSize * 0.78f,
                        areaPaint
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
    modifier: Modifier = Modifier,
    shapeKey: String = "AUTO",
    customSegments: List<SnappedSegment> = emptyList()
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

        fun fitNormalizedPolygon(normPts: List<Offset>): List<Offset> {
            val minX = normPts.minOf { it.x }
            val maxX = normPts.maxOf { it.x }
            val minY = normPts.minOf { it.y }
            val maxY = normPts.maxOf { it.y }
            val spanX = (maxX - minX).coerceAtLeast(1e-3f)
            val spanY = (maxY - minY).coerceAtLeast(1e-3f)
            val aspect = spanX / spanY
            val drawW: Float
            val drawH: Float
            if (availW / availH > aspect) {
                drawH = availH
                drawW = drawH * aspect
            } else {
                drawW = availW
                drawH = drawW / aspect
            }
            val ox = (size.width - drawW) / 2f
            val oy = (size.height - drawH) / 2f
            return normPts.map { pt ->
                Offset(
                    x = ox + ((pt.x - minX) / spanX) * drawW,
                    y = oy + ((pt.y - minY) / spanY) * drawH
                )
            }
        }

        if (walls.size == 1) {
            val wall = walls.first()
            val isActive = wall.index == activeWallIndex
            val y = size.height / 2f
            val x1 = padX
            val x2 = size.width - padX
            val lineColor = when {
                isActive && hasWarning -> WarningRed
                isActive -> DeepGreen
                wall.lengthMeters > 0.0 -> PureBlack
                else -> BlackBorderSubtle
            }
            drawLine(
                color = lineColor,
                start = Offset(x1, y),
                end = Offset(x2, y),
                strokeWidth = if (isActive) 12f else 8f,
                cap = StrokeCap.Round
            )
            drawCircle(color = lineColor, radius = 7f, center = Offset(x1, y))
            drawCircle(color = lineColor, radius = 7f, center = Offset(x2, y))
            val labelText = if (wall.lengthMeters > 0.0) {
                "${Formatters.formatCleanNumber(wall.lengthMeters)} м"
            } else {
                "Стена 1"
            }
            labelPaint.color = if (isActive) DeepGreen.toArgb() else PureBlack.toArgb()
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                size.width / 2f,
                y - 16f,
                labelPaint
            )
            return@Canvas
        }

        val pts: List<Offset> = when {
            customSegments.size == walls.size && customSegments.size >= 3 -> {
                fitNormalizedPolygon(customSegments.map { it.start })
            }
            walls.size == 4 -> {
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
            }
            shapeKey == "U_SHAPE" || (shapeKey == "AUTO" && walls.size == 8) -> {
                // П-образная (8 стен) без ромба
                fitNormalizedPolygon(
                    listOf(
                        Offset(0f, 0f),
                        Offset(1.5f, 0f),
                        Offset(1.5f, 1.0f),
                        Offset(1.02f, 1.0f),
                        Offset(1.02f, 0.44f),
                        Offset(0.48f, 0.44f),
                        Offset(0.48f, 1.0f),
                        Offset(0f, 1.0f)
                    )
                )
            }
            shapeKey == "L_SHAPE" || (shapeKey == "AUTO" && walls.size == 6) -> {
                // Г-образная (6 стен)
                fitNormalizedPolygon(
                    listOf(
                        Offset(0f, 0f),
                        Offset(1.35f, 0f),
                        Offset(1.35f, 0.45f),
                        Offset(0.62f, 0.45f),
                        Offset(0.62f, 1.0f),
                        Offset(0f, 1.0f)
                    )
                )
            }
            walls.size == 5 -> {
                // Со скошенным углом (5 стен)
                fitNormalizedPolygon(
                    listOf(
                        Offset(0f, 0f),
                        Offset(0.95f, 0f),
                        Offset(1.35f, 0.38f),
                        Offset(1.35f, 1.0f),
                        Offset(0f, 1.0f)
                    )
                )
            }
            else -> {
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
            if (wall != null) {
                val midX = (p1.x + p2.x) / 2f
                val midY = (p1.y + p2.y) / 2f
                val labelText = when {
                    wall.lengthMeters > 0.0 -> "${Formatters.formatCleanNumber(wall.lengthMeters)} м"
                    isActive -> "${wall.index}"
                    else -> null
                }
                if (labelText != null) {
                    labelPaint.color = if (isActive) DeepGreen.toArgb() else PureBlack.toArgb()
                    drawContext.canvas.nativeCanvas.drawText(
                        labelText,
                        midX,
                        midY - 8f,
                        labelPaint
                    )
                }
            }
        }
    }
}

/**
 * Compact minimap of the ENTIRE apartment layout:
 * - Currently measured room is shaded/hatched in green (зелёным заштрихована)
 * - Currently measured wall is highlighted boldly in pure black (выделена чёрным)
 * - Hallway entry door shown in bright green so apartment orientation is unmistakable
 */
@Composable
fun FloorPlanMinimapCanvas(
    plan: MinimapPlan,
    showLabels: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val pad = if (showLabels) 12f else 5f
        val availW = (size.width - pad * 2f).coerceAtLeast(16f)
        val availH = (size.height - pad * 2f).coerceAtLeast(16f)

        val targetRatio = plan.aspectRatio.coerceIn(0.75f, 2.1f)
        val drawW: Float
        val drawH: Float
        if (availW / availH > targetRatio) {
            drawH = availH
            drawW = drawH * targetRatio
        } else {
            drawW = availW
            drawH = drawW / targetRatio
        }
        val offsetX = (size.width - drawW) / 2f
        val offsetY = (size.height - drawH) / 2f

        fun toCanvas(p: Offset) = Offset(offsetX + p.x * drawW, offsetY + p.y * drawH)

        fun buildPolyPath(pts: List<Offset>): Path = Path().apply {
            if (pts.isNotEmpty()) {
                moveTo(pts.first().x, pts.first().y)
                for (i in 1 until pts.size) {
                    lineTo(pts[i].x, pts[i].y)
                }
                close()
            }
        }

        val labelPaint = if (showLabels) {
            Paint().apply {
                color = PureBlack.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = (minOf(drawW, drawH) * 0.068f).coerceIn(18f, 30f)
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                isAntiAlias = true
            }
        } else {
            null
        }

        // 1. Pass 1: draw inactive rooms first
        plan.rooms.filter { !it.isActive }.forEach { room ->
            if (room.polygon.size < 2) return@forEach
            val pts = room.polygon.map(::toCanvas)
            if (pts.size >= 3) {
                val path = buildPolyPath(pts)
                drawPath(
                    path = path,
                    color = if (room.isBalcony) Color(0xFFF3F8F5) else Color(0xFFF9FBFA)
                )
                drawPath(
                    path = path,
                    color = BlackBorderSubtle,
                    style = Stroke(
                        width = if (showLabels) 3.2f else 1.8f,
                        join = StrokeJoin.Round
                    )
                )
            } else {
                drawLine(
                    color = BlackBorderSubtle,
                    start = pts[0],
                    end = pts[1],
                    strokeWidth = if (showLabels) 3.2f else 1.8f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 2. Pass 2: draw active room with distinct GREEN diagonal hatching + green outline
        plan.rooms.filter { it.isActive }.forEach { room ->
            if (room.polygon.size < 2) return@forEach
            val pts = room.polygon.map(::toCanvas)
            if (pts.size >= 3) {
                val path = buildPolyPath(pts)
                // Translucent green base fill
                drawPath(
                    path = path,
                    color = BrightDoorGreen.copy(alpha = 0.22f)
                )
                // Diagonal green hatching clipped strictly inside the active room polygon
                val rMinX = pts.minOf { it.x }
                val rMaxX = pts.maxOf { it.x }
                val rMinY = pts.minOf { it.y }
                val rMaxY = pts.maxOf { it.y }
                val rSpan = (rMaxX - rMinX) + (rMaxY - rMinY)
                val hatchStep = if (showLabels) 16f else 8.5f
                val hatchStroke = if (showLabels) 3.0f else 2.2f
                clipPath(path) {
                    var d = -rSpan
                    while (d <= rSpan * 1.5f) {
                        drawLine(
                            color = DeepGreen,
                            start = Offset(rMinX + d, rMaxY + 4f),
                            end = Offset(rMinX + d + (rMaxY - rMinY) + 8f, rMinY - 4f),
                            strokeWidth = hatchStroke,
                            cap = StrokeCap.Square
                        )
                        d += hatchStep
                    }
                }
                // Crisp green outline around the active room
                drawPath(
                    path = path,
                    color = DeepGreen,
                    style = Stroke(
                        width = if (showLabels) 4.5f else 2.8f,
                        join = StrokeJoin.Round
                    )
                )
            } else if (pts.size == 2) {
                drawLine(
                    color = BrightDoorGreen.copy(alpha = 0.45f),
                    start = pts[0],
                    end = pts[1],
                    strokeWidth = if (showLabels) 16f else 11f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Pass 3: apartment entry door green mark for spatial orientation (edge-based or side-based)
        plan.rooms.forEach { room ->
            if (room.polygon.size < 3) return@forEach
            val pts = room.polygon.map(::toCanvas)
            val doorLen = if (showLabels) 26f else 13f
            val strokeW = if (showLabels) 5.5f else 3.5f
            if (room.entryDoorEdgeIndex in pts.indices) {
                val p1 = pts[room.entryDoorEdgeIndex]
                val p2 = pts[(room.entryDoorEdgeIndex + 1) % pts.size]
                val mx = (p1.x + p2.x) / 2f
                val my = (p1.y + p2.y) / 2f
                val cx = pts.map { it.x }.average().toFloat()
                val cy = pts.map { it.y }.average().toFloat()
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                val ux = dx / len
                val uy = dy / len
                var nx = -uy
                var ny = dx / len
                if (nx * (cx - mx) + ny * (cy - my) < 0f) {
                    nx = -nx
                    ny = -ny
                }
                val start = Offset(mx - ux * doorLen * 0.35f, my - uy * doorLen * 0.35f)
                val end = Offset(start.x + ux * doorLen * 0.7f + nx * doorLen * 0.6f, start.y + uy * doorLen * 0.7f + ny * doorLen * 0.6f)
                drawLine(BrightDoorGreen, start, end, strokeWidth = strokeW, cap = StrokeCap.Round)
            } else if (room.entryDoorSide != EntryDoorSide.NONE) {
                val x1 = pts.minOf { it.x }
                val x2 = pts.maxOf { it.x }
                val y1 = pts.minOf { it.y }
                val y2 = pts.maxOf { it.y }
                val rw = x2 - x1
                val rh = y2 - y1
                when (room.entryDoorSide) {
                    EntryDoorSide.BOTTOM -> {
                        val mx = x1 + rw * 0.5f
                        drawLine(BrightDoorGreen, Offset(mx - doorLen * 0.4f, y2), Offset(mx + doorLen * 0.4f, y2 - doorLen * 0.55f), strokeWidth = strokeW, cap = StrokeCap.Round)
                    }
                    EntryDoorSide.TOP -> {
                        val mx = x1 + rw * 0.5f
                        drawLine(BrightDoorGreen, Offset(mx - doorLen * 0.4f, y1), Offset(mx + doorLen * 0.4f, y1 + doorLen * 0.55f), strokeWidth = strokeW, cap = StrokeCap.Round)
                    }
                    EntryDoorSide.LEFT -> {
                        val my = y1 + rh * 0.5f
                        drawLine(BrightDoorGreen, Offset(x1, my - doorLen * 0.4f), Offset(x1 + doorLen * 0.55f, my + doorLen * 0.4f), strokeWidth = strokeW, cap = StrokeCap.Round)
                    }
                    EntryDoorSide.RIGHT -> {
                        val my = y1 + rh * 0.5f
                        drawLine(BrightDoorGreen, Offset(x2, my - doorLen * 0.4f), Offset(x2 - doorLen * 0.55f, my + doorLen * 0.4f), strokeWidth = strokeW, cap = StrokeCap.Round)
                    }
                    EntryDoorSide.NONE -> Unit
                }
            }
        }

        // 4. Pass 4: optional room labels when expanded
        if (showLabels && labelPaint != null) {
            plan.rooms.forEach { room ->
                if (room.label.isBlank() || room.polygon.size < 3) return@forEach
                val pts = room.polygon.map(::toCanvas)
                val cx = pts.map { it.x }.average().toFloat()
                val cy = pts.map { it.y }.average().toFloat()
                val rw = (pts.maxOf { it.x } - pts.minOf { it.x }).coerceAtLeast(12f)
                val baseSz = (minOf(drawW, drawH) * 0.065f).coerceIn(16f, 28f)
                labelPaint.textSize = baseSz
                val measured = labelPaint.measureText(room.label)
                if (measured > rw * 0.88f && measured > 0f) {
                    labelPaint.textSize = (baseSz * (rw * 0.88f / measured)).coerceAtLeast(11f)
                }
                labelPaint.color = if (room.isActive) DeepGreen.toArgb() else PureBlack.toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    room.label,
                    cx,
                    cy + labelPaint.textSize * 0.35f,
                    labelPaint
                )
            }
        }

        // 5. Pass 5 (TOP-MOST): highlight the active wall in BOLD PURE BLACK
        plan.rooms.filter { it.isActive }.forEach { room ->
            val edge = room.activeWallEdgeIndex
            if (edge in room.polygon.indices && room.polygon.size >= 2) {
                val p1 = toCanvas(room.polygon[edge])
                val p2 = toCanvas(room.polygon[(edge + 1) % room.polygon.size])
                drawLine(
                    color = PureBlack,
                    start = p1,
                    end = p2,
                    strokeWidth = if (showLabels) 11f else 7.5f,
                    cap = StrokeCap.Round
                )
                val dotRadius = if (showLabels) 7f else 4.2f
                drawCircle(color = PureBlack, radius = dotRadius, center = p1)
                drawCircle(color = PureBlack, radius = dotRadius, center = p2)
            }
        }
    }
}

@Composable
fun RenameRoomDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameText by remember(currentName) { mutableStateOf(currentName) }
    val presets = listOf(
        "Комната",
        "Зал",
        "Спальня",
        "Кухня",
        "Прихожая",
        "Коридор",
        "Санузел",
        "Ванная",
        "Туалет",
        "Лоджия",
        "Гардеробная"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        title = {
            Text(
                text = "Название комнаты",
                style = MaterialTheme.typography.titleLarge,
                color = PureBlack
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ZamerInputField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = "Введите или выберите ниже",
                    imeAction = ImeAction.Done,
                    onDone = {
                        if (nameText.isNotBlank()) {
                            onConfirm(nameText.trim())
                        }
                    },
                    testTag = "rename_room_input"
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        OutlinedButton(
                            onClick = { nameText = preset },
                            border = BorderStroke(1.5.dp, DeepGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(preset, color = DeepGreen)
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onConfirm(nameText.trim())
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DeepGreen,
                    contentColor = PureWhite
                ),
                modifier = Modifier.testTag("confirm_rename_room_button")
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = PureBlack)
            }
        }
    )
}

@Composable
fun rememberVoiceInputLauncher(
    promptText: String = "Говорите",
    onResult: (String) -> Unit,
    onUnavailable: () -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim()
            if (!spoken.isNullOrBlank()) {
                onResult(spoken)
            }
        }
    }
    return remember(promptText, launcher) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
            }
            try {
                launcher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                onUnavailable()
            }
        }
    }
}
