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

        val result = Parser(tokens).parseExpr()
        return if (result.isNaN() || result.isInfinite()) 0.0 else result
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0

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

        private fun parseTerm(): Double {
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

        private fun parseFactor(): Double {
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

    fun re
