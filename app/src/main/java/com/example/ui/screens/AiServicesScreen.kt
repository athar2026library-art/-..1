package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.AudioPlayer

data class ChatMessage(val text: String, val isUser: Boolean, val isLoading: Boolean = false)

private val suggestedPrompts = listOf(
    "أشعر بالقلق وأريد ذكراً صحيحاً",
    "أريد أذكاراً قبل النوم",
    "ما الذكر المناسب للسفر؟"
)

private fun extractSuggestedZekr(response: String): String? {
    if (!response.contains("الذكر:")) return null
    val value = response.substringAfter("الذكر:")
        .substringBefore("فضله:")
        .substringBefore("المصدر:")
        .substringBefore("المصدر والتخريج:")
        .trim()
        .removePrefix("[")
        .removeSuffix("]")
        .trim()
    return value.takeIf { it.length >= 3 && !it.equals("النص", ignoreCase = true) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiServicesScreen(viewModel: AppViewModel) {
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isLoadingAi by viewModel.isLoadingAi.collectAsState()
    val audioSpeed by viewModel.audioSpeed.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(listOf(ChatMessage("السلام عليكم، يمكنني مساعدتك في البحث عن الأذكار وشرح معانيها. تحقق دائماً من المصدر الشرعي المذكور.", isUser = false)))
    }

    val context = LocalContext.current
    val player = remember(context) { AudioPlayer(context) }
    LaunchedEffect(audioSpeed) { player.setSpeechRate(audioSpeed) }
    DisposableEffect(player) { onDispose { player.shutdown() } }

    val submitPrompt: (String) -> Unit = { prompt ->
        if (prompt.isNotBlank() && !isLoadingAi) {
            messages = messages + ChatMessage(prompt, isUser = true)
            viewModel.suggestZekr(prompt)
        }
    }

    LaunchedEffect(aiResponse, isLoadingAi) {
        if (isLoadingAi) {
            if (messages.lastOrNull()?.isLoading != true) {
                messages = messages + ChatMessage("جاري البحث...", isUser = false, isLoading = true)
            }
        } else if (aiResponse.isNotEmpty()) {
            messages = messages.filterNot { it.isLoading } + ChatMessage(aiResponse, isUser = false)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("المساعد الذكي", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("بماذا تشعر؟ أو أدخل ذكراً لتدبره...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            val prompt = inputText.trim()
                            inputText = ""
                            submitPrompt(prompt)
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).padding(4.dp),
                        enabled = inputText.isNotBlank() && !isLoadingAi
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (messages.size <= 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("أسئلة مقترحة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        suggestedPrompts.forEach { prompt ->
                            OutlinedButton(
                                onClick = { submitPrompt(prompt) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoadingAi
                            ) { Text(prompt) }
                        }
                    }
                }
            }
            items(messages) { message ->
                ChatBubble(
                    message = message,
                    onSaveZekr = { text -> viewModel.saveMyZekr(text) },
                    onPlayZekr = { text -> player.stop(); player.playLoop(text, 1) }
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSaveZekr: (String) -> Unit,
    onPlayZekr: (String) -> Unit
) {
    val alignment = if (message.isUser) Alignment.CenterStart else Alignment.CenterEnd
    val bubbleColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shape = if (message.isUser) RoundedCornerShape(24.dp, 24.dp, 4.dp, 24.dp) else RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp)
    val suggestedZekr = if (!message.isUser && !message.isLoading) extractSuggestedZekr(message.text) else null

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (message.isUser) 0.dp else 1.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (message.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                } else {
                    Text(text = message.text, color = textColor, fontSize = 16.sp, lineHeight = 24.sp)
                    if (suggestedZekr != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onPlayZekr(suggestedZekr) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("استماع")
                            }
                            TextButton(onClick = { onSaveZekr(suggestedZekr) }) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("أضف إلى أذكاري")
                            }
                        }
                    }
                }
            }
        }
    }
}
