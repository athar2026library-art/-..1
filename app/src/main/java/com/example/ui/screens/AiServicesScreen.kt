package com.example.ui.screens

import com.example.ui.components.BaqiyatBackButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import com.example.ui.AppViewModel
import com.example.ui.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiServicesScreen(
    viewModel: AppViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isLoadingAi by viewModel.isLoadingAi.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("المساعد الذكي", fontWeight = FontWeight.Bold) },
                navigationIcon = { if (onNavigateBack != null) BaqiyatBackButton(onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it.take(500) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("اسأل عن ذكر أو صف شعورك...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (isLoadingAi) {
                        TextButton(onClick = viewModel::cancelAiRequest) {
                            Text("إلغاء")
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val t = inputText.trim()
                                if (t.isNotEmpty()) {
                                    viewModel.sendChatMessage(t)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(4.dp),
                            enabled = inputText.isNotBlank()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            if (messages.size <= 1 && !isLoadingAi) {
                item { MoodChips(onPick = { viewModel.sendChatMessage(it) }) }
            }
        }
    }
}

private val MOODS = listOf(
    "قلق" to "أشعر بالقلق",
    "حزن" to "أشعر بالحزن",
    "ضيق" to "أشعر بضيق في صدري",
    "خوف" to "أشعر بالخوف",
    "شكر" to "أريد أن أشكر الله",
    "فرح" to "أشعر بالفرح والسرور",
    "سفر" to "أنا مسافر الآن",
    "استخارة" to "أريد أن أستخير الله في أمر",
    "مرض" to "أنا أو أحد أحبابي مريض",
    "توبة" to "أريد التوبة والاستغفار"
)

/** رقائق شعور سريعة قبل الكتابة: تُرسل طلباً جاهزاً للمساعد. */
@Composable
private fun MoodChips(onPick: (String) -> Unit) {
    Column {
        Text("كيف تشعر الآن؟", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        MOODS.chunked(5).forEach { row ->
            Row(
                Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, phrase) ->
                    com.example.ui.components.BaqiyatChip(
                        text = label,
                        onClick = { onPick("$phrase. اقترح لي ذكراً أو دعاءً مناسباً من الأحاديث الصحيحة مع مصدره.") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterStart else Alignment.CenterEnd
    val bubbleColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shape = if (message.isUser) {
        RoundedCornerShape(24.dp, 24.dp, 4.dp, 24.dp)
    } else {
        RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (message.isUser) 0.dp else 1.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (message.isLoading) {
                    ThinkingIndicator()
                } else {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    val transition = rememberInfiniteTransition(label = "ai-thinking")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai-thinking-pulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f + pulse * 0.35f),
                strokeWidth = 2.5.dp
            )
            Text(
                text = "✦",
                color = MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                fontSize = 12.sp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "جاري التحضير...",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = (0.35f + pulse * 0.5f) - index * 0.08f
                                ),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
