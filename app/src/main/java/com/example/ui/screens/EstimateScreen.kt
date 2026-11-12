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
import com.example.ui.components.ZamerSecondaryButton
import com.example.ui.components.ZamerTopBar
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

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var viewingSavedEstimate by remember { mutableStateOf<SavedEstimateEntity?>(null) }

    val floorArea = if (useOuterFloor) t.totalOuterFloorAreaSqm else t.totalInnerFloorAreaSqm

    fun qtyFor(cat: EstimateCategoryConfig): Double {
        return when (cat.sourceKind) {
            "WALLS" -> t.totalWallAreaSqm
            "FLOOR" -> floorArea
            "CORNERS" -> t.totalCornersMeters
            else -> if (cat.customQuantity > 0.0) cat.customQuantity else t.totalWallAreaSqm
        }
    }

    val liveGrandTotal = categories
        .filter { it.isSelected && it.pricePerUnit > 0.0 }
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
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Surface(color = PureWhite, border = BorderStroke(1.dp, PureBlack)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZamerPrimaryButton(
                        text = "Сохранить смету",
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
                    ZamerSecondaryButton(
                        text = "Печать / PDF (1 лист)",
                        icon = Icons.Filled.PictureAsPdf,
                        onClick = {
                            printSinglePagePdf(
                                context = context,
                                aggregate = aggregate,
                                categories = categories.toList(),
                                useOuterFloor = useOuterFloor,
                                grandTotal = liveGrandTotal
                            )
                        },
                        modifier = Modifier.widthIn(max = 600.dp),
                        testTag = "print_pdf_estimate_button"
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
                // Top category selector strip (Шпаклёвка, Штукатурка, Обои, Уголки, Ниши, Зачистка, Покраска, Стяжка + Своя)
                item {
                    Column(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)) {
                        Text(
                            text = "Выберите виды работ:",
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
                                OutlinedButton(
                                    onClick = {
                                        categories[idx] = cat.copy(isSelected = !cat.isSelected)
                                        persist()
                                    },
                                    modifier = Modifier.heightIn(min = 50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(2.dp, if (cat.isSelected) DeepGreen else PureBlack),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (cat.isSelected) DeepGreen else PureWhite,
                                        contentColor = if (cat.isSelected) PureWhite else PureBlack
                                    )
                                ) {
                                    Text(cat.title, style = MaterialTheme.typography.labelLarge)
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

                // Grand Total card
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
                            text = "Стены: ${Formatters.formatArea(t.totalWallAreaSqm)} · Пол: ${Formatters.formatArea(floorArea)} · Углы: ${Formatters.formatRunningMeters(t.totalCornersMeters)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhite
                        )
                    }
                }

                // Price per m2 / running meter for selected categories
                val selectedItems = categories.mapIndexedNotNull { idx, c -> if (c.isSelected) idx to c else null }
                if (selectedItems.isNotEmpty()) {
                    item {
                        ZamerCard(modifier = Modifier.widthIn(max = 600.dp), borderColor = PureBlack) {
                            Text("Цена за 1 м² / п.м.", style = MaterialTheme.typography.titleLarge, color = PureBlack)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        useOuterFloor = !useOuterFloor
                                        persist()
                                    }
                            ) {
                                Checkbox(
                                    checked = useOuterFloor,
                                    onCheckedChange = {
                                        useOuterFloor = it
                                        persist()
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = DeepGreen)
                                )
                                Text("Пол считать со стенами", style = MaterialTheme.typography.bodyMedium, color = PureBlack)
                            }
                            Spacer(Modifier.height(8.dp))

                            selectedItems.forEach { (idx, cat) ->
                                val qty = qtyFor(cat)
                                val sum = qty * cat.pricePerUnit
                                var priceText by remember(cat.id, p.id) {
                                    mutableStateOf(
                                        if (cat.pricePerUnit > 0) Formatters.formatCleanNumber(cat.pricePerUnit) else ""
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .background(DeepGreenSoft, RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cat.title, style = MaterialTheme.typography.titleMedium, color = PureBlack)
                                            Text(
                                                text = "Объём: ${Formatters.formatCleanNumber(qty)} ${cat.unit}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = DeepGreen
                                            )
                                        }
                                        Text(
                                            text = Formatters.formatMoney(sum),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = PureBlack
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    ZamerInputField(
                                        value = priceText,
                                        onValueChange = { txt ->
                                            priceText = txt
                                            val newPrice = Formatters.parseDecimalInput(txt) ?: 0.0
                                            categories[idx] = cat.copy(pricePerUnit = newPrice)
                                            persist()
                                        },
                                        label = "Цена за ${cat.unit} (₽)",
                                        placeholder = "0",
                                        keyboardType = KeyboardType.Decimal
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
                            text = "Доп. работы (${Formatters.formatMoney(t.minorWorksTotalRub)})",
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("WALLS" to "Стены", "FLOOR" to "Пол", "CORNERS" to "Углы").forEach { (kind, lbl) ->
                            OutlinedButton(
                                onClick = { sourceKind = kind },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(2.dp, if (sourceKind == kind) DeepGreen else PureBlack),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (sourceKind == kind) DeepGreen else PureWhite,
                                    contentColor = if (sourceKind == kind) PureWhite else PureBlack
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
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
                            val unit = if (sourceKind == "CORNERS") "п.м." else "м²"
                            categories.add(
                                EstimateCategoryConfig(
                                    id = UUID.randomUUID().toString(),
                                    title = catTitle.trim(),
                                    unit = unit,
                                    sourceKind = sourceKind,
                                    isSelected = true
                                )
                            )
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
            // Single A4 page: 595 x 842 pt
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
            val selected = categories.filter { it.isSelected }
            if (selected.isEmpty()) {
                c.drawText("Стены: ${Formatters.formatArea(t.totalWallAreaSqm)}  |  Пол: ${Formatters.formatArea(floorArea)}", 40f, y, bodyPaint)
                y += 18f
            } else {
                selected.forEach { cat ->
                    if (y < 760f) {
                        val qty = when (cat.sourceKind) {
                            "WALLS" -> t.totalWallAreaSqm
                            "FLOOR" -> floorArea
                            "CORNERS" -> t.totalCornersMeters
                            else -> if (cat.customQuantity > 0) cat.customQuantity else t.totalWallAreaSqm
                        }
                        val sum = qty * cat.pricePerUnit
                        c.drawText("${cat.title}: ${Formatters.formatCleanNumber(qty)} ${cat.unit} × ${Formatters.formatMoney(cat.pricePerUnit)}", 40f, y, bodyPaint)
                        c.drawText(Formatters.formatMoney(sum), 450f, y, bodyPaint)
                        y += 18f
                    }
                }
            }

            if (aggregate.minorWorks.isNotEmpty() && y < 740f) {
                y += 8f
                c.drawText("ДОПОЛНИТЕЛЬНЫЕ РАБОТЫ", 40f, y, headPaint)
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
