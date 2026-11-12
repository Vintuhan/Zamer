package com.example.data

import com.example.domain.ApartmentTemplates
import com.example.domain.EstimateCategoryConfig
import com.example.domain.FloorPlanSnapper
import com.example.domain.Formatters
import com.example.domain.OpeningItem
import com.example.domain.ProjectTotals
import com.example.domain.RoomCalculations
import com.example.domain.RoomGeometryHelper
import com.example.domain.SnappedSegment
import com.example.domain.TemplateRoomSpec
import com.example.domain.WallMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

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
    val grandTotalRub: Double
)

data class ProjectAggregate(
    val project: ProjectEntity,
    val rooms: List<RoomDetail>,
    val minorWorks: List<MinorWorkEntity>,
    val estimateCategories: List<EstimateCategoryConfig>,
    val savedEstimates: List<SavedEstimateEntity>,
    val history: List<HistoryEntryEntity>,
    val calculatorState: CalculatorStateEntity,
    val totals: ProjectTotals
)

class ZamerRepository(private val db: AppDatabase) {
    private val projectDao = db.projectDao()
    private val roomDao = db.roomDao()
    private val minorWorkDao = db.minorWorkDao()
    private val savedEstimateDao = db.savedEstimateDao()
    private val historyDao = db.historyDao()
    private val calcDao = db.calculatorStateDao()

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
                    grandTotalRub = totals.grandTotalRub
                )
            }
        }
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
        return combine(
            baseFlow,
            historyDao.observeHistoryForProject(projectId),
            calcDao.observeState(projectId)
        ) { (project, rooms, works, savedEstimates), history, calcState ->
            if (project == null) {
                null
            } else {
                val roomDetails = rooms.map { toRoomDetail(it, project) }
                val categories = ZamerJsonCodec.decodeEstimateCategories(project.estimateCategoriesJson)
                val totals = computeProjectTotals(project, roomDetails, works, categories)
                ProjectAggregate(
                    project = project,
                    rooms = roomDetails,
                    minorWorks = works,
                    estimateCategories = categories,
                    savedEstimates = savedEstimates,
                    history = history,
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
        templateId: String
    ): Long {
        val template = ApartmentTemplates.findById(templateId)
            ?: ApartmentTemplates.allTemplates.first()
        val now = System.currentTimeMillis()
        val defaultCats = ZamerJsonCodec.encodeEstimateCategories(ApartmentTemplates.defaultEstimateCategories)
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name.trim(),
                address = address.trim(),
                startDate = startDate.trim().ifEmpty { Formatters.todayDate() },
                layoutType = template.id,
                layoutTitle = template.title,
                estimateCategoriesJson = defaultCats,
                createdAt = now,
                updatedAt = now
            )
        )
        val roomEntities = template.rooms.mapIndexed { idx, spec ->
            createRoomEntity(projectId, idx, spec)
        }
        roomDao.insertRooms(roomEntities)
        calcDao.upsertState(CalculatorStateEntity(projectId = projectId))
        logHistory(
            projectId = projectId,
            source = "MEASURE",
            title = "Создан объект",
            expression = "${name.trim()} (${template.title}), комнат: ${roomEntities.size}",
            resultValue = roomEntities.size.toDouble(),
            unit = "комн."
        )
        return projectId
    }

    suspend fun createProjectFromDrawing(
        name: String,
        address: String,
        startDate: String,
        drawnRooms: List<Pair<String, List<SnappedSegment>>>
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
                defaultWalls(4)
            }
            RoomEntity(
                projectId = projectId,
                name = roomName,
                sortOrder = idx,
                wallCount = walls.size,
                wallsJson = ZamerJsonCodec.encodeWalls(walls),
                openingsJson = ZamerJsonCodec.encodeOpenings(ZamerJsonCodec.defaultOpenings()),
                innerCorners = walls.size.coerceAtLeast(4),
                outerCorners = 0
            )
        }
        roomDao.insertRooms(entities)
        calcDao.upsertState(CalculatorStateEntity(projectId = projectId))
        logHistory(
            projectId = projectId,
            source = "MEASURE",
            title = "Создан объект (свой чертёж)",
            expression = "${name.trim()}, комнат: ${entities.size}",
            resultValue = entities.size.toDouble(),
            unit = "комн."
        )
        return projectId
    }

    suspend fun createProjectWithoutPlan(
        name: String,
        address: String,
        startDate: String,
        roomSpecs: List<Pair<String, Int>>
    ): Long {
        val now = System.currentTimeMillis()
        val defaultCats = ZamerJsonCodec.encodeEstimateCategories(ApartmentTemplates.defaultEstimateCategories)
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name.trim(),
                address = address.trim(),
                startDate = startDate.trim().ifEmpty { Formatters.todayDate() },
                layoutType = "NO_PLAN",
                layoutTitle = "Без чертежа",
                estimateCategoriesJson = defaultCats,
                createdAt = now,
                updatedAt = now
            )
        )
        val entities = roomSpecs.mapIndexed { idx, (roomName, wallCount) ->
            val count = wallCount.coerceIn(1, 12)
            RoomEntity(
                projectId = projectId,
                name = roomName.trim().ifEmpty { "Комната ${idx + 1}" },
                sortOrder = idx,
                wallCount = count,
                wallsJson = ZamerJsonCodec.encodeWalls(defaultWalls(count)),
                openingsJson = ZamerJsonCodec.encodeOpenings(ZamerJsonCodec.defaultOpenings()),
                innerCorners = count,
                outerCorners = 0
            )
        }
        roomDao.insertRooms(entities)
        calcDao.upsertState(CalculatorStateEntity(projectId = projectId))
        logHistory(
            projectId = projectId,
            source = "MEASURE",
            title = "Создан объект (без чертежа)",
            expression = "${name.trim()}, комнат: ${entities.size}",
            resultValue = entities.size.toDouble(),
            unit = "комн."
        )
        return projectId
    }

    suspend fun addRoomToProject(projectId: Long, roomName: String, wallCount: Int = 4): Long {
        val project = projectDao.getProjectById(projectId) ?: return -1
        val count = wallCount.coerceIn(1, 12)
        val id = roomDao.insertRoom(
            RoomEntity(
                projectId = projectId,
                name = roomName.trim().ifEmpty { "Новая комната" },
                sortOrder = 999,
                wallCount = count,
                wallsJson = ZamerJsonCodec.encodeWalls(defaultWalls(count)),
                openingsJson = ZamerJsonCodec.encodeOpenings(ZamerJsonCodec.defaultOpenings()),
                innerCorners = count,
                outerCorners = 0
            )
        )
        touchProject(project)
        logHistory(
            projectId = projectId,
            source = "MEASURE",
            title = "Добавлена комната",
            expression = "$roomName ($count стен)",
            resultValue = count.toDouble(),
            unit = "стен"
        )
        return id
    }

    suspend fun deleteRoom(room: RoomEntity) {
        roomDao.deleteRoomById(room.id)
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
        val currentWalls = ZamerJsonCodec.decodeWalls(room.wallsJson).toMutableList()
        val idx = currentWalls.indexOfFirst { it.index == wallIndex }
        if (idx >= 0) {
            currentWalls[idx] = currentWalls[idx].copy(lengthMeters = lengthMeters.coerceAtLeast(0.0))
        }
        roomDao.updateRoom(room.copy(wallsJson = ZamerJsonCodec.encodeWalls(currentWalls)))
        projectDao.getProjectById(room.projectId)?.let { touchProject(it) }
        if (recordHistory && lengthMeters > 0.0) {
            logHistory(
                projectId = room.projectId,
                source = "MEASURE",
                title = "${room.name} — Стена $wallIndex",
                expression = "Длина стены $wallIndex: ${Formatters.formatMeters(lengthMeters)}",
                resultValue = lengthMeters,
                unit = "м"
            )
        }
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
        val updatedRoom = room.copy(
            heightOverrideMeters = h,
            hasStretchCeiling = hasStretchCeiling,
            heightBeforeCeilingMeters = heightBeforeCeiling,
            heightAfterCeilingMeters = heightAfterCeiling
        )
        roomDao.updateRoom(updatedRoom)
        if (applyToAllRooms) {
            projectDao.updateProject(
                project.copy(wallHeightMeters = h, updatedAt = System.currentTimeMillis())
            )
        } else {
            touchProject(project)
        }
        val ceilingStatus = if (hasStretchCeiling) "после натяжного потолка" else "до натяжных потолков"
        logHistory(
            projectId = room.projectId,
            source = "MEASURE",
            title = "${room.name} — Высота ($ceilingStatus)",
            expression = "Высота стен: ${Formatters.formatMeters(h)} ($ceilingStatus)",
            resultValue = h,
            unit = "м"
        )
    }

    suspend fun updateRoomOpenings(
        room: RoomEntity,
        openings: List<OpeningItem>,
        recordHistory: Boolean = true
    ) {
        roomDao.updateRoom(room.copy(openingsJson = ZamerJsonCodec.encodeOpenings(openings)))
        projectDao.getProjectById(room.projectId)?.let { touchProject(it) }
        if (recordHistory) {
            val totalArea = openings.sumOf { it.totalAreaSqm }
            logHistory(
                projectId = room.projectId,
                source = "MEASURE",
                title = "${room.name} — Проёмы",
                expression = if (openings.isEmpty() || totalArea == 0.0) {
                    "Окна и двери выкинуты (без вычета)"
                } else {
                    "Площадь проёмов: ${Formatters.formatArea(totalArea)}"
                },
                resultValue = totalArea,
                unit = "м²"
            )
        }
    }

    suspend fun updateRoomCorners(
        room: RoomEntity,
        innerCorners: Int,
        outerCorners: Int,
        extraMeters: Double,
        recordHistory: Boolean = true
    ) {
        val updated = room.copy(
            innerCorners = innerCorners.coerceAtLeast(0),
            outerCorners = outerCorners.coerceAtLeast(0),
            extraCornerMeters = extraMeters.coerceAtLeast(0.0)
        )
        roomDao.updateRoom(updated)
        val project = projectDao.getProjectById(room.projectId)
        if (project != null) {
            touchProject(project)
            if (recordHistory) {
                val h = effectiveHeight(room, project)
                val total = (updated.innerCorners + updated.outerCorners) * h + updated.extraCornerMeters
                logHistory(
                    projectId = room.projectId,
                    source = "MEASURE",
                    title = "${room.name} — Уголки",
                    expression = "Внутр: ${updated.innerCorners}, внеш: ${updated.outerCorners}, доп: ${Formatters.formatRunningMeters(updated.extraCornerMeters)}",
                    resultValue = total,
                    unit = "м.п."
                )
            }
        }
    }

    suspend fun updateRoomFloorOverride(room: RoomEntity, overrideArea: Double?) {
        roomDao.updateRoom(room.copy(floorAreaOverrideMeters = overrideArea?.coerceAtLeast(0.0)))
        projectDao.getProjectById(room.projectId)?.let { touchProject(it) }
    }

    suspend fun updateProjectWallThickness(project: ProjectEntity, thicknessMeters: Double) {
        val t = thicknessMeters.coerceAtLeast(0.0)
        projectDao.updateProject(
            project.copy(wallThicknessMeters = t, updatedAt = System.currentTimeMillis())
        )
        logHistory(
            projectId = project.id,
            source = "MEASURE",
            title = "Толщина стен",
            expression = "Толщина стен: ${Formatters.formatMeters(t)}",
            resultValue = t,
            unit = "м"
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
        logHistory(
            projectId = projectId,
            source = "ESTIMATE",
            title = "Сохранена смета",
            expression = "$title — ${Formatters.formatMoney(grandTotalRub)}",
            resultValue = grandTotalRub,
            unit = "₽"
        )
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
        logHistory(
            projectId = work.projectId,
            source = "WORK",
            title = "Доп. работа: ${work.title}",
            expression = "${work.title} — ${Formatters.formatMoney(work.totalCost)}",
            resultValue = work.totalCost,
            unit = "₽"
        )
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

    private fun effectiveHeight(room: RoomEntity, project: ProjectEntity): Double {
        return if (room.hasStretchCeiling) {
            room.heightAfterCeilingMeters ?: room.heightOverrideMeters ?: project.wallHeightMeters
        } else {
            room.heightBeforeCeilingMeters ?: room.heightOverrideMeters ?: project.wallHeightMeters
        }
    }

    private fun toRoomDetail(room: RoomEntity, project: ProjectEntity): RoomDetail {
        val walls = ZamerJsonCodec.decodeWalls(room.wallsJson)
        val openings = ZamerJsonCodec.decodeOpenings(room.openingsJson)
        val h = effectiveHeight(room, project)
        val calc = RoomGeometryHelper.calculateRoom(
            walls = walls,
            heightMeters = h,
            wallThicknessMeters = project.wallThicknessMeters,
            openings = openings,
            innerCorners = room.innerCorners,
            outerCorners = room.outerCorners,
            extraCornerMeters = room.extraCornerMeters,
            floorAreaOverrideMeters = room.floorAreaOverrideMeters
        )
        return RoomDetail(
            entity = room,
            walls = walls,
            openings = openings,
            calculations = calc
        )
    }

    fun quantityForCategory(
        cat: EstimateCategoryConfig,
        wallArea: Double,
        floorArea: Double,
        cornersMeters: Double
    ): Double {
        return when (cat.sourceKind) {
            "WALLS" -> wallArea
            "FLOOR" -> floorArea
            "CORNERS" -> cornersMeters
            else -> if (cat.customQuantity > 0.0) cat.customQuantity else wallArea
        }
    }

    private fun computeProjectTotals(
        project: ProjectEntity,
        rooms: List<RoomDetail>,
        works: List<MinorWorkEntity>,
        categories: List<EstimateCategoryConfig>
    ): ProjectTotals {
        val wallArea = rooms.sumOf { it.calculations.netWallAreaSqm }
        val grossWallArea = rooms.sumOf { it.calculations.grossWallAreaSqm }
        val innerFloor = rooms.sumOf { it.calculations.innerFloorAreaSqm }
        val outerFloor = rooms.sumOf { it.calculations.outerFloorAreaSqm }
        val inCorners = rooms.sumOf { it.calculations.innerCornersMeters }
        val outCorners = rooms.sumOf { it.calculations.outerCornersMeters }
        val extraCorners = rooms.sumOf { it.calculations.extraCornersMeters }
        val totalCorners = rooms.sumOf { it.calculations.totalCornersMeters }
        val openingsArea = rooms.sumOf { it.calculations.openingsAreaSqm }
        val minorWorksTotal = works.sumOf { it.totalCost }

        val floorForEstimate = if (project.useOuterFloorInEstimate) outerFloor else innerFloor
        val categoriesTotal = categories
            .filter { it.isSelected && it.pricePerUnit > 0.0 }
            .sumOf { cat ->
                val qty = quantityForCategory(cat, wallArea, floorForEstimate, totalCorners)
                qty * cat.pricePerUnit
            }

        return ProjectTotals(
            totalWallAreaSqm = wallArea,
            totalGrossWallAreaSqm = grossWallArea,
            totalInnerFloorAreaSqm = innerFloor,
            totalOuterFloorAreaSqm = outerFloor,
            totalCornersMeters = totalCorners,
            totalInnerCornersMeters = inCorners,
            totalOuterCornersMeters = outCorners,
            totalExtraCornersMeters = extraCorners,
            totalOpeningsAreaSqm = openingsArea,
            minorWorksTotalRub = minorWorksTotal,
            categoriesTotalRub = categoriesTotal,
            grandTotalRub = categoriesTotal + minorWorksTotal
        )
    }

    private fun createRoomEntity(
        projectId: Long,
        sortOrder: Int,
        spec: TemplateRoomSpec
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
            wallsJson = ZamerJsonCodec.encodeWalls(walls),
            openingsJson = ZamerJsonCodec.encodeOpenings(ZamerJsonCodec.defaultOpenings()),
            innerCorners = spec.defaultInnerCorners,
            outerCorners = spec.defaultOuterCorners
        )
    }

    private fun defaultWalls(count: Int): List<WallMeasurement> {
        val hints = listOf(
            "От входной двери направо. Мерь по низу, у пола",
            "Вторая стена (часто с окном). Мерь от угла до угла у пола",
            "Противоположная стена. Мерь по низу, у плинтуса",
            "Последняя стена к двери. Мерь от угла до угла у пола"
        )
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
