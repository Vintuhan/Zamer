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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Straighten
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
    onOpenHistory: () -> Unit
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

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = p.name,
                subtitle = buildString {
                    if (p.address.isNotBlank()) append("${p.address} · ")
                    append("${p.layoutTitle} · ${aggregate.rooms.size} комн.")
                },
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
                        icon = Icons.Filled.ContentPaste,
                        isPrimary = true,
                        onClick = { onOpenCategory(MeasurementCategory.WALLS) },
                        testTag = "btn_wall_area"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "Площадь пола",
                        valueText = Formatters.formatArea(t.totalInnerFloorAreaSqm),
                        icon = Icons.Filled.GridOn,
                        isPrimary = true,
                        onClick = { onOpenCategory(MeasurementCategory.FLOOR) },
                        testTag = "btn_floor_area"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "Длина уголков",
                        valueText = Formatters.formatRunningMeters(t.totalCornersMeters),
                        icon = Icons.Filled.Straighten,
                        isPrimary = true,
                        onClick = { onOpenCategory(MeasurementCategory.CORNERS) },
                        testTag = "btn_corners_length"
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
                item {
                    DashboardBigButton(
                        title = "Смета (итого)",
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
                        valueText = "Для отделки + автоистория",
                        icon = Icons.Filled.Calculate,
                        isPrimary = false,
                        onClick = onOpenCalculator,
                        testTag = "btn_calculator"
                    )
                }
                item {
                    DashboardBigButton(
                        title = "История",
                        valueText = "Записей: ${aggregate.history.size}",
                        icon = Icons.Filled.History,
                        isPrimary = false,
                        onClick = onOpenHistory,
                        testTag = "btn_history"
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
    testTag: String
) {
    val bg = if (isPrimary) DeepGreen else PureWhite
    val fg = if (isPrimary) PureWhite else PureBlack
    val badgeBg = if (isPrimary) PureWhite else DeepGreenSoft
    val badgeFg = if (isPrimary) DeepGreen else DeepGreen

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
                .padding(horizontal = 18.dp, vertical = 18.dp),
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
                        contentDescription = null,
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
                }
            }
        }
    }
}
