package com.example.make.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Refresh
import com.example.make.data.repository.SmsRepository
import com.example.make.data.model.SourceType
import com.example.make.data.model.ItemPriority
import com.example.make.data.model.SMSMessage
import com.example.make.data.model.SMSCategory
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(dao: com.example.make.data.local.dao.IntelligenceDao) {
    // Collect from DB
    val allItems by dao.searchItems("").collectAsState(initial = emptyList())
    val messages = allItems.filter { it.item.sourceType == SourceType.SMS }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // State for Detail View
    var selectedSms by remember { mutableStateOf<SMSMessage?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Permission Launcher
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
           scope.launch {
               isRefreshing = true
               com.example.make.data.repository.SmsRepository().syncSmsFromDevice(context, dao)
               isRefreshing = false
           }
        }
    }

    // Auto-scan if permission already granted
    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
             if (messages.isEmpty()) { // Only auto-scan if empty to avoid spamming
                 com.example.make.data.repository.SmsRepository().syncSmsFromDevice(context, dao)
             }
        }
    }
    
    // Detail Dialog
    var isComposingReply by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    
    if (selectedSms != null) {
        AlertDialog(
            onDismissRequest = { 
                selectedSms = null
                isComposingReply = false
                replyText = ""
            },
            title = { 
                Text(
                    text = if (isComposingReply) "SMS 답장" else (selectedSms?.sender ?: "Unknown"), 
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    if (isComposingReply) {
                        // Reply compose UI
                        Text(
                            text = "받는 사람: ${selectedSms?.sender ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            placeholder = { Text("답장 내용을 입력하세요...") },
                            maxLines = 5
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "━━━ 원본 메시지 ━━━",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = selectedSms?.body ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    } else {
                        // View mode
                        Text(
                            text = selectedSms?.body ?: "", 
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "수신: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(selectedSms?.receivedAt ?: 0L))}",
                            style = MaterialTheme.typography.labelSmall, 
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                if (isComposingReply) {
                    TextButton(
                        onClick = {
                            // Send SMS using Android intent
                            try {
                                val smsIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("smsto:${selectedSms?.sender}")
                                    putExtra("sms_body", replyText)
                                }
                                context.startActivity(smsIntent)
                                
                                android.widget.Toast.makeText(
                                    context,
                                    "SMS 앱에서 전송 버튼을 눌러주세요.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                
                                isComposingReply = false
                                replyText = ""
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "SMS 앱을 실행할 수 없습니다.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = replyText.isNotBlank()
                    ) {
                        Text("SMS 앱에서 보내기")
                    }
                } else {
                    Row {
                        TextButton(
                            onClick = { isComposingReply = true }
                        ) {
                            Text("답장")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { 
                            selectedSms = null
                            isComposingReply = false
                            replyText = ""
                        }) {
                            Text("닫기")
                        }
                    }
                }
            },
            dismissButton = {
                if (isComposingReply) {
                    TextButton(
                        onClick = {
                            isComposingReply = false
                            replyText = ""
                        }
                    ) {
                        Text("취소")
                    }
                } else {
                    TextButton(
                        onClick = {
                            scope.launch {
                                selectedSms?.let { dao.deleteItem(it.id) }
                                selectedSms = null
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("삭제")
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "SMS ANALYTICS", 
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                            Text(
                                "Financial & Delivery", 
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        IconButton(
                            onClick = { 
                                val perm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS)
                                if (perm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    scope.launch {
                                        isRefreshing = true
                                        com.example.make.data.repository.SmsRepository().syncSmsFromDevice(context, dao)
                                        isRefreshing = false
                                    }
                                } else {
                                    launcher.launch(android.Manifest.permission.READ_SMS)
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // 1. Permission Banner
            val perm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS)
            if (perm != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⚠️ 문자 읽기 권한 필요", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("결제 내역 및 배송 정보를 분석하려면 권한이 필요합니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(
                            onClick = { launcher.launch(android.Manifest.permission.READ_SMS) },
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("권한 허용하기")
                        }
                    }
                }
            }

            // 2. Content
            if (isRefreshing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("수집된 문자 메시지가 없습니다.", style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (perm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            Text(
                                "우측 상단의 동기화 버튼을 눌러\n최근 50건의 문자를 가져오세요.", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = androidx.compose.ui.graphics.Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            Text(
                                "권한을 허용해야 메시지를 읽어올 수 있습니다.", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    item {
                        Text(
                            "최근 메시지 (${messages.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
        
                    // List Items
                    items(messages) { populated ->
                         // Map PopulatedItem to SMSMessage for UI
                         val msg = SMSMessage(
                            id = populated.item.id,
                            sender = populated.item.senderOrSource ?: "Unknown",
                            body = populated.item.summary ?: "", // Or rawContent
                            receivedAt = populated.item.capturedAt,
                            category = SMSCategory.DELIVERY // Default
                         )
                        SmsItemCard(msg, onClick = { selectedSms = msg })
                    }
                }
            }
        }
    }
}

@Composable
fun SmsItemCard(msg: SMSMessage, onClick: () -> Unit) {
    val icon = when(msg.category) {
        SMSCategory.DELIVERY -> Icons.Filled.LocalShipping
        SMSCategory.SPAM -> Icons.Filled.Block
        else -> Icons.AutoMirrored.Filled.Message
    }

    ListItem(
        modifier = Modifier.clickable { onClick() },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(msg.sender, fontWeight = FontWeight.SemiBold) },
        supportingContent = { 
            Text(
                msg.body, 
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        trailingContent = {
            val timeText = remember(msg.receivedAt) {
                val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.KOREA)
                sdf.format(java.util.Date(msg.receivedAt))
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    )
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}
