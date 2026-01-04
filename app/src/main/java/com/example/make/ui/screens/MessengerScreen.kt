package com.example.make.ui.screens

import android.content.Intent
import androidx.compose.ui.draw.clip
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.launch
import com.example.make.data.model.IMMessage
import com.example.make.data.model.MessengerType
import com.example.make.data.model.SourceType
import com.example.make.data.model.ItemPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerScreen(dao: com.example.make.data.local.dao.IntelligenceDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember { mutableStateOf(false) }

    // Check permission
    LaunchedEffect(Unit) {
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val packageName = context.packageName
        hasPermission = enabledListeners != null && enabledListeners.contains(packageName)
    }

    // Collect from DB
    val allItems by dao.searchItems("").collectAsState(initial = emptyList())
    // Filter for IM types (KAKAOTALK, LINE, SLACK, etc. are specialized Enums, but stored as MESSENGER SourceType in DB)
    // We assume sourceType = MESSENGER.
    val messages = allItems.filter { it.item.sourceType == SourceType.MESSENGER }

    // Demo Data if empty
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
             dao.insertItem(com.example.make.data.local.entity.IntelligenceEntity(
                id = "m_demo_1", sourceType = SourceType.MESSENGER, sourceId = "kakao_1",
                rawContent = "Please check design.", summary = "Attached design draft", 
                priority = ItemPriority.NORMAL, capturedAt = System.currentTimeMillis(),
                senderOrSource = "Team Lead", type = "KAKAOTALK"
            ))
            dao.insertItem(com.example.make.data.local.entity.IntelligenceEntity(
                id = "m_demo_2", sourceType = SourceType.MESSENGER, sourceId = "slack_1",
                rawContent = "Deploy success", summary = "Deployment to Staging successful", 
                priority = ItemPriority.NORMAL, capturedAt = System.currentTimeMillis() - 300000,
                senderOrSource = "DevOps Bot", type = "SLACK"
            ))
        }
    }
    var selectedMessage by remember { mutableStateOf<IMMessage?>(null) }
    var isComposingReply by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }


    // Detail Dialog
    if (selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                selectedMessage = null
                isComposingReply = false
                replyText = ""
            },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isComposingReply) {
                        Surface(
                            color = when(selectedMessage?.type) {
                                MessengerType.KAKAOTALK -> Color(0xFFFFE812)
                                MessengerType.SLACK -> Color(0xFF4A154B)
                                MessengerType.LINE -> Color(0xFF06C755)
                                else -> Color.Gray
                            },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.size(24.dp),
                            content = {}
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isComposingReply) "답장 작성" else (selectedMessage?.senderName ?: "Unknown"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isComposingReply) {
                        // Reply compose UI
                        Text(
                            text = "받는 사람: ${selectedMessage?.senderName ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedMessage?.isGroupChat == true && selectedMessage?.roomName != null) {
                            Text(
                                text = "채팅방: ${selectedMessage?.roomName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            placeholder = { Text("답장 내용을 입력하세요...") },
                            maxLines = 10
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "━━━ 원본 메시지 ━━━",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = selectedMessage?.content ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        // View mode
                        Text(
                            text = selectedMessage?.content ?: "내용 없음",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        
                        if (selectedMessage?.isGroupChat == true && selectedMessage?.roomName != null) {
                            Text(
                                text = "채팅방: ${selectedMessage?.roomName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        val timeText = remember(selectedMessage?.receivedAt) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.KOREA)
                            sdf.format(java.util.Date(selectedMessage?.receivedAt ?: 0L))
                        }
                        Text(
                            text = "수신 일시: $timeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                if (isComposingReply) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val messengerType = when(selectedMessage?.type) {
                                    MessengerType.KAKAOTALK -> "카카오톡"
                                    MessengerType.SLACK -> "슬랙"
                                    MessengerType.LINE -> "라인"
                                    MessengerType.TELEGRAM -> "텔레그램"
                                    else -> "메신저"
                                }
                                
                                // Open messenger app with intent
                                try {
                                    val packageName = when(selectedMessage?.type) {
                                        MessengerType.KAKAOTALK -> "com.kakao.talk"
                                        MessengerType.SLACK -> "com.slack"
                                        MessengerType.LINE -> "jp.naver.line.android"
                                        MessengerType.TELEGRAM -> "org.telegram.messenger"
                                        else -> null
                                    }
                                    
                                    if (packageName != null) {
                                        // Check if app is installed
                                        val isInstalled = try {
                                            context.packageManager.getPackageInfo(packageName, 0)
                                            true
                                        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                                            false
                                        }
                                        
                                        if (isInstalled) {
                                            // Copy to clipboard first
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("reply", replyText)
                                            clipboard.setPrimaryClip(clip)
                                            
                                            // Try to launch the app
                                            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                            if (intent != null) {
                                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                                
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "$messengerType 앱에서 답장을 보내주세요.\n내용이 클립보드에 복사되었습니다.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                // Fallback: try to open with action view
                                                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                                    setPackage(packageName)
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(fallbackIntent)
                                                
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "$messengerType 앱에서 답장을 보내주세요.\n내용이 클립보드에 복사되었습니다.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "$messengerType 앱이 설치되어 있지 않습니다.",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "앱 실행 중 오류가 발생했습니다: ${e.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                
                                isComposingReply = false
                                replyText = ""
                            }
                        },
                        enabled = replyText.isNotBlank()
                    ) {
                        Text("앱에서 보내기")
                    }
                } else {
                    Row {
                        TextButton(
                            onClick = { isComposingReply = true }
                        ) {
                            Text("답장")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { selectedMessage = null }) {
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
                                selectedMessage?.let { msg ->
                                    dao.deleteItem("noti_${msg.type}_${msg.receivedAt}")
                                }
                                selectedMessage = null
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
                    Text(
                        "MESSENGER DETECTION", 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Real-time Alerts", 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Permission Warning Banner
            if (!hasPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    onClick = {
                        // Open Notification Listener Settings
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("권한 필요", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("앱 알림 접근 권한을 허용하려면 여기를 누르세요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Message List
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { populated ->
                    // Map Entity -> IMMessage
                    val type = try {
                        MessengerType.valueOf(populated.item.type ?: "KAKAOTALK")
                    } catch (e: Exception) { MessengerType.KAKAOTALK }
                    
                    val msg = IMMessage(
                        id = populated.item.id,
                        type = type,
                        senderName = populated.item.senderOrSource ?: "Unknown",
                        content = populated.item.summary ?: "", // In DB we store content in summary or raw. Summary is safer if analyzed.
                        receivedAt = populated.item.capturedAt,
                        isGroupChat = false, // DB schema doesn't have isGroupChat yet, assuming false
                        roomName = null
                    )
                    MessengerItemCard(msg, onClick = { selectedMessage = msg })
                }
            }
        }
    }
}

@Composable
fun MessengerItemCard(msg: IMMessage, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Icon (Source)
                Surface(
                    color = when(msg.type) {
                        MessengerType.KAKAOTALK -> Color(0xFFFFE812)
                        MessengerType.SLACK -> Color(0xFF4A154B)
                        MessengerType.LINE -> Color(0xFF06C755)
                        MessengerType.TELEGRAM -> Color(0xFF24A1DE)
                        else -> Color.Gray
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp),
                    content = {}
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (msg.isGroupChat && msg.roomName != null) "${msg.roomName} (${msg.senderName})" else msg.senderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    val appName = when(msg.type) {
                        MessengerType.KAKAOTALK -> "카카오톡"
                        MessengerType.SLACK -> "슬랙"
                        MessengerType.LINE -> "라인"
                        MessengerType.TELEGRAM -> "텔레그램"
                        else -> "메신저"
                    }
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                
                val timeText = remember(msg.receivedAt) {
                    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.KOREA)
                    sdf.format(java.util.Date(msg.receivedAt))
                }
                Text(
                    text = timeText, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = msg.content, 
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )
        }
    }
}
