package com.example.data

import com.example.domain.ApartmentTemplates
import com.example.domain.EstimateCategoryConfig
import com.example.domain.FloorPlanSnapper
import com.example.domain.Formatters
import com.example.domain.NoPlanRoomSpec
import com.example.domain.OpeningItem
import com.example.domain.ProjectTotals
import com.example.domain.RoomCalculations
import com.example.domain.RoomGeometryHelper
import com.example.domain.SimpleRoomShape
import com.example.domain.SnappedSegment
import com.example.domain.TemplateRoomSpec
import com.example.domain.WallMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RoomDetail(
    val entity: RoomEntity,
    val walls: List<WallMeasurement>,
    val openings: List<OpeningItem>,
    val calculations: RoomCalculations
)

data class ProjectSummaryItem(
    val project: ProjectEntity,
    val roomCount: Int,
    val totalWallAreaSqm: Double,
    val totalFloorAreaSqm: Double,
    val grandTotalRub: Double,
    val allWallsMeasured: Boolean = false
)

data class ProjectAggregate(
    val project: ProjectEntity,
    val rooms: List<RoomDetail>,
    val projectOpenings: List<OpeningItem>,
    val minorWorks: List<MinorWorkEntity>,
    val estimateCategories: List<EstimateCategoryConfig>,
    val savedEstimates: List<SavedEstimateEntity>,
    val history: List<HistoryEntryEntity>,
    val materialExpenses: List<MaterialExpenseEntity>,
    val moneyEntries: List<MoneyEntryEntity>,
    val calculatorState: CalculatorStateEntity,
    val totals: ProjectTotals
)

data class MonthlyBucketStats(
    val monthKey: String, // e.g. "11.2026"
    val monthTitle: String, // e.g. "Ноябрь 2026"
    val earnedTotalRub: Double,
    val cashRub: Double,
    val cardRub: Double,
    val materialsTotalRub: Double,
    val measuredWallAreaSqm: Double,
    val measuredFloorAreaSqm: Double,
    val objectsCount: Int
)

class ZamerRepository(private val db: AppDatabase) {
    private val projectDao = db.projectDao()
    private val roomDao = db.roomDao()
    private val minorWorkDao = db.minorWorkDao()
    private val savedEstimateDao = db.savedEstimateDao()
    private val historyDao = db.historyDao()
    private val calcDao = db.calculatorStateDao()
    private val materialDao = db.materialExpenseDao()
    private val moneyDao = db.moneyEntryDao()

    fun observeProjectSummaries(): Flow<List<ProjectSummaryItem>> {
        return combine(
            projectDao.observeAllProjects(),
            roomDao.observeAllRooms(),
            minorWorkDao.observeAllWorks()
        ) { projects, allRooms, allWorks ->
            projects.map { project ->
                val projectRooms = allRooms.filter { it.projectId == project.id }
                val projectWorks = allWorks.filter { it.projectId == project.id }
                val roomDetails = projectRooms.map { toRoomDetail(it, project) }
                val categories = ZamerJsonCodec.decodeEstimateCategories(project.estimateCategoriesJson)
                val totals = computeProjectTotals(project, roomDetails, projectWorks, categories)
                ProjectSummaryItem(
                    project = project,
                    roomCount = projectRooms.size,
                    totalWallAreaSqm = totals.totalWallAreaSqm,
                    totalFloorAreaSqm = totals.totalInnerFloorAreaSqm,
                    grandTotalRub = totals.grandTotalRub,
                    allWallsMeasured = totals.allWallsMeasured
                )
            }
        }
    }

    fun observeMonthlyStats(): Flow<List<MonthlyBucketStats>> {
        val projectRoomFlow = combine(
            projectDao.observeAllProjects(),
            roomDao.observeAllRooms()
        ) { projects, rooms -> projects to rooms }

        return combine(
            projectRoomFlow,
            moneyDao.observeAll(),
            materialDao.observeAll()
        ) { (projects, allRooms), allMoney, allMaterials ->
            buildMonthlyStats(projects, allRooms, allMoney, allMaterials)
        }
    }

