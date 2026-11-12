package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ZamerInputField
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerSecondaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.ProjectDraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    draft: ProjectDraft,
    onDraftChange: (name: String?, address: String?, date: String?) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PureWhite,
        topBar = {
            ZamerTopBar(
                title = "Новый объект",
                subtitle = "Шаг 1 из 2 — основные данные",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        AnimatedScreenEntrance(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ZamerInputField(
                        value = draft.name,
                        onValueChange = { onDraftChange(it, null, null) },
                        label = "Название объекта",
                        placeholder = "Например: Квартира Иванова",
                        imeAction = ImeAction.Next,
                        testTag = "project_name_input"
                    )

                    ZamerInputField(
                        value = draft.address,
                        onValueChange = { onDraftChange(null, it, null) },
                        label = "Адрес",
                        placeholder = "г. Новомосковск, ул. ...",
                        imeAction = ImeAction.Next,
                        testTag = "project_address_input"
                    )

                    ZamerInputField(
                        value = draft.startDate,
                        onValueChange = { onDraftChange(null, null, it) },
                        label = "Дата начала работ",
                        placeholder = "дд.мм.гггг",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                        onDone = onNextClick,
                        testTag = "project_date_input"
                    )

                    ZamerSecondaryButton(
                        text = "Выбрать дату в календаре",
                        onClick = { showDatePicker = true },
                        testTag = "pick_date_button"
                    )

                    Spacer(Modifier.height(8.dp))

                    ZamerPrimaryButton(
                        text = "Дальше — планировка",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onNextClick,
                        testTag = "next_to_layout_button"
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            val formatted = SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU")).format(Date(ms))
                            onDraftChange(null, null, formatted)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Готово", color = PureBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена", color = PureBlack)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
