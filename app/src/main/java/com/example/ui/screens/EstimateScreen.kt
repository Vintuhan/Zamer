package com.example.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ProjectAggregate
import com.example.data.ProjectEntity
import com.example.data.SavedEstimateEntity
import com.example.domain.EstimateCategoryConfig
import com.example.domain.Formatters
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ZamerCard
import com.example.ui.components.ZamerInputField
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.BrightDoorGreen
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.DeepGreenSoft
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun EstimateScreen(
    aggregate: ProjectAggregate?,
    onSaveCategories: (ProjectEntity, List<EstimateCategoryConfig>, Boolean) -> Unit,
    onSaveEstimateInsideApp: (ProjectAggregate) -> Unit,
    onDeleteSavedEstimate: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    if (aggregate == null) return

    val p = aggregate.project
    val t = aggregate.totals
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val categories = remember(p.id, p.estimateCategoriesJson) {
        mutableStateListOf<EstimateCategoryConfig>().apply {
            addAll(aggregate.estimateCategories)
        }
    }
    var useOuterFloor by remember(p.id, p.useOuterFloorInEstimate) {
        mutableStateOf(p.useOuterFloorInEstimate)
    }
    var selectedIndex by remember(p.id) { mutableIntStateOf(0) }
    if (selectedIndex > categories.lastIndex && categories.isNotEmpty()) {
        selectedIndex = 0
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var viewingSavedEstimate by remember { mutableStateOf<SavedEstimateEntity?>(null) }

    val floorArea = if (useOuterFloor) t.totalOuterFloorAreaSqm else t.totalInnerFloorAreaSqm

    fun qtyFor(cat: EstimateCategoryConfig): Double {
        if (cat.usePartial) return cat.partialQuantity.coerceAtLeast(0.0)
        return when (cat.sourceKind) {
            "WALLS" -> t.totalWallAreaSqm
            "FLOOR" -> floorArea
            "CORNERS" -> t.totalCornersMeters
            else -> if (cat.customQuantity > 0.0) cat.customQuantity else t.totalWallAreaSqm
        }
    }

    val liveGrandTotal = categories
        .filter { it.pricePerUnit > 0.0 }
        .sumOf { qtyFor(it) * it.pricePerUnit } + t.minorWorksTotalRub

    fun persist() {
        onSaveCategories(p, categories.toList(), useOuterFloor)
    }

    Scaffold(
        containerColor = PureWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ZamerTopBar(
                title = "Смета",
                subtitle = p.name,
                onBackClick = onBackClick,
                trailing = {
                    OutlinedButton(
                        onClick = {
                            printSinglePagePdf(
                                context = context,
                                aggregate = aggregate,
                                categories = categories.toList(),
                                useOuterFloor = useOuterFloor,
                                grandTotal = liveGrandTotal
                            )
                        },
                        modifier = Modifier
                            .heightIn(min = 50.dp)
                            .testTag("print_pdf_estimate_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, DeepGreen),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DeepGreenSoft,
                            contentColor = DeepGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "В PDF", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("В PDF", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = PureWhite, border = BorderStroke(1.dp, PureBlack)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZamerPrimaryButton(
                        text = "Сохранить",
                        icon = Icons.Filled.Save,
                        onClick = {
                            onSaveEstimateInsideApp(aggregate)
                            scope.launch {
                                snackbarHostState.showSnackbar("Смета сохранена в приложении")
                            }
                        },
                        modifier = Modifier.widthIn(max = 600.dp),
                        testTag = "save_estimate_button"
                    )
                }
            }
        }
    ) { inner ->
        AnimatedScreenEntrance(Modifier.fillMaxSize().padding(inner)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. ИТОГО ПО СМЕТЕ — AT THE VERY TOP
                item {
                    ZamerCard(
                        modifier = Modifier.widthIn(max = 600.dp),
                        borderColor = DeepGreen,
                        containerColor = DeepGreen
                    ) {
                        Text("ИТОГО ПО СМЕТЕ", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatMoney(liveGrandTotal),
                            style = MaterialTheme.typography.displayLarge,
                            color = PureWhite,
                            modifier = Modifier.testTag("estimate_grand_total")
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Стены: ${Formatters.formatArea(t.totalWallAreaSqm)} · Пол: ${Formatters.formatArea(floorArea)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite
                        )
                    }
                }

                // 2. Single-active horizontal work type selector strip
                item {
                    Column(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
                        Text(
                            text = "Вид работы:",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureBlack
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEachIndexed { idx, cat ->
                                val isActive = idx == selectedIndex
                                val hasPrice = cat.pricePerUnit > 0.0
                                OutlinedButton(
                                    onClick = { selectedIndex = idx },
                                    modifier = Modifier
                                        .heightIn(min = 50.dp)
                                        .testTag("estimate_tab_${cat.id}"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        2.dp,
                                        when {
                                            isActive -> DeepGreen
                                            hasPrice -> BrightDoorGreen
                                            else -> PureBlack
                                        }
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isActive) DeepGreen else PureWhite,
                                        contentColor = if (isActive) PureWhite else PureBlack
                                    )
                                ) {
                                    Text(
                                        text = if (hasPrice) "${cat.title} •" else cat.title,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showAddCategoryDialog = true },
                                modifier = Modifier.heightIn(min = 50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(2.dp, DeepGreen),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = DeepGreenSoft,
                                    contentColor = DeepGreen
                                )
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Добавить", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // 3. Active category editor card
                val activeCat = categories.getOrNull(selectedIndex)
                if (activeCat != null) {
                    item {
                        val qty = qtyFor(activeCat)
                        val sum = qty * activeCat.pricePerUnit
                        var priceText by remember(activeCat.id, p.id) {
                            mutableStateOf(
                                if (activeCat.pricePerUnit > 0) Formatters.formatCleanNumber(activeCat.pricePerUnit) else ""
                            )
                        }
                        var partialText by remember(activeCat.id, p.id) {
                            mutableStateOf(
                                if (activeCat.partialQuantity > 0) Formatters.formatCleanNumber(activeCat.partialQuantity) else ""
                            )
                        }

                        ZamerCard(
                            modifier = Modifier.widthIn(max = 600.dp),
                            borderColor = PureBlack
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeCat.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = PureBlack
                                )
                                Text(
                                    text = Formatters.formatMoney(sum),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = DeepGreen
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Идёт в расчёт: ${Formatters.formatCleanNumber(qty)} ${activeCat.unit}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = DeepGreen
                            )

                            Spacer(Modifier.height(10.dp))

                            ZamerInputField(
                                value = priceText,
                                onValueChange = { txt ->
                                    priceText = txt
                                    val newPrice = Formatters.parseDecimalInput(txt) ?: 0.0
                                    categories[selectedIndex] = activeCat.copy(
                                        pricePerUnit = newPrice,
                                        isSelected = newPrice > 0.0
                                    )
                                    persist()
                                },
                                label = "Сумма за 1 ${activeCat.unit} (₽)",
                                placeholder = "0",
                                keyboardType = KeyboardType.Decimal,
                                isGiantNumeric = true,
                                testTag = "estimate_price_input"
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val nextPartial = !activeCat.usePartial
                                        categories[selectedIndex] = activeCat.copy(usePartial = nextPartial)
                                        persist()
                                    }
                                    .testTag("estimate_partial_checkbox")
                            ) {
                                Checkbox(
                                    checked = activeCat.usePartial,
                                    onCheckedChange = { checked ->
                                        categories[selectedIndex] = activeCat.copy(usePartial = checked)
                                        persist()
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = DeepGreen)
                                )
                                Text(
                                    text = "Посчитать частично",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = PureBlack
                                )
                            }

                            if (activeCat.usePartial) {
                                Spacer(Modifier.height(6.dp))
                                ZamerInputField(
                                    value = partialText,
                                    onValueChange = { txt ->
                                        partialText = txt
                                        val q = Formatters.parseDecimalInput(txt) ?: 0.0
                                        categories[selectedIndex] = activeCat.copy(partialQuantity = q)
                                        persist()
                                    },
                                    label = "Сколько считать (${activeCat.unit})",
                                    placeholder = "0.0",
                                    keyboardType = KeyboardType.Decimal,
                                    testTag = "estimate_partial_qty_input"
                                )
                            }

                            // ONLY shown for Ламинат / FLOOR category
                            if (activeCat.sourceKind == "FLOOR") {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            useOuterFloor = !useOuterFloor
                                            persist()
                                        }
                                        .testTag("estimate_outer_floor_checkbox")
                                ) {
                                    Checkbox(
                                        checked = useOuterFloor,
                                        onCheckedChange = {
                                            useOuterFloor = it
                                            persist()
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = DeepGreen)
                                    )
                                    Text(
                                        text = "Пол считать со стенами",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = PureBlack
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary of all priced work items
                val pricedCats = categories.filter { it.pricePerUnit > 0.0 }
                if (pricedCats.isNotEmpty()) {
                    item {
                        ZamerCard(modifier = Modifier.widthIn(max = 600.dp), borderColor = DeepGreen) {
                            Text("Включено в смету:", style = MaterialTheme.typography.titleMedium, color = DeepGreen)
                            Spacer(Modifier.height(6.dp))
                            pricedCats.forEach { cat ->
                                val q = qtyFor(cat)
                                val s = q * cat.pricePerUnit
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${cat.title} (${Formatters.formatCleanNumber(q)} ${cat.unit})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PureBlack,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = Formatters.formatMoney(s),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeepGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // Minor works in estimate
                if (aggregate.minorWorks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Мелкая работа (${Formatters.formatMoney(t.minorWorksTotalRub)})",
                            style = MaterialTheme.typography.titleLarge,
                            color = PureBlack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                        )
                    }
                    items(aggregate.minorWorks, key = { it.id }) { work ->
                        ZamerCard(modifier = Modifier.widthIn(max = 600.dp), borderColor = PureBlack) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(work.title, style = MaterialTheme.typography.titleMedium, color = PureBlack, modifier = Modifier.weight(1f))
                                Text(Formatters.formatMoney(work.totalCost), style = MaterialTheme.typography.titleLarge, color = DeepGreen)
                            }
                        }
                    }
                }

                // Saved estimates inside the app
                if (aggregate.savedEstimates.isNotEmpty()) {
                    item {
                        Text(
                            text = "Сохранённые сметы в приложении",
                            style = MaterialTheme.typography.titleLarge,
                            color = PureBlack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                        )
                    }
                    items(aggregate.savedEstimates, key = { it.id }) { est ->
                        ZamerCard(
                            modifier = Modifier
                                .widthIn(max = 600.dp)
                                .clickable { viewingSavedEstimate = est },
                            borderColor = DeepGreen
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(est.title, style = MaterialTheme.typography.titleMedium, color = PureBlack)
                                    Text(
                                        text = "${Formatters.formatDateTime(est.savedAt)} · ${Formatters.formatMoney(est.grandTotalRub)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepGreen
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteSavedEstimate(est.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Удалить", tint = PureBlack)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showAddCategoryDialog) {
        var catTitle by remember { mutableStateOf("") }
        var sourceKind by remember { mutableStateOf("WALLS") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = PureWhite,
            title = { Text("Добавить категорию", style = MaterialTheme.typography.titleLarge, color = PureBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZamerInputField(
                        value = catTitle,
                        onValueChange = { catTitle = it },
                        label = "Название (например: Плитка)",
                        placeholder = "Плитка"
                    )
                    Text("От чего считать площадь:", style = MaterialTheme.typography.bodyMedium, color = PureBlack)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WALLS" to "Стены", "FLOOR" to "Пол").forEach { (kind, lbl) ->
                            OutlinedButton(
                                onClick = { sourceKind = kind },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(2.dp, if (sourceKind == kind) DeepGreen else PureBlack),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (sourceKind == kind) DeepGreen else PureWhite,
                                    contentColor = if (sourceKind == kind) PureWhite else PureBlack
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                Text(lbl, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        if (catTitle.isNotBlank()) {
                            categories.add(
                                EstimateCategoryConfig(
                                    id = UUID.randomUUID().toString(),
                                    title = catTitle.trim(),
                                    unit = "м²",
                                    sourceKind = sourceKind,
                                    isSelected = true
                                )
                            )
                            selectedIndex = categories.lastIndex
                            persist()
                        }
                        showAddCategoryDialog = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DeepGreen, contentColor = PureWhite)
                ) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Отмена", color = PureBlack)
                }
            }
        )
    }

    viewingSavedEstimate?.let { est ->
        AlertDialog(
            onDismissRequest = { viewingSavedEstimate = null },
            containerColor = PureWhite,
            title = { Text(est.title, style = MaterialTheme.typography.titleLarge, color = PureBlack) },
            text = {
                Text(est.summaryText, style = MaterialTheme.typography.bodyMedium, color = PureBlack)
            },
            confirmButton = {
                TextButton(onClick = { viewingSavedEstimate = null }) {
                    Text("Закрыть", color = DeepGreen)
                }
            }
        )
    }
}

private fun printSinglePagePdf(
    context: Context,
    aggregate: ProjectAggregate,
    categories: List<EstimateCategoryConfig>,
    useOuterFloor: Boolean,
    grandTotal: Double
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val p = aggregate.project
    val t = aggregate.totals
    val floorArea = if (useOuterFloor) t.totalOuterFloorAreaSqm else t.totalInnerFloorAreaSqm

    val adapter = object : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("Smeta_${p.name}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdf.startPage(pageInfo)
            val c: Canvas = page.canvas

            val titlePaint = Paint().apply {
                textSize = 20f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                color = android.graphics.Color.BLACK
            }
            val headPaint = Paint().apply {
                textSize = 14f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                color = android.graphics.Color.rgb(15, 61, 46)
            }
            val bodyPaint = Paint().apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                color = android.graphics.Color.BLACK
            }
            val linePaint = Paint().apply {
                strokeWidth = 1f
                color = android.graphics.Color.BLACK
            }

            var y = 44f
            c.drawText("СМЕТА: ${p.name}", 40f, y, titlePaint)
            y += 22f
            if (p.address.isNotBlank()) {
                c.drawText("Адрес: ${p.address}", 40f, y, bodyPaint)
                y += 18f
            }
            c.drawText("Дата: ${p.startDate}  |  ${p.layoutTitle}", 40f, y, bodyPaint)
            y += 14f
            c.drawLine(40f, y, 555f, y, linePaint)
            y += 22f

            c.drawText("ЗАМЕРЫ И РАБОТЫ", 40f, y, headPaint)
            y += 18f
            val priced = categories.filter { it.pricePerUnit > 0.0 }
            if (priced.isEmpty()) {
                c.drawText("Стены: ${Formatters.formatArea(t.totalWallAreaSqm)}  |  Пол: ${Formatters.formatArea(floorArea)}", 40f, y, bodyPaint)
                y += 18f
            } else {
                priced.forEach { cat ->
                    if (y < 760f) {
                        val qty = if (cat.usePartial) {
                            cat.partialQuantity.coerceAtLeast(0.0)
                        } else when (cat.sourceKind) {
                            "WALLS" -> t.totalWallAreaSqm
                            "FLOOR" -> floorArea
                            "CORNERS" -> t.totalCornersMeters
                            else -> if (cat.customQuantity > 0) cat.customQuantity else t.totalWallAreaSqm
                        }
                        val sum = qty * cat.pricePerUnit
                        val partialTag = if (cat.usePartial) " (част.)" else ""
                        c.drawText("${cat.title}$partialTag: ${Formatters.formatCleanNumber(qty)} ${cat.unit} × ${Formatters.formatMoney(cat.pricePerUnit)}", 40f, y, bodyPaint)
                        c.drawText(Formatters.formatMoney(sum), 450f, y, bodyPaint)
                        y += 18f
                    }
                }
            }

            if (aggregate.minorWorks.isNotEmpty() && y < 740f) {
                y += 8f
                c.drawText("МЕЛКАЯ РАБОТА", 40f, y, headPaint)
                y += 18f
                aggregate.minorWorks.forEach { w ->
                    if (y < 760f) {
                        c.drawText(w.title, 40f, y, bodyPaint)
                        c.drawText(Formatters.formatMoney(w.totalCost), 450f, y, bodyPaint)
                        y += 18f
                    }
                }
            }

            y = (y + 14f).coerceAtMost(790f)
            c.drawLine(40f, y, 555f, y, linePaint)
            y += 26f
            c.drawText("ИТОГО: ${Formatters.formatMoney(grandTotal)}", 40f, y, titlePaint)

            pdf.finishPage(page)
            try {
                FileOutputStream(destination.fileDescriptor).use { out ->
                    pdf.writeTo(out)
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                pdf.close()
            }
        }
    }

    printManager.print("Смета_${p.name}", adapter, PrintAttributes.Builder().build())
}
