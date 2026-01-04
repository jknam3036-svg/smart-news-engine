package com.example.make.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.example.make.data.model.IntelligenceItem
import com.example.make.data.model.ItemPriority
import com.example.make.data.model.SourceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailScreen(mailRepository: com.example.make.data.repository.MailRepository) {
    // Collect from DB via repository flow
    val allItems by mailRepository.getAllEmails().collectAsState(initial = emptyList())
    val emailItems = allItems.filter { it.item.sourceType == SourceType.EMAIL }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Use token from Application/Settings
    val app = context.applicationContext as? com.example.make.MakeApplication
    val gmailAccessToken = app?.getGmailAccessToken() ?: ""

    // State for Detail View
    var selectedItem by remember { mutableStateOf<IntelligenceItem?>(null) }
    var isLoadingFullBody by remember { mutableStateOf(false) }
    var fullEmailBody by remember { mutableStateOf<String?>(null) }
    var isComposingReply by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }

    // Detail Dialog
    if (selectedItem != null) {
        // Load full email body when dialog opens
        LaunchedEffect(selectedItem) {
            isLoadingFullBody = true
            // First check if it's already in the DB entity
            val dbItem = emailItems.find { it.item.id == selectedItem?.id }
            if (dbItem?.item?.rawContent != null && dbItem.item.rawContent.length > 50) {
                 fullEmailBody = dbItem.item.rawContent
            } else {
                // Fetch from API
                try {
                    if (gmailAccessToken.isNotBlank()) {
                        val detail = mailRepository.getFullMessage(gmailAccessToken, selectedItem!!.id)
                        fullEmailBody = com.example.make.data.remote.GmailBodyExtractor.extractBody(detail.payload).getDisplayText()
                    } else {
                        fullEmailBody = "Gmail 인증 토큰이 설정되지 않았습니다. 설정 메뉴에서 토큰을 입력해주세요."
                    }
                } catch (e: Exception) {
                    fullEmailBody = "본문을 불러오지 못했습니다. (권한 또는 네트워크 오류)"
                }
            }
            isLoadingFullBody = false
        }
        
        AlertDialog(
            onDismissRequest = { 
                if (!isSendingReply) {
                    selectedItem = null
                    isComposingReply = false
                    replyText = ""
                    fullEmailBody = null
                }
            },
            title = { 
                Column {
                    Text(
                        text = if (isComposingReply) "답장 작성" else "메시지 상세", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold
                    )
                    if (!isComposingReply) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "From: ${selectedItem?.sourceId ?: "Unknown"}", 
                            style = MaterialTheme.typography.labelMedium, 
                            color = Color.Gray
                        )
                    }
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
                            text = "받는 사람: ${selectedItem?.sourceId ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            placeholder = { Text("답장 내용을 입력하세요...") },
                            maxLines = 10,
                            enabled = !isSendingReply
                        )
                        
                        if (isSendingReply) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "━━━ 원본 메시지 ━━━",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = fullEmailBody ?: selectedItem?.summary ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        // View mode
                        if (isLoadingFullBody) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Text(
                                text = fullEmailBody ?: selectedItem?.summary ?: "내용 없음",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "수집 일시: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(selectedItem?.capturedAt ?: 0L))}",
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
                                isSendingReply = true
                                val success = mailRepository.sendReply(gmailAccessToken, selectedItem!!.id, replyText)
                                isSendingReply = false
                                
                                if (success) {
                                    android.widget.Toast.makeText(context, "답장이 성공적으로 전송되었습니다!", android.widget.Toast.LENGTH_SHORT).show()
                                    selectedItem = null
                                    isComposingReply = false
                                    replyText = ""
                                } else {
                                    android.widget.Toast.makeText(context, "전송 실패. API 설정을 확인하세요.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = replyText.isNotBlank() && !isSendingReply
                    ) {
                        Text("전송")
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
                            selectedItem = null
                            fullEmailBody = null
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
                        },
                        enabled = !isSendingReply
                    ) {
                        Text("취소")
                    }
                } else {
                    TextButton(
                        onClick = {
                            scope.launch {
                                // For now, since we deleted the direct DAO access, 
                                // we'll just close the dialog. Real delete should be in Repo.
                                selectedItem = null
                                fullEmailBody = null
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
        topBar = {
            TopAppBar(
                title = { Text("스마트 인박스") },
                actions = {
                    IconButton(onClick = { 
                        scope.launch {
                            isRefreshing = true
                            try {
                                // Fetch real data from Gmail API
                                if (gmailAccessToken.isNotBlank()) {
                                    mailRepository.syncEmails(gmailAccessToken)
                                    android.widget.Toast.makeText(context, "이메일 동기화 완료", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Gmail 토큰이 설정되지 않았습니다", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "동기화 실패: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                            isRefreshing = false
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // 1. Permission Check Banner
            val context = androidx.compose.ui.platform.LocalContext.current
            val isListenerGranted = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true

            if (!isListenerGranted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⚠️ 알림 접근 권한 필요 (필수)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("이메일을 실시간으로 수집하려면 권한이 필요합니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(
                            onClick = { 
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
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
            } else if (emailItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("수신된 이메일이 없습니다.", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "이 기능은 GMail/삼성 이메일 앱의 '알림'을 자동으로 감지하여 분석합니다.\n새로운 이메일 알림이 도착하면 이곳에 표시됩니다.", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(emailItems) { populated ->
                        val itemModel = IntelligenceItem(
                            id = populated.item.id, sourceType = populated.item.sourceType, sourceId = populated.item.senderOrSource ?: "Unknown",
                            summary = populated.item.summary ?: "", priority = populated.item.priority,
                            tags = populated.tags.map { it.name }, capturedAt = populated.item.capturedAt,
                            suggestedAction = populated.item.suggestedAction
                        )
                        EmailCard(itemModel, onClick = { selectedItem = itemModel })
                    }
                }
            }
        }
    }
}

@Composable
fun EmailCard(item: IntelligenceItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.priority == ItemPriority.CRITICAL) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) 
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.priority == ItemPriority.CRITICAL) {
                    Text("‼️ 긴급", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                item.tags.forEach { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = tag, 
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Body
            Text(text = item.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(text = "From: ${item.sourceId}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            // Action Footer
            if (item.suggestedAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {}, 
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("추천 액션: ${item.suggestedAction}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
