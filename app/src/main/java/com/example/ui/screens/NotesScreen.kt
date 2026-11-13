package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProjectAggregate
import com.example.ui.components.AnimatedScreenEntrance
import com.example.ui.components.ZamerPrimaryButton
import com.example.ui.components.ZamerSecondaryButton
import com.example.ui.components.ZamerTopBar
import com.example.ui.components.rememberVoiceInputLauncher
import com.example.ui.theme.BlackBorderSubtle
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.PtSerifFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch

@Composable
fun NotesScreen(
    aggregate: ProjectAggregate?,
    onBackClick: () -> Unit,
    onSaveNotes: (Long, String) -> Unit
) {
    if (aggregate == null) return

    val p = aggregate.project
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var notesText by remember(p.id, p.notesText) { mutableStateOf(p.notesText) }

    val launchVoice = rememberVoiceInputLauncher(
        promptText = "Продиктуйте заметку",
        onResult = { spoken ->
            val updated = if (notesText.isBlank()) spoken else "${notesText.trimEnd()}\n$spoken"
            notesText = updated
            onSaveNotes(p.id, updated)
        },
        onUnavailable = {
            scope.launch {
                snackbarHostState.showSnackbar("Голосовой ввод недоступен на устройстве")
            }
        }
    )

    Scaffold(
        containerColor = PureWhite,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ZamerTopBar(
                title = "Заметки",
                subtitle = p.name,
                onBackClick = {
                    onSaveNotes(p.id, notesText)
                    onBackClick()
                }
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ZamerSecondaryButton(
                        text = "Голосовой ввод",
                        icon = Icons.Filled.Mic,
                        borderColor = DeepGreen,
                        textColor = DeepGreen,
                        onClick = launchVoice,
                        testTag = "notes_voice_button"
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp)
                            .testTag("notes_text_input"),
                        placeholder = {
                            Text(
                                text = "Запишите важные детали по объекту, коды домофона, пожелания заказчика...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = BlackBorderSubtle
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = PtSerifFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            color = PureBlack
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepGreen,
                            unfocusedBorderColor = PureBlack,
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite,
                            cursorColor = DeepGreen
                        )
                    )

                    ZamerPrimaryButton(
                        text = "Сохранить заметки",
                        icon = Icons.Filled.Save,
                        onClick = {
                            onSaveNotes(p.id, notesText)
                            scope.launch {
                                snackbarHostState.showSnackbar("Заметки сохранены")
                            }
                        },
                        testTag = "notes_save_button"
                    )

                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