    fun buildMonthlyStats(
        projects: List<ProjectEntity>,
        allRooms: List<RoomEntity>,
        allMoney: List<MoneyEntryEntity>,
        allMaterials: List<MaterialExpenseEntity>
    ): List<MonthlyBucketStats> {
        val currentCal = Calendar.getInstance()
        val currentKey = "%02d.%04d".format(currentCal.get(Calendar.MONTH) + 1, currentCal.get(Calendar.YEAR))

        val keys = linkedSetOf(currentKey)
        projects.forEach { keys.add(extractMonthKey(it.startDate, it.createdAt)) }
        allMoney.forEach { keys.add(extractMonthKey(it.dateStr, it.createdAt)) }
        allMaterials.forEach { keys.add(extractMonthKey(it.purchaseDate, it.createdAt)) }

        return keys.sortedByDescending { key ->
            val parts = key.split('.')
            val m = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val y = parts.getOrNull(1)?.toIntOrNull() ?: 2000
            y * 100 + m
        }.map { monthKey ->
            val monthProjects = projects.filter { extractMonthKey(it.startDate, it.createdAt) == monthKey }
            val monthMoney = allMoney.filter { extractMonthKey(it.dateStr, it.createdAt) == monthKey }
            val monthMaterials = allMaterials.filter { extractMonthKey(it.purchaseDate, it.createdAt) == monthKey }

            var wallSqm = 0.0
            var floorSqm = 0.0
            monthProjects.forEach { proj ->
                val pRooms = allRooms.filter { it.projectId == proj.id }.map { toRoomDetail(it, proj) }
                val totals = computeProjectTotals(proj, pRooms, emptyList(), emptyList())
                wallSqm += totals.totalWallAreaSqm
                floorSqm += totals.totalInnerFloorAreaSqm
            }

            val cashRub = monthMoney.filter { it.paymentMethod == "CASH" }.sumOf { it.amountRub }
            val cardRub = monthMoney.filter { it.paymentMethod == "CARD" }.sumOf { it.amountRub }
            val earnedTotal = cashRub + cardRub
            val materialsRub = monthMaterials.sumOf { it.priceRub }

            MonthlyBucketStats(
                monthKey = monthKey,
                monthTitle = formatMonthTitle(monthKey),
                earnedTotalRub = earnedTotal,
                cashRub = cashRub,
                cardRub = cardRub,
                materialsTotalRub = materialsRub,
                measuredWallAreaSqm = wallSqm,
                measuredFloorAreaSqm = floorSqm,
                objectsCount = monthProjects.size
            )
        }
    }

