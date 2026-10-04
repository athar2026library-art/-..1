package com.example.ui.screens

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel
) {
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val autoDnd by viewModel.autoDnd.collectAsStateWithLifecycle()
    val hideVirtues by viewModel.hideVirtues.collectAsStateWithLifecycle()
    val hideSources by viewModel.hideSources.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val autoPlayEnabled by viewModel.autoPlayEnabled.collectAsStateWithLifecycle()

    val userSignedIn by viewModel.userSignedIn.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف الحساب نهائياً؟") },
            text = {
                Text("سيُحذف تقدمك السحابي والشكاوى وحساب تسجيل الدخول. لا يمكن التراجع.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccount()
                    }
                ) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("إلغاء") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (statusMessage.isNotBlank()) {
                Text(
                    statusMessage,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Text("المظهر", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الوضع الليلي", fontSize = 16.sp)
                        Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("حجم الخط (${fontSize.toInt()})", fontSize = 16.sp)
                    var localFontSize by remember(fontSize) { mutableFloatStateOf(fontSize) }
                    Slider(
                        value = localFontSize,
                        onValueChange = { localFontSize = it },
                        onValueChangeFinished = { viewModel.setFontSize(localFontSize) },
                        valueRange = 16f..48f,
                        steps = 8
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("التذكيرات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تذكيرات الأذكار", fontSize = 16.sp)
                            Text("صباح ومساء — يمكن إيقافها بالكامل", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = notificationsEnabled, onCheckedChange = { viewModel.setNotificationsEnabled(it) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("القراءة والتفاعل", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SettingSwitchRow("الاهتزاز عند التسبيح", isVibrationEnabled) { viewModel.setVibration(it) }
                    SettingDivider()
                    SettingSwitchRow("القراءة المستمرة", autoPlayEnabled, "تشغيل الصوت والانتقال للذكر التالي تلقائياً") { viewModel.setAutoPlay(it) }
                    SettingDivider()
                    SettingSwitchRow("إبقاء الشاشة مضاءة", keepScreenOn, "أثناء قراءة الأذكار فقط") { viewModel.setKeepScreenOn(it) }
                    SettingDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("وضع التركيز", fontSize = 16.sp)
                            Text("كتم الإشعارات تلقائياً في شاشة الأذكار", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoDnd,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    if (!nm.isNotificationPolicyAccessGranted) {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                    } else viewModel.setAutoDnd(true)
                                } else viewModel.setAutoDnd(false)
                            }
                        )
                    }
                    SettingDivider()
                    SettingSwitchRow("إخفاء الفضائل", hideVirtues, "قراءة النص فقط بدون تفاصيل إضافية") { viewModel.setHideVirtues(it) }
                    SettingDivider()
                    SettingSwitchRow("إخفاء المصادر", hideSources, "تبسيط شاشة القراءة") { viewModel.setHideSources(it) }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("المزامنة السحابية", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "المزامنة التلقائية تعمل في الخلفية كل 6 ساعات عند تسجيل الدخول.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (userSignedIn) {
                        Text("تم تسجيل الدخول", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.syncData() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text("مزامنة الآن")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Text("تسجيل الخروج")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("حذف الحساب نهائياً", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Text("سجل الدخول لحفظ تقدمك", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                activity?.let { viewModel.signIn(it) }
                                    ?: run { /* no activity */ }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("google-sign-in-button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = activity != null
                        ) {
                            Text("تسجيل الدخول باستخدام Google")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("عن التطبيق", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(com.example.R.drawable.logo_baqiyat),
                        contentDescription = "شعار الباقيات",
                        modifier = Modifier.size(112.dp).clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("الباقيات", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "تطبيق هادئ يساعدك على المحافظة على الأذكار والورد اليومي.\nالمساعد الذكي: إجابة آلية، ليست فتوى.",
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "جرّب تطبيق الباقيات")
                                putExtra(Intent.EXTRA_TEXT, "جرّب تطبيق الباقيات للمحافظة على الأذكار والورد اليومي.")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الباقيات"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مشاركة التطبيق")
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingDivider() {
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
