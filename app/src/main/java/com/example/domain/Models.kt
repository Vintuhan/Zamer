package com.example.domain

import androidx.compose.ui.geometry.Offset
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

enum class MeasurementCategory {
    WALLS,
    FLOOR,
    CORNERS
}

enum class OpeningType {
    DOOR,
    WINDOW
}

enum class SimpleRoomShape(
    val title: String,
    val shortTitle: String,
    val wallCount: Int,
    val shapeKey: String
) {
    RECT("Квадратная / прямоугольная", "Прямоуг. (4)", 4, "RECT"),
    L_SHAPE("Г-образная", "Г-образная (6)", 6, "L_SHAPE"),
    U_SHAPE("П-образная", "П-образная (8)", 8, "U_SHAPE");

    companion object {
        fun fromKey(key: String?, wallCount: Int): SimpleRoomShape = when (key) {
            U_SHAPE.shapeKey -> U_SHAPE
            L_SHAPE.shapeKey -> L_SHAPE
            RECT.shapeKey -> RECT
            else -> when (wallCount) {
                8 -> U_SHAPE
                6 -> L_SHAPE
                else -> RECT
            }
        }
    }
}

data class NoPlanRoomSpec(
    val name: String,
    val shape: SimpleRoomShape
)

enum class EntryDoorSide {
    NONE,
    BOTTOM,
    LEFT,
    RIGHT,
    TOP
}

enum class TemplateHouseGroup(val title: String) {
    ALL("Все дома"),
    NEW_BUILD("Новостройка"),
    SECONDARY("Вторичка / СССР"),
    SPECIAL("Особые")
}

data class WallMeasurement(
    val index: Int,
    val label: String,
    val hint: String,
    val lengthMeters: Double = 0.0
)

data class OpeningItem(
    val id: String,
    val type: OpeningType,
    val count: Int,
    val widthMeters: Double,
    val heightMeters: Double,
    val label: String = "",
    val directAreaSqm: Double? = null
) {
    val totalAreaSqm: Double
        get() = if (directAreaSqm != null) {
            directAreaSqm.coerceAtLeast(0.0)
        } else {
            (count.coerceAtLeast(0) * widthMeters.coerceAtLeast(0.0) * heightMeters.coerceAtLeast(0.0))
        }
}

enum class BlueprintOpeningKind {
    WINDOW,
    DOOR,
    BALCONY_DOOR
}

data class BlueprintOpening(
    val edgeIndex: Int,
    val centerFraction: Float = 0.5f,
    val widthFraction: Float = 0.32f,
    val kind: BlueprintOpeningKind
)

enum class BlueprintFixtureKind {
    NONE,
    KITCHEN,
    BATH,
    TOILET,
    COMBINED_BATH,
    WARDROBE
}

data class RoomGeometryRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String = "",
    val hasDoorBottom: Boolean = false,
    val hasDoorRight: Boolean = false,
    val hasDoorLeft: Boolean = false,
    val hasDoorTop: Boolean = false,
    val hasWindowTop: Boolean = false,
    val hasWindowBottom: Boolean = false,
    val hasWindowLeft: Boolean = false,
    val hasWindowRight: Boolean = false,
    val entryDoorSide: EntryDoorSide = EntryDoorSide.NONE,
    val isBalcony: Boolean = false,
    val polygon: List<Offset> = emptyList(),
    val windowEdgeIndices: List<Int> = emptyList(),
    val doorEdgeIndices: List<Int> = emptyList(),
    val entryDoorEdgeIndex: Int = -1,
    val labelOffset: Offset? = null,
    val openings: List<BlueprintOpening> = emptyList(),
    val typicalAreaSqm: Double? = null,
    val fixtureKind: BlueprintFixtureKind = BlueprintFixtureKind.NONE
) {
    fun resolvedPolygon(): List<Offset> =
        if (polygon.size >= 3) {
            polygon
        } else {
            listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(right, bottom),
                Offset(left, bottom)
            )
        }

    fun resolvedLabelCenter(): Offset {
        if (labelOffset != null) return labelOffset
        val poly = resolvedPolygon()
        return Offset(
            x = poly.map { it.x }.average().toFloat(),
            y = poly.map { it.y }.average().toFloat()
        )
    }

    fun hasAnyEntryDoor(): Boolean = entryDoorSide != EntryDoorSide.NONE || entryDoorEdgeIndex >= 0
}

data class TemplateRoomSpec(
    val name: String,
    val wallCount: Int = 4,
    val defaultInnerCorners: Int = 4,
    val defaultOuterCorners: Int = 0,
    val wallHints: List<String> = listOf(
        "От входной двери направо. Мерь по низу, у пола",
        "Вторая стена (часто с окном). Мерь от угла до угла у пола",
        "Дальняя стена напротив первой. Мерь по низу, у плинтуса",
        "Последняя стена к двери. Мерь от угла до угла по полу"
    )
)

data class ApartmentTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val roomCountCategory: Int, // 0 = studio, 1, 2, 3, 4 = 4+/special
    val houseGroup: TemplateHouseGroup,
    val planAspectRatio: Float = 1.45f, // width / height for true proportional rendering
    val rooms: List<TemplateRoomSpec>,
    val schematicRooms: List<RoomGeometryRect>,
    val hasNonRectangularShape: Boolean = false
)

data class LayoutPlanVariant(
    val title: String,
    val badge: String,
    val rooms: List<RoomGeometryRect>
)

object FloorPlanVariants {

    fun buildVariants(template: ApartmentTemplate): List<LayoutPlanVariant> {
        val original = template.schematicRooms
        val variants = mutableListOf(
            LayoutPlanVariant(
                title = "Оригинал",
                badge = "1 · Изначальная",
                rooms = original
            ),
            LayoutPlanVariant(
                title = "Зеркальный",
                badge = "2 · Зеркало",
                rooms = mirrorHorizontally(original)
            )
        )
        val altEntry = relocateEntryDoorInHallway(original)
        if (altEntry != null) {
            variants.add(
                LayoutPlanVariant(
                    title = "Другой вход",
                    badge = "3 · Дверь сбоку/снизу",
                    rooms = altEntry
                )
            )
            variants.add(
                LayoutPlanVariant(
                    title = "Зеркало + вход",
                    badge = "4 · Зеркальный + другой вход",
                    rooms = mirrorHorizontally(altEntry)
                )
            )
        }
        return variants
    }

