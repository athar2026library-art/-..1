package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiServicesScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val aiResponse by viewModel.aiResponse.collectAsState()
    val aiImageBase64 by viewModel.aiImageBase64.collectAsState()
    val userSignedIn by viewModel.userSignedIn.collectAsState()

    var textInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("خدمات الذكاء الاصطناعي والمزامنة") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // --- Section 1: Auth and Sync ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("المزامنة السحابية (Firestore)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (userSignedIn) {
                        Text("تم تسجيل الدخول", color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(onClick = { viewModel.backupData() }) {
                                Text("مزامنة الإنجاز 🌴")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = { viewModel.signOut() }) {
                                Text("تسجيل الخروج")
                            }
                        }
                    } else {
                        Button(onClick = { viewModel.signIn() }) {
                            Text("تسجيل الدخول باستخدام Google")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Section 2: AI Inputs ---
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("اكتب سؤالك أو وصف الصورة/الفيديو...") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.askAi(textInput) }, modifier = Modifier.weight(1f)) {
                    Text("اسأل الذكاء (بحث)")
                }
                Button(onClick = { viewModel.generateAiImage(textInput) }, modifier = Modifier.weight(1f)) {
                    Text("رسم خلفية (صورة)")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.generateAiVideo(textInput) }, modifier = Modifier.fillMaxWidth()) {
                Text("توليد فيديو إسلامي (Veo)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Section 3: AI Response ---
            if (aiResponse.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(text = aiResponse, modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            aiImageBase64?.let { base64String ->
                val bitmap = remember(base64String) {
                    try {
                        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "AI Generated Image",
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                } else {
                    Text("خطأ في عرض الصورة")
                }
            }
        }
    }
}
