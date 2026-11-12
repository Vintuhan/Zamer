package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.ApartmentTemplates
import com.example.domain.FloorPlanSnapper
import com.example.domain.SnappedSegment
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.SchematicPlanCanvas
import com.example.ui.components.ZamerCard
import com.example.ui.components.ZamerInputField
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerSecondaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.BlackBorderSubtle
import com.example.ui.theme.BrightDoorGreen
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

enum class LayoutMode(val title: String) {
    TEMPLATES("Типовые"),
    DRAW("Нарисовать"),
    NO_PLAN("Без чертежа")
}

@Composable
fun LayoutSelectionScreen(
    onSelectTemplate: (String) -> Unit,
    onCreateFromDrawing: (List<Pair<String, List<SnappedSegment>>>) -> Unit,
    onCreateWithoutPlan: (List<Pair<String, Int>>) -> Unit,
    onBackClick: () -> Unit
) {
    var mode by remember { mutableStateOf(LayoutMode.TEMPLATES) }
    var showFullScreenDraw by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = "Планировка",
                subtitle = "Шаг 2 из 2 — выберите тип квартиры",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        AnimatedScreenEntrance(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LayoutMode.entries.forEach { tab ->
                        val selected = mode == tab
                        OutlinedButton(
                            onClick = {
                                mode = tab
                                if (tab == LayoutMode.DRAW) {
                                    showFullScreenDraw = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .testTag("layout_tab_${tab.name}"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(2.dp, if (selected) DeepGreen else PureBlack),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) DeepGreen else PureWhite,
                                contentColor = if (selected) PureWhite else PureBlack
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                when (mode) {
                    LayoutMode.TEMPLATES -> TemplatesGridSection(
                        onSelectTemplate = onSelectTemplate,
                        modifier = Modifier.weight(1f)
                    )
                    LayoutMode.DRAW -> DrawEntrySection(
                        onOpenFullScreenDraw = { showFullScreenDraw = true },
                        modifier = Modifier.weight(1f)
                    )
                    LayoutMode.NO_PLAN -> NoPlanRoomsSection(
                        onComplete = onCreateWithoutPlan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showFullScreenDraw) {
        FullScreenDrawingDialog(
            onDismiss = { showFullScreenDraw = false },
            onCompleteDrawing = { rooms ->
                showFullScreenDraw = false
                onCreateFromDrawing(rooms)
            }
        )
    }
}

@Composable
private fun TemplatesGridSection(
    onSelectTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Simple room count filter: null = All, 1, 2, 3
    var roomFilter by remember { mutableStateOf<Int?>(null) }
    // House type filter: null = All, true = Новостройка, false = Не новостройка
    var newBuildFilter by remember { mutableStateOf<Boolean?>(null) }

    val filtered = remember(roomFilter, newBuildFilter) {
        ApartmentTemplates.allTemplates.filter { tpl ->
            (roomFilter == null || tpl.roomCountCategory == roomFilter) &&
                (newBuildFilter == null || tpl.isNewBuild == newBuildFilter)
        }
    }

    Column(modifier = modifier.fillMaxWidth().widthIn(max = 620.dp)) {
        // Filter row 1: 1 - 2 - 3 rooms
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipBtn(
                label = "Все",
                selected = roomFilter == null,
                onClick = { roomFilter = null },
                modifier = Modifier.weight(1f)
            )
            FilterChipBtn(
                label = "1-комн.",
                selected = roomFilter == 1,
                onClick = { roomFilter = 1 },
                modifier = Modifier.weight(1f)
            )
            FilterChipBtn(
                label = "2-комн.",
                selected = roomFilter == 2,
                onClick = { roomFilter = 2 },
                modifier = Modifier.weight(1f)
            )
            FilterChipBtn(
                label = "3-комн.",
                selected = roomFilter == 3,
                onClick = { roomFilter = 3 },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Filter row 2: Новостройка / Не новостройка
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChipBtn(
                label = "Любой дом",
                selected = newBuildFilter == null,
                onClick = { newBuildFilter = null },
                modifier = Modifier.weight(1f)
            )
            FilterChipBtn(
                label = "Новостройка",
                selected = newBuildFilter == true,
                onClick = { newBuildFilter = true },
                modifier = Modifier.weight(1f)
            )
            FilterChipBtn(
                label = "Не новостройка",
                selected = newBuildFilter == false,
                onClick = { newBuildFilter = false },
                modifier = Modifier.weight(1.2f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp, 6.dp)
                    .background(BrightDoorGreen, RoundedCornerShape(3.dp))
            )
            Text(
                text = "Ярко-зелёная дверь снизу — вход в квартиру",
                style = MaterialTheme.typography.bodyMedium,
                color = PureBlack
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { tpl ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("template_tile_${tpl.id}")
                        .clickable { onSelectTemplate(tpl.id) },
                    shape = RoundedCornerShape(18.dp),
                    color = PureWhite,
                    border = BorderStroke(2.dp, PureBlack)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        SchematicPlanCanvas(
                            rooms = tpl.schematicRooms,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = tpl.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = PureBlack,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tpl.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepGreen,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipBtn(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, if (selected) DeepGreen else PureBlack),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) DeepGreen else PureWhite,
            contentColor = if (selected) PureWhite else PureBlack
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DrawEntrySection(
    onOpenFullScreenDraw: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ZamerCard(modifier = Modifier.widthIn(max = 560.dp), borderColor = DeepGreen) {
            Text(
                text = "Рисование планировки",
                style = MaterialTheme.typography.headlineMedium,
                color = PureBlack
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Откроется окно на весь экран. Рисуйте комнаты пальцем жирно как удобно, а встроенный ИИ сам соберёт ровный план со стенами.",
                style = MaterialTheme.typography.bodyMedium,
                color = PureBlack
            )
            Spacer(Modifier.height(16.dp))
            ZamerPrimaryButton(
                text = "Открыть окно рисования",
                icon = Icons.Filled.Draw,
                onClick = onOpenFullScreenDraw,
                testTag = "open_fullscreen_draw_button"
            )
        }
    }
}

@Composable
private fun FullScreenDrawingDialog(
    onDismiss: () -> Unit,
    onCompleteDrawing: (List<Pair<String, List<SnappedSegment>>>) -> Unit
) {
    val rawStrokes = remember { mutableStateListOf<List<Offset>>() }
    val activeStroke = remember { mutableStateListOf<Offset>() }
    val committedRooms = remember { mutableStateListOf<Pair<String, List<SnappedSegment>>>() }
    var aiPreviewSegments by remember { mutableStateOf<List<SnappedSegment>>(emptyList()) }
    var roomNameInput by remember { mutableStateOf("Комната 1") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PureWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Рисуй пальцем на весь экран",
                        style = MaterialTheme.typography.titleLarge,
                        color = PureBlack,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = PureBlack)
                    }
                }

                ZamerInputField(
                    value = roomNameInput,
                    onValueChange = { roomNameInput = it },
                    label = "Название комнаты",
                    placeholder = "Например: Зал"
                )

                // Giant full-screen freehand canvas (bold strokes, NO 90-degree snap during drawing)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(PureWhite, RoundedCornerShape(18.dp))
                        .border(2.5.dp, PureBlack, RoundedCornerShape(18.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    aiPreviewSegments = emptyList()
                                    activeStroke.clear()
                                    activeStroke.add(start)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    activeStroke.add(change.position)
                                },
                                onDragEnd = {
                                    if (activeStroke.size > 1) {
                                        rawStrokes.add(activeStroke.toList())
                                    }
                                    activeStroke.clear()
                                },
                                onDragCancel = { activeStroke.clear() }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Subtle light grid
                        val step = 48f
                        var gx = step
                        while (gx < size.width) {
                            drawLine(BlackBorderSubtle, Offset(gx, 0f), Offset(gx, size.height), 1f)
                            gx += step
                        }
                        var gy = step
                        while (gy < size.height) {
                            drawLine(BlackBorderSubtle, Offset(0f, gy), Offset(size.width, gy), 1f)
                            gy += step
                        }

                        // Already AI-committed rooms
                        committedRooms.forEach { (_, segs) ->
                            segs.forEach { s ->
                                drawLine(
                                    color = DeepGreen,
                                    start = s.start,
                                    end = s.end,
                                    strokeWidth = 10f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        // Raw freehand strokes as drawn by finger (bold!)
                        val strokeStyle = Stroke(
                            width = 14f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                        rawStrokes.forEach { pts ->
                            if (pts.size > 1) {
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                                }
                                drawPath(path, color = PureBlack, style = strokeStyle)
                            }
                        }
                        if (activeStroke.size > 1) {
                            val path = Path().apply {
                                moveTo(activeStroke.first().x, activeStroke.first().y)
                                for (i in 1 until activeStroke.size) lineTo(activeStroke[i].x, activeStroke[i].y)
                            }
                            drawPath(path, color = PureBlack, style = strokeStyle)
                        }

                        // AI-interpreted clean result overlay
                        aiPreviewSegments.forEach { s ->
                            drawLine(
                                color = BrightDoorGreen,
                                start = s.start,
                                end = s.end,
                                strokeWidth = 12f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(color = DeepGreen, radius = 8f, center = s.start)
                        }
                    }

                    if (rawStrokes.isEmpty() && activeStroke.isEmpty() && committedRooms.isEmpty() && aiPreviewSegments.isEmpty()) {
                        Text(
                            text = "Проведите пальцем контур комнаты.\nВыравнивать не нужно — ИИ соберёт итог сам!",
                            style = MaterialTheme.typography.titleMedium,
                            color = BlackBorderSubtle,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(20.dp)
                        )
                    }
                }

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZamerSecondaryButton(
                        text = "Шаг назад",
                        icon = Icons.Filled.Undo,
                        onClick = {
                            aiPreviewSegments = emptyList()
                            if (rawStrokes.isNotEmpty()) rawStrokes.removeAt(rawStrokes.lastIndex)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ZamerSecondaryButton(
                        text = "Очистить",
                        icon = Icons.Filled.DeleteSweep,
                        onClick = {
                            rawStrokes.clear()
                            activeStroke.clear()
                            aiPreviewSegments = emptyList()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZamerSecondaryButton(
                        text = "ИИ: собрать",
                        icon = Icons.Filled.AutoAwesome,
                        borderColor = DeepGreen,
                        textColor = DeepGreen,
                        onClick = {
                            if (rawStrokes.isNotEmpty()) {
                                aiPreviewSegments = FloorPlanSnapper.interpretFreehandStrokesToRoom(rawStrokes.toList())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "ai_interpret_drawing_button"
                    )
                    ZamerSecondaryButton(
                        text = "+ Ещё комната",
                        icon = Icons.Filled.Add,
                        onClick = {
                            val segs = aiPreviewSegments.ifEmpty {
                                FloorPlanSnapper.interpretFreehandStrokesToRoom(rawStrokes.toList())
                            }
                            if (segs.isNotEmpty()) {
                                val name = roomNameInput.trim().ifEmpty { "Комната ${committedRooms.size + 1}" }
                                committedRooms.add(name to segs)
                                rawStrokes.clear()
                                aiPreviewSegments = emptyList()
                                roomNameInput = "Комната ${committedRooms.size + 1}"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                ZamerPrimaryButton(
                    text = "Итоговый результат (${committedRooms.size + if (rawStrokes.isNotEmpty() || aiPreviewSegments.isNotEmpty()) 1 else 0} комн.)",
                    icon = Icons.Filled.Check,
                    enabled = committedRooms.isNotEmpty() || rawStrokes.isNotEmpty() || aiPreviewSegments.isNotEmpty(),
                    onClick = {
                        val all = committedRooms.toMutableList()
                        val lastSegs = aiPreviewSegments.ifEmpty {
                            FloorPlanSnapper.interpretFreehandStrokesToRoom(rawStrokes.toList())
                        }
                        if (lastSegs.isNotEmpty()) {
                            val name = roomNameInput.trim().ifEmpty { "Комната ${all.size + 1}" }
                            all.add(name to lastSegs)
                        }
                        if (all.isNotEmpty()) {
                            onCompleteDrawing(all)
                        }
                    },
                    testTag = "finish_drawing_button"
                )
            }
        }
    }
}

@Composable
private fun NoPlanRoomsSection(
    onComplete: (List<Pair<String, Int>>) -> Unit,
    modifier: Modifier = Modifier
) {
    val rooms = remember {
        mutableStateListOf(
            "Комната" to 4,
            "Кухня" to 4,
            "Коридор" to 4,
            "Санузел" to 4
        )
    }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomWalls by remember { mutableIntStateOf(4) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ZamerCard(borderColor = DeepGreen) {
            ZamerInputField(
                value = newRoomName,
                onValueChange = { newRoomName = it },
                label = "Добавить комнату",
                placeholder = "Название (например: Балкон)",
                testTag = "no_plan_room_name_input"
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Стен: $newRoomWalls",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureBlack
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { if (newRoomWalls > 1) newRoomWalls-- },
                        border = BorderStroke(2.dp, PureBlack),
                        modifier = Modifier.size(50.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("−", style = MaterialTheme.typography.titleLarge, color = PureBlack)
                    }
                    OutlinedButton(
                        onClick = { if (newRoomWalls < 12) newRoomWalls++ },
                        border = BorderStroke(2.dp, PureBlack),
                        modifier = Modifier.size(50.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge, color = PureBlack)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            ZamerSecondaryButton(
                text = "Добавить в список",
                icon = Icons.Filled.Add,
                borderColor = DeepGreen,
                textColor = DeepGreen,
                onClick = {
                    val title = newRoomName.trim().ifEmpty { "Комната ${rooms.size + 1}" }
                    rooms.add(title to newRoomWalls)
                    newRoomName = ""
                    newRoomWalls = 4
                },
                testTag = "add_room_to_list_button"
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(rooms) { idx, (name, walls) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepGreenSoft, RoundedCornerShape(14.dp))
                        .border(1.5.dp, PureBlack, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium, color = PureBlack)
                        Text("Стен: $walls", style = MaterialTheme.typography.bodyMedium, color = DeepGreen)
                    }
                    IconButton(
                        onClick = { if (rooms.size > 1) rooms.removeAt(idx) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Удалить комнату",
                            tint = PureBlack
                        )
                    }
                }
            }
        }

        ZamerPrimaryButton(
            text = "Создать объект (${rooms.size} комн.)",
            icon = Icons.Filled.Check,
            enabled = rooms.isNotEmpty(),
            onClick = { onComplete(rooms.toList()) },
            testTag = "create_no_plan_button"
        )
        Spacer(Modifier.height(8.dp))
    }
}
