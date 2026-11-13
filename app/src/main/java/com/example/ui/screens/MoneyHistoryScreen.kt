package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ProjectAggregate
import com.example.domain.Formatters
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ZamerCard
import com.example.ui.components.ZamerInputField
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.components.rememberVoiceInputLauncher
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.MutedGray
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarningRed
import kotlinx.coroutines.launch

@Composable
fun MoneyHistoryScreen(
    aggregate: ProjectAggregate?,
    onBackClick: () -> Unit,
    onAddMoney: (Long, Double, String, String, String) -> Unit,
    onDeleteMoney: (Long) -> Unit
) {
    if (aggregate == null) return

    val p = aggregate.project
    val entries = aggregate.moneyEntries
    val cashTotal = entries.filter { it.paymentMethod == "CASH" }.sumOf { it.amountRub }
    val cardTotal = entries.filter { it.paymentMethod == "CARD" }.sumOf { it.amountRub }
    val grandTotal = cashTotal + cardTotal

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") } // "CASH" or "CARD"
    var dateStr by remember { mutableStateOf(Formatters.todayDate()) }
    var reasonText by remember { mutableStateOf("") }
    var tapsRemaining by remember { mutableIntStateOf(3) }

    val parsedAmount = Formatters.parseDecimalInput(amountText) ?: 0.0
    val canSubmit = parsedAmount > 0.0 && dateStr.isNotBlank() && reasonText.isNotBlank()

    val launchReasonVoice = rememberVoiceInputLauncher(
        promptText = "За что получены деньги?",
        onResult = { spoken ->
            reasonText = if (reasonText.isBlank()) spoken else "${reasonText.trim()} $spoken"
            tapsRemaining = 3
        },
        onUnavailable = {
            scope.launch {
                snackbarHostState.showSnackbar("Голосовой ввод недоступен на устройстве")
            }
        }
    )

    Scaffold(
        containerColor = PureWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ZamerTopBar(
                title = "История денег",
                subtitle = p.name,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        AnimatedScreenEntrance(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ZamerCard(
                        modifier = Modifier.widthIn(max = 600.dp),
                        borderColor = DeepGreen,
                        containerColor = DeepGreen
                    ) {
                        Text("Всего получено", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatMoney(grandTotal),
                            style = MaterialTheme.typography.displayLarge,
                            color = PureWhite,
                            modifier = Modifier.testTag("money_grand_total")
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Наличка: ${Formatters.formatMoney(cashTotal)} · Карта: ${Formatters.formatMoney(cardTotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite
                        )
                    }
                }

                item {
                    ZamerCard(
                        modifier = Modifier.widthIn(max = 600.dp),
                        borderColor = PureBlack
                    ) {
                        Text("Новое поступление", style = MaterialTheme.typography.titleLarge, color = PureBlack)
                        Spacer(Modifier.height(10.dp))

                        ZamerInputField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                tapsRemaining = 3
                            },
                            label = "Сумма (₽)",
                            placeholder = "0",
                            keyboardType = KeyboardType.Decimal,
                            isGiantNumeric = true,
                            testTag = "money_amount_input"
                        )
                        Spacer(Modifier.height(10.dp))

                        Text("Способ оплаты:", style = MaterialTheme.typography.bodyLarge, color = PureBlack)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("CASH" to "Наличка", "CARD" to "Карта").forEach { (code, lbl) ->
                                val isSel = paymentMethod == code
                                OutlinedButton(
                                    onClick = {
                                        paymentMethod = code
                                        tapsRemaining = 3
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 50.dp)
                                        .testTag("money_method_$code"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(2.dp, if (isSel) DeepGreen else PureBlack),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSel) DeepGreen else PureWhite,
                                        contentColor = if (isSel) PureWhite else PureBlack
                                    )
                                ) {
                                    Text(lbl, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        ZamerInputField(
                            value = dateStr,
                            onValueChange = {
                                dateStr = it
                                tapsRemaining = 3
                            },
                            label = "Дата (обязательно)",
                            placeholder = "дд.мм.гггг",
                            keyboardType = KeyboardType.Number,
                            testTag = "money_date_input"
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZamerInputField(
                                value = reasonText,
                                onValueChange = {
                                    reasonText = it
                                    tapsRemaining = 3
                                },
                                label = "За что (обязательно)",
                                placeholder = "Например: Аванс за стены",
                                modifier = Modifier.weight(1f),
                                testTag = "money_reason_input"
                            )
                            OutlinedButton(
                                onClick = launchReasonVoice,
                                modifier = Modifier
                                    .size(56.dp)
                                    .testTag("money_voice_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(2.dp, DeepGreen),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = DeepGreenSoft,
                                    contentColor = DeepGreen
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = "Голосовой ввод причины", modifier = Modifier.size(26.dp))
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        val btnLabel = when (tapsRemaining) {
                            3 -> "Добавить (нажмите 3 раза)"
                            2 -> "Нажмите ещё 2 раза"
                            else -> "Нажмите ещё 1 раз — Сохранить!"
                        }
                        ZamerPrimaryButton(
                            text = btnLabel,
                            icon = Icons.Filled.Add,
                            enabled = canSubmit,
                            onClick = {
                                if (!canSubmit) return@ZamerPrimaryButton
                                if (tapsRemaining > 1) {
                                    tapsRemaining--
                                } else {
                                    onAddMoney(p.id, parsedAmount, paymentMethod, dateStr.trim(), reasonText.trim())
                                    amountText = ""
                                    reasonText = ""
                                    tapsRemaining = 3
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Платёж добавлен")
                                    }
                                }
                            },
                            testTag = "money_add_3tap_button"
                        )
                    }
                }

                if (entries.isEmpty()) {
                    item {
                        Text(
                            text = "Пока нет поступлений денег",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MutedGray
                        )
                    }
                } else {
                    items(entries, key = { it.id }) { entry ->
                        val methodLabel = if (entry.paymentMethod == "CARD") "Карта" else "Наличка"
                        ZamerCard(
                            modifier = Modifier.widthIn(max = 600.dp),
                            borderColor = PureBlack
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Formatters.formatMoney(entry.amountRub),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = DeepGreen
                                    )
                                    Text(
                                        text = "$methodLabel · ${entry.dateStr}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PureBlack
                                    )
                                    Text(
                                        text = entry.reasonText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = PureBlack
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteMoney(entry.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Удалить платёж", tint = WarningRed)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}
