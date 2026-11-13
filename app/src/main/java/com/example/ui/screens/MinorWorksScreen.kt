package com.example.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.MinorWorkEntity
import com.example.data.ProjectAggregate
import com.example.domain.Formatters
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ZamerCard
import com.example.ui.components.ZamerInputField
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MinorWorksScreen(
    aggregate: ProjectAggregate?,
    onBackClick: () -> Unit,
    onSaveWork: (MinorWorkEntity) -> Unit,
    onDeleteWork: (MinorWorkEntity) -> Unit
) {
    if (aggregate == null) return

    var editingId by remember { mutableStateOf(0L) }
    var commentInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim()
            if (!spoken.isNullOrBlank()) {
                commentInput = if (commentInput.isBlank()) {
                    spoken.replaceFirstChar { it.uppercase() }
                } else {
                    "$commentInput $spoken"
                }
            }
        }
    }

    val quickSuggestions = listOf(
        "Откосы",
        "Подмазать штробы",
        "Убраться",
        "Перевоз",
        "Зачистка"
    )

    val parsedPrice = Formatters.parseDecimalInput(priceInput) ?: 0.0

    Scaffold(
        containerColor = PureWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ZamerTopBar(
                title = "Мелкая работа",
                subtitle = "Итого: ${Formatters.formatMoney(aggregate.totals.minorWorksTotalRub)}",
                onBackClick = onBackClick
            )
        }
    ) { inner ->
        AnimatedScreenEntrance(Modifier.fillMaxSize().padding(inner)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    ZamerCard(
                        modifier = Modifier.widthIn(max = 600.dp),
                        borderColor = DeepGreen
                    ) {
                        Text(
                            text = if (editingId == 0L) "Добавить работу" else "Изменить работу",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepGreen
                        )

                        Spacer(Modifier.height(10.dp))

                        // 5 core quick chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickSuggestions.forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DeepGreenSoft,
                                    border = BorderStroke(1.5.dp, PureBlack),
                                    modifier = Modifier.clickable { commentInput = preset }
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PureBlack,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Comment + Voice Input Microphone button!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ZamerInputField(
                                value = commentInput,
                                onValueChange = { commentInput = it },
                                label = "Комментарий (что сделать)",
                                placeholder = "Напишите или нажмите микрофон",
                                singleLine = false,
                                modifier = Modifier.weight(1f),
                                testTag = "minor_work_title_input"
                            )
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите, какую работу добавить")
                                    }
                                    try {
                                        voiceLauncher.launch(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Голосовой ввод недоступен на этом устройстве")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(62.dp)
                                    .testTag("voice_input_button"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(2.dp, DeepGreen),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = DeepGreen,
                                    contentColor = PureWhite
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Голосовой ввод",
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        ZamerInputField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            label = "Цена (₽)",
                            placeholder = "1500",
                            keyboardType = KeyboardType.Decimal,
                            isGiantNumeric = true,
                            testTag = "minor_work_price_input"
                        )

                        Spacer(Modifier.height(14.dp))
                        ZamerPrimaryButton(
                            text = if (editingId == 0L) "Добавить работу" else "Сохранить изменения",
                            icon = Icons.Filled.Add,
                            enabled = commentInput.isNotBlank(),
                            onClick = {
                                onSaveWork(
                                    MinorWorkEntity(
                                        id = editingId,
                                        projectId = aggregate.project.id,
                                        title = commentInput.trim(),
                                        price = parsedPrice,
                                        quantity = 1.0,
                                        unitLabel = "шт"
                                    )
                                )
                                editingId = 0L
                                commentInput = ""
                                priceInput = ""
                            },
                            testTag = "save_minor_work_button"
                        )
                    }
                }

                items(aggregate.minorWorks, key = { it.id }) { work ->
                    ZamerCard(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .clickable {
                                editingId = work.id
                                commentInput = work.title
                                priceInput = Formatters.formatCleanNumber(work.totalCost)
                            },
                        borderColor = PureBlack
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(work.title, style = MaterialTheme.typography.titleLarge, color = PureBlack)
                                Text(
                                    text = Formatters.formatMoney(work.totalCost),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = DeepGreen
                                )
                            }
                            IconButton(
                                onClick = { onDeleteWork(work) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Удалить", tint = PureBlack)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