    fun mirrorHorizontally(rooms: List<RoomGeometryRect>): List<RoomGeometryRect> {
        return rooms.map { r ->
            val mirroredOpenings = if (r.polygon.size >= 3) {
                r.openings
            } else {
                r.openings.map { op ->
                    val newEdge = when (op.edgeIndex) {
                        1 -> 3
                        3 -> 1
                        else -> op.edgeIndex
                    }
                    op.copy(
                        edgeIndex = newEdge,
                        centerFraction = (1f - op.centerFraction).coerceIn(0.08f, 0.92f)
                    )
                }
            }
            r.copy(
                left = (1f - r.right).coerceIn(0f, 1f),
                right = (1f - r.left).coerceIn(0f, 1f),
                hasDoorLeft = r.hasDoorRight,
                hasDoorRight = r.hasDoorLeft,
                hasWindowLeft = r.hasWindowRight,
                hasWindowRight = r.hasWindowLeft,
                entryDoorSide = when (r.entryDoorSide) {
                    EntryDoorSide.LEFT -> EntryDoorSide.RIGHT
                    EntryDoorSide.RIGHT -> EntryDoorSide.LEFT
                    else -> r.entryDoorSide
                },
                polygon = r.polygon.map { p -> Offset((1f - p.x).coerceIn(0f, 1f), p.y) },
                labelOffset = r.labelOffset?.let { p -> Offset((1f - p.x).coerceIn(0f, 1f), p.y) },
                openings = mirroredOpenings
            )
        }
    }

    /**
     * Moves the hallway's bright green entry door to another valid exterior wall
     * (supports both rectangular rooms and arbitrary polygonal hallways).
     */
    fun relocateEntryDoorInHallway(rooms: List<RoomGeometryRect>): List<RoomGeometryRect>? {
        val entryIdx = rooms.indexOfFirst { it.hasAnyEntryDoor() }
        if (entryIdx < 0) return null
        val r = rooms[entryIdx]
        val eps = 0.03f

        // Polygon-edge hallway relocation
        if (r.entryDoorEdgeIndex >= 0 || r.polygon.size >= 3) {
            val poly = r.resolvedPolygon()
            val curEdge = if (r.entryDoorEdgeIndex >= 0) r.entryDoorEdgeIndex else when (r.entryDoorSide) {
                EntryDoorSide.TOP -> 0
                EntryDoorSide.RIGHT -> 1
                EntryDoorSide.BOTTOM -> 2
                EntryDoorSide.LEFT -> 3
                EntryDoorSide.NONE -> -1
            }
            val altEdge = poly.indices.firstOrNull { edgeIdx ->
                if (edgeIdx == curEdge) return@firstOrNull false
                if (edgeIdx in r.windowEdgeIndices || edgeIdx in r.doorEdgeIndices || r.openings.any { it.edgeIndex == edgeIdx }) {
                    return@firstOrNull false
                }
                val a = poly[edgeIdx]
                val b = poly[(edgeIdx + 1) % poly.size]
                val edgeLen = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
                if (edgeLen < 0.09f) return@firstOrNull false
                val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                val touchesOther = rooms.any { o ->
                    if (o === r) return@any false
                    val oPoly = o.resolvedPolygon()
                    oPoly.indices.any { oi ->
                        val oa = oPoly[oi]
                        val ob = oPoly[(oi + 1) % oPoly.size]
                        pointToSegDist(mid, oa, ob) < eps
                    }
                }
                !touchesOther
            } ?: return null

            return rooms.mapIndexed { idx, room ->
                if (idx == entryIdx) {
                    room.copy(
                        entryDoorSide = EntryDoorSide.NONE,
                        entryDoorEdgeIndex = altEdge
                    )
                } else room
            }
        }

        fun horizOverlap(a: RoomGeometryRect, b: RoomGeometryRect): Boolean =
            minOf(a.right, b.right) - maxOf(a.left, b.left) > eps

        fun vertOverlap(a: RoomGeometryRect, b: RoomGeometryRect): Boolean =
            minOf(a.bottom, b.bottom) - maxOf(a.top, b.top) > eps

        val touchesBottom = rooms.any { o -> o !== r && horizOverlap(r, o) && abs(o.top - r.bottom) < eps }
        val touchesTop = rooms.any { o -> o !== r && horizOverlap(r, o) && abs(o.bottom - r.top) < eps }
        val touchesLeft = rooms.any { o -> o !== r && vertOverlap(r, o) && abs(o.right - r.left) < eps }
        val touchesRight = rooms.any { o -> o !== r && vertOverlap(r, o) && abs(o.left - r.right) < eps }

        val preferredOrder = when (r.entryDoorSide) {
            EntryDoorSide.BOTTOM -> listOf(EntryDoorSide.LEFT, EntryDoorSide.RIGHT, EntryDoorSide.TOP)
            EntryDoorSide.LEFT -> listOf(EntryDoorSide.BOTTOM, EntryDoorSide.TOP, EntryDoorSide.RIGHT)
            EntryDoorSide.RIGHT -> listOf(EntryDoorSide.BOTTOM, EntryDoorSide.TOP, EntryDoorSide.LEFT)
            EntryDoorSide.TOP -> listOf(EntryDoorSide.LEFT, EntryDoorSide.RIGHT, EntryDoorSide.BOTTOM)
            EntryDoorSide.NONE -> emptyList()
        }

        val altSide = preferredOrder.firstOrNull { candidate ->
            when (candidate) {
                EntryDoorSide.BOTTOM -> !touchesBottom && !r.hasWindowBottom && !r.hasDoorBottom && r.openings.none { it.edgeIndex == 2 }
                EntryDoorSide.TOP -> !touchesTop && !r.hasWindowTop && !r.hasDoorTop && r.openings.none { it.edgeIndex == 0 }
                EntryDoorSide.LEFT -> !touchesLeft && !r.hasWindowLeft && !r.hasDoorLeft && r.openings.none { it.edgeIndex == 3 }
                EntryDoorSide.RIGHT -> !touchesRight && !r.hasWindowRight && !r.hasDoorRight && r.openings.none { it.edgeIndex == 1 }
                EntryDoorSide.NONE -> false
            }
        } ?: return null

        return rooms.mapIndexed { idx, room ->
            if (idx == entryIdx) room.copy(entryDoorSide = altSide) else room
        }
    }

    private fun pointToSegDist(p: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-6f) return hypot((p.x - a.x).toDouble(), (p.y - a.y).toDouble()).toFloat()
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = a.x + t * dx
        val projY = a.y + t * dy
        return hypot((p.x - projX).toDouble(), (p.y - projY).toDouble()).toFloat()
    }
}

data class EstimateCategoryConfig(
    val id: String,
    val title: String,
    val unit: String,
    val sourceKind: String,
    val isSelected: Boolean = false,
    val pricePerUnit: Double = 0.0,
    val customQuantity: Double = 0.0,
    val usePartial: Boolean = false,
    val partialQuantity: Double = 0.0
)

data class RoomCalculations(
    val perimeterMeters: Double,
    val heightMeters: Double,
    val openingsAreaSqm: Double,
    val grossWallAreaSqm: Double,
    val netWallAreaSqm: Double,
    val innerFloorAreaSqm: Double,
    val outerFloorAreaSqm: Double,
    val innerCornersMeters: Double,
    val outerCornersMeters: Double,
    val extraCornersMeters: Double,
    val totalCornersMeters: Double
)

data class ProjectTotals(
    val totalWallAreaSqm: Double,
    val totalGrossWallAreaSqm: Double,
    val totalInnerFloorAreaSqm: Double,
    val totalOuterFloorAreaSqm: Double,
    val totalCornersMeters: Double,
    val totalInnerCornersMeters: Double,
    val totalOuterCornersMeters: Double,
    val totalExtraCornersMeters: Double,
    val totalOpeningsAreaSqm: Double,
    val minorWorksTotalRub: Double,
    val categoriesTotalRub: Double,
    val grandTotalRub: Double,
    val allWallsMeasured: Boolean = false
)

