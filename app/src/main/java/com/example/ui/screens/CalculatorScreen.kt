package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryEntryEntity
import com.example.data.ProjectAggregate
import com.example.domain.Formatters
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.MutedGray
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.CalculatorUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen(
    aggregate: ProjectAggregate?,
    calcUi: CalculatorUiState,
    onBackClick: () -> Unit,
    onDigit: (Long, String) -> Unit,
    onBackspace: (Long) -> Unit,
    onClear: (Long) -> Unit,
    onPercent: (Long) -> Unit,
    onOperator: (Long, String) -> Unit,
    onEquals: (Long) -> Unit,
    onInsertMeasurement: (Long, String, Double, String) -> Unit,
    onRestoreRecent: (Long, HistoryEntryEntity) -> Unit,
    onFlushToHistory: (Long) -> Unit
) {
    if (aggregate == null) return
    val projectId = aggregate.project.id
    val totals = aggregate.totals

    var showMeasurementsDialog by remember { mutableStateOf(false) }

    // Most recent calculation entry for the small gray top line
    val recentCalc = remember(aggregate.history) {
        aggregate.history.firstOrNull { it.source == "CALC" && it.resultValue != null }
    }

    DisposableEffect(projectId) {
        onDispose {
            onFlushToHistory(projectId)
        }
    }

    val displayExpr = buildString {
        if (calcUi.expression.isNotBlank()) {
            append(calcUi.expression)
            if (calcUi.currentInput.isNotBlank()) append(" ")
        }
        append(calcUi.currentInput)
    }.ifBlank { "0" }

    val unitTag = if (calcUi.activeUnit.isNotBlank()) " ${calcUi.activeUnit}" else ""

    Scaffold(
        containerColor = PureWhite
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top bar: Back on left + centered "Замеры" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Назад", style = MaterialTheme.typography.labelLarge)
                }

                // Single centered "Замеры" button
                OutlinedButton(
                    onClick = { showMeasurementsDialog = true },
                    modifier = Modifier
                        .heightIn(min = 50.dp)
                        .testTag("calc_measurements_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, DeepGreen),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DeepGreen,
                        contentColor = PureWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Straighten, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Замеры", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.width(48.dp))
            }

            // Classic Display with small gray recent calculation (double-tap to restore!)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(2.dp, PureBlack)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (recentCalc != null) {
                        Text(
                            text = recentCalc.expression,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MutedGray,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onDoubleClick = {
                                        onRestoreRecent(projectId, recentCalc)
                                    }
                                )
                                .padding(bottom = 6.dp)
                                .testTag("calc_recent_history_line")
                        )
                    }

                    Text(
                        text = "$displayExpr$unitTag",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureBlack,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_expression_text")
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "= ${Formatters.formatCleanNumber(calcUi.livePreview)}$unitTag",
                        style = MaterialTheme.typography.displayLarge,
                        color = DeepGreen,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_result_preview")
                    )
                }
            }

            // Non-scrolling classic 5-row keypad filling remaining space cleanly
            val rows = listOf(
                listOf("C", "⌫", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("00", "0", ".", "=")
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            ClassicCalcKey(
                                label = key,
                                isOperator = key in listOf("C", "⌫", "%", "÷", "×", "−", "+"),
                                isEquals = key == "=",
                                onClick = {
                                    when (key) {
                                        "C" -> onClear(projectId)
                                        "⌫" -> onBackspace(projectId)
                                        "%" -> onPercent(projectId)
                                        "=" -> onEquals(projectId)
                                        "÷", "×", "−", "+" -> onOperator(projectId, key)
                                        else -> onDigit(projectId, key)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // Concise Measurements Picker Dialog (tap any number to insert into calculator with unit!)
    if (showMeasurementsDialog) {
        AlertDialog(
            onDismissRequest = { showMeasurementsDialog = false },
            containerColor = PureWhite,
            title = {
                Text("Замеры объекта", style = MaterialTheme.typography.titleLarge, color = PureBlack)
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepGreenSoft, RoundedCornerShape(14.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Вся квартира", style = MaterialTheme.typography.titleMedium, color = DeepGreen)
                            MeasureInsertRow(
                                label = "Стены (чистая)",
                                valueStr = Formatters.formatArea(totals.totalWallAreaSqm),
                                onClick = {
                                    onInsertMeasurement(projectId, "Стены", totals.totalWallAreaSqm, "м²")
                                    showMeasurementsDialog = false
                                }
                            )
                            MeasureInsertRow(
                                label = "Стены (с окнами)",
                                valueStr = Formatters.formatArea(totals.totalGrossWallAreaSqm),
                                onClick = {
                                    onInsertMeasurement(projectId, "Стены (без вычета)", totals.totalGrossWallAreaSqm, "м²")
                                    showMeasurementsDialog = false
                                }
                            )
                            MeasureInsertRow(
                                label = "Пол (без стен)",
                                valueStr = Formatters.formatArea(totals.totalInnerFloorAreaSqm),
                                onClick = {
                                    onInsertMeasurement(projectId, "Пол", totals.totalInnerFloorAreaSqm, "м²")
                                    showMeasurementsDialog = false
                                }
                            )
                            MeasureInsertRow(
                                label = "Уголки",
                                valueStr = Formatters.formatRunningMeters(totals.totalCornersMeters),
                                onClick = {
                                    onInsertMeasurement(projectId, "Уголки", totals.totalCornersMeters, "п.м.")
                                    showMeasurementsDialog = false
                                }
                            )
                        }
                    }

                    items(aggregate.rooms, key = { it.entity.id }) { room ->
                        val c = room.calculations
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PureWhite, RoundedCornerShape(14.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(room.entity.name, style = MaterialTheme.typography.titleMedium, color = PureBlack)
                            MeasureInsertRow(
                                label = "Стены (чистая)",
                                valueStr = Formatters.formatArea(c.netWallAreaSqm),
                                onClick = {
                                    onInsertMeasurement(projectId, "${room.entity.name} стены", c.netWallAreaSqm, "м²")
                                    showMeasurementsDialog = false
                                }
                            )
                            MeasureInsertRow(
                                label = "Стены (без вычета окон)",
                                valueStr = Formatters.formatArea(c.grossWallAreaSqm),
                                onClick = {
                                    onInsertMeasurement(projectId, "${room.entity.name} стены полн.", c.grossWallAreaSqm, "м²")
                                    showMeasurementsDialog = false
                                }
                            )
                            MeasureInsertRow(
                                label = "Пол (без стен)",
                                valueStr = Formatters.formatArea(c.innerFloorAreaSqm),
                                onClick = {
                                    onInsertMeasurement(projectId, "${room.entity.name} пол", c.innerFloorAreaSqm, "м²")
                                    showMeasurementsDialog = false
                                }
                            )
                            MeasureInsertRow(
                                label = "Уголки",
                                valueStr = Formatters.formatRunningMeters(c.totalCornersMeters),
                                onClick = {
                                    onInsertMeasurement(projectId, "${room.entity.name} углы", c.totalCornersMeters, "п.м.")
                                    showMeasurementsDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMeasurementsDialog = false }) {
                    Text("Закрыть", color = PureBlack)
                }
            }
        )
    }
}

@Composable
private fun MeasureInsertRow(
    label: String,
    valueStr: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = PureWhite,
        border = BorderStroke(1.5.dp, DeepGreen),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = PureBlack, modifier = Modifier.weight(1f))
            Text(valueStr, style = MaterialTheme.typography.titleMedium, color = DeepGreen)
        }
    }
}

@Composable
private fun ClassicCalcKey(
    label: String,
    isOperator: Boolean,
    isEquals: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        isEquals -> DeepGreen
        isOperator -> DeepGreenSoft
        else -> PureWhite
    }
    val fg = if (isEquals) PureWhite else PureBlack
    val borderColor = if (isEquals) DeepGreen else PureBlack

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.testTag("calc_key_$label"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = bg,
            contentColor = fg
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        if (label == "⌫") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Стереть",
                tint = fg,
                modifier = Modifier.size(26.dp)
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = fg
            )
        }
    }
}
