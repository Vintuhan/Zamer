package com.example.domain

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

enum class ConstructorShapeKind(val title: String) {
    SQUARE("Квадрат"),
    RECTANGLE("Прямоугольник"),
    L_SHAPE("Г-образная"),
    U_SHAPE("П-образная"),
    WALL_LINE("Просто стена"),
    TRAPEZOID("Косая (трапеция)"),
    CHAMFERED("Со скошенным углом"),
    CORRIDOR("Узкий коридор"),
    BAY_WINDOW("Эркер")
}

enum class ConstructorOpeningKind(val title: String) {
    DOOR("🚪 Дверь"),
    WINDOW("🪟 Окно"),
    BALCONY_DOOR("🚪 Балконная"),
    CORNER_WINDOW("🪟 Г-окно")
}

data class ConstructorOpening(
    val id: String,
    val edgeIndex: Int,
    val fraction: Float,
    val kind: ConstructorOpeningKind
)

data class ConstructorRoom(
    val id: String,
    val name: String,
    val shapeKind: ConstructorShapeKind,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val normalizedVertices: List<Offset>,
    val openings: List<ConstructorOpening> = emptyList()
) {
    fun worldVertices(): List<Offset> =
        normalizedVertices.map { v -> Offset(left + v.x * width, top + v.y * height) }

    fun worldSegments(): List<SnappedSegment> {
        val pts = worldVertices()
        if (pts.size < 2) return emptyList()
        if (pts.size == 2) return listOf(SnappedSegment(pts[0], pts[1]))
        return pts.indices.map { i ->
            SnappedSegment(pts[i], pts[(i + 1) % pts.size])
        }
    }

    fun centroid(): Offset {
        val pts = worldVertices()
        if (pts.isEmpty()) return Offset(left + width / 2f, top + height / 2f)
        if (pts.size == 2) {
            return Offset((pts[0].x + pts[1].x) / 2f, (pts[0].y + pts[1].y) / 2f)
        }
        var signedArea = 0f
        var cx = 0f
        var cy = 0f
        for (i in pts.indices) {
            val p0 = pts[i]
            val p1 = pts[(i + 1) % pts.size]
            val cross = p0.x * p1.y - p1.x * p0.y
            signedArea += cross
            cx += (p0.x + p1.x) * cross
            cy += (p0.y + p1.y) * cross
        }
        val denom = 3f * signedArea
        return if (abs(denom) > 1e-3f) {
            Offset(cx / denom, cy / denom)
        } else {
            Offset(left + width / 2f, top + height / 2f)
        }
    }
}

data class ConstructorSnapResult(
    val room: ConstructorRoom,
    val guideX: Float? = null,
    val guideY: Float? = null
)

object LayoutConstructorEngine {
    // 1 grid cell = 1 meter (96 px) -> fits ~7x7 cells on default mobile viewport!
    const val GRID_STEP_PX = 96f
    const val WALL_SNAP_THRESHOLD_PX = 24f
    const val MIN_ROOM_SIZE_PX = 96f
    const val MIN_ZOOM = 0.25f
    const val MAX_ZOOM = 3.2f

    fun worldToScreen(world: Offset, zoom: Float, pan: Offset): Offset =
        Offset(world.x * zoom + pan.x, world.y * zoom + pan.y)

    fun screenToWorld(screen: Offset, zoom: Float, pan: Offset): Offset {
        val safeZoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        return Offset((screen.x - pan.x) / safeZoom, (screen.y - pan.y) / safeZoom)
    }