data class GeometryCheckResult(
    val isValid: Boolean,
    val warningMessage: String? = null
)

object GeometryAiAdvisor {

    fun validateRoomWalls(walls: List<WallMeasurement>): GeometryCheckResult {
        val entered = walls.filter { it.lengthMeters > 0.0 }
        if (entered.size < 2) return GeometryCheckResult(true)

        val maxWall = entered.maxBy { it.lengthMeters }
        val minWall = entered.minBy { it.lengthMeters }

        if (maxWall.lengthMeters > 25.0) {
            return GeometryCheckResult(
                isValid = false,
                warningMessage = "Точно ли вы верно ввели? ${maxWall.label} = ${Formatters.formatCleanNumber(maxWall.lengthMeters)} м слишком длинная. Возможно, пропущена точка?"
            )
        }

        if (minWall.lengthMeters > 0.1 && maxWall.lengthMeters / minWall.lengthMeters >= 6.0 && entered.size >= 3) {
            return GeometryCheckResult(
                isValid = false,
                warningMessage = "Точно ли вы верно ввели? ${maxWall.label} (${Formatters.formatCleanNumber(maxWall.lengthMeters)} м) сильно отличается от ${minWall.label} (${Formatters.formatCleanNumber(minWall.lengthMeters)} м)."
            )
        }

        if (entered.size == walls.size && walls.size >= 3) {
            val totalSum = walls.sumOf { it.lengthMeters }
            val sumOthers = totalSum - maxWall.lengthMeters
            if (maxWall.lengthMeters >= sumOthers) {
                return GeometryCheckResult(
                    isValid = false,
                    warningMessage = "Точно ли вы верно ввели? Контур не сходится: ${maxWall.label} (${Formatters.formatCleanNumber(maxWall.lengthMeters)} м) длиннее суммы остальных стен."
                )
            }
            if (walls.size == 4) {
                val w0 = walls[0].lengthMeters
                val w1 = walls[1].lengthMeters
                val w2 = walls[2].lengthMeters
                val w3 = walls[3].lengthMeters
                val opp1Diff = abs(w0 - w2)
                val opp2Diff = abs(w1 - w3)
                val base1 = max(w0, w2).coerceAtLeast(0.5)
                val base2 = max(w1, w3).coerceAtLeast(0.5)
                if (opp1Diff / base1 > 0.10) {
                    return GeometryCheckResult(
                        isValid = false,
                        warningMessage = "Точно ли вы верно ввели? Противоположные стены 1 (${Formatters.formatCleanNumber(w0)} м) и 3 (${Formatters.formatCleanNumber(w2)} м) отличаются!"
                    )
                }
                if (opp2Diff / base2 > 0.10) {
                    return GeometryCheckResult(
                        isValid = false,
                        warningMessage = "Точно ли вы верно ввели? Противоположные стены 2 (${Formatters.formatCleanNumber(w1)} м) и 4 (${Formatters.formatCleanNumber(w3)} м) отличаются!"
                    )
                }
            } else if (walls.size == 6) {
                val hTop = walls[0].lengthMeters
                val hBot = walls[2].lengthMeters + walls[4].lengthMeters
                val vLeft = walls[5].lengthMeters
                val vRight = walls[1].lengthMeters + walls[3].lengthMeters
                if (abs(hTop - hBot) / max(hTop, hBot).coerceAtLeast(0.5) > 0.15 ||
                    abs(vLeft - vRight) / max(vLeft, vRight).coerceAtLeast(0.5) > 0.15
                ) {
                    return GeometryCheckResult(
                        isValid = false,
                        warningMessage = "Точно ли вы верно ввели? Противоположные стороны Г-образной комнаты не совпадают."
                    )
                }
            }
        }

        return GeometryCheckResult(true)
    }
}

object RoomGeometryHelper {

    fun calculateRoom(
        walls: List<WallMeasurement>,
        heightMeters: Double,
        wallThicknessMeters: Double,
        openings: List<OpeningItem>,
        innerCorners: Int,
        outerCorners: Int,
        extraCornerMeters: Double,
        floorAreaOverrideMeters: Double?
    ): RoomCalculations {
        val h = heightMeters.coerceAtLeast(0.0)
        val t = wallThicknessMeters.coerceAtLeast(0.0)
        val perimeter = walls.sumOf { it.lengthMeters.coerceAtLeast(0.0) }
        val openingsArea = openings.sumOf { it.totalAreaSqm }
        val grossWall = perimeter * h
        val netWall = (grossWall - openingsArea).coerceAtLeast(0.0)

        val innerFloor = floorAreaOverrideMeters?.coerceAtLeast(0.0)
            ?: computeInnerFloorAreaFromWalls(walls.map { it.lengthMeters.coerceAtLeast(0.0) })

        val outerFloor = if (innerFloor > 0.0 && perimeter > 0.0) {
            innerFloor + perimeter * t + 4.0 * t * t
        } else {
            innerFloor
        }

        val inCornersM = innerCorners.coerceAtLeast(0) * h
        val outCornersM = outerCorners.coerceAtLeast(0) * h
        val extraM = extraCornerMeters.coerceAtLeast(0.0)
        val totalCorners = inCornersM + outCornersM + extraM

        return RoomCalculations(
            perimeterMeters = perimeter,
            heightMeters = h,
            openingsAreaSqm = openingsArea,
            grossWallAreaSqm = grossWall,
            netWallAreaSqm = netWall,
            innerFloorAreaSqm = innerFloor,
            outerFloorAreaSqm = outerFloor,
            innerCornersMeters = inCornersM,
            outerCornersMeters = outCornersM,
            extraCornersMeters = extraM,
            totalCornersMeters = totalCorners
        )
    }

    fun computeInnerFloorAreaFromWalls(lengths: List<Double>): Double {
        if (lengths.isEmpty() || lengths.all { it <= 0.0 }) return 0.0
        return when (lengths.size) {
            1 -> 0.0
            2 -> lengths[0] * lengths[1]
            3 -> {
                val a = lengths[0]
                val b = lengths[1]
                val c = lengths[2]
                val s = (a + b + c) / 2.0
                val sq = s * (s - a) * (s - b) * (s - c)
                if (sq > 0) kotlin.math.sqrt(sq) else 0.0
            }
            4 -> {
                val avgWidth = (lengths[0] + lengths[2]) / 2.0
                val avgDepth = (lengths[1] + lengths[3]) / 2.0
                (avgWidth * avgDepth).coerceAtLeast(0.0)
            }
            else -> {
                var hSum = 0.0
                var vSum = 0.0
                lengths.forEachIndexed { idx, len ->
                    if (idx % 2 == 0) hSum += len else vSum += len
                }
                val avgW = hSum / 2.0
                val avgD = vSum / 2.0
                (avgW * avgD).coerceAtLeast(0.0)
            }
        }
    }
}

