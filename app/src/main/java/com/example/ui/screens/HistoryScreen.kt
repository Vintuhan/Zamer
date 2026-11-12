package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.HistoryEntryEntity
import com.example.data.ProjectAggregate
import com.example.domain.Formatters
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ZamerCard
import com.example.ui.components.ZamerSecondaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun HistoryScreen(
    aggregate: ProjectAggregate?,
    onBackClick: () -> Unit,
    onReturnToCalculator: (HistoryEntryEntity) -> Unit,
    onClearHistory: (Long) -> Unit
) {
    if (aggregate == null) return
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = "История",
                subtitle = "Все замеры и вычисления (новые сверху)",
                onBackClick = onBackClick,
                trailing = if (aggregate.history.isNotEmpty()) {
                    {
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            border = BorderStroke(2.dp, PureBlack),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Очистить", tint = PureBlack)
                        }
                    }
                } else null
            )
        }
    ) { inner ->
        AnimatedScreenEntrance(Modifier.fillMaxSize().padding(inner)) {
            if (aggregate.history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZamerCard(modifier = Modifier.widthIn(max = 560.dp), borderColor = DeepGreen) {
                        Text("История пока пуста", style = MaterialTheme.typography.headlineMedium, color = PureBlack)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Все ваши замеры, мелкие работы и действия в калькуляторе автоматически появятся здесь.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureBlack
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(aggregate.history, key = { it.id }) { entry ->
                        ZamerCard(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .testTag("history_item_${entry.id}"),
                            borderColor = if (entry.source == "CALC") DeepGreen else PureBlack
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = Formatters.formatDateTime(entry.timestamp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureBlack
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepGreenSoft, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = entry.expression,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PureBlack
                                )
                            }
                            if (entry.resultValue != null) {
                                Spacer(Modifier.height(10.dp))
                                ZamerSecondaryButton(
                                    text = "В калькулятор (${Formatters.formatCleanNumber(entry.resultValue)})",
                                    icon = Icons.Filled.Calculate,
                                    borderColor = DeepGreen,
                                    textColor = DeepGreen,
                                    onClick = { onReturnToCalculator(entry) },
                                    testTag = "history_to_calc_${entry.id}"
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = PureWhite,
            title = { Text("Очистить историю?", style = MaterialTheme.typography.titleLarge, color = PureBlack) },
            text = { Text("Замеры объекта останутся, удалятся только записи журнала.", style = MaterialTheme.typography.bodyMedium, color = PureBlack) },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        onClearHistory(aggregate.project.id)
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepGreen, contentColor = PureWhite)
                ) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена", color = PureBlack)
                }
            }
        )
    }
}
