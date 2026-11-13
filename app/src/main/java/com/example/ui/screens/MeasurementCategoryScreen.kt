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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ProjectAggregate
import com.example.data.ProjectEntity
import com.example.data.RoomDetail
import com.example.data.RoomEntity
import com.example.domain.Formatters
import com.example.domain.MeasurementCategory
import com.example.domain.OpeningItem
import com.example.domain.OpeningType
import com.example.domain.SimpleRoomShape
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.RenameRoomDialog
import com.example.ui.components.ZamerCard
import com.example.ui.components.ZamerInputField
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerSecondaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.BrightDoorGreen
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.MutedGray
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarningRed
import java.util.UUID
import kotlin.math.round

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MeasurementCategoryScreen(
    category: MeasurementCategory,
    aggregate: ProjectAggregate?,
    onBackClick: () -> Unit,
    onStartWizard: (roomId: Long?) -> Unit,
    onSaveWallThickness: (ProjectEntity, Double) -> Unit,
    onSaveFloorOverride: (RoomEntity, Double?) -> Unit,
    onAddRoom: (Long, String, Int, String) -> Unit,
    onRenameRoom: (RoomEntity, String) -> Unit,
    onSaveProjectWallHeight: (Long, Double) -> Unit,
    onSaveProjectOpenings: (Long, List<OpeningItem>) -> Unit
) {
    if (aggregate == null) return

    val p = aggregate.project
    val t = aggregate.totals

    val title = when (category) {
        MeasurementCategory.WALLS -> "Площадь стен"
        MeasurementCategory.FLOOR -> "Площадь пола"
        MeasurementCategory.CORNERS -> "Площадь стен"
    }

    var showOpeningsDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var editingFloorRoom by remember { mutableStateOf<RoomDetail?>(null) }
    var renamingRoom by remember { mutableStateOf<RoomEntity?>(null) }

    var heightInput by remember(p.id) {
        mutableStateOf(Formatters.formatCleanNumber(p.wallHeightMeters))
    }
    LaunchedEffect(p.wallHeightMeters) {
        val currentParsed = Formatters.parseDecimalInput(heightInput)
        if (currentParsed == null || kotlin.math.abs(currentParsed - p.wallHeightMeters) > 0.005) {
            if (heightInput.isNotEmpty()) {
                heightInput = Formatters.formatCleanNumber(p.wallHeightMeters)
            }
        }
    }

    var thicknessSlider by remember(p.id, p.wallThicknessMeters) {
        mutableFloatStateOf(p.wallThicknessMeters.toFloat().coerceIn(0.05f, 0.50f))
    }
    var thicknessInput by remember(p.id, p.wallThicknessMeters) {
        mutableStateOf(Formatters.formatCleanNumber(p.wallThicknessMeters))
    }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = title,
                subtitle = p.name,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            if (category == MeasurementCategory.WALLS) {
                Surface(color = PureWhite, border = BorderStroke(1.dp, PureBlack)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                // 1. Top Total Card
                item {
                    if (category == MeasurementCategory.WALLS) {
                        val allDone = t.allWallsMeasured
                        val cardBg = if (allDone) BrightDoorGreen else Color(0xFFE5E7EB)
                        val cardBorder = if (allDone) BrightDoorGreen else MutedGray
                        val cardTextColor = if (allDone) PureWhite else MutedGray
                        ZamerCard(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .testTag("walls_grand_total_card"),
                            borderColor = cardBorder,
                            containerColor = cardBg
                        ) {
                            Text(
                                text = if (allDone) "ИТОГО (всё замерено)" else "ИТОГО (не все стены замерены)",
                                style = MaterialTheme.typography.titleMedium,
                                color = cardTextColor
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatArea(t.totalWallAreaSqm),
                                style = MaterialTheme.typography.displayLarge,
                                color = cardTextColor,
                                modifier = Modifier.testTag("walls_grand_total_value")
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Стены: ${Formatters.formatArea(t.totalGrossWallAreaSqm)} · Проёмы: −${Formatters.formatArea(t.totalOpeningsAreaSqm)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cardTextColor
                            )
                        }
                    } else {
                        ZamerCard(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .testTag("floor_grand_total_card"),
                            borderColor = DeepGreen,
                            containerColor = DeepGreen
                        ) {
                            Text(
                                text = "ИТОГО ПО ПОЛУ",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhite
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatArea(t.totalInnerFloorAreaSqm),
                                style = MaterialTheme.typography.displayLarge,
                                color = PureWhite
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "со стенами: ${Formatters.formatArea(t.totalOuterFloorAreaSqm)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PureWhite.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // 2. For FLOOR mode: Only wall thickness slider/input right here
                if (category == MeasurementCategory.FLOOR) {
                    item {
                        ZamerCard(
                            modifier = Modifier.widthIn(max = 600.dp),
                            borderColor = PureBlack
                        ) {
                            Text(
                                text = "Толщина стен: ${Formatters.formatMeters(thicknessSlider.toDouble())}",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureBlack
                            )
                            Slider(
                                value = thicknessSlider,
                                onValueChange = { raw ->
                                    val snapped = (round(raw * 100f) / 100f).coerceIn(0.05f, 0.50f)
                                    thicknessSlider = snapped
                                    thicknessInput = Formatters.formatCleanNumber(snapped.toDouble())
                                    onSaveWallThickness(p, snapped.toDouble())
                                },
                                valueRange = 0.05f..0.50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = DeepGreen,
                                    activeTrackColor = DeepGreen
                                ),
                                modifier = Modifier.testTag("wall_thickness_slider")
                            )
                            ZamerInputField(
                                value = thicknessInput,
                                onValueChange = { txt ->
                                    thicknessInput = txt
                                    val parsed = Formatters.parseDecimalInput(txt)
                                    if (parsed != null && parsed >= 0.0) {
                                        thicknessSlider = parsed.toFloat().coerceIn(0.05f, 0.50f)
                                        onSaveWallThickness(p, parsed)
                                    }
                                },
                                label = "Или введите точно (м)",
                                placeholder = "0.15",
                                keyboardType = KeyboardType.Decimal,
                                testTag = "wall_thickness_input"
                            )
                        }
                    }
                }

                // 3. Minimalist Room List
                items(aggregate.rooms, key = { it.entity.id }) { room ->
                    val c = room.calculations
                    val isRoomMeasured = room.walls.isNotEmpty() && room.walls.all { it.lengthMeters > 0.0 }

                    ZamerCard(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .testTag("room_result_card_${room.entity.id}")
                            .combinedClickable(
                                onClick = {
                                    if (category == MeasurementCategory.FLOOR) {
                                        editingFloorRoom = room
                                    } else {
                                        onStartWizard(room.entity.id)
                                    }
                                },
                                onDoubleClick = {
                                    renamingRoom = room.entity
                                }
                            ),
                        borderColor = if (category == MeasurementCategory.WALLS && isRoomMeasured) BrightDoorGreen else PureBlack
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
                                Spacer(Modifier.height(4.dp))
                                if (category == MeasurementCategory.FLOOR) {
                                    Text(
                                        text = "Пол: ${Formatters.formatArea(c.innerFloorAreaSqm)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeepGreen
                                    )
                                    Text(
                                        text = "со стенами: ${Formatters.formatArea(c.outerFloorAreaSqm)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedGray
                                    )
                                } else {
                                    Text(
                                        text = "Стены: ${Formatters.formatArea(c.netWallAreaSqm)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeepGreen
                                    )
                                }
                            }
                            if (category == MeasurementCategory.WALLS) {
                                Icon(
                                    imageVector = if (isRoomMeasured) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = if (isRoomMeasured) "Замерено" else "Не замерено",
                                    tint = if (isRoomMeasured) BrightDoorGreen else WarningRed,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("room_measured_icon_${room.entity.id}")
                                )
                            }
                        }
                    }
                }

                // 4. For WALLS mode: Whole-apartment Openings Deduction + Ceiling Height + Add Room
                if (category == MeasurementCategory.WALLS) {
                    item {
                        ZamerSecondaryButton(
                            text = if (aggregate.projectOpenings.isEmpty()) {
                                "Вычет проёмов (окна, двери)"
                            } else {
                                "Вычет проёмов: ${aggregate.projectOpenings.size} шт · −${Formatters.formatArea(t.totalOpeningsAreaSqm)}"
                            },
                            icon = Icons.Filled.MeetingRoom,
                            borderColor = PureBlack,
                            textColor = PureBlack,
                            onClick = { showOpeningsDialog = true },
                            modifier = Modifier.widthIn(max = 600.dp),
                            testTag = "project_openings_button"
                        )
                    }

                    item {
                        ZamerCard(
                            modifier = Modifier.widthIn(max = 600.dp),
                            borderColor = DeepGreen
                        ) {
                            ZamerInputField(
                                value = heightInput,
                                onValueChange = { txt ->
                                    heightInput = txt
                                    val v = Formatters.parseDecimalInput(txt)
                                    if (v != null && v > 0.0) {
                                        onSaveProjectWallHeight(p.id, v)
                                    }
                                },
                                label = "Высота потолков (м)",
                                placeholder = "2.50",
                                keyboardType = KeyboardType.Decimal,
                                isGiantNumeric = true,
                                testTag = "project_wall_height_input"
                            )
                        }
                    }

                    item {
                        ZamerSecondaryButton(
                            text = "Добавить комнату",
                            icon = Icons.Filled.Add,
                            borderColor = DeepGreen,
                            textColor = DeepGreen,
                            onClick = { showAddRoomDialog = true },
                            modifier = Modifier.widthIn(max = 600.dp),
                            testTag = "add_room_button"
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showOpeningsDialog) {
        ProjectOpeningsDialog(
            initialOpenings = aggregate.projectOpenings,
            onDismiss = { showOpeningsDialog = false },
            onSave = { list ->
                onSaveProjectOpenings(p.id, list)
                showOpeningsDialog = false
            }
        )
    }

    if (showAddRoomDialog) {
        var rName by remember { mutableStateOf("") }
        var selectedWallCount by remember { mutableIntStateOf(4) }
        var selectedShapeKey by remember { mutableStateOf(SimpleRoomShape.RECT.shapeKey) }
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            containerColor = PureWhite,
            title = { Text("Новая комната / ригель", style = MaterialTheme.typography.titleLarge, color = PureBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZamerInputField(
                        value = rName,
                        onValueChange = { rName = it },
                        label = "Название",
                        placeholder = "Например: Лоджия"
                    )
                    Text("Форма:", style = MaterialTheme.typography.bodyLarge, color = PureBlack)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple(4, SimpleRoomShape.RECT.shapeKey, "4 стены"),
                            Triple(6, SimpleRoomShape.L_SHAPE.shapeKey, "6"),
                            Triple(8, SimpleRoomShape.U_SHAPE.shapeKey, "8"),
                            Triple(1, "WALL_LINE", "Ригель")
                        ).forEach { (cnt, key, lbl) ->
                            val isSel = selectedWallCount == cnt
                            OutlinedButton(
                                onClick = {
                                    selectedWallCount = cnt
                                    selectedShapeKey = key
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(2.dp, if (isSel) DeepGreen else PureBlack),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSel) DeepGreen else PureWhite,
                                    contentColor = if (isSel) PureWhite else PureBlack
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(lbl, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        val fallback = if (selectedWallCount == 1) "Ригель" else "Комната"
                        onAddRoom(p.id, rName.ifBlank { fallback }, selectedWallCount, selectedShapeKey)
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
                        "Оставьте пустым, чтобы считать автоматически по стенам.",
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

    renamingRoom?.let { target ->
        RenameRoomDialog(
            currentName = target.name,
            onDismiss = { renamingRoom = null },
            onConfirm = { newName ->
                onRenameRoom(target, newName)
                renamingRoom = null
            }
        )
    }
}

@Composable
private fun ProjectOpeningsDialog(
    initialOpenings: List<OpeningItem>,
    onDismiss: () -> Unit,
    onSave: (List<OpeningItem>) -> Unit
) {
    val openings = remember {
        mutableStateListOf<OpeningItem>().apply { addAll(initialOpenings) }
    }
    var selectedPreset by remember { mutableStateOf("Окно") }
    var customLabel by remember { mutableStateOf("Окно") }
    var directSqmMode by remember { mutableStateOf(false) }
    var sliderWidth by remember { mutableFloatStateOf(1.40f) }
    var sliderHeight by remember { mutableFloatStateOf(1.40f) }
    var directAreaInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        title = {
            Text(
                text = "Вычет проёмов (вся квартира)",
                style = MaterialTheme.typography.titleLarge,
                color = PureBlack
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val presets = listOf("Окно", "Дверь", "Балконная", "Своё")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        val isSel = selectedPreset == preset
                        OutlinedButton(
                            onClick = {
                                selectedPreset = preset
                                when (preset) {
                                    "Окно" -> {
                                        customLabel = "Окно"
                                        sliderWidth = 1.40f
                                        sliderHeight = 1.40f
                                    }
                                    "Дверь" -> {
                                        customLabel = "Дверь"
                                        sliderWidth = 0.80f
                                        sliderHeight = 2.00f
                                    }
                                    "Балконная" -> {
                                        customLabel = "Балконная дверь"
                                        sliderWidth = 0.70f
                                        sliderHeight = 2.10f
                                    }
                                    else -> {
                                        customLabel = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, if (isSel) DeepGreen else PureBlack),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSel) DeepGreen else PureWhite,
                                contentColor = if (isSel) PureWhite else PureBlack
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(preset, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }

                ZamerInputField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = "Подпись проёма",
                    placeholder = "Например: Окно зал"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { directSqmMode = false },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, if (!directSqmMode) DeepGreen else PureBlack),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!directSqmMode) DeepGreen else PureWhite,
                            contentColor = if (!directSqmMode) PureWhite else PureBlack
                        )
                    ) {
                        Text("Слайдеры (м)", style = MaterialTheme.typography.bodyMedium)
                    }
                    OutlinedButton(
                        onClick = { directSqmMode = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, if (directSqmMode) DeepGreen else PureBlack),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (directSqmMode) DeepGreen else PureWhite,
                            contentColor = if (directSqmMode) PureWhite else PureBlack
                        )
                    ) {
                        Text("Сразу м²", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (!directSqmMode) {
                    Text(
                        text = "Ширина: ${Formatters.formatMeters(sliderWidth.toDouble())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureBlack
                    )
                    Slider(
                        value = sliderWidth,
                        onValueChange = { sliderWidth = (round(it * 20f) / 20f).coerceIn(0.3f, 3.5f) },
                        valueRange = 0.3f..3.5f,
                        colors = SliderDefaults.colors(thumbColor = DeepGreen, activeTrackColor = DeepGreen)
                    )
                    Text(
                        text = "Высота: ${Formatters.formatMeters(sliderHeight.toDouble())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureBlack
                    )
                    Slider(
                        value = sliderHeight,
                        onValueChange = { sliderHeight = (round(it * 20f) / 20f).coerceIn(0.3f, 3.0f) },
                        valueRange = 0.3f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = DeepGreen, activeTrackColor = DeepGreen)
                    )
                    Text(
                        text = "Площадь проёма: ${Formatters.formatArea((sliderWidth * sliderHeight).toDouble())}",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepGreen
                    )
                } else {
                    ZamerInputField(
                        value = directAreaInput,
                        onValueChange = { directAreaInput = it },
                        label = "Площадь вычета (м²)",
                        placeholder = "2.1",
                        keyboardType = KeyboardType.Decimal,
                        isGiantNumeric = true
                    )
                }

                OutlinedButton(
                    onClick = {
                        val finalLabel = customLabel.trim().ifBlank { selectedPreset }
                        val isDoor = "двер" in finalLabel.lowercase() || selectedPreset.contains("Двер") || selectedPreset == "Балконная"
                        val type = if (isDoor) OpeningType.DOOR else OpeningType.WINDOW
                        if (directSqmMode) {
                            val sqm = Formatters.parseDecimalInput(directAreaInput) ?: 0.0
                            if (sqm > 0.0) {
                                openings.add(
                                    OpeningItem(
                                        id = UUID.randomUUID().toString(),
                                        type = type,
                                        count = 1,
                                        widthMeters = 0.0,
                                        heightMeters = 0.0,
                                        label = finalLabel,
                                        directAreaSqm = sqm
                                    )
                                )
                                directAreaInput = ""
                            }
                        } else {
                            openings.add(
                                OpeningItem(
                                    id = UUID.randomUUID().toString(),
                                    type = type,
                                    count = 1,
                                    widthMeters = sliderWidth.toDouble(),
                                    heightMeters = sliderHeight.toDouble(),
                                    label = finalLabel,
                                    directAreaSqm = null
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("add_project_opening_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, DeepGreen),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DeepGreenSoft,
                        contentColor = DeepGreen
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Добавить вычет", style = MaterialTheme.typography.labelLarge)
                }

                if (openings.isNotEmpty()) {
                    Text(
                        text = "Список вычетов (${Formatters.formatArea(openings.sumOf { it.totalAreaSqm })}):",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureBlack
                    )
                    openings.forEachIndexed { idx, op ->
                        val titleLbl = op.label.ifBlank {
                            if (op.type == OpeningType.DOOR) "Дверь" else "Окно"
                        }
                        val sub = if (op.directAreaSqm != null) {
                            "Введено вручную"
                        } else {
                            "${Formatters.formatCleanNumber(op.widthMeters)} × ${Formatters.formatCleanNumber(op.heightMeters)} м"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepGreenSoft, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(titleLbl, style = MaterialTheme.typography.bodyLarge, color = PureBlack)
                                Text(
                                    text = "$sub · ${Formatters.formatArea(op.totalAreaSqm)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepGreen
                                )
                            }
                            IconButton(
                                onClick = { openings.removeAt(idx) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Удалить проём", tint = WarningRed)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = { onSave(openings.toList()) },
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepGreen, contentColor = PureWhite),
                modifier = Modifier.testTag("save_project_openings_button")
            ) {
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = PureBlack)
            }
        }
    )
}