data class SnappedSegment(
    val start: Offset,
    val end: Offset
)

/**
 * Interactive room box shape used in the intuitive custom floorplan builder.
 */
data class DrawnRoomBox(
    val id: String,
    val name: String,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    fun toSegments(): List<SnappedSegment> {
        val tl = Offset(left, top)
        val tr = Offset(left + width, top)
        val br = Offset(left + width, top + height)
        val bl = Offset(left, top + height)
        return listOf(
            SnappedSegment(tl, tr),
            SnappedSegment(tr, br),
            SnappedSegment(br, bl),
            SnappedSegment(bl, tl)
        )
    }
}

/**
 * Non-rectangular orthogonal room (e.g. L-shaped hallway) extracted by the whole-apartment AI.
 */
data class DrawnCustomRoom(
    val id: String,
    val name: String,
    val segments: List<SnappedSegment>,
    val center: Offset
)

data class WholeApartmentInterpretation(
    val boxes: List<DrawnRoomBox>,
    val customRooms: List<DrawnCustomRoom>
) {
    val totalRooms: Int get() = boxes.size + customRooms.size
}

/**
 * Smart orthogonal AI interpreter for finger-drawn whole apartments, room boxes, and straight walls.
 */
object FloorPlanSnapper {

    private data class HWall(val yIdx: Int, val x1: Float, val x2: Float)
    private data class VWall(val xIdx: Int, val y1: Float, val y2: Float)
    private data class GridEdge(val start: Offset, val end: Offset)

