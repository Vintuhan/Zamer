package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AddressLookupUiState
import com.example.data.AppDatabase
import com.example.data.FirebaseAddressLayoutAiRepository
import com.example.data.HistoryEntryEntity
import com.example.data.MinorWorkEntity
import com.example.data.MonthlyBucketStats
import com.example.data.ProjectAggregate
import com.example.data.ProjectEntity
import com.example.data.ProjectSummaryItem
import com.example.data.RoomEntity
import com.example.data.SketchLayoutAiRepository
import com.example.data.SketchMatchUiState
import com.example.data.ZamerJsonCodec
import com.example.data.ZamerRepository
import com.example.domain.ConstructorRoom
import com.example.domain.ConstructorShapeKind
import com.example.domain.EstimateCategoryConfig
import com.example.domain.Formatters
import com.example.domain.LayoutConstructorEngine
import com.example.domain.NoPlanRoomSpec
import com.example.domain.OpeningItem
import com.example.domain.SimpleRoomShape
import com.example.domain.SnappedSegment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

data class ProjectDraft(
    val name: String = "",
    val address: String = "",
    val startDate: String = Formatters.todayDate(),
    val hasStretchCeiling: Boolean = false
)

data class CalculatorUiState(
    val expression: String = "",
    val currentInput: String = "0",
    val livePreview: Double = 0.0,
    val activeUnit: String = ""
)

data class AiWorkSuggestion(
    val title: String,
    val suggestedPrice: Double,
    val reason: String
)

object CalculatorExpressionEvaluator {
    private sealed interface Token {
        data class Num(val value: Double) : Token
        data class Op(val ch: Char) : Token
        data object LParen : Token
        data object RParen : Token
    }

    fun evaluate(expr: String): Double {
        val rawTokens = tokenize(expr)
        if (rawTokens.isEmpty()) return 0.0

        // Strip trailing binary operators cleanly for live mid-expression preview
        val tokens = rawTokens.toMutableList()
        while (tokens.isNotEmpty() && tokens.last() is Token.Op) {
            tokens.removeAt(tokens.lastIndex)
        }
        if (tokens.isEmpty()) return 0.0

        // Auto-balance any unclosed parentheses for clean live preview
        var openCount = 0
        tokens.forEach { tok ->
            when (tok) {
                Token.LParen -> openCount++
                Token.RParen -> if (openCount > 0) openCount--
                else -> Unit
            }
        }
        repeat(openCount) {
            if (tokens.lastOrNull() is Token.LParen) {
                tokens.removeAt(tokens.lastIndex)
            } else {
                tokens.add(Token.RParen)
            }
        }
        while (tokens.isNotEmpty() && tokens.last() is Token.Op) {
            tokens.removeAt(tokens.lastIndex)
        }
        if (tokens.isEmpty()) return 0.0

        var pos = 0

        fun parseExpr(): Double {
            var left = parseTerm()
            while (pos < tokens.size) {
                val t = tokens[pos]
                if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
                    pos++
                    val right = parseTerm()
                    left = if (t.ch == '+') left + right else left - right
                } else {
                    break
                }
            }
            return left
        }

        fun parseTerm(): Double {
            var left = parseFactor()
            while (pos < tokens.size) {
                val t = tokens[pos]
                if (t is Token.Op && (t.ch == '*' || t.ch == '/')) {
                    pos++
                    val right = parseFactor()
                    left = if (t.ch == '*') {
                        left * right
                    } else {
                        if (abs(right) < 1e-12) 0.0 else left / right
                    }
                } else {
                    break
                }
            }
            return left
        }

        fun parseFactor(): Double {
            if (pos >= tokens.size) return 0.0
            return when (val t = tokens[pos]) {
                is Token.Op -> {
                    if (t.ch == '-') {
                        pos++
                        -parseFactor()
                    } else if (t.ch == '+') {
                        pos++
                        parseFactor()
                    } else {
                        pos++
                        0.0
                    }
                }
                Token.LParen -> {
                    pos++
                    val v = parseExpr()
                    if (pos < tokens.size && tokens[pos] == Token.RParen) pos++
                    v
                }
                is Token.Num -> {
                    pos++
                    t.value
                }
                Token.RParen -> {
                    pos++
                    0.0
                }
            }
        }

