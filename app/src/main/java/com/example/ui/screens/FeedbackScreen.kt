package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import com.example.data.FeedbackDraft
import com.example.ui.AppViewModel

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun FeedbackScreen(viewModel: AppViewModel) {
    val response by viewModel.aiResponse.collectAsState()
    val signedIn by viewModel.userSignedIn.collectAsState()
    val history by viewModel.myFeedback.collectAsState()
    val unreadCount by viewModel.unreadFeedbackCount.collectAsState()
    var type by remember { mutableStateOf("suggestion") }
    var message by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> attachmentUri = uri }

    Scaffold(
        topBar = { TopAppBar(title = { Text("الشكاوى والاقتراحات", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            contentPadding = PaddingValues(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Column(Modifier.padding(19.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SupportAgent, null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.padding(5.dp))
                            Text("المساعد يسمعك", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("اكتب ما تريد بحرية، وسنقترح تصنيفاً وملخصاً قبل الإرسال.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .82f), fontSize = 13.sp, lineHeight = 21.sp)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "suggestion", onClick = { type = "suggestion" }, label = { Text("اقتراح") })
                    FilterChip(selected = type == "complaint", onClick = { type = "complaint" }, label = { Text("شكوى") })
                    FilterChip(selected = type == "technical", onClick = { type = "technical" }, label = { Text("مشكلة تقنية") })
                }
            }
            item {
                OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.fillMaxWidth(), minLines = 6, label = { Text("اكتب رسالتك") }, placeholder = { Text("مثلاً: أقترح إضافة...") }, shape = RoundedCornerShape(16.dp))
            }
            item {
                OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text(if (attachmentUri == null) "إرفاق صورة للمشكلة" else "تم اختيار صورة ✓")
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.padding(5.dp))
                        Text(if (signedIn) "أنت مسجل الدخول، وستستطيع متابعة الرد." else "سيُطلب تسجيل الدخول عند الإرسال لمتابعة الطلب والرد." , fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Button(onClick = { if (message.isNotBlank()) showConfirmation = true }, enabled = message.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                    Spacer(Modifier.padding(4.dp))
                    Text("مراجعة وإرسال")
                }
                if (response.isNotBlank()) { Spacer(Modifier.height(9.dp)); Text(response, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
            }
            if (history.isNotEmpty()) {
                item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("طلباتي السابقة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); if (unreadCount > 0) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(20.dp)) { Text("$unreadCount تحديث غير مقروء", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } } }
                items(history) { item ->
                    Card(onClick = { if (item.replyUnread) viewModel.markFeedbackReplyRead(item.id) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (item.replyUnread) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(15.dp)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(item.title.ifBlank { item.message.take(52) }, fontWeight = FontWeight.Bold); if (item.replyUnread) Text("جديد", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(5.dp)); Text(statusLabel(item.status), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp); if (item.adminReply.isNotBlank()) { Spacer(Modifier.height(7.dp)); Text("رد المالك: ${item.adminReply}", fontSize = 12.sp) } }
                    }
                }
            }
        }
    }
    if (showConfirmation) {
        androidx.compose.material3.AlertDialog(onDismissRequest = { showConfirmation = false }, title = { Text("تأكيد الإرسال") }, text = { Text("سيتم إرسال رسالتك إلى فريق أذكار، ويمكنك متابعة حالتها بعد تسجيل الدخول.") }, confirmButton = { Button(onClick = { showConfirmation = false; viewModel.submitFeedback(FeedbackDraft(type = type, title = if (type == "suggestion") "اقتراح مستخدم" else "طلب من المستخدم", message = message, aiSummary = message.take(140), aiCategory = type, attachmentUri = attachmentUri)) }) { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.padding(3.dp)); Text("تأكيد") } }, dismissButton = { Button(onClick = { showConfirmation = false }) { Text("تعديل") } })
    }
}

private fun statusLabel(status: String): String = when (status) { "new" -> "جديد"; "in_progress" -> "قيد المعالجة"; "resolved" -> "تم الحل"; "completed" -> "مكتملة"; else -> "مغلق" }