    /**
     * Built-in Whole-Apartment AI:
     * Takes a freehand sketch of the entire apartment (e.g. a big outer square + dividing walls inside),
     * straightens and aligns all walls orthogonally, snaps small finger gaps, and splits the layout
     * into crisp individual rooms (rectangular DrawnRoomBoxes and L-shaped DrawnCustomRooms).
     */
    fun interpretWholeApartmentFromStrokes(
        strokes: List<List<Offset>>,
        startNumber: Int = 1,
        idFactory: () -> String = { java.util.UUID.randomUUID().toString() }
    ): WholeApartmentInterpretation {
        val validStrokes = strokes.filter { it.size >= 2 }
        if (validStrokes.isEmpty()) return WholeApartmentInterpretation(emptyList(), emptyList())

        val allPoints = validStrokes.flatten()
        val rawMinX = allPoints.minOf { it.x }
        val rawMaxX = allPoints.maxOf { it.x }
        val rawMinY = allPoints.minOf { it.y }
        val rawMaxY = allPoints.maxOf { it.y }
        val totalW = (rawMaxX - rawMinX).coerceAtLeast(1f)
        val totalH = (rawMaxY - rawMinY).coerceAtLeast(1f)

        if (totalW < 40f || totalH < 40f) {
            return WholeApartmentInterpretation(emptyList(), emptyList())
        }

        // 1. Simplify each finger stroke into clean straight segments and close loop strokes
        val rawHoriz = mutableListOf<Triple<Float, Float, Float>>() // (y, x1, x2)
        val rawVert = mutableListOf<Triple<Float, Float, Float>>()  // (x, y1, y2)

        val rdpEpsilon = (minOf(totalW, totalH) * 0.06f).coerceIn(18f, 34f)
        validStrokes.forEach { stroke ->
            val sMinX = stroke.minOf { it.x }
            val sMaxX = stroke.maxOf { it.x }
            val sMinY = stroke.minOf { it.y }
            val sMaxY = stroke.maxOf { it.y }
            val sw = sMaxX - sMinX
            val sh = sMaxY - sMinY
            if (max(sw, sh) < 28f) return@forEach

            val simplified = rdpSimplify(stroke, rdpEpsilon).toMutableList()
            val endDist = hypot(
                (stroke.last().x - stroke.first().x).toDouble(),
                (stroke.last().y - stroke.first().y).toDouble()
            ).toFloat()
            if (simplified.size >= 3 && sw >= 60f && sh >= 60f && endDist <= max(sw, sh) * 0.48f) {
                simplified.add(simplified.first())
            }

            for (i in 0 until simplified.lastIndex) {
                val p1 = simplified[i]
                val p2 = simplified[i + 1]
                val dx = abs(p2.x - p1.x)
                val dy = abs(p2.y - p1.y)
                if (max(dx, dy) < 22f) continue
                if (dx >= dy) {
                    val yMid = (p1.y + p2.y) / 2f
                    rawHoriz.add(Triple(yMid, minOf(p1.x, p2.x), maxOf(p1.x, p2.x)))
                } else {
                    val xMid = (p1.x + p2.x) / 2f
                    rawVert.add(Triple(xMid, minOf(p1.y, p2.y), maxOf(p1.y, p2.y)))
                }
            }
        }

        // If only 1 single box/stroke was drawn without partitions, keep a single aligned box
        if (rawHoriz.isEmpty() && rawVert.isEmpty()) {
            val single = interpretFreehandToSquareBox(validStrokes, "Комната $startNumber", idFactory())
            return WholeApartmentInterpretation(listOfNotNull(single), emptyList())
        }

        // 2. Cluster X and Y lines so crooked hand-drawn walls snap onto shared clean axes
        val clusterTolX = (totalW * 0.085f).coerceIn(36f, 68f)
        val clusterTolY = (totalH * 0.085f).coerceIn(36f, 68f)

        val xs = buildAxisClusters(
            rawMin = rawMinX,
            rawMax = rawMaxX,
            values = rawVert.map { it.first },
            tolerance = clusterTolX
        )
        val ys = buildAxisClusters(
            rawMin = rawMinY,
            rawMax = rawMaxY,
            values = rawHoriz.map { it.first },
            tolerance = clusterTolY
        )

        if (xs.size < 2 || ys.size < 2) {
            val single = interpretFreehandToSquareBox(validStrokes, "Комната $startNumber", idFactory())
            return WholeApartmentInterpretation(listOfNotNull(single), emptyList())
        }

        val cols = xs.size - 1
        val rows = ys.size - 1

        // 3. Snap each horizontal and vertical wall onto the nearest axis & extend endpoints to close small finger gaps
        val endSnapX = (totalW * 0.14f).coerceIn(48f, 88f)
        val endSnapY = (totalH * 0.14f).coerceIn(48f, 88f)

        val hWalls = mutableListOf<HWall>()
        // Outer top and bottom walls ensure enclosed contour
        hWalls.add(HWall(0, xs.first(), xs.last()))
        hWalls.add(HWall(ys.lastIndex, xs.first(), xs.last()))
        rawHoriz.forEach { (y, x1, x2) ->
            val yIdx = ys.indices.minByOrNull { abs(ys[it] - y) } ?: 0
            val sx1 = snapValueToList(x1, xs, endSnapX)
            val sx2 = snapValueToList(x2, xs, endSnapX)
            if (sx2 - sx1 >= 24f) {
                hWalls.add(HWall(yIdx, sx1, sx2))
            }
        }

        val vWalls = mutableListOf<VWall>()
        // Outer left and right walls ensure enclosed contour
        vWalls.add(VWall(0, ys.first(), ys.last()))
        vWalls.add(VWall(xs.lastIndex, ys.first(), ys.last()))
        rawVert.forEach { (x, y1, y2) ->
            val xIdx = xs.indices.minByOrNull { abs(xs[it] - x) } ?: 0
            val sy1 = snapValueToList(y1, ys, endSnapY)
            val sy2 = snapValueToList(y2, ys, endSnapY)
            if (sy2 - sy1 >= 24f) {
                vWalls.add(VWall(xIdx, sy1, sy2))
            }
        }

        // 4. Determine wall barriers between elementary grid cells
        // Vertical barrier between cell(c, r) and cell(c+1, r) for c in 0 until cols-1
        val vBlocked = Array(cols + 1) { BooleanArray(rows) }
        for (c in 0..cols) {
            for (r in 0 until rows) {
                if (c == 0 || c == cols) {
                    vBlocked[c][r] = true
                } else {
                    val segLen = (ys[r + 1] - ys[r]).coerceAtLeast(1f)
                    val covered = vWalls.any { w ->
                        w.xIdx == c && intervalOverlap(w.y1, w.y2, ys[r], ys[r + 1]) >= segLen * 0.45f
                    }
                    vBlocked[c][r] = covered
                }
            }
        }

        // Horizontal barrier between cell(c, r) and cell(c, r+1) for r in 0 until rows-1
        val hBlocked = Array(cols) { BooleanArray(rows + 1) }
        for (c in 0 until cols) {
            for (r in 0..rows) {
                if (r == 0 || r == rows) {
                    hBlocked[c][r] = true
                } else {
                    val segLen = (xs[c + 1] - xs[c]).coerceAtLeast(1f)
                    val covered = hWalls.any { w ->
                        w.yIdx == r && intervalOverlap(w.x1, w.x2, xs[c], xs[c + 1]) >= segLen * 0.45f
                    }
                    hBlocked[c][r] = covered
                }
            }
        }

        // 5. BFS flood fill to merge open adjacent cells into separate rooms
        val visited = Array(cols) { BooleanArray(rows) }
        val components = mutableListOf<List<Pair<Int, Int>>>()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (!visited[c][r]) {
                    val comp = mutableListOf<Pair<Int, Int>>()
                    val queue = ArrayDeque<Pair<Int, Int>>()
                    queue.addLast(c to r)
                    visited[c][r] = true
                    while (queue.isNotEmpty()) {
                        val (cc, rr) = queue.removeFirst()
                        comp.add(cc to rr)
                        if (cc + 1 < cols && !visited[cc + 1][rr] && !vBlocked[cc + 1][rr]) {
                            visited[cc + 1][rr] = true
                            queue.addLast((cc + 1) to rr)
                        }
                        if (cc - 1 >= 0 && !visited[cc - 1][rr] && !vBlocked[cc][rr]) {
                            visited[cc - 1][rr] = true
                            queue.addLast((cc - 1) to rr)
                        }
                        if (rr + 1 < rows && !visited[cc][rr + 1] && !hBlocked[cc][rr + 1]) {
                            visited[cc][rr + 1] = true
                            queue.addLast(cc to (rr + 1))
                        }
                        if (rr - 1 >= 0 && !visited[cc][rr - 1] && !hBlocked[cc][rr]) {
                            visited[cc][rr - 1] = true
                            queue.addLast(cc to (rr - 1))
                        }
                    }
                    components.add(comp)
                }
            }
        }

        // Sort rooms nicely top-to-bottom, left-to-right
        val sortedComps = components.sortedWith(
            compareBy<List<Pair<Int, Int>>> { comp -> comp.minOf { it.second } }
                .thenBy { comp -> comp.minOf { it.first } }
        )

        val outBoxes = mutableListOf<DrawnRoomBox>()
        val outCustom = mutableListOf<DrawnCustomRoom>()

        sortedComps.forEachIndexed { idx, comp ->
            val name = "Комната ${startNumber + idx}"
            val minC = comp.minOf { it.first }
            val maxC = comp.maxOf { it.first }
            val minR = comp.minOf { it.second }
            val maxR = comp.maxOf { it.second }
            val expectedRectCells = (maxC - minC + 1) * (maxR - minR + 1)
            if (comp.size == expectedRectCells) {
                outBoxes.add(
                    DrawnRoomBox(
                        id = idFactory(),
                        name = name,
                        left = xs[minC],
                        top = ys[minR],
                        width = (xs[maxC + 1] - xs[minC]).coerceAtLeast(48f),
                        height = (ys[maxR + 1] - ys[minR]).coerceAtLeast(48f)
                    )
                )
            } else {
                val cellSet = comp.toSet()
                val rawEdges = mutableListOf<GridEdge>()
                comp.forEach { (c, r) ->
                    val x1 = xs[c]
                    val x2 = xs[c + 1]
                    val y1 = ys[r]
                    val y2 = ys[r + 1]
                    if ((c to r - 1) !in cellSet) rawEdges.add(GridEdge(Offset(x1, y1), Offset(x2, y1)))
                    if ((c + 1 to r) !in cellSet) rawEdges.add(GridEdge(Offset(x2, y1), Offset(x2, y2)))
                    if ((c to r + 1) !in cellSet) rawEdges.add(GridEdge(Offset(x2, y2), Offset(x1, y2)))
                    if ((c - 1 to r) !in cellSet) rawEdges.add(GridEdge(Offset(x1, y2), Offset(x1, y1)))
                }
                val orderedSegs = chainAndMergeEdges(rawEdges)
                val cx = comp.map { (xs[it.first] + xs[it.first + 1]) / 2f }.average().toFloat()
                val cy = comp.map { (ys[it.second] + ys[it.second + 1]) / 2f }.average().toFloat()
                outCustom.add(
                    DrawnCustomRoom(
                        id = idFactory(),
                        name = name,
                        segments = orderedSegs,
                        center = Offset(cx, cy)
                    )
                )
            }
        }

        return WholeApartmentInterpretation(boxes = outBoxes, customRooms = outCustom)
    }

    private fun buildAxisClusters(
        rawMin: Float,
        rawMax: Float,
        values: List<Float>,
        tolerance: Float
    ): List<Float> {
        val sorted = values.sorted()
        val clusters = mutableListOf<MutableList<Float>>()
        sorted.forEach { v ->
            val last = clusters.lastOrNull()
            if (last != null && abs(v - last.average().toFloat()) <= tolerance) {
                last.add(v)
            } else {
                clusters.add(mutableListOf(v))
            }
        }
        val minSnapped = snapToGrid(rawMin, 24f)
        val maxSnapped = snapToGrid(rawMax, 24f).coerceAtLeast(minSnapped + 72f)
        val inner = clusters
            .map { snapToGrid(it.average().toFloat(), 24f) }
            .filter { it > minSnapped + tolerance * 0.7f && it < maxSnapped - tolerance * 0.7f }

        val result = mutableListOf(minSnapped)
        inner.sorted().forEach { v ->
            if (v - result.last() >= 48f && maxSnapped - v >= 48f) {
                result.add(v)
            }
        }
        result.add(maxSnapped)
        return result
    }

    private fun snapValueToList(v: Float, targets: List<Float>, maxDist: Float): Float {
        val best = targets.minByOrNull { abs(it - v) }
        return if (best != null && abs(best - v) <= maxDist) best else snapToGrid(v, 24f)
    }

    private fun intervalOverlap(a1: Float, a2: Float, b1: Float, b2: Float): Float =
        (minOf(a2, b2) - maxOf(a1, b1)).coerceAtLeast(0f)

    private fun chainAndMergeEdges(edges: List<GridEdge>): List<SnappedSegment> {
        if (edges.isEmpty()) return emptyList()
        val remaining = edges.toMutableList()
        val chained = mutableListOf<GridEdge>()
        var current = remaining.removeAt(0)
        chained.add(current)

        while (remaining.isNotEmpty()) {
            val nextIdx = remaining.indexOfFirst {
                abs(it.start.x - current.end.x) < 1.5f && abs(it.start.y - current.end.y) < 1.5f
            }
            if (nextIdx >= 0) {
                current = remaining.removeAt(nextIdx)
                chained.add(current)
            } else {
                current = remaining.removeAt(0)
                chained.add(current)
            }
        }

        // Merge consecutive collinear edges (e.g. two top edges along the same horizontal wall)
        val merged = mutableListOf<SnappedSegment>()
        chained.forEach { e ->
            val seg = SnappedSegment(e.start, e.end)
            val prev = merged.lastOrNull()
            if (prev != null &&
                abs(prev.end.x - seg.start.x) < 1.5f &&
                abs(prev.end.y - seg.start.y) < 1.5f &&
                (
                    (abs(prev.start.y - prev.end.y) < 1.5f && abs(seg.start.y - seg.end.y) < 1.5f) ||
                        (abs(prev.start.x - prev.end.x) < 1.5f && abs(seg.start.x - seg.end.x) < 1.5f)
                    )
            ) {
                merged[merged.lastIndex] = SnappedSegment(prev.start, seg.end)
            } else {
                merged.add(seg)
            }
        }
        if (merged.size >= 2) {
            val first = merged.first()
            val last = merged.last()
            if (abs(last.end.x - first.start.x) < 1.5f &&
                abs(last.end.y - first.start.y) < 1.5f &&
                (
                    (abs(last.start.y - last.end.y) < 1.5f && abs(first.start.y - first.end.y) < 1.5f) ||
                        (abs(last.start.x - last.end.x) < 1.5f && abs(first.start.x - first.end.x) < 1.5f)
                    )
            ) {
                merged[0] = SnappedSegment(last.start, first.end)
                merged.removeAt(merged.lastIndex)
            }
        }
        return merged
    }

    private fun rdpSimplify(points: List<Offset>, epsilon: Float): List<Offset> {
        if (points.size < 3) return points
        var maxDist = 0f
        var index = 0
        val start = points.first()
        val end = points.last()
        for (i in 1 until points.lastIndex) {
            val d = perpendicularDistance(points[i], start, end)
            if (d > maxDist) {
                index = i
                maxDist = d
            }
        }
        return if (maxDist > epsilon) {
            val left = rdpSimplify(points.subList(0, index + 1), epsilon)
            val right = rdpSimplify(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }

    private fun perpendicularDistance(p: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-4f) return hypot((p.x - a.x).toDouble(), (p.y - a.y).toDouble()).toFloat()
        val num = abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x)
        return num / kotlin.math.sqrt(lenSq)
    }

    /**
     * Built-in AI: converts freehand finger strokes into clean rectangular (square/box) room shapes,
     * since real rooms are predominantly square/rectangular. Preserves the drawn aspect ratio!
     */
    fun interpretFreehandToSquareBox(
        strokes: List<List<Offset>>,
        defaultName: String,
        id: String
    ): DrawnRoomBox? {
        val all = strokes.flatten()
        if (all.size < 2) return null
        val minX = all.minOf { it.x }
        val maxX = all.maxOf { it.x }
        val minY = all.minOf { it.y }
        val maxY = all.maxOf { it.y }
        val w = (maxX - minX).coerceAtLeast(80f)
        val h = (maxY - minY).coerceAtLeast(80f)
        return DrawnRoomBox(
            id = id,
            name = defaultName,
            left = snapToGrid(minX, 24f),
            top = snapToGrid(minY, 24f),
            width = snapToGrid(w, 24f).coerceAtLeast(96f),
            height = snapToGrid(h, 24f).coerceAtLeast(96f)
        )
    }

    /**
     * Snaps a straight wall stick between start and end to 90-degree horizontal/vertical
     * if within ~20 degrees, and snaps endpoints to grid/nearby corners.
     */
    fun snapWallStick(start: Offset, end: Offset, existingCorners: List<Offset>, gridPx: Float = 24f): SnappedSegment {
        val s = snapToNearbyCorner(start, existingCorners, 36f) ?: Offset(snapToGrid(start.x, gridPx), snapToGrid(start.y, gridPx))
        val rawEnd = snapToNearbyCorner(end, existingCorners, 36f)
        if (rawEnd != null) {
            return SnappedSegment(s, rawEnd)
        }
        val dx = end.x - s.x
        val dy = end.y - s.y
        val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
        val snappedEnd = when {
            abs(dy) / len < 0.35f -> Offset(snapToGrid(end.x, gridPx), s.y)
            abs(dx) / len < 0.35f -> Offset(s.x, snapToGrid(end.y, gridPx))
            else -> Offset(snapToGrid(end.x, gridPx), snapToGrid(end.y, gridPx))
        }
        return SnappedSegment(s, snappedEnd)
    }

    fun directionHintForSegment(seg: SnappedSegment, index: Int): String {
        val dx = seg.end.x - seg.start.x
        val dy = seg.end.y - seg.start.y
        return when {
            abs(dx) >= abs(dy) && dx >= 0 -> "Верхняя/прямая стена (слева направо). Мерь по низу у пола"
            abs(dx) >= abs(dy) && dx < 0 -> "Нижняя стена (справа налево). Мерь по низу у пола"
            dy >= 0 -> "Правая стена (сверху вниз). Мерь от угла до угла у пола"
            else -> "Левая стена (снизу вверх). Мерь от угла до угла у пола"
        }
    }

    const val GRID_STEP_PX = 24f
    const val PX_PER_METER = 96f // 1 major grid square (4 * 24f) = 1.0 m, 1 minor cell = 0.25 m

    fun pxToApproxMeters(px: Float): Double {
        if (px <= 0f) return 0.0
        val raw = px / PX_PER_METER
        return kotlin.math.round(raw * 20.0) / 20.0
    }

    fun formatApproxMeters(px: Float, withTilde: Boolean = true): String {
        val m = pxToApproxMeters(px)
        val num = Formatters.formatCleanNumber(m)
        return if (withTilde) "~$num м" else "$num м"
    }

    fun approxBoxAreaSqm(widthPx: Float, heightPx: Float): Double {
        val w = widthPx.coerceAtLeast(0f) / PX_PER_METER
        val h = heightPx.coerceAtLeast(0f) / PX_PER_METER
        return kotlin.math.round((w * h).toDouble() * 10.0) / 10.0
    }

    fun approxPolygonAreaSqm(segments: List<SnappedSegment>): Double {
        if (segments.size < 3) return 0.0
        var sum = 0.0
        for (s in segments) {
            sum += (s.start.x.toDouble() * s.end.y.toDouble()) - (s.end.x.toDouble() * s.start.y.toDouble())
        }
        val pxArea = abs(sum) * 0.5
        val sqm = pxArea / (PX_PER_METER * PX_PER_METER)
        return kotlin.math.round(sqm * 10.0) / 10.0
    }

    fun formatApproxArea(sqm: Double): String {
        val num = Formatters.formatCleanNumber(sqm)
        return "≈ $num м²"
    }

    fun translateCustomRoom(room: DrawnCustomRoom, dx: Float, dy: Float): DrawnCustomRoom =
        room.copy(
            segments = room.segments.map { seg ->
                SnappedSegment(
                    start = Offset(seg.start.x + dx, seg.start.y + dy),
                    end = Offset(seg.end.x + dx, seg.end.y + dy)
                )
            },
            center = Offset(room.center.x + dx, room.center.y + dy)
        )

    fun isPointInsidePolygon(p: Offset, segments: List<SnappedSegment>): Boolean {
        if (segments.size < 3) return false
        var inside = false
        for (s in segments) {
            val a = s.start
            val b = s.end
            val denom = (b.y - a.y).let { if (abs(it) < 1e-4f) 1e-4f else it }
            val intersects = ((a.y > p.y) != (b.y > p.y)) &&
                (p.x < (b.x - a.x) * (p.y - a.y) / denom + a.x)
            if (intersects) inside = !inside
        }
        return inside
    }

    fun pointToSegmentDistance(p: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-6f) return hypot((p.x - a.x).toDouble(), (p.y - a.y).toDouble()).toFloat()
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = a.x + t * dx
        val projY = a.y + t * dy
        return hypot((p.x - projX).toDouble(), (p.y - projY).toDouble()).toFloat()
    }

    private fun snapToGrid(value: Float, step: Float): Float =
        kotlin.math.round(value / step) * step

    private fun snapToNearbyCorner(p: Offset, corners: List<Offset>, threshold: Float): Offset? {
        return corners.minByOrNull { hypot((it.x - p.x).toDouble(), (it.y - p.y).toDouble()) }
            ?.takeIf { hypot((it.x - p.x).toDouble(), (it.y - p.y).toDouble()) <= threshold }
    }
}