        val result = parseExpr()
        return if (result.isNaN() || result.isInfinite()) 0.0 else result
    }

    private fun tokenize(input: String): List<Token> {
        val out = mutableListOf<Token>()
        val s = input.replace(',', '.')
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c == '(' -> {
                    out.add(Token.LParen)
                    i++
                }
                c == ')' -> {
                    out.add(Token.RParen)
                    i++
                }
                c == '+' -> {
                    out.add(Token.Op('+'))
                    i++
                }
                c == '-' || c == '−' -> {
                    out.add(Token.Op('-'))
                    i++
                }
                c == '*' || c == '×' -> {
                    out.add(Token.Op('*'))
                    i++
                }
                c == '/' || c == '÷' -> {
                    out.add(Token.Op('/'))
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    val num = s.substring(start, i).toDoubleOrNull()
                    if (num != null) out.add(Token.Num(num))
                }
                else -> i++
            }
        }
        return out
    }
}

class ZamerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val GLOBAL_CALC_PROJECT_ID = -1L
        private const val GLOBAL_CALC_PREFS = "global_calculator_prefs"
        private const val KEY_EXPR = "expr"
        private const val KEY_INPUT = "input"
        private const val KEY_PREVIEW = "preview"
        private const val KEY_UNIT = "unit"
        private const val KEY_HISTORY = "history"
    }

    private val repository = ZamerRepository(AppDatabase.getInstance(application))
    private val addressAiRepository = FirebaseAddressLayoutAiRepository()
    private val sketchAiRepository = SketchLayoutAiRepository()
    private val globalPrefs = application.getSharedPreferences(GLOBAL_CALC_PREFS, Context.MODE_PRIVATE)
    private var addressLookupJob: Job? = null
    private var sketchMatchJob: Job? = null

    val projectSummaries: StateFlow<List<ProjectSummaryItem>> = repository.observeProjectSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyStats: StateFlow<List<MonthlyBucketStats>> = repository.observeMonthlyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeProject: StateFlow<ProjectAggregate?> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null || id <= 0L) flowOf(null) else repository.observeProjectAggregate(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _draft = MutableStateFlow(ProjectDraft())
    val draft: StateFlow<ProjectDraft> = _draft.asStateFlow()

    private val _calcUi = MutableStateFlow(CalculatorUiState())
    val calcUi: StateFlow<CalculatorUiState> = _calcUi.asStateFlow()

    private val _globalCalcHistory = MutableStateFlow<List<HistoryEntryEntity>>(loadGlobalCalcHistory())
    val globalCalcHistory: StateFlow<List<HistoryEntryEntity>> = _globalCalcHistory.asStateFlow()

    private val _addressLookupState = MutableStateFlow<AddressLookupUiState>(AddressLookupUiState.Idle)
    val addressLookupState: StateFlow<AddressLookupUiState> = _addressLookupState.asStateFlow()

    private val _sketchMatchState = MutableStateFlow<SketchMatchUiState>(SketchMatchUiState.Idle)
    val sketchMatchState: StateFlow<SketchMatchUiState> = _sketchMatchState.asStateFlow()

    private var loadedCalcProjectId: Long? = null

    fun updateDraft(
        name: String? = null,
        address: String? = null,
        startDate: String? = null,
        hasStretchCeiling: Boolean? = null
    ) {
        val cur = _draft.value
        _draft.value = cur.copy(
            name = name ?: cur.name,
            address = address ?: cur.address,
            startDate = startDate ?: cur.startDate,
            hasStretchCeiling = hasStretchCeiling ?: cur.hasStretchCeiling
        )
    }

    fun resetDraft() {
        _draft.value = ProjectDraft(startDate = Formatters.todayDate(), hasStretchCeiling = false)
        _addressLookupState.value = AddressLookupUiState.Idle
        _sketchMatchState.value = SketchMatchUiState.Idle
    }

    fun searchLayoutByAddress(address: String, roomCountHint: Int? = null) {
        val query = address.trim()
        if (query.length < 4) {
            _addressLookupState.value = AddressLookupUiState.Error(
                "Введите точный адрес дома (город, улица и номер дома)"
            )
            return
        }
        addressLookupJob?.cancel()
        addressLookupJob = viewModelScope.launch {
            _addressLookupState.value = AddressLookupUiState.Loading(
                "Ищем дом в OpenStreetMap, МинЖКХ и PhotoBuildings..."
            )
            val evidence = addressAiRepository.collectHouseEvidence(query)
            val foundSources = buildList {
                if (evidence.osm.foundOnMap) add("OSM")
                if (evidence.minZhkh.found) add("МинЖКХ")
                if (evidence.photoBuildings.found) add("PhotoBuildings")
            }
            _addressLookupState.value = AddressLookupUiState.Loading(
                if (foundSources.isNotEmpty()) {
                    "Найдено (${foundSources.joinToString(", ")})! ИИ определяет серию и планировки..."
                } else {
                    "ИИ анализирует адрес, возможную серию и планировки..."
                }
            )
            val result = addressAiRepository.analyzeAddressAndMatchLayouts(
                address = query,
                roomCountHint = roomCountHint,
                evidence = evidence
            )
            _addressLookupState.value = result.fold(
                onSuccess = { AddressLookupUiState.Success(it) },
                onFailure = { err ->
                    AddressLookupUiState.Error(
                        err.localizedMessage ?: "Ошибка ИИ-поиска по адресу"
                    )
                }
            )
        }
    }

    fun clearAddressLookup() {
        addressLookupJob?.cancel()
        _addressLookupState.value = AddressLookupUiState.Idle
    }

    fun analyzeDrawnSketch(bitmap: Bitmap, roomSummaryHint: String) {
        sketchMatchJob?.cancel()
        sketchMatchJob = viewModelScope.launch {
            _sketchMatchState.value = SketchMatchUiState.Loading
            val result = sketchAiRepository.suggestLayoutsForSketch(
                sketchBitmap = bitmap,
                roomSummaryHint = roomSummaryHint
            )
            _sketchMatchState.value = result.fold(
                onSuccess = { SketchMatchUiState.Success(it) },
                onFailure = { err ->
                    SketchMatchUiState.Error(
                        err.localizedMessage ?: "Не удалось подобрать планировки по рисунку"
                    )
                }
            )
        }
    }

    fun clearSketchMatch() {
        sketchMatchJob?.cancel()
        _sketchMatchState.value = SketchMatchUiState.Idle
    }

    fun selectProject(projectId: Long) {
        if (projectId <= 0L) {
            loadGlobalCalc()
            return
        }
        _selectedProjectId.value = projectId
        viewModelScope.launch {
            val existing = AppDatabase.getInstance(getApplication()).calculatorStateDao().getState(projectId)
            loadedCalcProjectId = projectId
            if (existing != null) {
                _calcUi.value = CalculatorUiState(
                    expression = existing.expressionText,
                    currentInput = existing.currentInput,
                    livePreview = existing.resultPreview,
                    activeUnit = existing.activeUnit
                )
            } else {
                _calcUi.value = CalculatorUiState()
            }
        }
    }

    fun loadGlobalCalc() {
        loadedCalcProjectId = GLOBAL_CALC_PROJECT_ID
        val expr = globalPrefs.getString(KEY_EXPR, "") ?: ""
        val input = globalPrefs.getString(KEY_INPUT, "0") ?: "0"
        val preview = java.lang.Double.longBitsToDouble(
            globalPrefs.getLong(KEY_PREVIEW, java.lang.Double.doubleToRawLongBits(0.0))
        )
        val unit = globalPrefs.getString(KEY_UNIT, "") ?: ""
        _calcUi.value = CalculatorUiState(
            expression = expr,
            currentInput = input,
            livePreview = if (preview.isNaN() || preview.isInfinite()) 0.0 else preview,
            activeUnit = unit
        )
        _globalCalcHistory.value = loadGlobalCalcHistory()
    }

    fun createFromTemplate(
        templateId: String,
        variantLabel: String? = null,
        onCreated: (Long) -> Unit
    ) {
        val d = _draft.value
        val finalName = d.name.trim().ifEmpty { "Объект от ${d.startDate}" }
        viewModelScope.launch {
            val id = repository.createProjectFromTemplate(
                name = finalName,
                address = d.address.trim(),
                startDate = d.startDate,
                templateId = templateId,
                variantLabel = variantLabel,
                hasStretchCeiling = false
            )
            resetDraft()
            selectProject(id)
            onCreated(id)
        }
    }

    fun createFromDrawing(drawnRooms: List<Pair<String, List<SnappedSegment>>>, onCreated: (Long) -> Unit) {
        val d = _draft.value
        val finalName = d.name.trim().ifEmpty { "Объект от ${d.startDate}" }
        viewModelScope.launch {
            val id = repository.createProjectFromDrawing(
                name = finalName,
                address = d.address.trim(),
                startDate = d.startDate,
                drawnRooms = drawnRooms,
                hasStretchCeiling = false
            )
            resetDraft()
            selectProject(id)
            onCreated(id)
        }
    }

    fun updateProjectDrawing(
        projectId: Long,
        drawnRooms: List<Pair<String, List<SnappedSegment>>>,
        onUpdated: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateProjectDrawing(projectId, drawnRooms)
            onUpdated()
        }
    }

    fun extractConstructorRooms(aggregate: ProjectAggregate?): List<ConstructorRoom> {
        if (aggregate == null) return emptyList()
        val orderedRooms = aggregate.rooms.sortedBy { it.entity.sortOrder }
        val drawnSegments = ZamerJsonCodec.decodeDrawingSegments(aggregate.project.customDrawingJson)
        if (drawnSegments.isNotEmpty()) {
            var cursor = 0
            val pairs = mutableListOf<Pair<String, List<SnappedSegment>>>()
            orderedRooms.forEach { r ->
                val count = r.walls.size.coerceAtLeast(r.entity.wallCount)
                if (cursor + count <= drawnSegments.size) {
                    pairs += r.entity.name to drawnSegments.subList(cursor, cursor + count)
                    cursor += count
                }
            }
            val restored = LayoutConstructorEngine.roomsFromSegments(pairs)
            if (restored.isNotEmpty()) return restored
        }
        return orderedRooms.mapIndexed { idx, r ->
            val shape = if (r.entity.shapeKey == "WALL_LINE" || r.walls.size == 1) {
                ConstructorShapeKind.WALL_LINE
            } else {
                when (SimpleRoomShape.fromKey(r.entity.shapeKey, r.entity.wallCount)) {
                    SimpleRoomShape.L_SHAPE -> ConstructorShapeKind.L_SHAPE
                    SimpleRoomShape.U_SHAPE -> ConstructorShapeKind.U_SHAPE
                    SimpleRoomShape.RECT -> if (r.entity.name.contains("коридор", ignoreCase = true)) {
                        ConstructorShapeKind.CORRIDOR
                    } else {
                        ConstructorShapeKind.RECTANGLE
                    }
                }
            }
            val col = idx % 2
            val row = idx / 2
            LayoutConstructorEngine.createRoom(
                id = "init_${r.entity.id}",
                kind = shape,
                centerX = 288f + col * 384f,
                centerY = 288f + row * 384f,
                existingCount = idx
            ).copy(name = r.entity.name)
        }
    }

    fun createWithoutPlan(rooms: List<NoPlanRoomSpec>, onCreated: (Long) -> Unit) {
        val d = _draft.value
        val finalName = d.name.trim().ifEmpty { "Объект от ${d.startDate}" }
        viewModelScope.launch {
            val id = repository.createProjectWithoutPlan(
                name = finalName,
                address = d.address.trim(),
                startDate = d.startDate,
                roomSpecs = rooms,
                hasStretchCeiling = false
            )
            resetDraft()
            selectProject(id)
            onCreated(id)
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            if (_selectedProjectId.value == projectId) {
                _selectedProjectId.value = null
            }
            repository.deleteProject(projectId)
        }
    }

    fun setProjectStretchCeiling(project: ProjectEntity, hasStretchCeiling: Boolean) {
        viewModelScope.launch {
            repository.setProjectStretchCeiling(project, hasStretchCeiling)
        }
    }

    fun addRoom(
        projectId: Long,
        name: String,
        wallCount: Int = 4,
        shapeKey: String = SimpleRoomShape.fromKey(null, wallCount).shapeKey
    ) {
        viewModelScope.launch {
            repository.addRoomToProject(projectId, name, wallCount, shapeKey)
        }
    }

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch {
            repository.deleteRoom(room)
        }
    }

    fun renameRoom(room: RoomEntity, newName: String) {
        viewModelScope.launch {
            repository.renameRoom(room, newName)
        }
    }

    fun saveWallLength(room: RoomEntity, wallIndex: Int, lengthMeters: Double, logToHistory: Boolean = true) {
        viewModelScope.launch {
            repository.updateWallLength(room, wallIndex, lengthMeters, recordHistory = logToHistory)
        }
    }

    fun saveProjectWallHeight(projectId: Long, heightMeters: Double) {
        viewModelScope.launch {
            repository.updateProjectWallHeight(projectId, heightMeters)
        }
    }

    fun saveProjectOpenings(projectId: Long, openings: List<OpeningItem>) {
        viewModelScope.launch {
            repository.updateProjectOpenings(projectId, openings)
        }
    }

    fun saveProjectNotes(projectId: Long, notes: String) {
        viewModelScope.launch {
            repository.updateProjectNotes(projectId, notes)
        }
    }

    fun addMaterialExpense(projectId: Long, itemName: String, priceRub: Double, purchaseDate: String) {
        viewModelScope.launch {
            repository.addMaterialExpense(projectId, itemName, priceRub, purchaseDate)
        }
    }

    fun deleteMaterialExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteMaterialExpense(id)
        }
    }

    fun addMoneyEntry(
        projectId: Long,
        amountRub: Double,
        paymentMethod: String,
        dateStr: String,
        reasonText: String
    ) {
        viewModelScope.launch {
            repository.addMoneyEntry(projectId, amountRub, paymentMethod, dateStr, reasonText)
        }
    }

    fun deleteMoneyEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteMoneyEntry(id)
        }
    }

    fun saveRoomCeilingAndHeight(
        room: RoomEntity,
        heightMeters: Double,
        hasStretchCeiling: Boolean,
        heightBeforeCeiling: Double?,
        heightAfterCeiling: Double?,
        applyToAll: Boolean
    ) {
        viewModelScope.launch {
            repository.updateRoomCeilingAndHeight(
                room = room,
                heightMeters = heightMeters,
                hasStretchCeiling = hasStretchCeiling,
                heightBeforeCeiling = heightBeforeCeiling,
                heightAfterCeiling = heightAfterCeiling,
                applyToAllRooms = applyToAll
            )
        }
    }

    fun saveRoomOpenings(room: RoomEntity, openings: List<OpeningItem>, logToHistory: Boolean = true) {
        viewModelScope.launch {
            repository.updateRoomOpenings(room, openings, recordHistory = logToHistory)
        }
    }

    fun saveRoomCorners(room: RoomEntity, inner: Int, outer: Int, extraMeters: Double, logToHistory: Boolean = true) {
        viewModelScope.launch {
            repository.updateRoomCorners(room, inner, outer, extraMeters, recordHistory = logToHistory)
        }
    }

    fun saveRoomFloorOverride(room: RoomEntity, overrideArea: Double?) {
        viewModelScope.launch {
            repository.updateRoomFloorOverride(room, overrideArea)
        }
    }

    fun saveProjectWallThickness(project: ProjectEntity, thicknessMeters: Double) {
        viewModelScope.launch {
            repository.updateProjectWallThickness(project, thicknessMeters)
        }
    }

    fun saveEstimateCategories(
        project: ProjectEntity,
        categories: List<EstimateCategoryConfig>,
        useOuterFloor: Boolean
    ) {
        viewModelScope.launch {
            repository.updateEstimateCategories(project, categories, useOuterFloor)
        }
    }

    fun saveEstimateSnapshot(aggregate: ProjectAggregate) {
        viewModelScope.launch {
            val text = buildEstimateText(aggregate)
            val title = "Смета (${Formatters.todayDate()})"
            repository.saveEstimateSnapshot(
                projectId = aggregate.project.id,
                title = title,
                summaryText = text,
                grandTotalRub = aggregate.totals.grandTotalRub
            )
        }
    }

    fun deleteSavedEstimate(estimateId: Long) {
        viewModelScope.launch {
            repository.deleteSavedEstimate(estimateId)
        }
    }

    fun saveMinorWork(work: MinorWorkEntity) {
        viewModelScope.launch {
            repository.addOrUpdateMinorWork(work)
        }
    }

    fun deleteMinorWork(work: MinorWorkEntity) {
        viewModelScope.launch {
            repository.deleteMinorWork(work)
        }
    }

    // --- CALCULATOR WITH PARENTHESES, GLOBAL MODE & PERSISTENT HISTORY ---

    private fun ensureCalcLoaded(projectId: Long) {
        if (loadedCalcProjectId != projectId) {
            selectProject(projectId)
        }
    }

    fun onCalcDigit(projectId: Long, digit: String) {
        ensureCalcLoaded(projectId)
        val cur = _calcUi.value
        val trimmedExpr = cur.expression.trimEnd()
        val updatedExpr = if (cur.currentInput.isEmpty() && trimmedExpr.endsWith(")")) {
            "$trimmedExpr ×"
        } else {
            cur.expression
        }

        val nextInput = when {
            digit == "." -> {
                if (cur.currentInput.contains(".")) cur.currentInput
                else if (cur.currentInput.isEmpty()) "0."
                else "${cur.currentInput}."
            }
            digit == "00" -> {
                if (cur.currentInput == "0" || cur.currentInput.isEmpty()) "0"
                else cur.currentInput + "00"
            }
            cur.currentInput == "0" -> digit
            else -> cur.currentInput + digit
        }
        val fullExpr = buildDisplayExpression(updatedExpr, nextInput)
        val preview = evaluateExpressionSafe(fullExpr)
        updateAndPersistCalc(projectId, updatedExpr, nextInput, preview, cur.activeUnit)
    }

    fun onCalcParen(projectId: Long, paren: String) {
        ensureCalcLoaded(projectId)
        val cur = _calcUi.value
        val trimmedExpr = cur.expression.trimEnd()
        if (paren == "(") {
            val nextExpr = when {
                trimmedExpr.isBlank() && (cur.currentInput.isBlank() || cur.currentInput == "0") -> "("
                cur.currentInput.isNotBlank() && cur.currentInput != "0" -> {
                    val base = if (trimmedExpr.isBlank()) cur.currentInput else "$trimmedExpr ${cur.currentInput}"
                    "$base × ("
                }
                trimmedExpr.endsWith(")") -> "$trimmedExpr × ("
                trimmedExpr.isBlank() -> "("
                else -> "$trimmedExpr ("
            }
            val preview = evaluateExpressionSafe(nextExpr)
            updateAndPersistCalc(projectId, nextExpr, "", preview, cur.activeUnit)
        } else {
            val openCount = trimmedExpr.count { it == '(' }
            val closeCount = trimmedExpr.count { it == ')' }
            if (openCount <= closeCount) return

            val nextExpr = when {
                cur.currentInput.isNotBlank() -> {
                    if (trimmedExpr.isBlank()) "${cur.currentInput} )" else "$trimmedExpr ${cur.currentInput} )"
                }
                trimmedExpr.endsWith(")") || trimmedExpr.lastOrNull()?.isDigit() == true -> "$trimmedExpr )"
                trimmedExpr.endsWith("(") -> "$trimmedExpr 0 )"
                else -> return
            }
            val preview = evaluateExpressionSafe(nextExpr)
            updateAndPersistCalc(projectId, nextExpr, "", preview, cur.activeUnit)
        }
    }

    fun onCalcBackspace(projectId: Long) {
        ensureCalcLoaded(projectId)
        val cur = _calcUi.value
        if (cur.currentInput.isNotEmpty() && cur.currentInput != "0") {
            val trimmed = cur.currentInput.dropLast(1)
            val nextInput = if (trimmed.isEmpty() && cur.expression.isBlank()) "0" else trimmed
            val fullExpr = buildDisplayExpression(cur.expression, nextInput)
            updateAndPersistCalc(projectId, cur.expression, nextInput, evaluateExpressionSafe(fullExpr), cur.activeUnit)
        } else if (cur.expression.isNotEmpty()) {
            val trimmedExpr = cur.expression.trimEnd().dropLast(1).trimEnd()
            val nextInput = if (trimmedExpr.isEmpty()) "0" else ""
            val preview = evaluateExpressionSafe(buildDisplayExpression(trimmedExpr, nextInput))
            updateAndPersistCalc(projectId, trimmedExpr, nextInput, preview, cur.activeUnit)
        }
    }

    fun onCalcClear(projectId: Long) {
        ensureCalcLoaded(projectId)
        val cur = _calcUi.value
        val activeVal = currentNumericValue(cur)
        if (cur.expression.isNotBlank() || activeVal != 0.0) {
            val exprText = buildDisplayExpression(cur.expression, cur.currentInput)
            logCalcHistory(projectId, "Сброс", "$exprText = ${Formatters.formatCleanNumber(cur.livePreview)}", cur.livePreview, cur.activeUnit)
        }
        updateAndPersistCalc(projectId, "", "0", 0.0, "")
    }

    fun onCalcPercent(projectId: Long) {
        ensureCalcLoaded(projectId)
        val cur = _calcUi.value
        val valNum = currentNumericValue(cur)
        val res = if (cur.expression.isBlank()) {
            valNum / 100.0
        } else {
            val inputNum = cur.currentInput.toDoubleOrNull() ?: 0.0
            val basePreview = evaluateExpressionSafe(cur.expression)
            val delta = basePreview * (inputNum / 100.0)
            val op = cur.expression.trimEnd().lastOrNull()
            when (op) {
                '+' -> basePreview + delta
                '−', '-' -> basePreview - delta
                else -> valNum / 100.0
            }
        }
        val formatted = Formatters.formatCleanNumber(res)
        val exprText = "${buildDisplayExpression(cur.expression, cur.currentInput)}% = $formatted"
        logCalcHistory(projectId, "Калькулятор (%)", exprText, res, cur.activeUnit)
        updateAndPersistCalc(projectId, "", formatted, res, cur.activeUnit)
    }

    fun onCalcOperator(projectId: Long, op: String) {
        ensureCalcLoaded(projectId)
        val cur = _calcUi.value
        val cleanOp = when (op) {
            "*" -> "×"
            "/" -> "÷"
            "-" -> "−"
            else -> op
        }
        val trimmedExpr = cur.expression.trimEnd()
        val newExpression = when {
            cur.currentInput.isNotEmpty() -> {
                if (trimmedExpr.isBlank()) "${cur.currentInput} $cleanOp"
                else "$trimmedExpr ${cur.currentInput} $cleanOp"
            }
            trimmedExpr.endsWith(")") -> "$trimmedExpr $cleanOp"
            trimmedExpr.endsWith("(") -> {
                if (cleanOp == "−") "$trimmedExpr -" else trimmedExpr
            }
            trimmedExpr.isBlank() -> "0 $cleanOp"
            trimmedExpr.lastOrNull() in listOf('+', '−', '-', '×', '*', '÷', '/') -> {
                trimmedExpr.dropLast(1).trimEnd().let { prefix ->
                    if (prefix.isBlank()) cleanOp else "$prefix $cleanOp"
                }
            }
            else -> "$trimmedExpr $cleanOp"
        }
        val interimPreview = evaluateExpressionSafe(newExpression)
        updateAndPersistCalc(projectId, newExpression, "", interimPreview, cur.activeUnit)
    }

    fun onCalcEquals(projectId: Long) {
        ensureCalcLoaded(projectId)
        val cur = _calcUi.value
        val fullExpr = buildDisplayExpression(cur.expression, cur.currentInput).trim()
        if (fullExpr.isEmpty()) return
        val result = evaluateExpressionSafe(fullExpr)
        val formatted = Formatters.formatCleanNumber(result)
        val unitSuffix = if (cur.activeUnit.isNotBlank()) " ${cur.activeUnit}" else ""
        logCalcHistory(projectId, "Калькулятор", "$fullExpr = $formatted$unitSuffix", result, cur.activeUnit)
        updateAndPersistCalc(projectId, "", formatted, result, cur.activeUnit)
    }

    fun insertMeasurementIntoCalc(projectId: Long, label: String, value: Double, unit: String) {
        ensureCalcLoaded(projectId)
        val cur = _calcUi.value
        val valueStr = Formatters.formatCleanNumber(value)
        val newExpr: String
        val newInput: String
        if (cur.expression.isNotBlank() && cur.currentInput.isEmpty()) {
            newExpr = cur.expression
            newInput = valueStr
        } else {
            newExpr = ""
            newInput = valueStr
        }
        val preview = evaluateExpressionSafe(buildDisplayExpression(newExpr, newInput))
        logCalcHistory(
            projectId,
            "Из замеров",
            "$label: $valueStr $unit",
            value,
            unit
        )
        updateAndPersistCalc(projectId, newExpr, newInput, preview, unit)
    }

    fun restoreHistoryValueToCalc(projectId: Long, entry: HistoryEntryEntity) {
        val value = entry.resultValue ?: return
        val valueStr = Formatters.formatCleanNumber(value)
        val cur = _calcUi.value
        val newExpr = if (cur.expression.isNotBlank() && cur.currentInput.isEmpty()) cur.expression else ""
        val preview = evaluateExpressionSafe(buildDisplayExpression(newExpr, valueStr))
        updateAndPersistCalc(projectId, newExpr, valueStr, preview, entry.unit)
    }

    fun flushPendingCalcToHistory(projectId: Long) {
        val cur = _calcUi.value
        val full = buildDisplayExpression(cur.expression, cur.currentInput).trim()
        if (full.isNotEmpty() && full != "0" && cur.expression.isNotBlank()) {
            val preview = evaluateExpressionSafe(full)
            logCalcHistory(
                projectId,
                "Калькулятор",
                "$full = ${Formatters.formatCleanNumber(preview)}",
                preview,
                cur.activeUnit
            )
        }
    }

    fun clearHistory(projectId: Long) {
        if (projectId <= 0L) {
            globalPrefs.edit().remove(KEY_HISTORY).apply()
            _globalCalcHistory.value = emptyList()
            return
        }
        viewModelScope.launch {
            repository.clearHistory(projectId)
        }
    }

    private fun updateAndPersistCalc(
        projectId: Long,
        expression: String,
        currentInput: String,
        preview: Double,
        activeUnit: String
    ) {
        _calcUi.value = CalculatorUiState(
            expression = expression,
            currentInput = currentInput,
            livePreview = preview,
            activeUnit = activeUnit
        )
        if (projectId <= 0L) {
            globalPrefs.edit()
                .putString(KEY_EXPR, expression)
                .putString(KEY_INPUT, currentInput)
                .putLong(KEY_PREVIEW, java.lang.Double.doubleToRawLongBits(preview))
                .putString(KEY_UNIT, activeUnit)
                .apply()
        } else {
            viewModelScope.launch {
                repository.saveCalculatorState(projectId, expression, currentInput, preview, activeUnit)
            }
        }
    }

    private fun logCalcHistory(
        projectId: Long,
        title: String,
        expression: String,
        result: Double,
        unit: String = ""
    ) {
        if (projectId <= 0L) {
            val entry = HistoryEntryEntity(
                id = System.currentTimeMillis(),
                projectId = GLOBAL_CALC_PROJECT_ID,
                timestamp = System.currentTimeMillis(),
                source = "CALC",
                title = title,
                expression = expression,
                resultValue = result,
                unit = unit
            )
            val updated = (listOf(entry) + _globalCalcHistory.value).take(80)
            _globalCalcHistory.value = updated
            saveGlobalCalcHistory(updated)
            return
        }
        viewModelScope.launch {
            repository.logHistory(
                projectId = projectId,
                source = "CALC",
                title = title,
                expression = expression,
                resultValue = result,
                unit = unit
            )
        }
    }

    private fun saveGlobalCalcHistory(items: List<HistoryEntryEntity>) {
        val arr = JSONArray()
        items.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("timestamp", e.timestamp)
            obj.put("title", e.title)
            obj.put("expression", e.expression)
            if (e.resultValue != null) obj.put("resultValue", e.resultValue)
            obj.put("unit", e.unit)
            arr.put(obj)
        }
        globalPrefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    private fun loadGlobalCalcHistory(): List<HistoryEntryEntity> {
        val raw = globalPrefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HistoryEntryEntity(
                    id = obj.optLong("id", i.toLong() + 1L),
                    projectId = GLOBAL_CALC_PROJECT_ID,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    source = "CALC",
                    title = obj.optString("title", "Калькулятор"),
                    expression = obj.optString("expression", ""),
                    resultValue = if (obj.has("resultValue")) obj.optDouble("resultValue") else null,
                    unit = obj.optString("unit", "")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun currentNumericValue(cur: CalculatorUiState): Double {
        val full = buildDisplayExpression(cur.expression, cur.currentInput)
        return if (full.isBlank()) 0.0 else evaluateExpressionSafe(full)
    }

    private fun buildDisplayExpression(expression: String, currentInput: String): String {
        return when {
            expression.isBlank() -> currentInput
            currentInput.isBlank() -> expression
            else -> "$expression $currentInput"
        }
    }

    private fun evaluateExpressionSafe(expr: String): Double =
        CalculatorExpressionEvaluator.evaluate(expr)

    fun buildEstimateText(aggregate: ProjectAggregate): String {
        val sb = StringBuilder()
        val p = aggregate.project
        val t = aggregate.totals
        sb.appendLine("СМЕТА: ${p.name}")
        if (p.address.isNotBlank()) sb.appendLine("Адрес: ${p.address}")
        sb.appendLine("Дата: ${p.startDate} · ${p.layoutTitle}")
        sb.appendLine()
        val floorTotal = if (p.useOuterFloorInEstimate) t.totalOuterFloorAreaSqm else t.totalInnerFloorAreaSqm
        val pricedCats = aggregate.estimateCategories.filter { it.pricePerUnit > 0.0 }
        if (pricedCats.isNotEmpty()) {
            sb.appendLine("РАБОТЫ ПО ЗАМЕРАМ:")
            pricedCats.forEach { cat ->
                val qty = repository.quantityForCategory(cat, t.totalWallAreaSqm, floorTotal, t.totalCornersMeters)
                val sum = qty * cat.pricePerUnit
                val partialTag = if (cat.usePartial) " (частично)" else ""
                sb.appendLine("• ${cat.title}$partialTag: ${Formatters.formatCleanNumber(qty)} ${cat.unit} × ${Formatters.formatMoney(cat.pricePerUnit)} = ${Formatters.formatMoney(sum)}")
            }
            sb.appendLine()
        }
        if (aggregate.minorWorks.isNotEmpty()) {
            sb.appendLine("ДОП. РАБОТЫ:")
            aggregate.minorWorks.forEach { w ->
                sb.appendLine("• ${w.title}: ${Formatters.formatMoney(w.totalCost)}")
            }
            sb.appendLine()
        }
        sb.appendLine("ИТОГО: ${Formatters.formatMoney(t.grandTotalRub)}")
        return sb.toString().trim()
    }
}
