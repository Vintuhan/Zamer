package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AddressLookupUiState
import com.example.data.SketchMatchUiState
import com.example.domain.ConstructorOpeningKind
import com.example.domain.ConstructorRoom
import com.example.domain.ConstructorShapeKind
import com.example.domain.LayoutConstructorEngine
import com.example.domain.NoPlanRoomSpec
import com.example.domain.SimpleRoomShape
import com.example.domain.SnappedSegment
import com.example.ui.components.AnimatedScreenEntrance
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
import com.example.ui.theme.WarningRed
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private enum class LayoutSection {
    NO_PLAN,
    CONSTRUCTOR
}

private enum class ConstructorToolMode {
    MOVE_RESIZE,
    PLACE_DOOR,
    PLACE_WINDOW,
    PLACE_BALCONY_DOOR,
    PLACE_CORNER_WINDOW
}

private sealed interface ConstructorDragTarget {
    data class Corner(val roomIdx: Int, val cornerIdx: Int) : ConstructorDragTarget
    data class Room(val roomIdx: Int, val startLeft: Float, val startTop: Float) : ConstructorDragTarget
    data class Pan(val startPan: Offset) : ConstructorDragTarget
}

private val LayoutSerifBoldTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp)
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun LayoutSelectionScreen(
    initialAddress: String = "",
    addressLookupState: AddressLookupUiState = AddressLookupUiState.Idle,
    sketchMatchState: SketchMatchUiState = SketchMatchUiState.Idle,
    onSearchByAddress: (String, Int?) -> Unit = { _, _ -> },
    onClearAddressLookup: () -> Unit = {},
    onAnalyzeSketch: (Bitmap, String) -> Unit = { _, _ -> },
    onClearSketchMatch: () -> Unit = {},
    onSelectTemplate: (String, String?) -> Unit = { _, _ -> },
    onCreateFromDrawing: (List<Pair<String, List<SnappedSegment>>>) -> Unit,
    onCreateWithoutPlan: (List<NoPlanRoomSpec>) -> Unit,
    onBackClick: () -> Unit
) {
    var activeSection by remember { mutableStateOf<LayoutSection?>(null) }

    BackHandler(enabled = activeSection != null) {
        activeSection = null
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        shapes = MaterialTheme.shapes,
        typography = LayoutSerifBoldTypography
    ) {
        when (activeSection) {
            null -> {
                Scaffold(
                    containerColor = PureWhite,
                    topBar = {
                        ZamerTopBar(
                            title = "Планировка",
                            subtitle = null,
                            onBackClick = onBackClick
                        )
                    }
                ) { innerPadding ->
                    AnimatedScreenEntrance(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        LayoutMainChoiceMenu(
                            onOpenNoPlan = { activeSection = LayoutSection.NO_PLAN },
                            onOpenConstructor = { activeSection = LayoutSection.CONSTRUCTOR }
                        )
                    }
                }
            }

            LayoutSection.NO_PLAN -> {
                Scaffold(
                    containerColor = PureWhite,
                    topBar = {
                        ZamerTopBar(
                            title = "Без плана",
                            subtitle = null,
                            onBackClick = { activeSection = null }
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
                                .padding(horizontal = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NoPlanRoomsSection(
                                onComplete = onCreateWithoutPlan,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            LayoutSection.CONSTRUCTOR -> {
                ConstructorWorkspace(
                    onBack = { activeSection = null },
                    onComplete = { payload ->
                        onCreateFromDrawing(payload)
                    }
                )
            }
        }
    }
}

@Composable
private fun LayoutMainChoiceMenu(
    onOpenNoPlan: () -> Unit,
    onOpenConstructor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 108.dp)
                    .clickable(onClick = onOpenNoPlan)
                    .testTag("layout_btn_no_plan"),
                shape = RoundedCornerShape(18.dp),
                color = DeepGreen,
                border = BorderStroke(2.dp, PureBlack)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatListBulleted,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(34.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Без плана",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureWhite,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = "Список комнат",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PureWhite,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 108.dp)
                    .clickable(onClick = onOpenConstructor)
                    .testTag("layout_btn_constructor"),
                shape = RoundedCornerShape(18.dp),
                color = DeepGreenSoft,
                border = BorderStroke(2.5.dp, DeepGreen)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DashboardCustomize,
                        contentDescription = null,
                        tint = DeepGreen,
                        modifier = Modifier.size(34.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Конструктор",
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureBlack,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = "Собрать планировку из готовых форм",
                            style = MaterialTheme.typography.bodyLarge,
                            color = DeepGreen,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConstructorEditorScreen(
    initialRooms: List<ConstructorRoom>,
    onBack: () -> Unit,
    onComplete: (List<Pair<String, List<SnappedSegment>>>) -> Unit
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        shapes = MaterialTheme.shapes,
        typography = LayoutSerifBoldTypography
    ) {
        ConstructorWorkspace(
            initialRooms = initialRooms,
            onBack = onBack,
            onComplete = onComplete
        )
    }
}

@Composable
private fun ConstructorWorkspace(
    initialRooms: List<ConstructorRoom> = emptyList(),
    onBack: () -> Unit,
    onComplete: (List<Pair<String, List<SnappedSegment>>>) -> Unit
) {
    val rooms = remember(initialRooms) {
        mutableStateListOf<ConstructorRoom>().apply { addAll(initialRooms) }
    }
    var selectedRoomId by remember(initialRooms) {
        mutableStateOf<String?>(initialRooms.firstOrNull()?.id)
    }
    var toolMode by remember { mutableStateOf(ConstructorToolMode.MOVE_RESIZE) }
    var snapToGrid by remember { mutableStateOf(true) }
    var snapToNeighbors by remember { mutableStateOf(true) }
    var showZoomBar by remember { mutableStateOf(false) }

    var zoomScale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize(800, 900)) }
    var didInitialSetup by remember(initialRooms) { mutableStateOf(false) }
    var snapGuideX by remember { mutableStateOf<Float?>(null) }
    var snapGuideY by remember { mutableStateOf<Float?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var customNameDraft by remember { mutableStateOf("") }

    fun applyStepZoom(targetZoom: Float) {
        val viewCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        val (nextZoom, nextPan) = LayoutConstructorEngine.zoomAroundAnchor(
            currentZoom = zoomScale,
            currentPan = panOffset,
            zoomChange = targetZoom / zoomScale,
            panChange = Offset.Zero,
            anchorScreen = viewCenter
        )
        zoomScale = nextZoom
        panOffset = nextPan
    }

    LaunchedEffect(initialRooms, canvasSize) {
        if (!didInitialSetup && canvasSize.width > 0 && canvasSize.height > 0) {
            if (initialRooms.isNotEmpty()) {
                val (fitZoom, fitPan) = LayoutConstructorEngine.fitRoomsToViewport(
                    rooms = initialRooms,
                    viewportWidth = canvasSize.width.toFloat(),
                    viewportHeight = canvasSize.height.toFloat()
                )
                zoomScale = fitZoom
                panOffset = fitPan
            } else {
                zoomScale = LayoutConstructorEngine.default7x7Zoom(
                    canvasSize.width.toFloat(),
                    canvasSize.height.toFloat()
                )
                panOffset = Offset.Zero
            }
            didInitialSetup = true
        }
    }

    val selectedIndex = rooms.indexOfFirst { it.id == selectedRoomId }
    val selectedRoom = rooms.getOrNull(selectedIndex)

    val roomNamePaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = PureBlack.toArgb()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = 30f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Верхняя панель: Назад | Конструктор | кол-во комнат
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("constructor_back_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, PureBlack),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PureWhite,
                    contentColor = PureBlack
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Назад", style = MaterialTheme.typography.labelMedium, overflow = TextOverflow.Clip)
            }

            Text(
                text = "Конструктор",
                style = MaterialTheme.typography.titleLarge,
                color = PureBlack,
                overflow = TextOverflow.Clip
            )

            Text(
                text = "${rooms.size} ф.",
                style = MaterialTheme.typography.bodyMedium,
                color = DeepGreen,
                modifier = Modifier.padding(end = 6.dp)
            )
        }

        // Сверху фигуры: квадрат, прямоугольник, Г, П, просто стена, косая, скос, коридор, эркер
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F8F6))
                .border(1.dp, BlackBorderSubtle)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConstructorShapeKind.entries.forEach { shapeKind ->
                ShapePaletteButton(
                    kind = shapeKind,
                    onClick = {
                        val centerWorld = LayoutConstructorEngine.screenToWorld(
                            Offset(canvasSize.width / 2f, canvasSize.height / 2f),
                            zoomScale,
                            panOffset
                        )
                        val created = LayoutConstructorEngine.createRoom(
                            id = UUID.randomUUID().toString(),
                            kind = shapeKind,
                            centerX = centerWorld.x,
                            centerY = centerWorld.y,
                            existingCount = rooms.size,
                            snapToGridEnabled = snapToGrid
                        )
                        rooms.add(created)
                        selectedRoomId = created.id
                        toolMode = ConstructorToolMode.MOVE_RESIZE
                    }
                )
            }
        }

        // Интерактивное поле сборки: сетка 1 клетка = 1 метр, масштаб 7x7 по умолчанию
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .background(PureWhite, RoundedCornerShape(14.dp))
                .border(2.dp, PureBlack, RoundedCornerShape(14.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(toolMode) {
                        detectTapGestures { tapScreen ->
                            val tapWorld = LayoutConstructorEngine.screenToWorld(tapScreen, zoomScale, panOffset)
                            val hitTolWorld = 42f / zoomScale
                            val openingKind = when (toolMode) {
                                ConstructorToolMode.PLACE_DOOR -> ConstructorOpeningKind.DOOR
                                ConstructorToolMode.PLACE_WINDOW -> ConstructorOpeningKind.WINDOW
                                ConstructorToolMode.PLACE_BALCONY_DOOR -> ConstructorOpeningKind.BALCONY_DOOR
                                ConstructorToolMode.PLACE_CORNER_WINDOW -> ConstructorOpeningKind.CORNER_WINDOW
                                ConstructorToolMode.MOVE_RESIZE -> null
                            }
                            if (openingKind != null) {
                                val hit = LayoutConstructorEngine.toggleOpeningAtTap(
                                    rooms = rooms,
                                    tapWorld = tapWorld,
                                    kind = openingKind,
                                    newOpeningId = UUID.randomUUID().toString(),
                                    maxDistPx = hitTolWorld
                                )
                                if (hit != null) {
                                    rooms[hit.first] = hit.second
                                    selectedRoomId = hit.second.id
                                }
                            } else {
                                var hitId: String? = null
                                for (i in rooms.indices.reversed()) {
                                    if (LayoutConstructorEngine.isPointInsideRoom(tapWorld, rooms[i])) {
                                        hitId = rooms[i].id
                                        break
                                    }
                                }
                                selectedRoomId = hitId
                            }
                        }
                    }
                    .pointerInput(snapToGrid, snapToNeighbors) {
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            val downWorld = LayoutConstructorEngine.screenToWorld(
                                firstDown.position,
                                zoomScale,
                                panOffset
                            )

                            val curSelIdx = rooms.indexOfFirst { it.id == selectedRoomId }
                            var candidateTarget: ConstructorDragTarget? = null
                            if (curSelIdx >= 0) {
                                val r = rooms[curSelIdx]
                                val corners = if (r.shapeKind == ConstructorShapeKind.WALL_LINE) {
                                    val pts = r.worldVertices()
                                    listOf(pts.first(), pts.last())
                                } else {
                                    listOf(
                                        Offset(r.left, r.top),
                                        Offset(r.left + r.width, r.top),
                                        Offset(r.left + r.width, r.top + r.height),
                                        Offset(r.left, r.top + r.height)
                                    )
                                }
                                var bestCorner = -1
                                var bestCornerDist = 48f / zoomScale
                                corners.forEachIndexed { cIdx, cPt ->
                                    val d = hypot(downWorld.x - cPt.x, downWorld.y - cPt.y)
                                    if (d < bestCornerDist) {
                                        bestCornerDist = d
                                        bestCorner = if (r.shapeKind == ConstructorShapeKind.WALL_LINE && cIdx == 1) 2 else cIdx
                                    }
                                }
                                if (bestCorner >= 0) {
                                    candidateTarget = ConstructorDragTarget.Corner(curSelIdx, bestCorner)
                                }
                            }
                            if (candidateTarget == null) {
                                var hitIdx = -1
                                for (i in rooms.indices.reversed()) {
                                    if (LayoutConstructorEngine.isPointInsideRoom(downWorld, rooms[i])) {
                                        hitIdx = i
                                        break
                                    }
                                }
                                candidateTarget = if (hitIdx >= 0) {
                                    val room = rooms[hitIdx]
                                    ConstructorDragTarget.Room(hitIdx, room.left, room.top)
                                } else {
                                    ConstructorDragTarget.Pan(panOffset)
                                }
                            }

                            var dragStarted = false
                            var isPinching = false
                            var totalScreenDrag = Offset.Zero

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount == 0) break

                                if (pressedCount >= 2) {
                                    if (!isPinching) {
                                        isPinching = true
                                        snapGuideX = null
                                        snapGuideY = null
                                    }
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    if (centroid != Offset.Unspecified) {
                                        val (nextZoom, nextPan) = LayoutConstructorEngine.zoomAroundAnchor(
                                            currentZoom = zoomScale,
                                            currentPan = panOffset,
                                            zoomChange = zoomChange,
                                            panChange = panChange,
                                            anchorScreen = centroid
                                        )
                                        zoomScale = nextZoom
                                        panOffset = nextPan
                                    }
                                    event.changes.forEach { if (it.pressed) it.consume() }
                                } else if (!isPinching) {
                                    val primaryChange = event.changes.firstOrNull { it.pressed } ?: break
                                    totalScreenDrag += primaryChange.positionChange()
                                    if (!dragStarted && totalScreenDrag.getDistance() > touchSlop) {
                                        dragStarted = true
                                        if (candidateTarget is ConstructorDragTarget.Room) {
                                            rooms.getOrNull(candidateTarget.roomIdx)?.let {
                                                selectedRoomId = it.id
                                            }
                                        }
                                    }
                                    if (dragStarted) {
                                        primaryChange.consume()
                                        when (val target = candidateTarget) {
                                            is ConstructorDragTarget.Corner -> {
                                                val room = rooms.getOrNull(target.roomIdx)
                                                if (room != null) {
                                                    val pointerWorld = LayoutConstructorEngine.screenToWorld(
                                                        primaryChange.position,
                                                        zoomScale,
                                                        panOffset
                                                    )
                                                    val others = rooms.filterIndexed { i, _ -> i != target.roomIdx }
                                                    val snapped = LayoutConstructorEngine.resizeCornerWithSnap(
                                                        room = room,
                                                        cornerIndex = target.cornerIdx,
                                                        rawDragX = pointerWorld.x,
                                                        rawDragY = pointerWorld.y,
                                                        others = others,
                                                        snapToGridEnabled = snapToGrid,
                                                        snapToNeighborsEnabled = snapToNeighbors
                                                    )
                                                    rooms[target.roomIdx] = snapped.room
                                                    snapGuideX = snapped.guideX
                                                    snapGuideY = snapped.guideY
                                                }
                                            }
                                            is ConstructorDragTarget.Room -> {
                                                val room = rooms.getOrNull(target.roomIdx)
                                                if (room != null) {
                                                    val others = rooms.filterIndexed { i, _ -> i != target.roomIdx }
                                                    val worldDelta = totalScreenDrag / zoomScale
                                                    val snapped = LayoutConstructorEngine.moveWithSnap(
                                                        room = room,
                                                        rawLeft = target.startLeft + worldDelta.x,
                                                        rawTop = target.startTop + worldDelta.y,
                                                        others = others,
                                                        snapToGridEnabled = snapToGrid,
                                                        snapToNeighborsEnabled = snapToNeighbors
                                                    )
                                                    rooms[target.roomIdx] = snapped.room
                                                    snapGuideX = snapped.guideX
                                                    snapGuideY = snapped.guideY
                                                }
                                            }
                                            is ConstructorDragTarget.Pan -> {
                                                panOffset = target.startPan + totalScreenDrag
                                                snapGuideX = null
                                                snapGuideY = null
                                            }
                                            null -> Unit
                                        }
                                    }
                                }
                            }
                            snapGuideX = null
                            snapGuideY = null
                        }
                    }
            ) {
                // Сетка: 1 крупная клетка (96f) = 1 метр; тонкая линия каждые 0.5м
                val subStep = LayoutConstructorEngine.GRID_STEP_PX / 2f
                val meterStep = LayoutConstructorEngine.GRID_STEP_PX
                val minorColor = Color(0xFFF0F3F1)
                val majorColor = Color(0xFFCCD6D0)

                val worldLeft = -panOffset.x / zoomScale
                val worldRight = (size.width - panOffset.x) / zoomScale
                val worldTop = -panOffset.y / zoomScale
                val worldBottom = (size.height - panOffset.y) / zoomScale

                val startGx = (floor(worldLeft / subStep).toInt() - 1) * subStep
                var gx = startGx
                while (gx <= worldRight + subStep) {
                    val sx = gx * zoomScale + panOffset.x
                    val isMeter = abs(gx % meterStep) < 1f
                    drawLine(
                        color = if (isMeter) majorColor else minorColor,
                        start = Offset(sx, 0f),
                        end = Offset(sx, size.height),
                        strokeWidth = if (isMeter) 2.2f else 1f
                    )
                    gx += subStep
                }

                val startGy = (floor(worldTop / subStep).toInt() - 1) * subStep
                var gy = startGy
                while (gy <= worldBottom + subStep) {
                    val sy = gy * zoomScale + panOffset.y
                    val isMeter = abs(gy % meterStep) < 1f
                    drawLine(
                        color = if (isMeter) majorColor else minorColor,
                        start = Offset(0f, sy),
                        end = Offset(size.width, sy),
                        strokeWidth = if (isMeter) 2.2f else 1f
                    )
                    gy += subStep
                }

                // Направляющие линии прилипания
                val guideDash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                snapGuideX?.let { wx ->
                    val sx = wx * zoomScale + panOffset.x
                    drawLine(
                        color = BrightDoorGreen,
                        start = Offset(sx, 0f),
                        end = Offset(sx, size.height),
                        strokeWidth = 2.5f,
                        pathEffect = guideDash
                    )
                }
                snapGuideY?.let { wy ->
                    val sy = wy * zoomScale + panOffset.y
                    drawLine(
                        color = BrightDoorGreen,
                        start = Offset(0f, sy),
                        end = Offset(size.width, sy),
                        strokeWidth = 2.5f,
                        pathEffect = guideDash
                    )
                }

                // Отрисовка комнат и отдельных стен
                rooms.forEach { room ->
                    val isSelected = room.id == selectedRoomId
                    val screenVerts = room.worldVertices().map {
                        LayoutConstructorEngine.worldToScreen(it, zoomScale, panOffset)
                    }

                    if (screenVerts.size == 2) {
                        val a = screenVerts[0]
                        val b = screenVerts[1]
                        drawLine(
                            color = if (isSelected) DeepGreen else PureBlack,
                            start = a,
                            end = b,
                            strokeWidth = (if (isSelected) 8f else 6f) * zoomScale.coerceIn(0.75f, 1.35f),
                            cap = StrokeCap.Round
                        )
                        drawCircle(color = if (isSelected) DeepGreen else PureBlack, radius = 6f, center = a)
                        drawCircle(color = if (isSelected) DeepGreen else PureBlack, radius = 6f, center = b)
                        drawConstructorOpenings(room, screenVerts, zoomScale, areaSign = 1f)
                    } else if (screenVerts.size >= 3) {
                        val polyPath = Path().apply {
                            moveTo(screenVerts.first().x, screenVerts.first().y)
                            for (i in 1 until screenVerts.size) {
                                lineTo(screenVerts[i].x, screenVerts[i].y)
                            }
                            close()
                        }

                        drawPath(
                            path = polyPath,
                            color = if (isSelected) DeepGreenSoft.copy(alpha = 0.72f) else Color(0xFFF6F9F7)
                        )
                        drawPath(
                            path = polyPath,
                            color = if (isSelected) DeepGreen else PureBlack,
                            style = Stroke(
                                width = (if (isSelected) 6f else 4.5f) * zoomScale.coerceIn(0.75f, 1.35f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        var signedArea = 0f
                        for (i in screenVerts.indices) {
                            val a = screenVerts[i]
                            val b = screenVerts[(i + 1) % screenVerts.size]
                            signedArea += a.x * b.y - b.x * a.y
                        }
                        val areaSign = if (signedArea >= 0f) 1f else -1f

                        drawConstructorOpenings(room, screenVerts, zoomScale, areaSign)
                    }
                }

                // Непересекающиеся подписи комнат
                if (rooms.isNotEmpty()) {
                    val desiredCenters = rooms.map { room ->
                        LayoutConstructorEngine.worldToScreen(room.centroid(), zoomScale, panOffset)
                    }
                    val fontSizes = rooms.map { room ->
                        val minDimScreen = min(room.width, room.height) * zoomScale
                        (minDimScreen * 0.22f).coerceIn(18f, 40f)
                    }
                    val widths = rooms.mapIndexed { idx, room ->
                        roomNamePaint.textSize = fontSizes[idx]
                        roomNamePaint.measureText(room.name)
                    }
                    val resolvedCenters = LayoutConstructorEngine.resolveNonOverlappingLabelCenters(
                        desiredCenters = desiredCenters,
                        labelWidths = widths,
                        labelHeights = fontSizes
                    )
                    rooms.forEachIndexed { idx, room ->
                        val pt = resolvedCenters[idx]
                        val sz = fontSizes[idx]
                        val w = widths[idx]
                        roomNamePaint.textSize = sz
                        drawRect(
                            color = Color.White.copy(alpha = 0.80f),
                            topLeft = Offset(pt.x - w * 0.56f, pt.y - sz * 0.65f),
                            size = Size(w * 1.12f, sz * 1.2f)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            room.name,
                            pt.x,
                            pt.y + sz * 0.32f,
                            roomNamePaint
                        )
                    }
                }

                // Рамка и угловые ручки выбранной комнаты / стены
                selectedRoom?.let { sr ->
                    if (sr.shapeKind == ConstructorShapeKind.WALL_LINE) {
                        val pts = sr.worldVertices().map { LayoutConstructorEngine.worldToScreen(it, zoomScale, panOffset) }
                        pts.forEach { pt ->
                            drawCircle(color = PureWhite, radius = 14f, center = pt)
                            drawCircle(color = DeepGreen, radius = 10f, center = pt)
                        }
                    } else {
                        val sLeft = sr.left * zoomScale + panOffset.x
                        val sTop = sr.top * zoomScale + panOffset.y
                        val sWidth = sr.width * zoomScale
                        val sHeight = sr.height * zoomScale
                        drawRect(
                            color = DeepGreen.copy(alpha = 0.55f),
                            topLeft = Offset(sLeft, sTop),
                            size = Size(sWidth, sHeight),
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        )
                        val cornerPts = listOf(
                            Offset(sLeft, sTop),
                            Offset(sLeft + sWidth, sTop),
                            Offset(sLeft + sWidth, sTop + sHeight),
                            Offset(sLeft, sTop + sHeight)
                        )
                        cornerPts.forEach { pt ->
                            drawCircle(color = PureWhite, radius = 14f, center = pt)
                            drawCircle(color = DeepGreen, radius = 10f, center = pt)
                        }
                    }
                }
            }

            if (rooms.isEmpty()) {
                Text(
                    text = "Нажмите на форму сверху",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF5D625F),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    overflow = TextOverflow.Clip
                )
            }

            // Две галочки в углу: Привязка к сетке и Привязка к соседним комнатам
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = PureWhite.copy(alpha = 0.94f),
                border = BorderStroke(1.5.dp, PureBlack)
            ) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { snapToGrid = !snapToGrid }
                            .testTag("constructor_snap_grid_checkbox")
                            .padding(end = 6.dp)
                    ) {
                        Checkbox(
                            checked = snapToGrid,
                            onCheckedChange = { snapToGrid = it },
                            colors = CheckboxDefaults.colors(checkedColor = DeepGreen),
                            modifier = Modifier.size(34.dp)
                        )
                        Text("К сетке", style = MaterialTheme.typography.labelSmall, color = PureBlack)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { snapToNeighbors = !snapToNeighbors }
                            .testTag("constructor_snap_neighbors_checkbox")
                            .padding(end = 6.dp)
                    ) {
                        Checkbox(
                            checked = snapToNeighbors,
                            onCheckedChange = { snapToNeighbors = it },
                            colors = CheckboxDefaults.colors(checkedColor = DeepGreen),
                            modifier = Modifier.size(34.dp)
                        )
                        Text("К комнатам", style = MaterialTheme.typography.labelSmall, color = PureBlack)
                    }
                }
            }

            // Панель масштаба (− / % / +) в правом верхнем углу
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = PureWhite.copy(alpha = 0.94f),
                border = BorderStroke(1.5.dp, PureBlack)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    IconButton(
                        onClick = { applyStepZoom(zoomScale - 0.25f) },
                        enabled = zoomScale > LayoutConstructorEngine.MIN_ZOOM + 0.01f,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("constructor_zoom_out")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomOut,
                            contentDescription = "Уменьшить масштаб",
                            tint = if (zoomScale > LayoutConstructorEngine.MIN_ZOOM + 0.01f) PureBlack else BlackBorderSubtle
                        )
                    }
                    Text(
                        text = "${(zoomScale * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = PureBlack,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .clickable {
                                val def7 = LayoutConstructorEngine.default7x7Zoom(
                                    canvasSize.width.toFloat(),
                                    canvasSize.height.toFloat()
                                )
                                applyStepZoom(def7)
                                panOffset = Offset.Zero
                            }
                            .padding(horizontal = 4.dp, vertical = 10.dp)
                            .testTag("constructor_zoom_reset")
                    )
                    IconButton(
                        onClick = { applyStepZoom(zoomScale + 0.25f) },
                        enabled = zoomScale < LayoutConstructorEngine.MAX_ZOOM - 0.01f,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("constructor_zoom_in")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = "Увеличить масштаб",
                            tint = if (zoomScale < LayoutConstructorEngine.MAX_ZOOM - 0.01f) PureBlack else BlackBorderSubtle
                        )
                    }
                }
            }
        }

        // Панель зума-слайдера при нажатии на "📐 Масштаб"
        if (showZoomBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepGreenSoft)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Масштаб: ${(zoomScale * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = PureBlack
                )
                Slider(
                    value = zoomScale,
                    onValueChange = { applyStepZoom(it) },
                    valueRange = LayoutConstructorEngine.MIN_ZOOM..LayoutConstructorEngine.MAX_ZOOM,
                    colors = SliderDefaults.colors(
                        thumbColor = DeepGreen,
                        activeTrackColor = DeepGreen
                    ),
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        val def7 = LayoutConstructorEngine.default7x7Zoom(
                            canvasSize.width.toFloat(),
                            canvasSize.height.toFloat()
                        )
                        applyStepZoom(def7)
                        panOffset = Offset.Zero
                    }
                ) {
                    Text("7×7", color = DeepGreen)
                }
            }
        }

        // Нижняя панель инструментов:
        // 1 строка: ✋ Двигать | 📐 Масштаб | 🔄 Поворот | 🪞 Зеркало | 🗑 Удалить | 🎯 Центр
        // 2 строка: 🚪 Дверь | 🪟 Окно | 🚪 Балконная | 🪟 Г-окно
        // 3 строка (если выбрана): Названия комнат
        // 4: Большая кнопка «Готово»
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F8F6))
                .border(1.dp, BlackBorderSubtle)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConstructorEmojiChip(
                    label = "✋ Двигать",
                    selected = toolMode == ConstructorToolMode.MOVE_RESIZE,
                    onClick = { toolMode = ConstructorToolMode.MOVE_RESIZE }
                )
                ConstructorEmojiChip(
                    label = "📐 Масштаб",
                    selected = showZoomBar,
                    onClick = { showZoomBar = !showZoomBar }
                )
                if (selectedRoom != null && selectedIndex >= 0) {
                    ConstructorEmojiChip(
                        label = "🔄 Поворот",
                        selected = false,
                        onClick = {
                            rooms[selectedIndex] = LayoutConstructorEngine.rotate90Clockwise(selectedRoom)
                        }
                    )
                    ConstructorEmojiChip(
                        label = "🪞 Зеркало",
                        selected = false,
                        onClick = {
                            rooms[selectedIndex] = LayoutConstructorEngine.mirrorHorizontally(selectedRoom)
                        }
                    )
                    ConstructorEmojiChip(
                        label = "🗑 Удалить",
                        selected = false,
                        tintColor = WarningRed,
                        onClick = {
                            rooms.removeAt(selectedIndex)
                            selectedRoomId = rooms.lastOrNull()?.id
                        }
                    )
                }
                ConstructorEmojiChip(
                    label = "🎯 Центр",
                    selected = false,
                    onClick = {
                        if (rooms.isNotEmpty()) {
                            val (fitZoom, fitPan) = LayoutConstructorEngine.fitRoomsToViewport(
                                rooms = rooms.toList(),
                                viewportWidth = canvasSize.width.toFloat(),
                                viewportHeight = canvasSize.height.toFloat()
                            )
                            zoomScale = fitZoom
                            panOffset = fitPan
                        } else {
                            zoomScale = LayoutConstructorEngine.default7x7Zoom(
                                canvasSize.width.toFloat(),
                                canvasSize.height.toFloat()
                            )
                            panOffset = Offset.Zero
                        }
                    }
                )
            }

            // Строка 2: Двери и окна
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConstructorEmojiChip(
                    label = "🚪 Дверь",
                    selected = toolMode == ConstructorToolMode.PLACE_DOOR,
                    onClick = {
                        toolMode = if (toolMode == ConstructorToolMode.PLACE_DOOR) {
                            ConstructorToolMode.MOVE_RESIZE
                        } else {
                            ConstructorToolMode.PLACE_DOOR
                        }
                    }
                )
                ConstructorEmojiChip(
                    label = "🪟 Окно",
                    selected = toolMode == ConstructorToolMode.PLACE_WINDOW,
                    onClick = {
                        toolMode = if (toolMode == ConstructorToolMode.PLACE_WINDOW) {
                            ConstructorToolMode.MOVE_RESIZE
                        } else {
                            ConstructorToolMode.PLACE_WINDOW
                        }
                    }
                )
                ConstructorEmojiChip(
                    label = "🚪 Балконная",
                    selected = toolMode == ConstructorToolMode.PLACE_BALCONY_DOOR,
                    onClick = {
                        toolMode = if (toolMode == ConstructorToolMode.PLACE_BALCONY_DOOR) {
                            ConstructorToolMode.MOVE_RESIZE
                        } else {
                            ConstructorToolMode.PLACE_BALCONY_DOOR
                        }
                    }
                )
                ConstructorEmojiChip(
                    label = "🪟 Г-окно",
                    selected = toolMode == ConstructorToolMode.PLACE_CORNER_WINDOW,
                    onClick = {
                        toolMode = if (toolMode == ConstructorToolMode.PLACE_CORNER_WINDOW) {
                            ConstructorToolMode.MOVE_RESIZE
                        } else {
                            ConstructorToolMode.PLACE_CORNER_WINDOW
                        }
                    }
                )
            }

            if (selectedRoom != null && selectedIndex >= 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LayoutConstructorEngine.presetRoomNames.forEach { preset ->
                        val isCur = selectedRoom.name == preset
                        OutlinedButton(
                            onClick = {
                                rooms[selectedIndex] = selectedRoom.copy(name = preset)
                            },
                            modifier = Modifier.heightIn(min = 44.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, if (isCur) DeepGreen else PureBlack),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isCur) DeepGreen else PureWhite,
                                contentColor = if (isCur) PureWhite else PureBlack
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.bodySmall,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            customNameDraft = selectedRoom.name
                            showRenameDialog = true
                        },
                        modifier = Modifier.heightIn(min = 44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.5.dp, DeepGreen),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DeepGreenSoft,
                            contentColor = DeepGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Своё", style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Clip)
                    }
                }
            }

            // Большая кнопка «Готово» внизу
            ZamerPrimaryButton(
                text = "Готово",
                icon = Icons.Filled.Check,
                enabled = rooms.isNotEmpty(),
                onClick = {
                    if (rooms.isNotEmpty()) {
                        onComplete(LayoutConstructorEngine.toProjectDrawingPayload(rooms.toList()))
                    }
                },
                testTag = "constructor_done_button"
            )
        }
    }

    if (showRenameDialog && selectedRoom != null && selectedIndex >= 0) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = PureWhite,
            title = {
                Text(
                    text = "Название комнаты",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureBlack,
                    overflow = TextOverflow.Clip
                )
            },
            text = {
                ZamerInputField(
                    value = customNameDraft,
                    onValueChange = { customNameDraft = it },
                    label = "Название",
                    placeholder = "Например: Гардеробная"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = customNameDraft.trim()
                        if (trimmed.isNotEmpty() && selectedIndex in rooms.indices) {
                            rooms[selectedIndex] = rooms[selectedIndex].copy(name = trimmed)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("Сохранить", style = MaterialTheme.typography.labelLarge, color = DeepGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Отмена", style = MaterialTheme.typography.labelLarge, color = PureBlack)
                }
            }
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstructorOpenings(
    room: ConstructorRoom,
    screenVerts: List<Offset>,
    zoomScale: Float,
    areaSign: Float
) {
    room.openings.forEach { op ->
        val a = screenVerts[op.edgeIndex % screenVerts.size]
        val b = if (screenVerts.size == 2) {
            screenVerts[1]
        } else {
            screenVerts[(op.edgeIndex + 1) % screenVerts.size]
        }
        val dx = b.x - a.x
        val dy = b.y - a.y
        val segLen = hypot(dx, dy).coerceAtLeast(1f)
        val ux = dx / segLen
        val uy = dy / segLen
        val inX = if (areaSign >= 0f) -uy else uy
        val inY = if (areaSign >= 0f) ux else -ux

        val center = Offset(a.x + dx * op.fraction, a.y + dy * op.fraction)
        val halfWidth = min(segLen * 0.24f, 22f * zoomScale.coerceIn(0.7f, 1.5f))
        val p1 = Offset(center.x - ux * halfWidth, center.y - uy * halfWidth)
        val p2 = Offset(center.x + ux * halfWidth, center.y + uy * halfWidth)

        drawLine(
            color = PureWhite,
            start = p1,
            end = p2,
            strokeWidth = 9f * zoomScale.coerceIn(0.75f, 1.4f),
            cap = StrokeCap.Butt
        )

        when (op.kind) {
            ConstructorOpeningKind.WINDOW -> {
                val nOff = 3.5f
                drawLine(
                    color = Color(0xFF0284C7),
                    start = Offset(p1.x + inX * nOff, p1.y + inY * nOff),
                    end = Offset(p2.x + inX * nOff, p2.y + inY * nOff),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF0284C7),
                    start = Offset(p1.x - inX * nOff, p1.y - inY * nOff),
                    end = Offset(p2.x - inX * nOff, p2.y - inY * nOff),
                    strokeWidth = 3f
                )
            }
            ConstructorOpeningKind.CORNER_WINDOW -> {
                // Г-образное окно: две перпендикулярные створки
                val cornerMid = center
                val arm = halfWidth * 1.15f
                val armEnd = Offset(cornerMid.x + inX * arm, cornerMid.y + inY * arm)
                val perpShiftX = inX * 3.5f
                val perpShiftY = inY * 3.5f
                drawLine(
                    color = Color(0xFF0284C7),
                    start = Offset(p1.x + perpShiftX, p1.y + perpShiftY),
                    end = Offset(cornerMid.x + perpShiftX, cornerMid.y + perpShiftY),
                    strokeWidth = 3.5f
                )
                drawLine(
                    color = Color(0xFF0284C7),
                    start = Offset(cornerMid.x, cornerMid.y),
                    end = armEnd,
                    strokeWidth = 3.5f
                )
                drawLine(
                    color = Color(0xFF0284C7),
                    start = Offset(cornerMid.x + ux * 4f, cornerMid.y + uy * 4f),
                    end = Offset(armEnd.x + ux * 4f, armEnd.y + uy * 4f),
                    strokeWidth = 2.5f
                )
            }
            ConstructorOpeningKind.BALCONY_DOOR -> {
                // Балконный блок: окно + открывающаяся дверь рядом
                val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                val nOff = 3f
                drawLine(
                    color = Color(0xFF0284C7),
                    start = Offset(mid.x + inX * nOff, mid.y + inY * nOff),
                    end = Offset(p2.x + inX * nOff, p2.y + inY * nOff),
                    strokeWidth = 2.8f
                )
                drawLine(
                    color = Color(0xFF0284C7),
                    start = Offset(mid.x - inX * nOff, mid.y - inY * nOff),
                    end = Offset(p2.x - inX * nOff, p2.y - inY * nOff),
                    strokeWidth = 2.8f
                )
                val doorLen = hypot(mid.x - p1.x, mid.y - p1.y).coerceAtLeast(12f)
                val tip = Offset(p1.x + inX * doorLen, p1.y + inY * doorLen)
                drawLine(color = DeepGreen, start = p1, end = tip, strokeWidth = 3.5f, cap = StrokeCap.Round)
            }
            ConstructorOpeningKind.DOOR -> {
                val doorLen = (halfWidth * 2f).coerceAtLeast(14f * zoomScale.coerceIn(0.7f, 1.5f))
                val tip = Offset(p1.x + inX * doorLen, p1.y + inY * doorLen)
                drawLine(
                    color = DeepGreen,
                    start = p1,
                    end = tip,
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
                val angleToHinge = Math.toDegrees(
                    atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                ).toFloat()
                val angleToTip = Math.toDegrees(
                    atan2((tip.y - p1.y).toDouble(), (tip.x - p1.x).toDouble())
                ).toFloat()
                var sweep = angleToHinge - angleToTip
                while (sweep > 180f) sweep -= 360f
                while (sweep < -180f) sweep += 360f
                drawArc(
                    color = DeepGreen.copy(alpha = 0.7f),
                    startAngle = angleToTip,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(p1.x - doorLen, p1.y - doorLen),
                    size = Size(doorLen * 2f, doorLen * 2f),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

@Composable
private fun ShapePaletteButton(
    kind: ConstructorShapeKind,
    onClick: () -> Unit
) {
    val normVerts = remember(kind) { LayoutConstructorEngine.baseNormalizedVertices(kind) }
    val (defW, defH) = remember(kind) { LayoutConstructorEngine.defaultSize(kind) }

    Surface(
        modifier = Modifier
            .widthIn(min = 102.dp)
            .heightIn(min = 76.dp)
            .clickable(onClick = onClick)
            .testTag("shape_btn_${kind.name}"),
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.5.dp, PureBlack)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 42.dp, height = 28.dp)) {
                if (normVerts.size == 2) {
                    val y = size.height / 2f
                    drawLine(
                        color = DeepGreen,
                        start = Offset(4f, y),
                        end = Offset(size.width - 4f, y),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = DeepGreen, radius = 3.5f, center = Offset(4f, y))
                    drawCircle(color = DeepGreen, radius = 3.5f, center = Offset(size.width - 4f, y))
                } else {
                    val aspect = (defW / defH).coerceIn(0.6f, 2.2f)
                    val drawW: Float
                    val drawH: Float
                    if (size.width / size.height > aspect) {
                        drawH = size.height - 4f
                        drawW = drawH * aspect
                    } else {
                        drawW = size.width - 4f
                        drawH = drawW / aspect
                    }
                    val ox = (size.width - drawW) / 2f
                    val oy = (size.height - drawH) / 2f

                    val path = Path().apply {
                        normVerts.forEachIndexed { idx, pt ->
                            val px = ox + pt.x * drawW
                            val py = oy + pt.y * drawH
                            if (idx == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                    drawPath(path, color = DeepGreenSoft)
                    drawPath(
                        path,
                        color = DeepGreen,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            Text(
                text = kind.title,
                style = MaterialTheme.typography.labelSmall,
                color = PureBlack,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun ConstructorEmojiChip(
    label: String,
    selected: Boolean,
    tintColor: Color = PureBlack,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.5.dp,
            if (selected) DeepGreen else if (tintColor == WarningRed) WarningRed else PureBlack
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) DeepGreen else PureWhite,
            contentColor = if (selected) PureWhite else tintColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun NoPlanRoomsSection(
    onComplete: (List<NoPlanRoomSpec>) -> Unit,
    modifier: Modifier = Modifier
) {
    // По умолчанию снизу пусто!
    val rooms = remember { mutableStateListOf<NoPlanRoomSpec>() }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomShape by remember { mutableStateOf(SimpleRoomShape.RECT) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ZamerCard(borderColor = DeepGreen) {
            ZamerInputField(
                value = newRoomName,
                onValueChange = { newRoomName = it },
                label = "Название",
                placeholder = "Например: Комната",
                testTag = "no_plan_room_name_input"
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val compactShapes = listOf(
                    SimpleRoomShape.RECT to "4 стены",
                    SimpleRoomShape.L_SHAPE to "6",
                    SimpleRoomShape.U_SHAPE to "8"
                )
                compactShapes.forEach { (shape, label) ->
                    val selected = newRoomShape == shape
                    OutlinedButton(
                        onClick = { newRoomShape = shape },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("no_plan_shape_${shape.name}"),
                        shape = RoundedCornerShape(10.dp),
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
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            ZamerPrimaryButton(
                text = "Добавить",
                icon = Icons.Filled.Add,
                onClick = {
                    val title = newRoomName.trim().ifEmpty { "Комната ${rooms.size + 1}" }
                    rooms.add(NoPlanRoomSpec(title, newRoomShape))
                    newRoomName = ""
                    newRoomShape = SimpleRoomShape.RECT
                },
                testTag = "add_room_to_list_button"
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(rooms) { idx, spec ->
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
                        Text(
                            text = spec.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = PureBlack,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = "${spec.shape.wallCount} стен",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepGreen,
                            overflow = TextOverflow.Clip
                        )
                    }
                    IconButton(
                        onClick = { rooms.removeAt(idx) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Удалить",
                            tint = PureBlack
                        )
                    }
                }
            }
        }

        ZamerSecondaryButton(
            text = "Добавить ригель",
            icon = Icons.Filled.Add,
            borderColor = PureBlack,
            textColor = PureBlack,
            onClick = {
                val rigelNum = rooms.count { it.name.startsWith("Ригель") } + 1
                val rigelName = if (rigelNum == 1) "Ригель" else "Ригель $rigelNum"
                rooms.add(NoPlanRoomSpec(rigelName, SimpleRoomShape.RECT))
            },
            testTag = "add_rigel_button"
        )

        ZamerPrimaryButton(
            text = "Создать объект",
            icon = Icons.Filled.Check,
            enabled = rooms.isNotEmpty(),
            onClick = { onComplete(rooms.toList()) },
            testTag = "create_no_plan_button"
        )
    }
}
