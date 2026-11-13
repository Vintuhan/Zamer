package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ProjectAggregate
import com.example.data.RoomDetail
import com.example.data.RoomEntity
import com.example.data.ZamerJsonCodec
import com.example.domain.FloorPlanMinimapHelper
import com.example.domain.Formatters
import com.example.domain.GeometryAiAdvisor
import com.example.domain.MinimapPlan
import com.example.domain.MinimapRoomSpec
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.FloorPlanMinimapCanvas
import com.example.ui.components.ProportionalRoomCanvas
import com.example.ui.components.RenameRoomDialog
import com.example.ui.components.ZamerInputField
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerSecondaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarningRed

@Composable
fun MeasurementWizardScreen(
    aggregate: ProjectAggregate?,
    initialRoomId: Long?,
    onBackExit: () -> Unit,
    onSaveWallLength: (RoomEntity, Int, Double) -> Unit,
    onRenameRoom: (RoomEntity, String) -> Unit
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

    val startRoomId = remember(initialRoomId, aggregate.rooms) {
        initialRoomId
            ?: aggregate.rooms.firstOrNull { r -> r.walls.any { it.lengthMeters <= 0.0 } }?.entity?.id
            ?: aggregate.rooms.first().entity.id
    }
    var selectedRoomId by remember(startRoomId) { mutableStateOf(startRoomId) }
    var currentWallIdx by remember(selectedRoomId) { mutableIntStateOf(0) }

    val activeRoom: RoomDetail = aggregate.rooms.find { it.entity.id == selectedRoomId }
        ?: aggregate.rooms.first()
    val walls = activeRoom.walls
    if (walls.isEmpty()) {
        onBackExit()
        return
    }
    val wall = walls.getOrElse(currentWallIdx) { walls.first() }

    val drawnSegments = remember(aggregate.project.customDrawingJson) {
        ZamerJsonCodec.decodeDrawingSegments(aggregate.project.customDrawingJson)
    }
    val showApartmentMinimap = aggregate.project.layoutType != "NO_PLAN"

    val activeRoomSegments = remember(
        aggregate.project.layoutType,
        drawnSegments,
        aggregate.rooms,
        activeRoom.entity.id
    ) {
        if (aggregate.project.layoutType == "CUSTOM_DRAW" && drawnSegments.isNotEmpty()) {
            var cursor = 0
            var matched: List<com.example.domain.SnappedSegment> = emptyList()
            aggregate.rooms.sortedBy { it.entity.sortOrder }.forEach { r ->
                val count = r.walls.size.coerceAtLeast(r.entity.wallCount)
                if (cursor + count <= drawnSegments.size) {
                    val segs = drawnSegments.subList(cursor, cursor + count)
                    cursor += count
                    if (r.entity.id == activeRoom.entity.id) {
                        matched = segs
                    }
                }
            }
            matched
        } else {
            emptyList()
        }
    }

    val minimapSpecs = remember(aggregate.rooms) {
        aggregate.rooms.map {
            MinimapRoomSpec(
                id = it.entity.id,
                name = it.entity.name,
                wallCount = it.walls.size.coerceAtLeast(it.entity.wallCount),
                sortOrder = it.entity.sortOrder
            )
        }
    }

    val wallPlan = remember(
        aggregate.project.layoutType,
        aggregate.project.layoutTitle,
        minimapSpecs,
        activeRoom.entity.id,
        wall.index
    ) {
        FloorPlanMinimapHelper.buildMinimap(
            layoutType = aggregate.project.layoutType,
            layoutTitle = aggregate.project.layoutTitle,
            drawnSegments = drawnSegments,
            rooms = minimapSpecs,
            activeRoomId = activeRoom.entity.id,
            activeWallIndex = wall.index
        )
    }

    var lengthInput by remember(activeRoom.entity.id, wall.index) {
        mutableStateOf(
            if (wall.lengthMeters > 0.0) Formatters.formatCleanNumber(wall.lengthMeters) else ""
        )
    }

    val liveTypedMeters = Formatters.parseDecimalInput(lengthInput) ?: 0.0
    val liveWalls = walls.map {
        if (it.index == wall.index) it.copy(lengthMeters = liveTypedMeters) else it
    }
    val aiCheck = GeometryAiAdvisor.validateRoomWalls(liveWalls)

    fun commitCurrentWall() {
        val parsed = Formatters.parseDecimalInput(lengthInput) ?: 0.0
        onSaveWallLength(activeRoom.entity, wall.index, parsed)
    }

    fun goNext() {
        commitCurrentWall()
        if (currentWallIdx < walls.lastIndex) {
            currentWallIdx++
        } else {
            onBackExit()
        }
    }

    var showExpandedPlanDialog by remember { mutableStateOf(false) }
    var renamingRoom by remember { mutableStateOf<RoomEntity?>(null) }

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

    if (showApartmentMinimap && showExpandedPlanDialog) {
        WizardExpandedPlanDialog(
            plan = wallPlan,
            roomName = activeRoom.entity.name,
            wallNumber = wall.index,
            onDismiss = { showExpandedPlanDialog = false }
        )
    }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = activeRoom.entity.name,
                subtitle = "Стена ${wall.index} из ${walls.size}",
                onBackClick = {
                    commitCurrentWall()
                    if (currentWallIdx > 0) {
                        currentWallIdx--
                    } else {
                        onBackExit()
                    }
                },
                onTitleClick = { renamingRoom = activeRoom.entity }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showApartmentMinimap) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .testTag("wizard_top_minimap")
                                .clickable { showExpandedPlanDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            color = PureWhite,
                            border = BorderStroke(2.dp, PureBlack)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                FloorPlanMinimapCanvas(
                                    plan = wallPlan,
                                    showLabels = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Icon(
                                    imageVector = Icons.Filled.ZoomOutMap,
                                    contentDescription = "Увеличить план квартиры",
                                    tint = DeepGreen,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                )
                            }
                        }
                    }

                    ProportionalRoomCanvas(
                        walls = liveWalls,
                        activeWallIndex = wall.index,
                        hasWarning = !aiCheck.isValid,
                        shapeKey = activeRoom.entity.shapeKey,
                        customSegments = activeRoomSegments,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
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
                                .padding(12.dp)
                                .testTag("wall_mismatch_warning"),
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

                    ZamerInputField(
                        value = lengthInput,
                        onValueChange = { lengthInput = it },
                        label = "Длина стены ${wall.index} (метры)",
                        placeholder = "0.00",
                        keyboardType = KeyboardType.Decimal,
                        imeAction = if (currentWallIdx < walls.lastIndex) ImeAction.Next else ImeAction.Done,
                        isGiantNumeric = true,
                        onDone = { goNext() },
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
                                } else {
                                    onBackExit()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        val isLastWall = currentWallIdx >= walls.lastIndex
                        ZamerPrimaryButton(
                            text = if (isLastWall) "Готово" else "Дальше",
                            icon = if (isLastWall) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = { goNext() },
                            modifier = Modifier.weight(1f),
                            testTag = "wizard_next_wall_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardExpandedPlanDialog(
    plan: MinimapPlan,
    roomName: String,
    wallNumber: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureWhite,
        title = {
            Column {
                Text("План квартиры", style = MaterialTheme.typography.titleLarge, color = PureBlack)
                Text(
                    text = "$roomName · стена $wallNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepGreen
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .background(PureWhite, RoundedCornerShape(16.dp))
                    .border(2.dp, PureBlack, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                FloorPlanMinimapCanvas(
                    plan = plan,
                    showLabels = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DeepGreen,
                    contentColor = PureWhite
                )
            ) {
                Text("Закрыть")
            }
        }
    )
}
