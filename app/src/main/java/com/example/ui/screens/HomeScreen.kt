package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ProjectSummaryItem
import com.example.domain.Formatters
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ZamerCard
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarningRed

@Composable
fun HomeScreen(
    projects: List<ProjectSummaryItem>,
    onAddProjectClick: () -> Unit,
    onOpenProjectClick: (Long) -> Unit,
    onDeleteProjectClick: (Long) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<ProjectSummaryItem?>(null) }
    var deleteConfirmStep by remember { mutableIntStateOf(1) }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = "Замерщик",
                subtitle = if (projects.isEmpty()) {
                    "Офлайн-замеры и смета без интернета"
                } else {
                    "Ваши объекты: ${projects.size}"
                }
            )
        },
        bottomBar = {
            Surface(
                color = PureWhite,
                border = BorderStroke(1.dp, PureBlack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZamerPrimaryButton(
                        text = "Добавить объект",
                        icon = Icons.Filled.Add,
                        onClick = onAddProjectClick,
                        modifier = Modifier.widthIn(max = 600.dp),
                        testTag = "add_project_button"
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedScreenEntrance(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZamerCard(
                        modifier = Modifier.widthIn(max = 560.dp),
                        borderColor = DeepGreen
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .background(DeepGreenSoft, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Architecture,
                                    contentDescription = "Чертёж",
                                    tint = DeepGreen,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Пока нет объектов",
                                style = MaterialTheme.typography.headlineMedium,
                                color = PureBlack
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Нажмите «Добавить объект» внизу, чтобы выбрать планировку и начать замеры.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureBlack
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(projects, key = { it.project.id }) { item ->
                        ZamerCard(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .testTag("project_card_${item.project.id}")
                                .clickable { onOpenProjectClick(item.project.id) },
                            borderColor = PureBlack
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(DeepGreen, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.HomeWork,
                                            contentDescription = null,
                                            tint = PureWhite,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.project.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = PureBlack,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.project.address.isNotBlank()) {
                                            Text(
                                                text = item.project.address,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = PureBlack,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        pendingDelete = item
                                        deleteConfirmStep = 1
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("delete_project_btn_${item.project.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Удалить объект",
                                        tint = PureBlack,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.project.layoutTitle} · ${item.roomCount} комн.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepGreen
                                )
                                Text(
                                    text = "с ${item.project.startDate}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureBlack
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepGreenSoft, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Стены: ${Formatters.formatArea(item.totalWallAreaSqm)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = PureBlack
                                )
                                Text(
                                    text = if (item.grandTotalRub > 0) {
                                        Formatters.formatMoney(item.grandTotalRub)
                                    } else {
                                        "Пол: ${Formatters.formatArea(item.totalFloorAreaSqm)}"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = DeepGreen
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }

    // Triple confirmation dialog with swapped Yes / No positions on every step
    pendingDelete?.let { target ->
        val titleText = when (deleteConfirmStep) {
            1 -> "Удалить объект? (Шаг 1 из 3)"
            2 -> "Точно удалить? (Шаг 2 из 3)"
            else -> "Последняя проверка! (Шаг 3 из 3)"
        }
        val bodyText = when (deleteConfirmStep) {
            1 -> "Вы хотите удалить «${target.project.name}»?"
            2 -> "Все замеры и сметы по объекту «${target.project.name}» исчезнут. Кнопки поменялись местами для защиты!"
            else -> "Удалить «${target.project.name}» навсегда без возможности вернуть?"
        }

        val noButton: @Composable (Modifier) -> Unit = { mod ->
            OutlinedButton(
                onClick = { pendingDelete = null },
                modifier = mod.heightIn(min = 54.dp),
                border = BorderStroke(2.dp, DeepGreen),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PureWhite,
                    contentColor = DeepGreen
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Нет", style = MaterialTheme.typography.labelLarge)
            }
        }

        val yesButton: @Composable (Modifier) -> Unit = { mod ->
            OutlinedButton(
                onClick = {
                    if (deleteConfirmStep < 3) {
                        deleteConfirmStep++
                    } else {
                        onDeleteProjectClick(target.project.id)
                        pendingDelete = null
                    }
                },
                modifier = mod.heightIn(min = 54.dp),
                border = BorderStroke(2.dp, if (deleteConfirmStep == 3) WarningRed else DeepGreen),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (deleteConfirmStep == 3) WarningRed else DeepGreen,
                    contentColor = PureWhite
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (deleteConfirmStep == 3) "Да, удалить" else "Да",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = PureWhite,
            title = {
                Text(text = titleText, style = MaterialTheme.typography.titleLarge, color = PureBlack)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = bodyText, style = MaterialTheme.typography.bodyMedium, color = PureBlack)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Swap Yes/No positions on step 2 vs step 1 & 3
                        if (deleteConfirmStep == 2) {
                            yesButton(Modifier.weight(1f))
                            noButton(Modifier.weight(1f))
                        } else {
                            noButton(Modifier.weight(1f))
                            yesButton(Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}
