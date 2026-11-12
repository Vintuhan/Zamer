package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ProjectAggregate
import com.example.data.RoomDetail
import com.example.data.RoomEntity
import com.example.domain.Formatters
import com.example.domain.GeometryAiAdvisor
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
import com.example.ui.theme.WarningRed
import java.util.UUID

private enum class WizardStep {
    PICK_ROOM,
    WALL,
    HEIGHT_AND_OPENINGS,
    RESULT
}

@Composable
fun MeasurementWizardScreen(
    aggregate: ProjectAggregate?,
    initialRoomId: Long?,
    onBackExit: () -> Unit,
    onSaveWallLength: (RoomEntity, Int, Double) -> Unit,
    onSaveCeilingAndHeight: (RoomEntity, Double, Boolean, Double?, Double?, Boolean) -> Unit,
    onSaveOpenings: (RoomEntity, List<OpeningItem>) -> Unit
) {
    if (aggregate == null || aggregate.rooms.isEmpty()) {
        Scaffold(
            containerColor = PureWhite,
            topBar = { ZamerTopBar(title = "Замер", onBackClick = onBackExit) }
        ) { inner ->
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("Нет комнат для замера", style = MaterialTheme.typography.titleLarge)
            }
        }
        return
    }

    var selectedRoomId by remember(initialRoomId) { mutableStateOf(initialRoomId) }
    var step by remember(initialRoomId) {
        mutableStateOf(if (initialRoomId != null) WizardStep.WALL else WizardStep.PICK_ROOM)
    }
    var currentWallIdx by remember(selectedRoomId) { mutableIntStateOf(0) }

    val activeRoom: RoomDetail = aggregate.rooms.find { it.entity.id == selectedRoomId }
        ?: aggregate.rooms.first()

    when (step) {
        WizardStep.PICK_ROOM -> {
            Scaffold(
                containerColor = PureWhite,
                topBar = {
                    ZamerTopBar(
                        title = "Выберите комнату",
                        subtitle = "Нажмите на комнату для замера или карандаш для правки",
                        onBackClick = onBackExit
                    )
                }
            ) { inner ->
                AnimatedScreenEntrance(Modifier.fillMaxSize().padding(inner)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(aggregate.rooms, key = { it.entity.id }) { r ->
                            val measured = r.walls.count { it.lengthMeters > 0.0 }
                            val isDone = measured == r.walls.size && r.walls.isNotEmpty()
                            ZamerCard(
                                modifier = Modifier
                                    .widthIn(max = 600.dp)
                                    .testTag("pick_room_${r.entity.id}")
                                    .clickable {
                                        selectedRoomId = r.entity.id
                                        currentWallIdx = 0
                                        step = if (isDone) WizardStep.RESULT else WizardStep.WALL
                                    },
                                borderColor = if (isDone) DeepGreen else PureBlack
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(r.entity.name, style = MaterialTheme.typography.titleLarge, color = PureBlack)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (isDone) {
                                                "Замерено · Стены: ${Formatters.formatArea(r.calculations.netWallAreaSqm)}"
                                            } else {
                                                "Стен: ${r.walls.size} · Замерено: $measured"
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = DeepGreen
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            selectedRoomId = r.entity.id
                                            currentWallIdx = 0
                                            step = WizardStep.WALL
                                        },
                                        modifier = Modifier.size(50.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(2.dp, DeepGreen),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Изменить", tint = DeepGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        WizardStep.WALL -> {
            val walls = activeRoom.walls
            val wall = walls.getOrElse(currentWallIdx) { walls.first() }
            var lengthInput by remember(activeRoom.entity.id, wall.index) {
                mutableStateOf(
                    if (wall.lengthMeters > 0.0) Formatters.formatCleanNumber(wall.lengthMeters) else ""
                )
            }

            // Build live preview list including what user is currently typing!
            val liveTypedMeters = Formatters.parseDecimalInput(lengthInput) ?: 0.0
            val liveWalls = walls.map {
                if (it.index == wall.index) it.copy(lengthMeters = liveTypedMeters) else it
            }
            val aiCheck = GeometryAiAdvisor.validateRoomWalls(liveWalls)

            fun commitCurrentWall() {
                val parsed = Formatters.parseDecimalInput(lengthInput) ?: 0.0
                onSaveWallLength(activeRoom.entity, wall.index, parsed)
            }

            Scaffold(
                containerColor = PureWhite,
                topBar = {
                    ZamerTopBar(
                        title = activeRoom.entity.name,
                        subtitle = "Замер по кругу",
                        onBackClick = {
                            commitCurrentWall()
                            if (currentWallIdx > 0) {
                                currentWallIdx--
                            } else if (initialRoomId == null) {
                                step = WizardStep.PICK_ROOM
                            } else {
                                onBackExit()
                            }
                        }
                    )
                }
            ) { inner ->
                AnimatedScreenEntrance(Modifier.fillMaxSize().padding(inner)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Стена ${wall.index} из ${walls.size}",
                                style = MaterialTheme.typography.displayLarge,
                                color = DeepGreen
                            )

                            // Live proportional room diagram that morphs as numbers are typed
                            ProportionalRoomCanvas(
                                walls = liveWalls,
                                activeWallIndex = wall.index,
                                hasWarning = !aiCheck.isValid,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                                    .background(DeepGreenSoft, RoundedCornerShape(16.dp))
                                    .border(
                                        1.5.dp,
                                        if (!aiCheck.isValid) WarningRed else DeepGreen,
                                        RoundedCornerShape(16.dp)
                                    )
                            )

                            if (!aiCheck.isValid && !aiCheck.warningMessage.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(PureWhite, RoundedCornerShape(14.dp))
                                        .border(2.dp, WarningRed, RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Предупреждение",
                                        tint = WarningRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = aiCheck.warningMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WarningRed
                                    )
                                }
                            }

                            Text(
                                text = wall.hint,
                                style = MaterialTheme.typography.titleMedium,
                                color = PureBlack
                            )

                            ZamerInputField(
                                value = lengthInput,
                                onValueChange = { lengthInput = it },
                                label = "Длина стены (метры)",
                                placeholder = "0.00",
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                                isGiantNumeric = true,
                                onDone = {
                                    commitCurrentWall()
                                    if (currentWallIdx < walls.lastIndex) {
                                        currentWallIdx++
                                    } else {
                                        step = WizardStep.HEIGHT_AND_OPENINGS
                                    }
                                },
                                testTag = "wall_length_input"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ZamerSecondaryButton(
                                    text = "Назад",
                                    onClick = {
                                        commitCurrentWall()
                                        if (currentWallIdx > 0) {
                                            currentWallIdx--
                                        } else if (initialRoomId == null) {
                                            step = WizardStep.PICK_ROOM
                                        } else {
                                            onBackExit()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ZamerPrimaryButton(
                                    text = "Дальше",
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    onClick = {
                                        commitCurrentWall()
                                        if (currentWallIdx < walls.lastIndex) {
                                            currentWallIdx++
                                        } else {
                                            step = WizardStep.HEIGHT_AND_OPENINGS
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "wizard_next_wall_button"
                                )
                            }
                        }
                    }
                }
            }
        }

        WizardStep.HEIGHT_AND_OPENINGS -> {
            var hasStretchCeiling by remember(activeRoom.entity.id) {
                mutableStateOf(activeRoom.entity.hasStretchCeiling)
            }
            val initialBefore = activeRoom.entity.heightBeforeCeilingMeters
                ?: activeRoom.entity.heightOverrideMeters
                ?: aggregate.project.wallHeightMeters
            val initialAfter = activeRoom.entity.heightAfterCeilingMeters
                ?: (initialBefore - 0.08).coerceAtLeast(1.80)

            var heightBeforeText by remember(activeRoom.entity.id) {
                mutableStateOf(Formatters.formatCleanNumber(initialBefore))
            }
            var heightAfterText by remember(activeRoom.entity.id) {
                mutableStateOf(Formatters.formatCleanNumber(initialAfter))
            }
            var applyHeightToAll by remember(activeRoom.entity.id) { mutableStateOf(false) }

            val openings = remember(activeRoom.entity.id) {
                mutableStateListOf<OpeningItem>().apply { addAll(activeRoom.openings) }
            }

            fun saveHeightAndOpenings() {
                val hBefore = Formatters.parseDecimalInput(heightBeforeText) ?: aggregate.project.wallHeightMeters
                val hAfter = Formatters.parseDecimalInput(heightAfterText) ?: hBefore
                val activeH = if (hasStretchCeiling) hAfter else hBefore
                onSaveCeilingAndHeight(
                    activeRoom.entity,
                    activeH,
                    hasStretchCeiling,
                    hBefore,
                    hAfter,
                    applyHeightToAll
                )
                onSaveOpenings(activeRoom.entity, openings.toList())
            }

            Scaffold(
                containerColor = PureWhite,
                topBar = {
                    ZamerTopBar(
                        title = "${activeRoom.entity.name}: высота и проёмы",
                        subtitle = "Уровень потолка, двери и окна",
                        onBackClick = {
                            saveHeightAndOpenings()
                            currentWallIdx = activeRoom.walls.lastIndex.coerceAtLeast(0)
                            step = WizardStep.WALL
                        }
                    )
                }
            ) { inner ->
                AnimatedScreenEntrance(Modifier.fillMaxSize().padding(inner)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Stretch ceiling before / after per room!
                            ZamerCard(borderColor = DeepGreen) {
                                Text(
                                    text = "Натяжной потолок в комнате",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = PureBlack
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { hasStretchCeiling = false },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(2.dp, if (!hasStretchCeiling) DeepGreen else PureBlack),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (!hasStretchCeiling) DeepGreen else PureWhite,
                                            contentColor = if (!hasStretchCeiling) PureWhite else PureBlack
                                        )
                                    ) {
                                        Text("До потолка", style = MaterialTheme.typography.bodyLarge)
                                    }
                                    OutlinedButton(
                                        onClick = { hasStretchCeiling = true },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(2.dp, if (hasStretchCeiling) DeepGreen else PureBlack),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (hasStretchCeiling) DeepGreen else PureWhite,
                                            contentColor = if (hasStretchCeiling) PureWhite else PureBlack
                                        )
                                    ) {
                                        Text("После потолка", style = MaterialTheme.typography.bodyLarge)
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                if (!hasStretchCeiling) {
                                    ZamerInputField(
                                        value = heightBeforeText,
                                        onValueChange = { heightBeforeText = it },
                                        label = "Высота стен до натяжного потолка (м)",
                                        placeholder = "2.50",
                                        keyboardType = KeyboardType.Decimal,
                                        isGiantNumeric = true,
                                        testTag = "ceiling_height_input"
                                    )
                                } else {
                                    ZamerInputField(
                                        value = heightAfterText,
                                        onValueChange = { heightAfterText = it },
                                        label = "Высота стен после натяжного потолка (м)",
                                        placeholder = "2.42",
                                        keyboardType = KeyboardType.Decimal,
                                        isGiantNumeric = true,
                                        testTag = "ceiling_height_after_input"
                                    )
                                }

                                Spacer(Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { applyHeightToAll = !applyHeightToAll }
                                ) {
                                    Checkbox(
                                        checked = applyHeightToAll,
                                        onCheckedChange = { applyHeightToAll = it },
                                        colors = CheckboxDefaults.colors(checkedColor = DeepGreen)
                                    )
                                    Text(
                                        text = "Установить базовой высотой квартиры",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PureBlack
                                    )
                                }
                            }

                            // Openings (windows and doors can ALL be deleted!)
                            if (openings.isEmpty()) {
                                ZamerCard(borderColor = PureBlack) {
                                    Text(
                                        text = "Проёмы не вычитаются (все окна и двери убраны).",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = PureBlack
                                    )
                                }
                            } else {
                                openings.forEachIndexed { idx, op ->
                                    OpeningEditorCard(
                                        item = op,
                                        onUpdate = { updated -> openings[idx] = updated },
                                        onDelete = { openings.removeAt(idx) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ZamerSecondaryButton(
                                    text = "+ Дверь",
                                    icon = Icons.Filled.Add,
                                    onClick = {
                                        openings.add(
                                            OpeningItem(
                                                id = UUID.randomUUID().toString(),
                                                type = OpeningType.DOOR,
                                                count = 1,
                                                widthMeters = 0.80,
                                                heightMeters = 2.00
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ZamerSecondaryButton(
                                    text = "+ Окно",
                                    icon = Icons.Filled.Add,
                                    onClick = {
                                        openings.add(
                                            OpeningItem(
                                                id = UUID.randomUUID().toString(),
                                                type = OpeningType.WINDOW,
                                                count = 1,
                                                widthMeters = 1.40,
                                                heightMeters = 1.40
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (openings.any { it.type == OpeningType.WINDOW }) {
                                ZamerSecondaryButton(
                                    text = "Выкинуть все окна (не вычитать)",
                                    onClick = {
                                        openings.removeAll { it.type == OpeningType.WINDOW }
                                    },
                                    testTag = "remove_all_windows_button"
                                )
                            }

                            // Straight to RESULT — Corners step removed from wizard!
                            ZamerPrimaryButton(
                                text = "Показать результат",
                                icon = Icons.Filled.Check,
                                onClick = {
                                    saveHeightAndOpenings()
                                    step = WizardStep.RESULT
                                },
                                testTag = "wizard_show_result_button"
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        WizardStep.RESULT -> {
            val c = activeRoom.calculations
            val currentIdxInList = aggregate.rooms.indexOfFirst { it.entity.id == activeRoom.entity.id }
            val nextRoom = if (currentIdxInList >= 0 && currentIdxInList < aggregate.rooms.lastIndex) {
                aggregate.rooms[currentIdxInList + 1]
            } else {
                null
            }

            Scaffold(
                containerColor = PureWhite,
                topBar = {
                    ZamerTopBar(
                        title = "Итог: ${activeRoom.entity.name}",
                        subtitle = if (activeRoom.entity.hasStretchCeiling) {
                            "Замер после натяжного потолка"
                        } else {
                            "Замер до натяжных потолков"
                        },
                        onBackClick = { step = WizardStep.HEIGHT_AND_OPENINGS }
                    )
                }
            ) { inner ->
                AnimatedScreenEntrance(Modifier.fillMaxSize().padding(inner)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ProportionalRoomCanvas(
                                walls = activeRoom.walls,
                                activeWallIndex = -1,
                                hasWarning = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(DeepGreenSoft, RoundedCornerShape(16.dp))
                            )

                            ZamerCard(borderColor = DeepGreen, containerColor = DeepGreen) {
                                Text("Площадь стен (чистая)", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                Text(Formatters.formatArea(c.netWallAreaSqm), style = MaterialTheme.typography.displayLarge, color = PureWhite)
                                Text(
                                    "Периметр ${Formatters.formatMeters(c.perimeterMeters)} × высота ${Formatters.formatMeters(c.heightMeters)} − проёмы ${Formatters.formatArea(c.openingsAreaSqm)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PureWhite
                                )
                            }

                            ZamerCard(borderColor = PureBlack) {
                                Text("Площадь пола", style = MaterialTheme.typography.titleMedium, color = DeepGreen)
                                Text("Без стен: ${Formatters.formatArea(c.innerFloorAreaSqm)}", style = MaterialTheme.typography.headlineMedium, color = PureBlack)
                                Text("Со стенами: ${Formatters.formatArea(c.outerFloorAreaSqm)}", style = MaterialTheme.typography.titleLarge, color = PureBlack)
                            }

                            if (nextRoom != null) {
                                ZamerPrimaryButton(
                                    text = "Следующая: ${nextRoom.entity.name}",
                                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                                    onClick = {
                                        selectedRoomId = nextRoom.entity.id
                                        currentWallIdx = 0
                                        step = WizardStep.WALL
                                    }
                                )
                            }

                            ZamerSecondaryButton(
                                text = "Изменить стены (карандаш)",
                                icon = Icons.Filled.Edit,
                                onClick = {
                                    currentWallIdx = 0
                                    step = WizardStep.WALL
                                }
                            )

                            ZamerPrimaryButton(
                                text = "Готово (к итогам)",
                                icon = Icons.Filled.Check,
                                onClick = onBackExit,
                                testTag = "wizard_done_button"
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpeningEditorCard(
    item: OpeningItem,
    onUpdate: (OpeningItem) -> Unit,
    onDelete: () -> Unit
) {
    val label = if (item.type == OpeningType.DOOR) "Дверь" else "Окно"
    var widthStr by remember(item.id) { mutableStateOf(Formatters.formatCleanNumber(item.widthMeters)) }
    var heightStr by remember(item.id) { mutableStateOf(Formatters.formatCleanNumber(item.heightMeters)) }

    LaunchedEffect(item.widthMeters, item.heightMeters) {
        widthStr = Formatters.formatCleanNumber(item.widthMeters)
        heightStr = Formatters.formatCleanNumber(item.heightMeters)
    }

    ZamerCard(borderColor = PureBlack) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$label (${Formatters.formatArea(item.totalAreaSqm)})", style = MaterialTheme.typography.titleLarge, color = PureBlack)
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("delete_opening_${item.id}")
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Удалить $label", tint = PureBlack)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Количество: ${item.count}", style = MaterialTheme.typography.titleMedium, color = PureBlack)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onUpdate(item.copy(count = (item.count - 1).coerceAtLeast(0))) },
                    border = BorderStroke(2.dp, PureBlack),
                    modifier = Modifier.size(50.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("−", style = MaterialTheme.typography.titleLarge, color = PureBlack) }
                OutlinedButton(
                    onClick = { onUpdate(item.copy(count = item.count + 1)) },
                    border = BorderStroke(2.dp, PureBlack),
                    modifier = Modifier.size(50.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text("+", style = MaterialTheme.typography.titleLarge, color = PureBlack) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ZamerInputField(
                value = widthStr,
                onValueChange = {
                    widthStr = it
                    val w = Formatters.parseDecimalInput(it) ?: item.widthMeters
                    onUpdate(item.copy(widthMeters = w))
                },
                label = "Ширина (м)",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            ZamerInputField(
                value = heightStr,
                onValueChange = {
                    heightStr = it
                    val h = Formatters.parseDecimalInput(it) ?: item.heightMeters
                    onUpdate(item.copy(heightMeters = h))
                },
                label = "Высота (м)",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