    fun zoomAroundAnchor(
        currentZoom: Float,
        currentPan: Offset,
        zoomChange: Float,
        panChange: Offset,
        anchorScreen: Offset
    ): Pair<Float, Offset> {
        val oldZoom = currentZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val newZoom = (oldZoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val factor = newZoom / oldZoom
        val prevAnchor = anchorScreen - panChange
        val newPan = anchorScreen - (prevAnchor - currentPan) * factor
        return newZoom to newPan
    }

    /**
     * Default viewport scale so 7 x 7 grid cells (1 cell = 1 meter) fit comfortably on screen.
     */
    fun default7x7Zoom(viewportWidth: Float, viewportHeight: Float): Float {
        if (viewportWidth <= 0f || viewportHeight <= 0f) return 1f
        val base = min(viewportWidth, viewportHeight) / (7f * GRID_STEP_PX)
        return base.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    fun fitRoomsToViewport(
        rooms: List<ConstructorRoom>,
        viewportWidth: Float,
        viewportHeight: Float,
        maxInitialZoom: Float = 1.6f
    ): Pair<Float, Offset> {
        if (rooms.isEmpty() || viewportWidth <= 0f || viewportHeight <= 0f) {
            return default7x7Zoom(viewportWidth, viewportHeight) to Offset.Zero
        }
        val minX = rooms.minOf { it.left }
        val maxX = rooms.maxOf { it.left + it.width }
        val minY = rooms.minOf { it.top }
        val maxY = rooms.maxOf { it.top + it.height }
        val bw = (maxX - minX).coerceAtLeast(1f)
        val bh = (maxY - minY).coerceAtLeast(1f)
        val fitScale = min(
            (viewportWidth * 0.78f) / bw,
            (viewportHeight * 0.78f) / bh
        ).coerceIn(MIN_ZOOM, maxInitialZoom)
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val pan = Offset(
            viewportWidth / 2f - centerX * fitScale,
            viewportHeight / 2f - centerY * fitScale
        )
        return fitScale to pan
    }

    val presetRoomNames: List<String> = listOf(
        "Комната",
        "Кухня",
        "Коридор",
        "Санузел",
        "Балкон",
        "Прихожая",
        "Спальня",
        "Гостиная",
        "Кладовая",
        "Стена",
        "Ригель"
    )

    fun baseNormalizedVertices(kind: ConstructorShapeKind): List<Offset> = when (kind) {
        ConstructorShapeKind.SQUARE,
        ConstructorShapeKind.RECTANGLE,
        ConstructorShapeKind.CORRIDOR -> listOf(
            Offset(0f, 0f),
            Offset(1f, 0f),
            Offset(1f, 1f),
            Offset(0f, 1f)
        )
        ConstructorShapeKind.WALL_LINE -> listOf(
            Offset(0f, 0.5f),
            Offset(1f, 0.5f)
        )
        ConstructorShapeKind.L_SHAPE -> listOf(
            Offset(0f, 0f),
            Offset(1f, 0f),
            Offset(1f, 0.45f),
            Offset(0.46f, 0.45f),
            Offset(0.46f, 1f),
            Offset(0f, 1f)
        )
        ConstructorShapeKind.U_SHAPE -> listOf(
            Offset(0f, 0f),
            Offset(1f, 0f),
            Offset(1f, 1f),
            Offset(0.68f, 1f),
            Offset(0.68f, 0.44f),
            Offset(0.32f, 0.44f),
            Offset(0.32f, 1f),
            Offset(0f, 1f)
        )
        ConstructorShapeKind.TRAPEZOID -> listOf(
            Offset(0f, 0f),
            Offset(0.70f, 0f),
            Offset(1f, 1f),
            Offset(0f, 1f)
        )
        ConstructorShapeKind.CHAMFERED -> listOf(
            Offset(0f, 0f),
            Offset(0.66f, 0f),
            Offset(1f, 0.34f),
            Offset(1f, 1f),
            Offset(0f, 1f)
        )
        ConstructorShapeKind.BAY_WINDOW -> listOf(
            Offset(0f, 0.28f),
            Offset(0.22f, 0.28f),
            Offset(0.34f, 0f),
            Offset(0.66f, 0f),
            Offset(0.78f, 0.28f),
            Offset(1f, 0.28f),
            Offset(1f, 1f),
            Offset(0f, 1f)
        )
    }

    fun defaultSize(kind: ConstructorShapeKind): Pair<Float, Float> = when (kind) {
        ConstructorShapeKind.SQUARE -> (GRID_STEP_PX * 3f) to (GRID_STEP_PX * 3f)
        ConstructorShapeKind.RECTANGLE -> (GRID_STEP_PX * 4f) to (GRID_STEP_PX * 3f)
        ConstructorShapeKind.L_SHAPE -> (GRID_STEP_PX * 4f) to (GRID_STEP_PX * 4f)
        ConstructorShapeKind.U_SHAPE -> (GRID_STEP_PX * 5f) to (GRID_STEP_PX * 4f)
        ConstructorShapeKind.WALL_LINE -> (GRID_STEP_PX * 4f) to GRID_STEP_PX
        ConstructorShapeKind.TRAPEZOID -> (GRID_STEP_PX * 4f) to (GRID_STEP_PX * 3f)
        ConstructorShapeKind.CHAMFERED -> (GRID_STEP_PX * 4f) to (GRID_STEP_PX * 3f)
        ConstructorShapeKind.CORRIDOR -> (GRID_STEP_PX * 5f) to (GRID_STEP_PX * 1.5f)
        ConstructorShapeKind.BAY_WINDOW -> (GRID_STEP_PX * 4f) to (GRID_STEP_PX * 3.5f)
    }

    fun defaultName(kind: ConstructorShapeKind): String = when (kind) {
        ConstructorShapeKind.CORRIDOR -> "Коридор"
        ConstructorShapeKind.BAY_WINDOW -> "Гостиная"
        ConstructorShapeKind.WALL_LINE -> "Стена"
        else -> "Комната"
    }

    fun createRoom(
        id: String,
        kind: ConstructorShapeKind,
        centerX: Float,
        centerY: Float,
        existingCount: Int,
        snapToGridEnabled: Boolean = true
    ): ConstructorRoom {
        val (w, h) = defaultSize(kind)
        val cascade = (existingCount % 4) * (GRID_STEP_PX * 0.5f)
        val rawLeft = centerX - w / 2f + cascade
        val rawTop = centerY - h / 2f + cascade
        return ConstructorRoom(
            id = id,
            name = defaultName(kind),
            shapeKind = kind,
            left = if (snapToGridEnabled) snapToGrid(rawLeft) else rawLeft,
            top = if (snapToGridEnabled) snapToGrid(rawTop) else rawTop,
            width = w,
            height = h,
            normalizedVertices = baseNormalizedVertices(kind),
            openings = emptyList()
        )
    }

    fun snapToGrid(value: Float, step: Float = GRID_STEP_PX): Float =
        round(value / step) * step

    fun rotate90Clockwise(room: ConstructorRoom): ConstructorRoom {
        val cx = room.left + room.width / 2f
        val cy = room.top + room.height / 2f
        val newWidth = room.height
        val newHeight = room.width
        val newLeft = snapToGrid(cx - newWidth / 2f)
        val newTop = snapToGrid(cy - newHeight / 2f)
        val rotatedVerts = room.normalizedVertices.map { v ->
            Offset((1f - v.y).coerceIn(0f, 1f), v.x.coerceIn(0f, 1f))
        }
        return room.copy(
            left = newLeft,
            top = newTop,
            width = newWidth,
            height = newHeight,
            normalizedVertices = rotatedVerts
        )
    }

    fun mirrorHorizontally(room: ConstructorRoom): ConstructorRoom {
        val n = room.normalizedVertices.size
        val mirroredVerts = room.normalizedVertices
            .reversed()
            .map { v -> Offset((1f - v.x).coerceIn(0f, 1f), v.y.coerceIn(0f, 1f)) }
        val mirroredOpenings = if (n >= 3) {
            room.openings.map { op ->
                val mappedEdge = Math.floorMod(n - 2 - op.edgeIndex, n)
                op.copy(
                    edgeIndex = mappedEdge,
                    fraction = (1f - op.fraction).coerceIn(0.12f, 0.88f)
                )
            }
        } else {
            room.openings.map { op ->
                op.copy(fraction = (1f - op.fraction).coerceIn(0.12f, 0.88f))
            }
        }
        return room.copy(
            normalizedVertices = mirroredVerts,
            openings = mirroredOpenings
        )
    }

    private fun collectOtherEdgeLines(others: List<ConstructorRoom>): Pair<List<Float>, List<Float>> {
        val xs = mutableListOf<Float>()
        val ys = mutableListOf<Float>()
        others.forEach { other ->
            val pts = other.worldVertices()
            pts.forEach { p ->
                xs += p.x
                ys += p.y
            }
            if (other.normalizedVertices.size >= 3) {
                xs += other.left
                xs += other.left + other.width
                ys += other.top
                ys += other.top + other.height
            }
        }
        return xs.distinct() to ys.distinct()
    }

    fun moveWithSnap(
        room: ConstructorRoom,
        rawLeft: Float,
        rawTop: Float,
        others: List<ConstructorRoom>,
        snapToGridEnabled: Boolean = true,
        snapToNeighborsEnabled: Boolean = true
    ): ConstructorSnapResult {
        var bestDx: Float? = null
        var bestAbsDx = WALL_SNAP_THRESHOLD_PX
        var guideX: Float? = null

        var bestDy: Float? = null
        var bestAbsDy = WALL_SNAP_THRESHOLD_PX
        var guideY: Float? = null

        if (snapToNeighborsEnabled && others.isNotEmpty()) {
            val (otherXs, otherYs) = collectOtherEdgeLines(others)
            val localXs = (room.normalizedVertices.map { it.x * room.width } +
                if (room.normalizedVertices.size >= 3) listOf(0f, room.width) else emptyList()).distinct()
            val localYs = (room.normalizedVertices.map { it.y * room.height } +
                if (room.normalizedVertices.size >= 3) listOf(0f, room.height) else emptyList()).distinct()

            for (ox in otherXs) {
                for (lx in localXs) {
                    val dx = ox - (rawLeft + lx)
                    if (abs(dx) < bestAbsDx) {
                        bestAbsDx = abs(dx)
                        bestDx = dx
                        guideX = ox
                    }
                }
            }

            for (oy in otherYs) {
                for (ly in localYs) {
                    val dy = oy - (rawTop + ly)
                    if (abs(dy) < bestAbsDy) {
                        bestAbsDy = abs(dy)
                        bestDy = dy
                        guideY = oy
                    }
                }
            }
        }

        val finalLeft = when {
            bestDx != null -> rawLeft + bestDx
            snapToGridEnabled -> snapToGrid(rawLeft)
            else -> rawLeft
        }
        val finalTop = when {
            bestDy != null -> rawTop + bestDy
            snapToGridEnabled -> snapToGrid(rawTop)
            else -> rawTop
        }

        return ConstructorSnapResult(
            room = room.copy(left = finalLeft, top = finalTop),
            guideX = guideX,
            guideY = guideY
        )
    }

    /**
     * cornerIndex: 0 = TopLeft, 1 = TopRight, 2 = BottomRight, 3 = BottomLeft
     */
    fun resizeCornerWithSnap(
        room: ConstructorRoom,
        cornerIndex: Int,
        rawDragX: Float,
        rawDragY: Float,
        others: List<ConstructorRoom>,
        snapToGridEnabled: Boolean = true,
        snapToNeighborsEnabled: Boolean = true
    ): ConstructorSnapResult {
        val (otherXs, otherYs) = if (snapToNeighborsEnabled && others.isNotEmpty()) {
            collectOtherEdgeLines(others)
        } else {
            emptyList<Float>() to emptyList()
        }
        val right = room.left + room.width
        val bottom = room.top + room.height
        val minSize = if (room.shapeKind == ConstructorShapeKind.WALL_LINE) 48f else MIN_ROOM_SIZE_PX

        fun snapCoord(raw: Float, candidates: List<Float>): Pair<Float, Float?> {
            var bestTarget: Float? = null
            var bestDist = WALL_SNAP_THRESHOLD_PX
            for (c in candidates) {
                val d = abs(c - raw)
                if (d < bestDist) {
                    bestDist = d
                    bestTarget = c
                }
            }
            return when {
                bestTarget != null -> bestTarget to bestTarget
                snapToGridEnabled -> snapToGrid(raw) to null
                else -> raw to null
            }
        }

        val movesLeft = cornerIndex == 0 || cornerIndex == 3
        val movesTop = cornerIndex == 0 || cornerIndex == 1

        val (snappedX, guideX) = snapCoord(rawDragX, otherXs)
        val (snappedY, guideY) = snapCoord(rawDragY, otherYs)

        val newLeft: Float
        val newRight: Float
        if (movesLeft) {
            newLeft = min(snappedX, right - minSize)
            newRight = right
        } else {
            newLeft = room.left
            newRight = max(snappedX, room.left + minSize)
        }

        val newTop: Float
        val newBottom: Float
        if (movesTop) {
            newTop = min(snappedY, bottom - minSize)
            newBottom = bottom
        } else {
            newTop = room.top
            newBottom = max(snappedY, room.top + minSize)
        }

        return ConstructorSnapResult(
            room = room.copy(
                left = newLeft,
                top = newTop,
                width = (newRight - newLeft).coerceAtLeast(minSize),
                height = (newBottom - newTop).coerceAtLeast(minSize)
            ),
            guideX = guideX,
            guideY = guideY
        )
    }

    fun isPointInsideRoom(p: Offset, room: ConstructorRoom): Boolean {
        val poly = room.worldVertices()
        if (poly.size == 2) {
            val (dist, _) = projectPointOnSegment(p, poly[0], poly[1])
            return dist <= 38f
        }
        if (poly.size < 3) return false
        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val xi = poly[i].x
            val yi = poly[i].y
            val xj = poly[j].x
            val yj = poly[j].y
            val intersect = ((yi > p.y) != (yj > p.y)) &&
                (p.x < (xj - xi) * (p.y - yi) / ((yj - yi).takeIf { abs(it) > 1e-6f } ?: 1e-6f) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    fun projectPointOnSegment(p: Offset, a: Offset, b: Offset): Pair<Float, Float> {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-4f) return hypot(p.x - a.x, p.y - a.y) to 0.5f
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = a.x + t * dx
        val projY = a.y + t * dy
        return hypot(p.x - projX, p.y - projY) to t
    }

    fun toggleOpeningAtTap(
        rooms: List<ConstructorRoom>,
        tapWorld: Offset,
        kind: ConstructorOpeningKind,
        newOpeningId: String,
        maxDistPx: Float = 44f
    ): Pair<Int, ConstructorRoom>? {
        var bestRoomIdx = -1
        var bestEdgeIdx = -1
        var bestDist = maxDistPx
        var bestFraction = 0.5f

        rooms.forEachIndexed { rIdx, room ->
            val segs = room.worldSegments()
            segs.forEachIndexed { eIdx, seg ->
                val (dist, t) = projectPointOnSegment(tapWorld, seg.start, seg.end)
                if (dist < bestDist) {
                    bestDist = dist
                    bestRoomIdx = rIdx
                    bestEdgeIdx = eIdx
                    bestFraction = t.coerceIn(0.16f, 0.84f)
                }
            }
        }
        if (bestRoomIdx < 0) return null

        val targetRoom = rooms[bestRoomIdx]
        val existingIdx = targetRoom.openings.indexOfFirst { op ->
            op.edgeIndex == bestEdgeIdx && abs(op.fraction - bestFraction) <= 0.16f
        }
        val updatedOpenings = if (existingIdx >= 0) {
            val existing = targetRoom.openings[existingIdx]
            if (existing.kind == kind) {
                targetRoom.openings.filterIndexed { i, _ -> i != existingIdx }
            } else {
                targetRoom.openings.mapIndexed { i, op ->
                    if (i == existingIdx) op.copy(kind = kind) else op
                }
            }
        } else {
            targetRoom.openings + ConstructorOpening(
                id = newOpeningId,
                edgeIndex = bestEdgeIdx,
                fraction = bestFraction,
                kind = kind
            )
        }
        return bestRoomIdx to targetRoom.copy(openings = updatedOpenings)
    }

    /**
     * Automatically nudges overlapping screen-space room labels so adjacent or nested rooms never collide visually.
     */
    fun resolveNonOverlappingLabelCenters(
        desiredCenters: List<Offset>,
        labelWidths: List<Float>,
        labelHeights: List<Float>
    ): List<Offset> {
        if (desiredCenters.isEmpty()) return emptyList()
        val placed = mutableListOf<Rect>()
        val result = mutableListOf<Offset>()

        for (i in desiredCenters.indices) {
            val base = desiredCenters[i]
            val halfW = (labelWidths.getOrElse(i) { 80f } * 0.55f).coerceAtLeast(24f)
            val halfH = (labelHeights.getOrElse(i) { 28f } * 0.65f).coerceAtLeast(14f)

            val offsets = listOf(
                Offset(0f, 0f),
                Offset(0f, -halfH * 2.2f),
                Offset(0f, halfH * 2.2f),
                Offset(halfW * 1.4f, 0f),
                Offset(-halfW * 1.4f, 0f),
                Offset(0f, -halfH * 4.2f),
                Offset(0f, halfH * 4.2f)
            )

            var chosen = base
            for (d in offsets) {
                val cand = base + d
                val rect = Rect(cand.x - halfW, cand.y - halfH, cand.x + halfW, cand.y + halfH)
                if (placed.none { it.overlaps(rect) }) {
                    chosen = cand
                    break
                }
            }
            placed.add(Rect(chosen.x - halfW, chosen.y - halfH, chosen.x + halfW, chosen.y + halfH))
            result.add(chosen)
        }
        return result
    }

    fun toProjectDrawingPayload(rooms: List<ConstructorRoom>): List<Pair<String, List<SnappedSegment>>> =
        rooms.map { room -> room.name to room.worldSegments() }

    fun roomsFromSegments(roomSegments: List<Pair<String, List<SnappedSegment>>>): List<ConstructorRoom> {
        return roomSegments.mapIndexedNotNull { idx, (name, segments) ->
            if (segments.isEmpty()) return@mapIndexedNotNull null
            if (segments.size == 1) {
                val seg = segments.first()
                val minX = min(seg.start.x, seg.end.x)
                val maxX = max(seg.start.x, seg.end.x)
                val midY = (seg.start.y + seg.end.y) / 2f
                val w = (maxX - minX).coerceAtLeast(GRID_STEP_PX)
                val h = max(abs(seg.end.y - seg.start.y), 48f)
                val top = min(seg.start.y, seg.end.y) - (if (h == 48f) 24f else 0f)
                val norm = listOf(
                    Offset(((seg.start.x - minX) / w).coerceIn(0f, 1f), ((seg.start.y - top) / h).coerceIn(0f, 1f)),
                    Offset(((seg.end.x - minX) / w).coerceIn(0f, 1f), ((seg.end.y - top) / h).coerceIn(0f, 1f))
                )
                return@mapIndexedNotNull ConstructorRoom(
                    id = "restored_${idx}_${name.hashCode()}",
                    name = name,
                    shapeKind = ConstructorShapeKind.WALL_LINE,
                    left = minX,
                    top = if (abs(seg.end.y - seg.start.y) < 1f) midY - 24f else top,
                    width = w,
                    height = h,
                    normalizedVertices = norm,
                    openings = emptyList()
                )
            }
            if (segments.size < 3) return@mapIndexedNotNull null
            val pts = segments.map { it.start }
            val minX = pts.minOf { it.x }
            val maxX = pts.maxOf { it.x }
            val minY = pts.minOf { it.y }
            val maxY = pts.maxOf { it.y }
            val w = (maxX - minX).coerceAtLeast(MIN_ROOM_SIZE_PX)
            val h = (maxY - minY).coerceAtLeast(MIN_ROOM_SIZE_PX)
            val norm = pts.map { p ->
                Offset(
                    x = ((p.x - minX) / w).coerceIn(0f, 1f),
                    y = ((p.y - minY) / h).coerceIn(0f, 1f)
                )
            }
            val kind = when (pts.size) {
                5 -> ConstructorShapeKind.CHAMFERED
                6 -> ConstructorShapeKind.L_SHAPE
                8 -> ConstructorShapeKind.U_SHAPE
                else -> if (abs(w - h) < 20f) ConstructorShapeKind.SQUARE else ConstructorShapeKind.RECTANGLE
            }
            ConstructorRoom(
                id = "restored_${idx}_${name.hashCode()}",
                name = name,
                shapeKind = kind,
                left = minX,
                top = minY,
                width = w,
                height = h,
                normalizedVertices = norm,
                openings = emptyList()
            )
        }
    }
}
