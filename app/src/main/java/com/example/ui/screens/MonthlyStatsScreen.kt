package com.example.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MonthlyBucketStats
import com.example.domain.Formatters
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ZamerCard
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.MutedGray
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun MonthlyStatsScreen(
    stats: List<MonthlyBucketStats>,
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = "Статистика по месяцам",
                subtitle = "Доход в ₽ и выполненные м²",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        AnimatedScreenEntrance(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (stats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Данных за месяцы пока нет",
                        style = MaterialTheme.typography.titleLarge,
                        color = MutedGray
                    )
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
                    items(stats, key = { it.monthKey }) { bucket ->
                        ZamerCard(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .testTag("stats_month_${bucket.monthKey}"),
                            borderColor = DeepGreen
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bucket.monthTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = PureBlack
                                )
                                Text(
                                    text = "Объектов: ${bucket.objectsCount}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = DeepGreen
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Заработано: ${Formatters.formatMoney(bucket.earnedTotalRub)}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = DeepGreen
                            )
                            Text(
                                text = "Наличка: ${Formatters.formatMoney(bucket.cashRub)} · Карта: ${Formatters.formatMoney(bucket.cardRub)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureBlack
                            )
                            if (bucket.materialsTotalRub > 0.0) {
                                Text(
                                    text = "Материалы: ${Formatters.formatMoney(bucket.materialsTotalRub)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedGray
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Замерено стен: ${Formatters.formatArea(bucket.measuredWallAreaSqm)} · Пол: ${Formatters.formatArea(bucket.measuredFloorAreaSqm)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = PureBlack
                            )
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}