data class MinimapRoomSpec(
    val id: Long,
    val name: String,
    val wallCount: Int,
    val sortOrder: Int
)

data class MinimapRoom(
    val label: String,
    val polygon: List<Offset>,
    val isActive: Boolean,
    val isBalcony: Boolean = false,
    val entryDoorSide: EntryDoorSide = EntryDoorSide.NONE,
    val entryDoorEdgeIndex: Int = -1,
    val activeWallEdgeIndex: Int = -1
)

data class MinimapPlan(
    val aspectRatio: Float,
    val rooms: List<MinimapRoom>
)

object FloorPlanMinimapHelper {

    fun buildMinimap(
        layoutType: String,
        layoutTitle: String,
        drawnSegments: List<SnappedSegment>,
        rooms: List<MinimapRoomSpec>,
        activeRoomId: Long,
        activeWallIndex: Int // 1-based wall index, or <= 0 if none
    ): MinimapPlan {
        if (rooms.isEmpty()) return MinimapPlan(1.4f, emptyList())
        val orderedRooms = rooms.sortedBy { it.sortOrder }
        val edgeIdx = if (activeWallIndex > 0) activeWallIndex - 1 else -1

        // 1. Template-based apartment (supports polygonal, slanted, bay-window & variant layouts)
        val template = ApartmentTemplates.findById(layoutType)
        if (template != null && template.schematicRooms.isNotEmpty()) {
            val variantRooms = when {
                "Зерк" in layoutTitle && "вход" in layoutTitle ->
                    FloorPlanVariants.mirrorHorizontally(
                        FloorPlanVariants.relocateEntryDoorInHallway(template.schematicRooms)
                            ?: template.schematicRooms
                    )
                "Зеркальный" in layoutTitle -> FloorPlanVariants.mirrorHorizontally(template.schematicRooms)
                "Другой вход" in layoutTitle ->
                    FloorPlanVariants.relocateEntryDoorInHallway(template.schematicRooms)
                        ?: template.schematicRooms
                else -> template.schematicRooms
            }
            val roomToSchematic = mapRoomsToSchematic(orderedRooms, variantRooms)
            val activeSchematicIdx = orderedRooms.indexOfFirst { it.id == activeRoomId }
                .takeIf { it >= 0 }
                ?.let { roomToSchematic[it] }
                ?: -1

            val minimapRooms = variantRooms.mapIndexed { sIdx, rect ->
                val isAct = sIdx == activeSchematicIdx
                val poly = rect.resolvedPolygon()
                MinimapRoom(
                    label = rect.label,
                    polygon = poly,
                    isActive = isAct,
                    isBalcony = rect.isBalcony,
                    entryDoorSide = rect.entryDoorSide,
                    entryDoorEdgeIndex = rect.entryDoorEdgeIndex,
                    activeWallEdgeIndex = if (isAct && edgeIdx in poly.indices) edgeIdx else -1
                )
            }
            return MinimapPlan(
                aspectRatio = template.planAspectRatio.coerceIn(0.8f, 2.1f),
                rooms = minimapRooms
            )
        }

        // 2. Custom drawn apartment (room boxes, Whole-Apartment AI polygons, or wall sticks)
        if (layoutType == "CUSTOM_DRAW" && drawnSegments.isNotEmpty()) {
            val allPts = drawnSegments.flatMap { listOf(it.start, it.end) }
            val minX = allPts.minOf { it.x }
            val maxX = allPts.maxOf { it.x }
            val minY = allPts.minOf { it.y }
            val maxY = allPts.maxOf { it.y }
            val spanX = (maxX - minX).coerceAtLeast(1f)
            val spanY = (maxY - minY).coerceAtLeast(1f)

            fun norm(p: Offset): Offset {
                val nx = 0.06f + ((p.x - minX) / spanX) * 0.88f
                val ny = 0.08f + ((p.y - minY) / spanY) * 0.84f
                return Offset(nx, ny)
            }

            var cursor = 0
            val out = mutableListOf<MinimapRoom>()
            orderedRooms.forEach { spec ->
                val count = spec.wallCount.coerceAtLeast(0)
                if (cursor + count <= drawnSegments.size && count >= 1) {
                    val segs = drawnSegments.subList(cursor, cursor + count)
                    cursor += count
                    val poly = if (count == 1) {
                        listOf(norm(segs[0].start), norm(segs[0].end))
                    } else {
                        segs.map { norm(it.start) }
                    }
                    val isAct = spec.id == activeRoomId
                    val activeEdge = if (isAct && count == 1) {
                        0
                    } else if (isAct && edgeIdx in poly.indices) {
                        edgeIdx
                    } else {
                        -1
                    }
                    out.add(
                        MinimapRoom(
                            label = spec.name,
                            polygon = poly,
                            isActive = isAct,
                            activeWallEdgeIndex = activeEdge
                        )
                    )
                }
            }
            if (out.isNotEmpty()) {
                return MinimapPlan(
                    aspectRatio = (spanX / spanY).coerceIn(0.75f, 2.0f),
                    rooms = out
                )
            }
        }

        // 3. Fallback schematic grid for "Без чертежа" or dynamically created rooms
        val count = orderedRooms.size
        val cols = if (count <= 2) count else if (count <= 4) 2 else 3
        val rows = ((count + cols - 1) / cols).coerceAtLeast(1)
        val pad = 0.05f
        val cellW = (1f - pad * 2f) / cols
        val cellH = (1f - pad * 2f) / rows
        val minimapRooms = orderedRooms.mapIndexed { idx, spec ->
            val c = idx % cols
            val r = idx / cols
            val x1 = pad + c * cellW
            val y1 = pad + r * cellH
            val x2 = x1 + cellW
            val y2 = y1 + cellH
            val isAct = spec.id == activeRoomId
            MinimapRoom(
                label = spec.name,
                polygon = listOf(Offset(x1, y1), Offset(x2, y1), Offset(x2, y2), Offset(x1, y2)),
                isActive = isAct,
                activeWallEdgeIndex = if (isAct && edgeIdx in 0..3) edgeIdx else -1
            )
        }
        return MinimapPlan(
            aspectRatio = (cols.toFloat() / rows.toFloat() * 1.25f).coerceIn(1.0f, 1.9f),
            rooms = minimapRooms
        )
    }

