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
import kotlin.math.min

enum class MeasurementCategory {
    WALLS,
    FLOOR,
    CORNERS
}

enum class OpeningType {
    DOOR,
    WINDOW
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
    val heightMeters: Double
) {
    val totalAreaSqm: Double
        get() = (count.coerceAtLeast(0) * widthMeters.coerceAtLeast(0.0) * heightMeters.coerceAtLeast(0.0))
}

data class RoomGeometryRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String = "",
    val hasDoorBottom: Boolean = false,
    val hasDoorRight: Boolean = false,
    val hasWindowTop: Boolean = false,
    val hasEntryDoorBottom: Boolean = false
)

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
    val roomCountCategory: Int, // 1, 2, or 3
    val isNewBuild: Boolean,
    val rooms: List<TemplateRoomSpec>,
    val schematicRooms: List<RoomGeometryRect>
)

data class EstimateCategoryConfig(
    val id: String,
    val title: String,
    val unit: String, // "м²" or "п.м."
    val sourceKind: String, // "WALLS", "FLOOR", "CORNERS", "CUSTOM"
    val isSelected: Boolean = false,
    val pricePerUnit: Double = 0.0,
    val customQuantity: Double = 0.0
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
    val grandTotalRub: Double
)

data class GeometryCheckResult(
    val isValid: Boolean,
    val warningMessage: String? = null
)

/**
 * Built-in offline smart geometry advisor.
 * Detects physically impossible wall lengths (e.g. 5m vs 50m or unclosable polygons).
 */
object GeometryAiAdvisor {

