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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ProjectAggregate
import com.example.domain.Formatters
import com.example.domain.MeasurementCategory
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun ProjectDashboardScreen(
    aggregate: ProjectAggregate?,
    onBackClick: () -> Unit,
    onOpenCategory: (MeasurementCategory) -> Unit,
    onOpenMinorWorks: () -> Unit,
    onOpenEstimate: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenMaterials: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenMoneyHistory: () -> Unit,
    onEditDrawing: () -> Unit
) {
    if (aggregate == null) {
        Scaffold(
            containerColor = PureWhite,
            topBar = { ZamerTopBar(title = "Загрузка...", onBackClick = onBackClick) }
        ) { inner ->
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("Загрузка объекта...", style = MaterialTheme.typography.titleLarge)
            }
        }
        return
    }

    val p = aggregate.project
    val t = aggregate.totals
    val materialsTotal = aggregate.materialExpenses.sumOf { it.priceRub }
    val moneyTotal = aggregate.moneyEntries.sumOf { it.amountRub }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = p.name,
                subtitle = "Комнат: ${aggregate.rooms.size}",
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
                    DashboardBigButton(
                        title = "Площадь стен",
                        valueText = Formatters.formatArea(t.totalWallAreaSqm),
                        icon = Icons.Filled.ViewQuilt,
                        isPrimary = true,
                        onClick = { onOpenCategory(MeasurementCategory.WALLS) },
                        testTag = "btn_wall_area"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "Площадь пола",
                        valueText = Formatters.formatArea(t.totalInnerFloorAreaSqm),
                        secondaryText = "со стенами: ${Formatters.formatArea(t.totalOuterFloorAreaSqm)}",
                        icon = Icons.Filled.Layers,
                        isPrimary = true,
                        onClick = { onOpenCategory(MeasurementCategory.FLOOR) },
                        testTag = "btn_floor_area"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "Смета",
                        valueText = Formatters.formatMoney(t.grandTotalRub),
                        icon = Icons.Filled.ReceiptLong,
                        isPrimary = false,
                        onClick = onOpenEstimate,
                        testTag = "btn_estimate"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "Калькулятор",
                        valueText = "Для расчётов и замеров",
                        icon = Icons.Filled.Calculate,
                        isPrimary = false,
                        onClick = onOpenCalculator,
                        testTag = "btn_calculator"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "Материальные затраты",
                        valueText = if (aggregate.materialExpenses.isEmpty()) {
                            "Записей пока нет"
                        } else {
                            "${aggregate.materialExpenses.size} поз. · ${Formatters.formatMoney(materialsTotal)}"
                        },
                        icon = Icons.Filled.ShoppingCart,
                        isPrimary = false,
                        onClick = onOpenMaterials,
                        testTag = "btn_materials"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "Заметки",
                        valueText = if (p.notesText.isBlank()) {
                            "Пусто · текст и голос"
                        } else {
                            p.notesText.replace('\n', ' ').take(48)
                        },
                        icon = Icons.Filled.EditNote,
                        isPrimary = false,
                        onClick = onOpenNotes,
                        testTag = "btn_notes"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "История денег",
                        valueText = if (aggregate.moneyEntries.isEmpty()) {
                            "Получено: 0 ₽"
                        } else {
                            "Получено: ${Formatters.formatMoney(moneyTotal)} (${aggregate.moneyEntries.size})"
                        },
                        icon = Icons.Filled.Payments,
                        isPrimary = false,
                        onClick = onOpenMoneyHistory,
                        testTag = "btn_money_history"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "Изменить чертёж",
                        valueText = "Собрать или поправить планировку",
                        icon = Icons.Filled.DashboardCustomize,
                        isPrimary = false,
                        onClick = onEditDrawing,
                        testTag = "btn_edit_drawing"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "Мелкая работа",
                        valueText = "${aggregate.minorWorks.size} поз. · ${Formatters.formatMoney(t.minorWorksTotalRub)}",
                        icon = Icons.Filled.Build,
                        isPrimary = false,
                        onClick = onOpenMinorWorks,
                        testTag = "btn_minor_works"
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun DashboardBigButton(
    title: String,
    valueText: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit,
    testTag: String,
    secondaryText: String? = null
) {
    val bg = if (isPrimary) DeepGreen else PureWhite
    val fg = if (isPrimary) PureWhite else PureBlack
    val badgeBg = if (isPrimary) PureWhite else DeepGreenSoft
    val badgeFg = DeepGreen

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(2.dp, if (isPrimary) DeepGreen else PureBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(badgeBg, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = badgeFg,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = fg
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isPrimary) PureWhite else DeepGreen
                    )
                    if (!secondaryText.isNullOrBlank()) {
                        Text(
                            text = secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPrimary) PureWhite.copy(alpha = 0.78f) else DeepGreen.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}