    fun mapRoomsToSchematic(
        rooms: List<MinimapRoomSpec>,
        schematic: List<RoomGeometryRect>
    ): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        val usedSchematic = mutableSetOf<Int>()

        data class PairScore(val rIdx: Int, val sIdx: Int, val score: Int)
        val candidates = mutableListOf<PairScore>()
        rooms.forEachIndexed { rIdx, room ->
            schematic.forEachIndexed { sIdx, rect ->
                val score = semanticMatchScore(room.name, rect.label, rIdx == sIdx)
                if (score > 0) {
                    candidates.add(PairScore(rIdx, sIdx, score))
                }
            }
        }
        candidates.sortByDescending { it.score }
        for (c in candidates) {
            if (c.rIdx !in result && c.sIdx !in usedSchematic) {
                result[c.rIdx] = c.sIdx
                usedSchematic.add(c.sIdx)
            }
        }

        // Fill any leftover rooms (e.g., two bathrooms mapped onto "С/У 1+2" or fallback by order)
        rooms.forEachIndexed { rIdx, room ->
            if (rIdx !in result && schematic.isNotEmpty()) {
                val bestSemantic = schematic.indices.maxByOrNull { sIdx ->
                    semanticMatchScore(room.name, schematic[sIdx].label, rIdx == sIdx)
                }
                val bestScore = bestSemantic?.let { semanticMatchScore(room.name, schematic[it].label, rIdx == it) } ?: 0
                if (bestSemantic != null && bestScore > 0) {
                    result[rIdx] = bestSemantic
                } else {
                    val unused = schematic.indices.firstOrNull { it !in usedSchematic }
                    val pick = unused ?: rIdx.coerceIn(0, schematic.lastIndex)
                    result[rIdx] = pick
                    usedSchematic.add(pick)
                }
            }
        }
        return result
    }

    private fun semanticMatchScore(roomName: String, rectLabel: String, sameIndex: Boolean): Int {
        val a = roomName.lowercase().trim()
        val b = rectLabel.lowercase().trim()
        if (a == b) return 100

        val bonus = if (sameIndex) 4 else 0
        val aDigits = a.filter { it.isDigit() }
        val bDigits = b.filter { it.isDigit() }
        val digitBonus = when {
            aDigits.isNotEmpty() && aDigits == bDigits -> 25
            aDigits.isNotEmpty() && bDigits.isNotEmpty() && aDigits != bDigits -> -20
            else -> 0
        }

        val groups: List<Pair<List<String>, List<String>>> = listOf(
            listOf("кухня-гост", "кухня гост") to listOf("кухня-гост", "кухня-г"),
            listOf("кухня") to listOf("кухня", "кухня-г", "кухня-гост"),
            listOf("ванная", "ван.") to listOf("ван.", "ванн"),
            listOf("туалет", "санузел", "гост. с/у", "с/у") to listOf("с/у", "сануз"),
            listOf("лоджия", "балкон") to listOf("лоджия", "балкон"),
            listOf("терраса") to listOf("терраса"),
            listOf("прихожая", "коридор", "холл", "входная") to listOf("прихож", "коридор", "холл", "входная"),
            listOf("гардероб") to listOf("гард"),
            listOf("кладов") to listOf("клад"),
            listOf("постироч") to listOf("постир"),
            listOf("кабинет") to listOf("кабин"),
            listOf("детск") to listOf("детск"),
            listOf("антресол") to listOf("антресол"),
            listOf("спальн", "сп.") to listOf("спальн", "сп.", "антресол"),
            listOf("гостин", "зал", "студия", "открытое", "общий зал") to listOf("гостин", "гост.", "зал", "студия", "открытое"),
            listOf("комнат", "комн") to listOf("комнат", "комн")
        )

        var best = 0
        for ((keysA, keysB) in groups) {
            val hitA = keysA.any { it in a }
            val hitB = keysB.any { it in b }
            if (hitA && hitB) {
                val base = 55 + bonus + digitBonus
                if (base > best) best = base
            }
        }
        return best
    }
}

object Formatters {
    private val ruSymbols = DecimalFormatSymbols(Locale("ru", "RU")).apply {
        decimalSeparator = '.'
        groupingSeparator = ' '
    }
    private val areaFormat = DecimalFormat("#,##0.00", ruSymbols)
    private val cleanNumberFormat = DecimalFormat("#,##0.##", ruSymbols)
    private val moneyFormat = DecimalFormat("#,##0", ruSymbols)
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU"))
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("ru", "RU"))

    fun todayDate(): String = dateFormat.format(Date())

    fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    fun formatArea(value: Double): String = "${areaFormat.format(value)} м²"

    fun formatMeters(value: Double): String = "${areaFormat.format(value)} м"

    fun formatRunningMeters(value: Double): String = "${areaFormat.format(value)} м.п."

    fun formatMoney(value: Double): String = "${moneyFormat.format(kotlin.math.round(value))} ₽"

    fun formatCleanNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        return cleanNumberFormat.format(value).replace(" ", "")
    }

    fun parseDecimalInput(input: String): Double? {
        val normalized = input.trim().replace(',', '.')
        if (normalized.isEmpty()) return 0.0
        return normalized.toDoubleOrNull()
    }
}