    private fun extractMonthKey(dateStr: String, fallbackMillis: Long): String {
        val cleaned = dateStr.trim()
        val match = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{2,4})""").find(cleaned)
        if (match != null) {
            val month = match.groupValues[2].toIntOrNull()?.coerceIn(1, 12)
            var year = match.groupValues[3].toIntOrNull()
            if (year != null && year < 100) year += 2000
            if (month != null && year != null) {
                return "%02d.%04d".format(month, year)
            }
        }
        val cal = Calendar.getInstance().apply { time = Date(fallbackMillis) }
        return "%02d.%04d".format(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    private fun formatMonthTitle(monthKey: String): String {
        val parts = monthKey.split('.')
        val m = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val y = parts.getOrNull(1)?.toIntOrNull() ?: 2026
        val names = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
        val name = names.getOrElse(m - 1) { "Месяц" }
        return "$name $y"
    }

    fun observeProjectAggregate(projectId: Long): Flow<ProjectAggregate?> {
        val baseFlow = combine(
            projectDao.observeProject(projectId),
            roomDao.observeRoomsForProject(projectId),
            minorWorkDao.observeWorksForProject(projectId),
            savedEstimateDao.observeEstimatesForProject(projectId)
        ) { project, rooms, works, savedEstimates ->
            Quadruple(project, rooms, works, savedEstimates)
        }
        val extrasFlow = combine(
            historyDao.observeHistoryForProject(projectId),
            materialDao.observeForProject(projectId),
            moneyDao.observeForProject(projectId),
            calcDao.observeState(projectId)
        ) { history, materials, money, calcState ->
            Quadruple(history, materials, money, calcState)
        }
        return combine(baseFlow, extrasFlow) { (project, rooms, works, savedEstimates), (history, materials, money, calcState) ->
            if (project == null) {
                null
            } else {
                val roomDetails = rooms.map { toRoomDetail(it, project) }
                val projectOpenings = ZamerJsonCodec.decodeProjectOpenings(project.projectOpeningsJson)
                val categories = ZamerJsonCodec.decodeEstimateCategories(project.estimateCategoriesJson)
                val totals = computeProjectTotals(project, roomDetails, works, categories)
                ProjectAggregate(
                    project = project,
                    rooms = roomDetails,
                    projectOpenings = projectOpenings,
                    minorWorks = works,
                    estimateCategories = categories,
                    savedEstimates = savedEstimates,
                    history = history,
                    materialExpenses = materials,
                    moneyEntries = money,
                    calculatorState = calcState ?: CalculatorStateEntity(projectId = projectId),
                    totals = totals
                )
            }
        }
    }

    suspend fun createProjectFromTemplate(
        name: String,
        address: String,
        startDate: String,
        templateId: String,
        variantLabel: String? = null,
        hasStretchCeiling: Boolean = false
    ): Long {
        val template = ApartmentTemplates.findById(templateId)
            ?: ApartmentTemplates.allTemplates.first()
        val now = System.currentTimeMillis()
        val fullLayoutTitle = if (!variantLabel.isNullOrBlank() && variantLabel != "Оригинал") {
            "${template.title} · $variantLabel"
        } else {
            template.title
        }
        val defaultCats = ZamerJsonCodec.encodeEstimateCategories(ApartmentTemplates.defaultEstimateCategories)
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name.trim(),
                address = address.trim(),
                startDate = startDate.trim().ifEmpty { Formatters.todayDate() },
                layoutType = template.id,
                layoutTitle = fullLayoutTitle,
                estimateCategoriesJson = defaultCats,
                hasStretchCeiling = hasStretchCeiling,
                createdAt = now,
                updatedAt = now
            )
        )
        val roomEntities = template.rooms.mapIndexed { idx, spec ->
            createRoomEntity(projectId, idx, spec, hasStretchCeiling)
        }
        roomDao.insertRooms(roomEntities)
        calcDao.upsertState(CalculatorStateEntity(projectId = projectId))
        return projectId
    }

    suspend fun createProjectFromDrawing(
        name: String,
        address: String,
        startDate: String,
        drawnRooms: List<Pair<String, List<SnappedSegment>>>,
        hasStretchCeiling: Boolean = false
    ): Long {
        val now = System.currentTimeMillis()
        val allSegments = drawnRooms.flatMap { it.second }
        val defaultCats = ZamerJsonCodec.encodeEstimateCategories(ApartmentTemplates.defaultEstimateCategories)
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name.trim(),
                address = address.trim(),
                startDate = startDate.trim().ifEmpty { Formatters.todayDate() },
                layoutType = "CUSTOM_DRAW",
                layoutTitle = "Свой чертёж",
                customDrawingJson = ZamerJsonCodec.encodeDrawingSegments(allSegments),
                estimateCategoriesJson = defaultCats,
                hasStretchCeiling = hasStretchCeiling,
                createdAt = now,
                updatedAt = now
            )
        )
        val entities = drawnRooms.mapIndexed { idx, (roomName, segments) ->
            val walls = if (segments.isNotEmpty()) {
                segments.mapIndexed { wIdx, seg ->
                    WallMeasurement(
                        index = wIdx + 1,
                        label = "Стена ${wIdx + 1}",
                        hint = FloorPlanSnapper.directionHintForSegment(seg, wIdx),
                        lengthMeters = 0.0
                    )
                }
            } else {
                defaultWalls(4, SimpleRoomShape.RECT.shapeKey)
            }
            RoomEntity(
                projectId = projectId,
                name = roomName,
                sortOrder = idx,
                wallCount = walls.size,
                shapeKey = if (walls.size == 1) "WALL_LINE" else SimpleRoomShape.fromKey(null, walls.size).shapeKey,
                wallsJson = ZamerJsonCodec.encodeWalls(walls),
                hasStretchCeiling = hasStretchCeiling,
                openingsJson = "[]",
                innerCorners = if (walls.size == 1) 0 else walls.size.coerceAtLeast(4),
                outerCorners = 0
            )
        }
        roomDao.insertRooms(entities)
        calcDao.upsertState(CalculatorStateEntity(projectId = projectId))
        return projectId
    }

    suspend fun updateProjectDrawing(
        projectId: Long,
        drawnRooms: List<Pair<String, List<SnappedSegment>>>
    ) {
        val project = projectDao.getProjectById(projectId) ?: return
        val existingRooms = roomDao.getRoomsForProjectSync(projectId).toMutableList()
        val allSegments = drawnRooms.flatMap { it.second }
        val updatedProject = project.copy(
            layoutType = "CUSTOM_DRAW",
            layoutTitle = "Свой чертёж",
            customDrawingJson = ZamerJsonCodec.encodeDrawingSegments(allSegments),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.updateProject(updatedProject)

        val rebuiltEntities = drawnRooms.mapIndexed { idx, (roomName, segments) ->
            val matchIdx = existingRooms.indexOfFirst {
                it.name == roomName && it.wallCount == segments.size
            }.takeIf { it >= 0 } ?: existingRooms.indexOfFirst {
                it.name == roomName
            }.takeIf { it >= 0 } ?: existingRooms.indexOfFirst {
                idx == it.sortOrder
            }
            val matched = if (matchIdx >= 0) existingRooms.removeAt(matchIdx) else null

            val oldWalls = matched?.let { ZamerJsonCodec.decodeWalls(it.wallsJson) }.orEmpty()
            val newWalls = if (segments.isNotEmpty()) {
                segments.mapIndexed { wIdx, seg ->
                    val preservedLen = if (oldWalls.size == segments.size) {
                        oldWalls.getOrNull(wIdx)?.lengthMeters ?: 0.0
                    } else {
                        0.0
                    }
                    WallMeasurement(
                        index = wIdx + 1,
                        label = "Стена ${wIdx + 1}",
                        hint = FloorPlanSnapper.directionHintForSegment(seg, wIdx),
                        lengthMeters = preservedLen
                    )
                }
            } else {
                defaultWalls(4, SimpleRoomShape.RECT.shapeKey)
            }

            RoomEntity(
                projectId = projectId,
                name = roomName,
                sortOrder = idx,
                wallCount = newWalls.size,
                shapeKey = if (newWalls.size == 1) "WALL_LINE" else SimpleRoomShape.fromKey(null, newWalls.size).shapeKey,
                wallsJson = ZamerJsonCodec.encodeWalls(newWalls),
                heightOverrideMeters = matched?.heightOverrideMeters,
                hasStretchCeiling = matched?.hasStretchCeiling ?: project.hasStretchCeiling,
                heightBeforeCeilingMeters = matched?.heightBeforeCeilingMeters,
                heightAfterCeilingMeters = matched?.heightAfterCeilingMeters,
                openingsJson = "[]",
                innerCorners = if (newWalls.size == 1) 0 else newWalls.size.coerceAtLeast(4),
                outerCorners = matched?.outerCorners ?: 0,
                extraCornerMeters = matched?.extraCornerMeters ?: 0.0,
                floorAreaOverrideMeters = matched?.floorAreaOverrideMeters
            )
        }
        roomDao.deleteRoomsForProject(projectId)
        roomDao.insertRooms(rebuiltEntities)
    }

    suspend fun createProjectWithoutPlan(
        name: String,
        address: String,
        startDate: String,
        roomSpecs: List<NoPlanRoomSpec>,
        hasStretchCeiling: Boolean = false
    ): Long {
        val now = System.currentTimeMillis()
        val defaultCats = ZamerJsonCodec.encodeEstimateCategories(ApartmentTemplates.defaultEstimateCategories)
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name.trim(),
                address = address.trim(),
                startDate = startDate.trim().ifEmpty { Formatters.todayDate() },
                layoutType = "NO_PLAN",
                layoutTitle = "Без плана",
                estimateCategoriesJson = defaultCats,
                hasStretchCeiling = hasStretchCeiling,
                createdAt = now,
                updatedAt = now
            )
        )
        val entities = roomSpecs.mapIndexed { idx, spec ->
            val count = spec.shape.wallCount
            val shapeKey = spec.shape.shapeKey
            val (inC, outC) = when (spec.shape) {
                SimpleRoomShape.U_SHAPE -> 6 to 2
                SimpleRoomShape.L_SHAPE -> 5 to 1
                SimpleRoomShape.RECT -> 4 to 0
            }
            RoomEntity(
                projectId = projectId,
                name = spec.name.trim().ifEmpty { "Комната ${idx + 1}" },
                sortOrder = idx,
                wallCount = count,
                shapeKey = shapeKey,
                wallsJson = ZamerJsonCodec.encodeWalls(defaultWalls(count, shapeKey)),
                hasStretchCeiling = hasStretchCeiling,
                openingsJson = "[]",
                innerCorners = inC,
                outerCorners = outC
            )
        }
        roomDao.insertRooms(entities)
        calcDao.upsertState(CalculatorStateEntity(projectId = projectId))
        return projectId
    }

    suspend fun addRoomToProject(
        projectId: Long,
        roomName: String,
        wallCount: Int = 4,
        shapeKey: String = SimpleRoomShape.fromKey(null, wallCount).shapeKey
    ): Long {
        val project = projectDao.getProjectById(projectId) ?: return -1
        val shape = SimpleRoomShape.fromKey(shapeKey, wallCount)
        val count = wallCount.coerceIn(1, 12)
        val (inC, outC) = when {
            count == 1 -> 0 to 0
            shape == SimpleRoomShape.U_SHAPE -> 6 to 2
            shape == SimpleRoomShape.L_SHAPE -> 5 to 1
            else -> count to 0
        }
        val id = roomDao.insertRoom(
            RoomEntity(
                projectId = projectId,
                name = roomName.trim().ifEmpty { "Новая комната" },
                sortOrder = 999,
                wallCount = count,
                shapeKey = if (count == 1) "WALL_LINE" else shape.shapeKey,
                wallsJson = ZamerJsonCodec.encodeWalls(defaultWalls(count, shape.shapeKey)),
                hasStretchCeiling = project.hasStretchCeiling,
                openingsJson = "[]",
                innerCorners = inC,
                outerCorners = outC
            )
        )
        touchProject(project)
        return id
    }

    suspend fun deleteRoom(room: RoomEntity) {
        roomDao.deleteRoomById(room.id)
        projectDao.getProjectById(room.projectId)?.let { touchProject(it) }
    }

    suspend fun renameRoom(room: RoomEntity, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty() || cleanName == room.name) return
        roomDao.updateRoomName(room.id, cleanName)
        projectDao.getProjectById(room.projectId)?.let { touchProject(it) }
    }

    suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun updateWallLength(
        room: RoomEntity,
        wallIndex: Int,
        lengthMeters: Double,
        recordHistory: Boolean = true
    ) {
        val latest = roomDao.getRoomById(room.id) ?: return
        val currentWalls = ZamerJsonCodec.decodeWalls(latest.wallsJson).toMutableList()
        val idx = currentWalls.indexOfFirst { it.index == wallIndex }
        if (idx >= 0) {
            currentWalls[idx] = currentWalls[idx].copy(lengthMeters = lengthMeters.coerceAtLeast(0.0))
        }
        roomDao.updateWallsJson(latest.id, ZamerJsonCodec.encodeWalls(currentWalls))
        projectDao.getProjectById(latest.projectId)?.let { touchProject(it) }
    }

    suspend fun updateProjectWallHeight(projectId: Long, heightMeters: Double) {
        val h = heightMeters.coerceAtLeast(0.0)
        val now = System.currentTimeMillis()
        projectDao.updateProjectWallHeight(projectId, h, now)
        roomDao.updateAllRoomHeights(projectId, h)
    }

    suspend fun updateProjectOpenings(projectId: Long, openings: List<OpeningItem>) {
        val encoded = ZamerJsonCodec.encodeOpenings(openings)
        projectDao.updateProjectOpeningsJson(projectId, encoded, System.currentTimeMillis())
    }

    suspend fun updateProjectNotes(projectId: Long, notes: String) {
        projectDao.updateProjectNotes(projectId, notes, System.currentTimeMillis())
    }

    suspend fun addMaterialExpense(projectId: Long, itemName: String, priceRub: Double, purchaseDate: String) {
        materialDao.insert(
            MaterialExpenseEntity(
                projectId = projectId,
                itemName = itemName.trim(),
                priceRub = priceRub.coerceAtLeast(0.0),
                purchaseDate = purchaseDate.trim()
            )
        )
        projectDao.getProjectById(projectId)?.let { touchProject(it) }
    }

    suspend fun deleteMaterialExpense(id: Long) {
        materialDao.deleteById(id)
    }

    suspend fun addMoneyEntry(
        projectId: Long,
        amountRub: Double,
        paymentMethod: String,
        dateStr: String,
        reasonText: String
    ) {
        moneyDao.insert(
            MoneyEntryEntity(
                projectId = projectId,
                amountRub = amountRub.coerceAtLeast(0.0),
                paymentMethod = paymentMethod,
                dateStr = dateStr.trim(),
                reasonText = reasonText.trim()
            )
        )
        projectDao.getProjectById(projectId)?.let { touchProject(it) }
    }

    suspend fun deleteMoneyEntry(id: Long) {
        moneyDao.deleteById(id)
    }

    suspend fun setProjectStretchCeiling(project: ProjectEntity, hasStretchCeiling: Boolean) {
        projectDao.updateProjectStretchCeiling(project.id, hasStretchCeiling, System.currentTimeMillis())
        roomDao.updateRoomsStretchCeilingForProject(project.id, hasStretchCeiling)
    }

    suspend fun updateRoomCeilingAndHeight(
        room: RoomEntity,
        heightMeters: Double,
        hasStretchCeiling: Boolean,
        heightBeforeCeiling: Double?,
        heightAfterCeiling: Double?,
        applyToAllRooms: Boolean
    ) {
        val h = heightMeters.coerceAtLeast(0.0)
        val project = projectDao.getProjectById(room.projectId) ?: return
        if (applyToAllRooms) {
            roomDao.applyCeilingAndHeightToAllRooms(
                projectId = room.projectId,
                heightOverride = h,
                hasStretchCeiling = hasStretchCeiling,
                heightBefore = heightBeforeCeiling,
                heightAfter = heightAfterCeiling
            )
            projectDao.updateProject(
                project.copy(
                    wallHeightMeters = h,
                    hasStretchCeiling = hasStretchCeiling,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            roomDao.updateCeilingAndHeight(
                roomId = room.id,
                heightOverride = h,
                hasStretchCeiling = hasStretchCeiling,
                heightBefore = heightBeforeCeiling,
                heightAfter = heightAfterCeiling
            )
            touchProject(project)
        }
    }

    suspend fun updateRoomOpenings(
        room: RoomEntity,
        openings: List<OpeningItem>,
        recordHistory: Boolean = true
    ) {
        val encoded = ZamerJsonCodec.encodeOpenings(openings)
        roomDao.updateOpeningsJson(room.id, encoded)
        projectDao.getProjectById(room.projectId)?.let { touchProject(it) }
    }

    suspend fun updateRoomCorners(
        room: RoomEntity,
        innerCorners: Int,
        outerCorners: Int,
        extraMeters: Double,
        recordHistory: Boolean = true
    ) {
        val latest = roomDao.getRoomById(room.id) ?: return
        val updated = latest.copy(
            innerCorners = innerCorners.coerceAtLeast(0),
            outerCorners = outerCorners.coerceAtLeast(0),
            extraCornerMeters = extraMeters.coerceAtLeast(0.0)
        )
        roomDao.updateRoom(updated)
        projectDao.getProjectById(latest.projectId)?.let { touchProject(it) }
    }

    suspend fun updateRoomFloorOverride(room: RoomEntity, overrideArea: Double?) {
        roomDao.updateFloorOverride(room.id, overrideArea?.coerceAtLeast(0.0))
        projectDao.getProjectById(room.projectId)?.let { touchProject(it) }
    }

    suspend fun updateProjectWallThickness(project: ProjectEntity, thicknessMeters: Double) {
        val t = thicknessMeters.coerceAtLeast(0.0)
        projectDao.updateProject(
            project.copy(wallThicknessMeters = t, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun updateEstimateCategories(
        project: ProjectEntity,
        categories: List<EstimateCategoryConfig>,
        useOuterFloor: Boolean
    ) {
        projectDao.updateProject(
            project.copy(
                estimateCategoriesJson = ZamerJsonCodec.encodeEstimateCategories(categories),
                useOuterFloorInEstimate = useOuterFloor,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveEstimateSnapshot(
        projectId: Long,
        title: String,
        summaryText: String,
        grandTotalRub: Double
    ): Long {
        val id = savedEstimateDao.insertEstimate(
            SavedEstimateEntity(
                projectId = projectId,
                title = title,
                summaryText = summaryText,
                grandTotalRub = grandTotalRub
            )
        )
        projectDao.getProjectById(projectId)?.let { touchProject(it) }
        return id
    }

    suspend fun deleteSavedEstimate(id: Long) {
        savedEstimateDao.deleteEstimateById(id)
    }

    suspend fun addOrUpdateMinorWork(work: MinorWorkEntity) {
        if (work.id == 0L) {
            minorWorkDao.insertWork(work)
        } else {
            minorWorkDao.updateWork(work)
        }
        projectDao.getProjectById(work.projectId)?.let { touchProject(it) }
    }

    suspend fun deleteMinorWork(work: MinorWorkEntity) {
        minorWorkDao.deleteWorkById(work.id)
        projectDao.getProjectById(work.projectId)?.let { touchProject(it) }
    }

    suspend fun saveCalculatorState(
        projectId: Long,
        expressionText: String,
        currentInput: String,
        resultPreview: Double,
        activeUnit: String
    ) {
        calcDao.upsertState(
            CalculatorStateEntity(
                projectId = projectId,
                expressionText = expressionText,
                currentInput = currentInput,
                resultPreview = resultPreview,
                activeUnit = activeUnit,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun logHistory(
        projectId: Long,
        source: String,
        title: String,
        expression: String,
        resultValue: Double?,
        unit: String = ""
    ) {
        historyDao.insertEntry(
            HistoryEntryEntity(
                projectId = projectId,
                timestamp = System.currentTimeMillis(),
                source = source,
                title = title,
                expression = expression,
                resultValue = resultValue,
                unit = unit
            )
        )
    }

    suspend fun clearHistory(projectId: Long) {
        historyDao.clearHistoryForProject(projectId)
    }

    private suspend fun touchProject(project: ProjectEntity) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    private fun toRoomDetail(room: RoomEntity, project: ProjectEntity): RoomDetail {
        val walls = ZamerJsonCodec.decodeWalls(room.wallsJson)
        val h = project.wallHeightMeters
        val calc = RoomGeometryHelper.calculateRoom(
            walls = walls,
            heightMeters = h,
            wallThicknessMeters = project.wallThicknessMeters,
            openings = emptyList(),
            innerCorners = room.innerCorners,
            outerCorners = room.outerCorners,
            extraCornerMeters = room.extraCornerMeters,
            floorAreaOverrideMeters = room.floorAreaOverrideMeters
        )
        return RoomDetail(
            entity = room,
            walls = walls,
            openings = emptyList(),
            calculations = calc
        )
    }

    fun quantityForCategory(
        cat: EstimateCategoryConfig,
        wallArea: Double,
        floorArea: Double,
        cornersMeters: Double
    ): Double {
        if (cat.usePartial) {
            return cat.partialQuantity.coerceAtLeast(0.0)
        }
        return when (cat.sourceKind) {
            "WALLS" -> wallArea
            "FLOOR" -> floorArea
            "CORNERS" -> cornersMeters
            else -> if (cat.customQuantity > 0.0) cat.customQuantity else wallArea
        }
    }

    fun computeProjectTotals(
        project: ProjectEntity,
        rooms: List<RoomDetail>,
        works: List<MinorWorkEntity>,
        categories: List<EstimateCategoryConfig>
    ): ProjectTotals {
        val grossWallArea = rooms.sumOf { it.calculations.grossWallAreaSqm }
        val projectOpeningsArea = ZamerJsonCodec.decodeProjectOpenings(project.projectOpeningsJson)
            .sumOf { it.totalAreaSqm }
        val netWallArea = (grossWallArea - projectOpeningsArea).coerceAtLeast(0.0)

        val innerFloor = rooms.sumOf { it.calculations.innerFloorAreaSqm }
        val outerFloor = rooms.sumOf { it.calculations.outerFloorAreaSqm }
        val inCorners = rooms.sumOf { it.calculations.innerCornersMeters }
        val outCorners = rooms.sumOf { it.calculations.outerCornersMeters }
        val extraCorners = rooms.sumOf { it.calculations.extraCornersMeters }
        val totalCorners = rooms.sumOf { it.calculations.totalCornersMeters }
        val minorWorksTotal = works.sumOf { it.totalCost }

        val floorForEstimate = if (project.useOuterFloorInEstimate) outerFloor else innerFloor
        val categoriesTotal = categories
            .filter { it.pricePerUnit > 0.0 }
            .sumOf { cat ->
                val qty = quantityForCategory(cat, netWallArea, floorForEstimate, totalCorners)
                qty * cat.pricePerUnit
            }

        val allMeasured = rooms.isNotEmpty() && rooms.all { r ->
            r.walls.isNotEmpty() && r.walls.all { it.lengthMeters > 0.0 }
        }

        return ProjectTotals(
            totalWallAreaSqm = netWallArea,
            totalGrossWallAreaSqm = grossWallArea,
            totalInnerFloorAreaSqm = innerFloor,
            totalOuterFloorAreaSqm = outerFloor,
            totalCornersMeters = totalCorners,
            totalInnerCornersMeters = inCorners,
            totalOuterCornersMeters = outCorners,
            totalExtraCornersMeters = extraCorners,
            totalOpeningsAreaSqm = projectOpeningsArea,
            minorWorksTotalRub = minorWorksTotal,
            categoriesTotalRub = categoriesTotal,
            grandTotalRub = categoriesTotal + minorWorksTotal,
            allWallsMeasured = allMeasured
        )
    }

    private fun createRoomEntity(
        projectId: Long,
        sortOrder: Int,
        spec: TemplateRoomSpec,
        hasStretchCeiling: Boolean
    ): RoomEntity {
        val walls = (0 until spec.wallCount).map { idx ->
            WallMeasurement(
                index = idx + 1,
                label = "Стена ${idx + 1}",
                hint = spec.wallHints.getOrElse(idx) { "Стена ${idx + 1}. Мерь по низу, у пола" },
                lengthMeters = 0.0
            )
        }
        return RoomEntity(
            projectId = projectId,
            name = spec.name,
            sortOrder = sortOrder,
            wallCount = spec.wallCount,
            shapeKey = SimpleRoomShape.fromKey(null, spec.wallCount).shapeKey,
            wallsJson = ZamerJsonCodec.encodeWalls(walls),
            hasStretchCeiling = hasStretchCeiling,
            openingsJson = "[]",
            innerCorners = spec.defaultInnerCorners,
            outerCorners = spec.defaultOuterCorners
        )
    }

    private fun defaultWalls(count: Int, shapeKey: String = "AUTO"): List<WallMeasurement> {
        val uHints = listOf(
            "Верхняя длинная стена (спинка П). Мерь по низу, у пола",
            "Правая внешняя стена. Мерь по низу, у пола",
            "Торец правого выступа. Мерь по низу, у пола",
            "Правая внутренняя стена ниши. Мерь по низу, у пола",
            "Задняя стена ниши. Мерь по низу, у пола",
            "Левая внутренняя стена ниши. Мерь по низу, у пола",
            "Торец левого выступа. Мерь по низу, у пола",
            "Левая внешняя стена. Мерь по низу, у пола"
        )
        val lHints = listOf(
            "Верхняя стена (длинная). Мерь по низу, у пола",
            "Правая верхняя стена. Мерь по низу, у пола",
            "Внутренний уступ. Мерь по низу, у пола",
            "Правая нижняя стена. Мерь по низу, у пола",
            "Нижняя стена. Мерь по низу, у пола",
            "Левая стена (длинная). Мерь по низу, у пола"
        )
        val rectHints = listOf(
            "От входной двери направо. Мерь по низу, у пола",
            "Вторая стена (часто с окном). Мерь от угла до угла у пола",
            "Противоположная стена. Мерь по низу, у плинтуса",
            "Последняя стена к двери. Мерь от угла до угла у пола"
        )
        val hints = when {
            count == 1 -> listOf("Отдельная стена / ригель. Введите длину в метрах")
            shapeKey == "U_SHAPE" || (shapeKey == "AUTO" && count == 8) -> uHints
            shapeKey == "L_SHAPE" || (shapeKey == "AUTO" && count == 6) -> lHints
            else -> rectHints
        }
        return List(count) { idx ->
            WallMeasurement(
                index = idx + 1,
                label = "Стена ${idx + 1}",
                hint = hints.getOrElse(idx) { "Стена ${idx + 1}. Мерь по низу, у пола" },
                lengthMeters = 0.0
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
