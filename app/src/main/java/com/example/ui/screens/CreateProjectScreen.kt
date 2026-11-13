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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ZamerInputField
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.ProjectDraft

@Composable
fun CreateProjectScreen(
    draft: ProjectDraft,
    onDraftChange: (name: String?, address: String?, date: String?, hasStretchCeiling: Boolean?) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit
) {
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
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        onValueChange = { onDraftChange(it, "", null, null) },
                        label = "Название / адрес",
                        placeholder = "Например: ул. Ленина 14, кв. 5",
                        imeAction = ImeAction.Next,
                        testTag = "project_name_input"
                    )

                    ZamerInputField(
                        value = draft.startDate,
                        onValueChange = { raw ->
                            // Allow typing digits and dots for numeric date entry (дд.мм.гггг)
                            val filtered = raw.filter { it.isDigit() || it == '.' }.take(10)
                            onDraftChange(null, null, filtered, null)
                        },
                        label = "Дата начала работ",
                        placeholder = "дд.мм.гггг",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                        onDone = onNextClick,
                        testTag = "project_date_input"
                    )

                    Spacer(Modifier.height(6.dp))

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
}
