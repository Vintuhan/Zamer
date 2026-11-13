package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.MutedGray
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarningRed

@Composable
fun MaterialsScreen(
    aggregate: ProjectAggregate?,
    onBackClick: () -> Unit,
    onAddMaterial: (Long, String, Double, String) -> Unit,
    onDeleteMaterial: (Long) -> Unit
) {
    if (aggregate == null) return

    val p = aggregate.project
    val expenses = aggregate.materialExpenses
    val totalMaterialsRub = expenses.sumOf { it.priceRub }

    var itemName by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(Formatters.todayDate()) }

    val parsedPrice = Formatters.parseDecimalInput(priceText) ?: 0.0
    val canAdd = itemName.isNotBlank() && parsedPrice > 0.0 && purchaseDate.isNotBlank()

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = "Материальные затраты",
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
                        Text("Итого на материалы", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatMoney(totalMaterialsRub),
                            style = MaterialTheme.typography.displayLarge,
                            color = PureWhite,
                            modifier = Modifier.testTag("materials_total_text")
                        )
                        Text(
                            text = "Позиций: ${expenses.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite.copy(alpha = 0.85f)
                        )
                    }
                }

                item {
                    ZamerCard(
                        modifier = Modifier.widthIn(max = 600.dp),
                        borderColor = PureBlack
                    ) {
                        Text("Добавить покупку", style = MaterialTheme.typography.titleLarge, color = PureBlack)
                        Spacer(Modifier.height(10.dp))
                        ZamerInputField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = "Наименование товара",
                            placeholder = "Например: Шпаклёвка 25 кг",
                            testTag = "material_name_input"
                        )
                        Spacer(Modifier.height(8.dp))
                        ZamerInputField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = "Цена (₽)",
                            placeholder = "0",
                            keyboardType = KeyboardType.Decimal,
                            isGiantNumeric = true,
                            testTag = "material_price_input"
                        )
                        Spacer(Modifier.height(8.dp))
                        ZamerInputField(
                            value = purchaseDate,
                            onValueChange = { purchaseDate = it },
                            label = "Дата покупки (обязательно)",
                            placeholder = "дд.мм.гггг",
                            keyboardType = KeyboardType.Number,
                            testTag = "material_date_input"
                        )
                        Spacer(Modifier.height(12.dp))
                        ZamerPrimaryButton(
                            text = "Добавить",
                            icon = Icons.Filled.Add,
                            enabled = canAdd,
                            onClick = {
                                if (canAdd) {
                                    onAddMaterial(p.id, itemName.trim(), parsedPrice, purchaseDate.trim())
                                    itemName = ""
                                    priceText = ""
                                }
                            },
                            testTag = "material_add_button"
                        )
                    }
                }

                if (expenses.isEmpty()) {
                    item {
                        Text(
                            text = "Пока нет записей о материалах",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MutedGray
                        )
                    }
                } else {
                    items(expenses, key = { it.id }) { item ->
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
                                    Text(item.itemName, style = MaterialTheme.typography.titleLarge, color = PureBlack)
                                    Text(
                                        text = "${item.purchaseDate} · ${Formatters.formatMoney(item.priceRub)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeepGreen
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteMaterial(item.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Удалить запись", tint = WarningRed)
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
