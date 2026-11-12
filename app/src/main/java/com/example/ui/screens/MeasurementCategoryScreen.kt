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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ProjectAggregate
import com.example.data.ProjectEntity
import com.example.data.RoomDetail
import com.example.data.RoomEntity
import com.example.domain.Formatters
import com.example.domain.GeometryAiAdvisor
import com.example.domain.MeasurementCategory
import com.example.domain.OpeningItem
import com.example.domain.OpeningType
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ProportionalRoomCanvas
import com.example.ui.components.ZamerCard
import com.example.ui.components.ZamerInputField
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerSecondaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

@Composable
fun MeasurementCategoryScreen(
    category: MeasurementCategory,
    aggregate: ProjectAggregate?,
    onBackClick: () -> Unit,
    onStartWizard: (roomId: Long?) -> Unit,
    onSaveWallThickness: (ProjectEntity, Double) -> Unit,
    onSaveFloorOverride: (RoomEntity, Double?) -> Unit,
    onSaveCorners: (RoomEntity, Int, Int, Double) -> Unit,
    onSaveOpenings: (RoomEntity, List<OpeningItem>) -> Unit,
    onAddRoom: (Long, String, Int) -> Unit
) {
    if (aggregate == null) return

    val title = when (category) {
        MeasurementCategory.WALLS -> "Площадь стен"
        MeasurementCategory.FLOOR -> "Площадь пола"
        MeasurementCategory.CORNERS -> "Длина уголков"
    }

    var showWallThicknessDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var viewingRoomResultId by remember { mutableStateOf<Long?>(null) }
    var editingFloorRoom by remember { mutableStateOf<RoomDetail?>(null) }
    var editingCornersRoom by remember { mutableStateOf<RoomDetail?>(null) }

    val t = aggregate.totals
    val viewingRoom = viewingRoomResultId?.let { id -> aggregate.rooms.find { it.entity.id == id } }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = title,
                subtitle = aggregate.project.name,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Surface(color = PureWhite, border = BorderStroke(1.dp, PureBlack)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZamerPrimaryButton(
                        text = "Начать замер",
                        icon = Icons.Filled.PlayArrow,
                        onClick = { onStartWizard(null) },
                        modifier = Modifier.widthIn(max = 600.dp),
                        testTag = "start_measurement_button"
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Grand apartment total card
                item {
                    ZamerCard(
                        modifier = Modifier.widthIn(max = 600.dp),
                        borderColor = DeepGreen,
                        containerColor = DeepGreen
                    ) {
                        Text(
                            text = "Итого по квартире",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite
                        )
                        Spacer(Modifier.height(6.dp))
                        when (category) {
                            MeasurementCategory.WALLS -> {
                                Text(
                                    text = Formatters.formatArea(t.totalWallAreaSqm),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = PureWhite
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Без вычета проёмов: ${Formatters.formatArea(t.totalGrossWallAreaSqm)} · Вычет: ${Formatters.formatArea(t.totalOpeningsAreaSqm)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite
                                )
                            }
                            MeasurementCategory.FLOOR -> {
                                Text(
                                    text = Formatters.formatArea(t.totalInnerFloorAreaSqm),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = PureWhite
                                )
                                Text(
                                    text = "По внутренним размерам (без стен)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Со стенами (${Formatters.formatMeters(aggregate.project.wallThicknessMeters)}): ${Formatters.formatArea(t.totalOuterFloorAreaSqm)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = PureWhite
                                )
                            }
                            MeasurementCategory.CORNERS -> {
                                Text(
                                    text = Formatters.formatRunningMeters(t.totalCornersMeters),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = PureWhite
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Внутренние: ${Formatters.formatRunningMeters(t.totalInnerCornersMeters)}\nВнешние: ${Formatters.formatRunningMeters(t.totalOuterCornersMeters)}\nДоп. вручную: ${Formatters.formatRunningMeters(t.totalExtraCornersMeters)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }

                if (category == MeasurementCategory.FLOOR) {
                    item {
                        ZamerSecondaryButton(
                            text = "Толщина стен: ${Formatters.formatMeters(aggregate.project.wallThicknessMeters)} (изменить)",
                            icon = Icons.Filled.Edit,
                            onClick = { showWallThicknessDialog = true },
                            modifier = Modifier.widthIn(max = 600.dp)
                        )
                    }
                }

                // Rooms breakdown — tapping room opens Results view, tapping Pencil edits!
                items(aggregate.rooms, key = { it.entity.id }) { room ->
                    val c = room.calculations
                    val ceilingLabel = if (room.entity.hasStretchCeiling) {
                        "После натяжного потолка (${Formatters.formatMeters(c.heightMeters)})"
                    } else {
                        "До натяжных потолков (${Formatters.formatMeters(c.heightMeters)})"
                    }
                    ZamerCard(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .testTag("room_result_card_${room.entity.id}")
                            .clickable { viewingRoomResultId = room.entity.id },
                        borderColor = PureBlack
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = room.entity.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PureBlack
                                )
                                Text(
                                    text = ceilingLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepGreen
                                )
                                val measuredCount = room.walls.count { it.lengthMeters > 0.0 }
                                Text(
                                    text = "Стен замерено: $measuredCount из ${room.walls.size} · Периметр: ${Formatters.formatMeters(c.perimeterMeters)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureBlack
                                )
                            }
                            // Pencil button specifically handles editing/re-measuring
                            OutlinedButton(
                                onClick = { onStartWizard(room.entity.id) },
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("edit_room_pencil_${room.entity.id}"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(2.dp, DeepGreen),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Изменить замеры комнаты",
                                    tint = DeepGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepGreenSoft, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            when (category) {
                                MeasurementCategory.WALLS -> {
                                    Column {
                                        Text(
                                            text = "Стены: ${Formatters.formatArea(c.netWallAreaSqm)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = PureBlack
                                        )
                                        Text(
                                            text = "Всего ${Formatters.formatArea(c.grossWallAreaSqm)} − проёмы ${Formatters.formatArea(c.openingsAreaSqm)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = PureBlack
                                        )
                                    }
                                }
                                MeasurementCategory.FLOOR -> {
                                    Column {
                                        Text(
                                            text = "Без стен: ${Formatters.formatArea(c.innerFloorAreaSqm)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = PureBlack
                                        )
                                        Text(
                                            text = "Со стенами: ${Formatters.formatArea(c.outerFloorAreaSqm)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = DeepGreen
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = { editingFloorRoom = room },
                                            border = BorderStroke(1.5.dp, PureBlack),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Ввести площадь пола вручную", color = PureBlack)
                                        }
                                    }
                                }
                                MeasurementCategory.CORNERS -> {
                                    Column {
                                        Text(
                                            text = Formatters.formatRunningMeters(c.totalCornersMeters),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = PureBlack
                                        )
                                        Text(
                                            text = "Внутр: ${room.entity.innerCorners} · Внеш: ${room.entity.outerCorners} · Доп: ${Formatters.formatRunningMeters(c.extraCornersMeters)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = PureBlack
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = { editingCornersRoom = room },
                                            border = BorderStroke(1.5.dp, PureBlack),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Изменить углы и доп. м.п.", color = PureBlack)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    ZamerSecondaryButton(
                        text = "Добавить комнату",
                        icon = Icons.Filled.Add,
                        borderColor = DeepGreen,
                        textColor = DeepGreen,
                        onClick = { showAddRoomDialog = true },
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // Room results view dialog (when tapping the room card)
    viewingRoom?.let { room ->
        val c = room.calculations
        val aiCheck = GeometryAiAdvisor.validateRoomWalls(room.walls)
        val hasWindows = room.openings.any { it.type == OpeningType.WINDOW && it.count > 0 }
        AlertDialog(
            onDismissRequest = { viewingRoomResultId = null },
            containerColor = PureWhite,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Результаты: ${room.entity.name}",
                        style = MaterialTheme.typography.titleLarge,
                        color = PureBlack,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            viewingRoomResultId = null
                            onStartWizard(room.entity.id)
                        }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать", tint = DeepGreen)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProportionalRoomCanvas(
                        walls = room.walls,
                        activeWallIndex = -1,
                        hasWarning = !aiCheck.isValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(DeepGreenSoft, RoundedCornerShape(12.dp))
                    )
                    Text(
                        text = if (room.entity.hasStretchCeiling) {
                            "Потолок: после натяжного (${Formatters.formatMeters(c.heightMeters)})"
                        } else {
                            "Потолок: до натяжных (${Formatters.formatMeters(c.heightMeters)})"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = DeepGreen
                    )
                    Text(
                        text = "Стены (чистая): ${Formatters.formatArea(c.netWallAreaSqm)}\nСтены (без вычетов): ${Formatters.formatArea(c.grossWallAreaSqm)}\nПроёмы: −${Formatters.formatArea(c.openingsAreaSqm)}\nПол (без стен): ${Formatters.formatArea(c.innerFloorAreaSqm)}\nПол (со стенами): ${Formatters.formatArea(c.outerFloorAreaSqm)}\nУголки: ${Formatters.formatRunningMeters(c.totalCornersMeters)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureBlack
                    )
                    if (hasWindows) {
                        ZamerSecondaryButton(
                            text = "Выкинуть окна (не вычитать)",
                            onClick = {
                                val withoutWindows = room.openings.filter { it.type != OpeningType.WINDOW }
                                onSaveOpenings(room.entity, withoutWindows)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        viewingRoomResultId = null
                        onStartWizard(room.entity.id)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepGreen, contentColor = PureWhite)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Изменить")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewingRoomResultId = null }) {
                    Text("Закрыть", color = PureBlack)
                }
            }
        )
    }

    if (showWallThicknessDialog) {
        var input by remember { mutableStateOf(Formatters.formatCleanNumber(aggregate.project.wallThicknessMeters)) }
        AlertDialog(
            onDismissRequest = { showWallThicknessDialog = false },
            containerColor = PureWhite,
            title = { Text("Толщина стен (в метрах)", style = MaterialTheme.typography.titleLarge, color = PureBlack) },
            text = {
                ZamerInputField(
                    value = input,
                    onValueChange = { input = it },
                    label = "Например: 0.15 или 0.30",
                    keyboardType = KeyboardType.Decimal,
                    isGiantNumeric = true
                )
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        val v = Formatters.parseDecimalInput(input) ?: 0.15
                        onSaveWallThickness(aggregate.project, v)
                        showWallThicknessDialog = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepGreen, contentColor = PureWhite)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWallThicknessDialog = false }) {
                    Text("Отмена", color = PureBlack)
                }
            }
        )
    }

    if (showAddRoomDialog) {
        var rName by remember { mutableStateOf("") }
        var rWalls by remember { mutableStateOf("4") }
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            containerColor = PureWhite,
            title = { Text("Новая комната", style = MaterialTheme.typography.titleLarge, color = PureBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZamerInputField(
                        value = rName,
                        onValueChange = { rName = it },
                        label = "Название",
                        placeholder = "Например: Лоджия"
                    )
                    ZamerInputField(
                        value = rWalls,
                        onValueChange = { rWalls = it },
                        label = "Количество стен",
                        keyboardType = KeyboardType.Number
                    )
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        onAddRoom(
                            aggregate.project.id,
                            rName.ifBlank { "Комната" },
                            (rWalls.toIntOrNull() ?: 4).coerceIn(1, 12)
                        )
                        showAddRoomDialog = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepGreen, contentColor = PureWhite)
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRoomDialog = false }) {
                    Text("Отмена", color = PureBlack)
                }
            }
        )
    }

    editingFloorRoom?.let { target ->
        var manualArea by remember {
            mutableStateOf(
                target.entity.floorAreaOverrideMeters?.let { Formatters.formatCleanNumber(it) } ?: ""
            )
        }
        AlertDialog(
            onDismissRequest = { editingFloorRoom = null },
            containerColor = PureWhite,
            title = { Text("${target.entity.name}: площадь пола", style = MaterialTheme.typography.titleLarge, color = PureBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Оставьте пустым, чтобы считать автоматически по длинам стен.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureBlack
                    )
                    ZamerInputField(
                        value = manualArea,
                        onValueChange = { manualArea = it },
                        label = "Площадь (м²)",
                        keyboardType = KeyboardType.Decimal,
                        isGiantNumeric = true
                    )
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        val parsed = if (manualArea.isBlank()) null else Formatters.parseDecimalInput(manualArea)
                        onSaveFloorOverride(target.entity, parsed)
                        editingFloorRoom = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepGreen, contentColor = PureWhite)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingFloorRoom = null }) {
                    Text("Отмена", color = PureBlack)
                }
            }
        )
    }

    editingCornersRoom?.let { target ->
        var inner by remember { mutableStateOf(target.entity.innerCorners.toString()) }
        var outer by remember { mutableStateOf(target.entity.outerCorners.toString()) }
        var extra by remember { mutableStateOf(Formatters.formatCleanNumber(target.entity.extraCornerMeters)) }
        AlertDialog(
            onDismissRequest = { editingCornersRoom = null },
            containerColor = PureWhite,
            title = { Text("${target.entity.name}: уголки", style = MaterialTheme.typography.titleLarge, color = PureBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZamerInputField(
                        value = inner,
                        onValueChange = { inner = it },
                        label = "Внутренние углы (шт)",
                        keyboardType = KeyboardType.Number
                    )
                    ZamerInputField(
                        value = outer,
                        onValueChange = { outer = it },
                        label = "Внешние углы (шт)",
                        keyboardType = KeyboardType.Number
                    )
                    ZamerInputField(
                        value = extra,
                        onValueChange = { extra = it },
                        label = "Доп. погонные метры (м.п.)",
                        keyboardType = KeyboardType.Decimal
                    )
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        onSaveCorners(
                            target.entity,
                            inner.toIntOrNull() ?: 0,
                            outer.toIntOrNull() ?: 0,
                            Formatters.parseDecimalInput(extra) ?: 0.0
                        )
                        editingCornersRoom = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepGreen, contentColor = PureWhite)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCornersRoom = null }) {
                    Text("Отмена", color = PureBlack)
                }
            }
        )
    }
}