    fun validateRoomWalls(walls: List<WallMeasurement>): GeometryCheckResult {
        val entered = walls.filter { it.lengthMeters > 0.0 }
        if (entered.size < 2) return GeometryCheckResult(true)

        val maxWall = entered.maxBy { it.lengthMeters }
        val minWall = entered.minBy { it.lengthMeters }

        // Check extreme typo like 5m vs 50m (missing decimal point)
        if (maxWall.lengthMeters > 25.0) {
            return GeometryCheckResult(
                isValid = false,
                warningMessage = "ИИ-проверка: ${maxWall.label} = ${Formatters.formatCleanNumber(maxWall.lengthMeters)} м слишком длинная. Возможно, пропущена точка (например, 5.0 вместо 50)?"
            )
        }

        if (minWall.lengthMeters > 0.1 && maxWall.lengthMeters / minWall.lengthMeters >= 6.0 && entered.size >= 3) {
            return GeometryCheckResult(
                isValid = false,
                warningMessage = "ИИ-проверка: ${maxWall.label} (${Formatters.formatCleanNumber(maxWall.lengthMeters)} м) сильно отличается от ${minWall.label} (${Formatters.formatCleanNumber(minWall.lengthMeters)} м). Проверьте размеры!"
            )
        }

        // Triangle / polygon closure inequality when all walls are entered
        if (entered.size == walls.size && walls.size >= 3) {
            val totalSum = walls.sumOf { it.lengthMeters }
            val sumOthers = totalSum - maxWall.lengthMeters
            if (maxWall.lengthMeters >= sumOthers) {
                return GeometryCheckResult(
                    isValid = false,
                    warningMessage = "ИИ-проверка: Контур не сходится! ${maxWall.label} (${Formatters.formatCleanNumber(maxWall.lengthMeters)} м) длиннее суммы остальных стен (${Formatters.formatCleanNumber(sumOthers)} м)."
                )
            }
            if (walls.size == 4) {
                val opp1Diff = abs(walls[0].lengthMeters - walls[2].lengthMeters)
                val opp2Diff = abs(walls[1].lengthMeters - walls[3].lengthMeters)
                val base1 = max(walls[0].lengthMeters, walls[2].lengthMeters).coerceAtLeast(1.0)
                val base2 = max(walls[1].lengthMeters, walls[3].lengthMeters).coerceAtLeast(1.0)
                if (opp1Diff / base1 > 0.55 || opp2Diff / base2 > 0.55) {
                    return GeometryCheckResult(
                        isValid = false,
                        warningMessage = "ИИ-проверка: Противоположные стены сильно не совпадают. Убедитесь, что размеры верны."
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
            1 -> lengths[0] * lengths[0]
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
 * Interprets freehand finger strokes into clean room wall segments and proportional polygons.
 */
object FloorPlanSnapper {

    /**
     * Built-in AI contour interpreter: takes freehand raw strokes (not snapped during drawing),
     * simplifies corners, aligns lines cleanly, closes the room loop, and produces wall segments.
     */
    fun interpretFreehandStrokesToRoom(strokes: List<List<Offset>>): List<SnappedSegment> {
        val allPoints = strokes.flatten()
        if (allPoints.size < 2) return emptyList()

        // If user drew a single continuous loop or multiple strokes, process each stroke then merge/close
        val segments = mutableListOf<SnappedSegment>()
        strokes.forEach { stroke ->
            if (stroke.size >= 2) {
                val simplified = douglasPeucker(stroke, epsilon = 24f)
                for (i in 0 until simplified.lastIndex) {
                    val a = simplified[i]
                    val b = simplified[i + 1]
                    if (hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()) >= 20f) {
                        segments.add(SnappedSegment(a, b))
                    }
                }
            }
        }

        if (segments.isEmpty()) return emptyList()

        // Gently straighten nearly horizontal/vertical walls (within ~18 deg) while preserving intentional diagonals
        val straightened = segments.map { seg ->
            val dx = seg.end.x - seg.start.x
            val dy = seg.end.y - seg.start.y
            val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
            when {
                abs(dy) / len < 0.32f -> SnappedSegment(seg.start, Offset(seg.end.x, seg.start.y))
                abs(dx) / len < 0.32f -> SnappedSegment(seg.start, Offset(seg.start.x, seg.end.y))
                else -> seg
            }
        }.toMutableList()

        // Connect consecutive endpoints smoothly
        for (i in 1 until straightened.size) {
            val prevEnd = straightened[i - 1].end
            straightened[i] = straightened[i].copy(start = prevEnd)
        }

        // If there are only 1-2 lines, or user drew a rough box, infer clean 4-wall bounding room if nearly rectangular
        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }

        if (straightened.size < 3 && (maxX - minX) > 40f && (maxY - minY) > 40f) {
            val tl = Offset(minX, minY)
            val tr = Offset(maxX, minY)
            val br = Offset(maxX, maxY)
            val bl = Offset(minX, maxY)
            return listOf(
                SnappedSegment(tl, tr),
                SnappedSegment(tr, br),
                SnappedSegment(br, bl),
                SnappedSegment(bl, tl)
            )
        }

        // Close loop
        val firstStart = straightened.first().start
        val lastEnd = straightened.last().end
        if (hypot((lastEnd.x - firstStart.x).toDouble(), (lastEnd.y - firstStart.y).toDouble()) > 25f) {
            straightened.add(SnappedSegment(lastEnd, firstStart))
        } else {
            straightened[straightened.lastIndex] = straightened.last().copy(end = firstStart)
        }

        // Cap reasonable wall count by merging tiny segments
        return mergeShortSegments(straightened, minLen = 28f)
    }

    fun snapStrokeToOrthogonalSegments(
        rawPoints: List<Offset>,
        gridStepPx: Float = 24f,
        minSegmentPx: Float = 32f
    ): List<SnappedSegment> {
        if (rawPoints.size < 2) return emptyList()
        val simplified = douglasPeucker(rawPoints, epsilon = 18f)
        if (simplified.size < 2) return emptyList()

        val rawSegments = mutableListOf<SnappedSegment>()
        var currentStart = Offset(
            kotlin.math.round(simplified.first().x / gridStepPx) * gridStepPx,
            kotlin.math.round(simplified.first().y / gridStepPx) * gridStepPx
        )

        for (i in 1 until simplified.size) {
            val target = simplified[i]
            val dx = target.x - currentStart.x
            val dy = target.y - currentStart.y
            if (hypot(dx.toDouble(), dy.toDouble()) < minSegmentPx) continue

            val snappedEnd = if (abs(dx) >= abs(dy)) {
                Offset(kotlin.math.round(target.x / gridStepPx) * gridStepPx, currentStart.y)
            } else {
                Offset(currentStart.x, kotlin.math.round(target.y / gridStepPx) * gridStepPx)
            }

            if (hypot((snappedEnd.x - currentStart.x).toDouble(), (snappedEnd.y - currentStart.y).toDouble()) >= minSegmentPx) {
                rawSegments.add(SnappedSegment(currentStart, snappedEnd))
                currentStart = snappedEnd
            }
        }
        return rawSegments
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

    private fun mergeShortSegments(segments: List<SnappedSegment>, minLen: Float): List<SnappedSegment> {
        val out = segments.filter {
            hypot((it.end.x - it.start.x).toDouble(), (it.end.y - it.start.y).toDouble()) >= minLen
        }.toMutableList()
        if (out.size >= 3) {
            for (i in 1 until out.size) {
                out[i] = out[i].copy(start = out[i - 1].end)
            }
            out[out.lastIndex] = out.last().copy(end = out.first().start)
        }
        return out
    }

    private fun douglasPeucker(points: List<Offset>, epsilon: Float): List<Offset> {
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
            val left = douglasPeucker(points.subList(0, index + 1), epsilon)
            val right = douglasPeucker(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }

    private fun perpendicularDistance(p: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (len == 0f) return hypot((p.x - lineStart.x).toDouble(), (p.y - lineStart.y).toDouble()).toFloat()
        return abs(dy * p.x - dx * p.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x) / len
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
