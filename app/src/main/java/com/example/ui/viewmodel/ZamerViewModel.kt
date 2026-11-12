package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryEntryEntity
import com.example.data.MinorWorkEntity
import com.example.data.ProjectAggregate
import com.example.data.ProjectEntity
import com.example.data.ProjectSummaryItem
import com.example.data.RoomEntity
import com.example.data.ZamerRepository
import com.example.domain.EstimateCategoryConfig
import com.example.domain.Formatters
import com.example.domain.OpeningItem
import com.example.domain.SnappedSegment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

data class ProjectDraft(
    val name: String = "",
    val address: String = "",
    val startDate: String = Formatters.todayDate()
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

class ZamerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ZamerRepository(AppDatabase.getInstance(application))

    val projectSummaries: StateFlow<List<ProjectSummaryItem>> = repository.observeProjectSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeProject: StateFlow<ProjectAggregate?> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.observeProjectAggregate(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _draft = MutableStateFlow(ProjectDraft())
    val draft: StateFlow<ProjectDraft> = _draft.asStateFlow()

    private val _calcUi = MutableStateFlow(CalculatorUiState())
    val calcUi: StateFlow<CalculatorUiState> = _calcUi.asStateFlow()

    private var loadedCalcProjectId: Long? = null

    fun updateDraft(name: String? = null, address: String? = null, startDate: String? = null) {
        val cur = _draft.value
        _draft.value = cur.copy(
            name = name ?: cur.name,
            address = address ?: cur.address,
            startDate = startDate ?: cur.startDate
        )
    }

    fun resetDraft() {
        _draft.value = ProjectDraft(startDate = Formatters.todayDate())
    }

    fun selectProject(projectId: Long) {
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

    fun createFromTemplate(templateId: String, onCreated: (Long) -> Unit) {
        val d = _draft.value
        val finalName = d.name.trim().ifEmpty { "Объект от ${d.startDate}" }
        viewModelScope.launch {
            val id = repository.createProjectFromTemplate(
                name = finalName,
                address = d.address.trim().ifEmpty { "Адрес не указан" },
                startDate = d.startDate,
                templateId = templateId
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
                address = d.address.trim().ifEmpty { "Адрес не указан" },
                startDate = d.startDate,
                drawnRooms = drawnRooms
            )
            resetDraft()
            selectProject(id)
            onCreated(id)
        }
    }

    fun createWithoutPlan(rooms: List<Pair<String, Int>>, onCreated: (Long) -> Unit) {
        val d = _draft.value
        val finalName = d.name.trim().ifEmpty { "Объект от ${d.startDate}" }
        viewModelScope.launch {
            val id = repository.createProjectWithoutPlan(
                name = finalName,
                address = d.address.trim().ifEmpty { "Адрес не указан" },
                startDate = d.startDate,
                roomSpecs = rooms
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

    fun addRoom(projectId: Long, name: String, wallCount: Int = 4) {
        viewModelScope.launch {
            repository.addRoomToProject(projectId, name, wallCount)
        }
    }

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch {
            repository.deleteRoom(room)
        }
    }

    fun saveWallLength(room: RoomEntity, wallIndex: Int, lengthMeters: Double, logToHistory: Boolean = true) {
        viewModelScope.launch {
            repository.updateWallLength(room, wallIndex, lengthMeters, recordHistory = logToHistory)
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

    /**
     * Built-in AI helper for suggesting minor works based on apartment measurements and layout.
     */
    fun getAiWorkSuggestions(aggregate: ProjectAggregate): List<AiWorkSuggestion> {
        val list = mutableListOf<AiWorkSuggestion>()
        val totalWindows = aggregate.rooms.sumOf { r ->
            r.openings.filter { it.type == com.example.domain.OpeningType.WINDOW }.sumOf { it.count }
        }
        val wallArea = aggregate.totals.totalWallAreaSqm
        val layout = aggregate.project.layoutTitle.lowercase()

        if (totalWindows > 0) {
            list.add(
                AiWorkSuggestion(
                    title = "Откосы (${totalWindows} окн.)",
                    suggestedPrice = totalWindows * 1800.0,
                    reason = "По замерам найдено окон: $totalWindows"
                )
            )
        } else {
            list.add(
                AiWorkSuggestion(
                    title = "Откосы",
                    suggestedPrice = 2000.0,
                    reason = "Стандартная доп. работа по проёмам"
                )
            )
        }

        if ("хрущ" in layout || "сталин" in layout || "брежн" in layout || "панел" in layout || "1-447" in layout) {
            list.add(
                AiWorkSuggestion(
                    title = "Зачистка стен и старой краски",
                    suggestedPrice = if (wallArea > 0) (wallArea * 120.0) else 3500.0,
                    reason = "Для вторичного жилья (${aggregate.project.layoutTitle})"
                )
            )
        }

        list.add(
            AiWorkSuggestion(
                title = "Подмазать штробы после электрики",
                suggestedPrice = 1500.0,
                reason = "Частая работа перед шпаклёвкой"
            )
        )
        list.add(
            AiWorkSuggestion(
                title = "Убраться и вынести мусор",
                suggestedPrice = 2000.0,
                reason = "Финальная уборка объекта"
            )
        )
        list.add(
            AiWorkSuggestion(
                title = "Перевоз материала и инструмента",
                suggestedPrice = 1500.0,
                reason = "Доставка / подъём"
            )
        )
        return list
    }

    // --- CALCULATOR WITH AUTOMATIC HISTORY & PERSISTENCE ---

    private fun ensureCalcLoaded(projectId: Long) {
        if (loadedCalcProjectId != projectId) {
            selectProject(projectId)
        }
    }

    fun onCalcDigit(projectId: Long, digit: String) {
        ensureCalcLoaded(projectId)
        val cur = _calcUi.value
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
        val fullExpr = buildDisplayExpression(cur.expression, nextInput)
        val preview = evaluateExpressionSafe(fullExpr)
        updateAndPersistCalc(projectId, cur.expression, nextInput, preview, cur.activeUnit)
    }

    fun onCalcBackspace(projectId: Long) {
        ensureCalcLoaded(projectId)
        val cur = _calcUi.value
        if (cur.currentInput.isNotEmpty() && cur.currentInput != "0") {
            val trimmed = cur.currentInput.dropLast(1).ifEmpty { "0" }
            val fullExpr = buildDisplayExpression(cur.expression, trimmed)
            updateAndPersistCalc(projectId, cur.expression, trimmed, evaluateExpressionSafe(fullExpr), cur.activeUnit)
        } else if (cur.expression.isNotEmpty()) {
            val trimmedExpr = cur.expression.trimEnd().dropLast(1).trimEnd()
            val preview = evaluateExpressionSafe(trimmedExpr)
            updateAndPersistCalc(projectId, trimmedExpr, "0", preview, cur.activeUnit)
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
            // Add percentage (common for +10% reserve if typed "10%")
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
        val currentValStr = cur.currentInput.ifEmpty { "0" }
        val newExpression = if (cur.expression.isBlank()) {
            "$currentValStr $cleanOp"
        } else if (cur.currentInput.isEmpty()) {
            cur.expression.trimEnd().dropLast(1).trimEnd() + " $cleanOp"
        } else {
            "${cur.expression} $currentValStr $cleanOp"
        }
        val interimPreview = evaluateExpressionSafe(
            if (cur.expression.isBlank()) currentValStr else "${cur.expression} $currentValStr"
        )
        if (cur.expression.isNotBlank() && cur.currentInput.isNotEmpty()) {
            logCalcHistory(
                projectId,
                "Калькулятор",
                "${cur.expression} $currentValStr = ${Formatters.formatCleanNumber(interimPreview)}",
                interimPreview,
                cur.activeUnit
            )
        } else {
            logCalcHistory(
                projectId,
                "Калькулятор",
                newExpression,
                interimPreview,
                cur.activeUnit
            )
        }
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
        viewModelScope.launch {
            repository.saveCalculatorState(projectId, expression, currentInput, preview, activeUnit)
        }
    }

    private fun logCalcHistory(
        projectId: Long,
        title: String,
        expression: String,
        result: Double,
        unit: String = ""
    ) {
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

    private fun evaluateExpressionSafe(expr: String): Double {
        val tokens = expr.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return 0.0
        val cleanTokens = tokens.toMutableList()
        while (cleanTokens.isNotEmpty() && cleanTokens.last() in listOf("+", "−", "-", "×", "*", "÷", "/")) {
            cleanTokens.removeAt(cleanTokens.lastIndex)
        }
        if (cleanTokens.isEmpty()) return 0.0

        val pass1 = mutableListOf<String>()
        var i = 0
        while (i < cleanTokens.size) {
            val tok = cleanTokens[i]
            if ((tok == "×" || tok == "*" || tok == "÷" || tok == "/") && pass1.isNotEmpty() && i + 1 < cleanTokens.size) {
                val left = pass1.removeAt(pass1.lastIndex).toDoubleOrNull() ?: 0.0
                val right = cleanTokens[i + 1].toDoubleOrNull() ?: 0.0
                val res = if (tok == "×" || tok == "*") {
                    left * right
                } else {
                    if (abs(right) < 1e-9) 0.0 else left / right
                }
                pass1.add(res.toString())
                i += 2
            } else {
                pass1.add(tok)
                i++
            }
        }

        var acc = pass1.firstOrNull()?.toDoubleOrNull() ?: 0.0
        var j = 1
        while (j + 1 < pass1.size) {
            val op = pass1[j]
            val rhs = pass1[j + 1].toDoubleOrNull() ?: 0.0
            when (op) {
                "+" -> acc += rhs
                "−", "-" -> acc -= rhs
            }
            j += 2
        }
        return if (acc.isNaN() || acc.isInfinite()) 0.0 else acc
    }

    fun buildEstimateText(aggregate: ProjectAggregate): String {
        val sb = StringBuilder()
        val p = aggregate.project
        val t = aggregate.totals
        sb.appendLine("СМЕТА: ${p.name}")
        if (p.address.isNotBlank()) sb.appendLine("Адрес: ${p.address}")
        sb.appendLine("Дата: ${p.startDate} · ${p.layoutTitle}")
        sb.appendLine()
        val floorTotal = if (p.useOuterFloorInEstimate) t.totalOuterFloorAreaSqm else t.totalInnerFloorAreaSqm
        val selectedCats = aggregate.estimateCategories.filter { it.isSelected }
        if (selectedCats.isNotEmpty()) {
            sb.appendLine("РАБОТЫ ПО ЗАМЕРАМ:")
            selectedCats.forEach { cat ->
                val qty = repository.quantityForCategory(cat, t.totalWallAreaSqm, floorTotal, t.totalCornersMeters)
                val sum = qty * cat.pricePerUnit
                sb.appendLine("• ${cat.title}: ${Formatters.formatCleanNumber(qty)} ${cat.unit} × ${Formatters.formatMoney(cat.pricePerUnit)} = ${Formatters.formatMoney(sum)}")
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
