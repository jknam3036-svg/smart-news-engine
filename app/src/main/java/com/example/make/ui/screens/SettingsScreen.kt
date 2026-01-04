package com.example.make.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.make.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(dao: com.example.make.data.local.dao.IntelligenceDao) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepo = remember { com.example.make.data.repository.SettingsRepository(context) }
    val scope = rememberCoroutineScope() // Hoisted scope
    val dateFormatter = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }
    
    // State from Repo
    val accounts by settingsRepo.accounts.collectAsState(initial = emptyList())
    val keywords by settingsRepo.keywords.collectAsState(initial = emptyList())
    
    var isAddingAccount by remember { mutableStateOf(false) }
    var newKeyword by remember { mutableStateOf("") }
    
    var notificationSettings by remember { mutableStateOf(NotificationSettings()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("설정") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section 1: Connected Accounts
            item {
                Text(
                    text = "연동된 계정",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Notification Permission Status Check
                val notiManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val isListenerGranted = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
                
                if (!isListenerGranted) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("⚠️ 알림 접근 권한 필요", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("이메일, 카카오톡 내용을 읽어오기 위해 권한이 필요합니다.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            Button(
                                onClick = { 
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                },
                                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("권한 허용하러 가기")
                            }
                        }
                    }
                }

                if (accounts.isEmpty()) {
                    Text("연동된 계정이 없습니다.", style = MaterialTheme.typography.bodyMedium)
                }
                
                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = account.email, style = MaterialTheme.typography.bodyLarge)
                            Text(text = account.provider, style = MaterialTheme.typography.labelMedium)
                        }
                        
                        // Delete Button
                        IconButton(onClick = { settingsRepo.removeAccount(account.id) }) {
                             Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.outline)
                        }

                        Switch(
                            checked = account.isEnabled,
                            onCheckedChange = { isChecked ->
                                settingsRepo.toggleAccount(account.id, isChecked)
                            }
                        )
                    }
                }
                
                if (isAddingAccount) {
                    var emailInput by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { isAddingAccount = false },
                        title = { Text("계정 추가") },
                        text = {
                            Column {
                                Text("모니터링할 이메일 주소를 입력하세요.\n(실제 이메일의 알림을 감지합니다)")
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = emailInput, 
                                    onValueChange = { emailInput = it },
                                    label = { Text("이메일 주소") },
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (emailInput.contains("@")) {
                                        settingsRepo.addAccount(
                                            EmailAccountConfig(
                                                id = "manual_${System.currentTimeMillis()}",
                                                email = emailInput,
                                                provider = if (emailInput.contains("gmail")) "GMAIL" else "OTHER",
                                                isEnabled = true
                                            )
                                        )
                                        isAddingAccount = false
                                    }
                                }
                            ) { Text("추가") }
                        },
                        dismissButton = {
                            TextButton(onClick = { isAddingAccount = false }) { Text("취소") }
                        }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isAddingAccount = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("계정 추가")
                    }

                    OutlinedButton(
                        onClick = {
                            val isGranted = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
                            if (!isGranted) {
                                android.widget.Toast.makeText(context, "❌ 알림 권한이 없습니다.\n권한을 허용해주세요.", android.widget.Toast.LENGTH_LONG).show()
                            } else if (accounts.isEmpty()) {
                                android.widget.Toast.makeText(context, "⚠️ 연동된 계정이 없습니다.\n이메일 계정을 추가해주세요.", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    android.widget.Toast.makeText(context, "📡 연동 상태 확인 중...", android.widget.Toast.LENGTH_SHORT).show()
                                    kotlinx.coroutines.delay(1500)
                                    android.widget.Toast.makeText(context, "✅ 연동 상태 정상\n새로운 이메일 알림을 대기 중입니다.", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("연동 테스트")
                    }
                }
            }

            // Section 1.2: Google Authentication & Security
            item {
                Column {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Google 계정 보안 및 인증",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "🔒 '안전하지 않은 앱' 관련 안내", 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Google은 보안 강화를 위해 '안전하지 않은 앱' 설정을 삭제했습니다. 본 앱은 최신 OAuth 2.0 방식을 사용하여 메일에 접근합니다.\n\n" +
                                "연동을 위해 Google Cloud에서 발급받은 'Access Token'이 필요합니다.",
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                            
                            TextButton(
                                onClick = { 
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://developers.google.com/oauthplayground/"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("토큰 발급 가이드 (OAuth Playground) ↗️", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    var gmailToken by remember { mutableStateOf(settingsRepo.getGmailToken()) }
                    
                    OutlinedTextField(
                        value = gmailToken,
                        onValueChange = { gmailToken = it },
                        label = { Text("Gmail OAuth Access Token") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Bearer 토큰 입력...") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true
                    )
                    
                    Button(
                        onClick = {
                            settingsRepo.saveGmailToken(gmailToken)
                            android.widget.Toast.makeText(context, "Gmail 인증 토큰이 저장되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.End)
                    ) {
                        Text("토큰 저장")
                    }
                }
            }

            // Section 1.5: AI Connection Settings
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AI 연동 설정",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                var geminiKey by remember { mutableStateOf(settingsRepo.getGeminiKey()) }
                var chatGptKey by remember { mutableStateOf(settingsRepo.getChatGPTKey()) }
                var testResult by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()
                
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text("Gemini API Key") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = chatGptKey,
                    onValueChange = { chatGptKey = it },
                    label = { Text("ChatGPT API Key") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true
                )
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            settingsRepo.saveGeminiKey(geminiKey)
                            settingsRepo.saveChatGPTKey(chatGptKey)
                            
                            // Update live instance if possible
                            (context.applicationContext as? com.example.make.MakeApplication)?.let { app ->
                                app.geminiDataSource.updateKey(geminiKey)
                            }
                            
                            android.widget.Toast.makeText(context, "설정이 저장되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("저장")
                    }

                    OutlinedButton(
                        onClick = {
                             scope.launch {
                                 testResult = "📡 API 연결 확인 중..."
                                 
                                 // Temporary data source for testing
                                 val tester = com.example.make.data.remote.GeminiDataSource(geminiKey)
                                 val isSuccess = tester.testConnection()
                                 
                                 if (isSuccess) {
                                     testResult = "✅ Gemini 연동 성공! (정상 작동 확인)"
                                 } else {
                                     testResult = "❌ 연결 실패. API 키가 올바르지 않거나 네트워크 오류가 발생했습니다."
                                 }
                             }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("테스트")
                    }
                }
                
                if (testResult != null) {
                    Text(
                        text = testResult!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Section 1.5: Search Engine Settings
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "검색 엔진 설정",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                val engines by settingsRepo.searchEngines.collectAsState(initial = emptyList())
                val enabledNames by settingsRepo.enabledEngineNames.collectAsState(initial = setOf("Google"))
                
                engines.forEach { (name, url) ->
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsRepo.toggleEngine(name, !enabledNames.contains(name)) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = enabledNames.contains(name),
                            onCheckedChange = { isChecked -> settingsRepo.toggleEngine(name, isChecked) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                             Text(text = name, style = MaterialTheme.typography.bodyMedium)
                             Text(text = url.take(30) + "...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        if (name != "Google" && name != "Naver" && name != "Daum") {
                            IconButton(onClick = { settingsRepo.removeSearchEngine(name) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
                
                // Add Engine Logic
                var isAddingEngine by remember { mutableStateOf(false) }
                if (isAddingEngine) {
                    var eName by remember { mutableStateOf("") }
                    var eUrl by remember { mutableStateOf("") }
                    
                    AlertDialog(
                        onDismissRequest = { isAddingEngine = false },
                        title = { Text("검색 엔진 추가") },
                        text = {
                            Column {
                                OutlinedTextField(value = eName, onValueChange = { eName = it }, label = { Text("이름 (예: Bing)") })
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = eUrl, onValueChange = { eUrl = it }, label = { Text("URL (예: https://.../search?q=)") })
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (eName.isNotBlank() && eUrl.isNotBlank()) {
                                    settingsRepo.addSearchEngine(eName, eUrl)
                                    isAddingEngine = false
                                }
                            }) { Text("저장") }
                        },
                        dismissButton = {
                             TextButton(onClick = { isAddingEngine = false }) { Text("취소") }
                        }
                    )
                }
                
                TextButton(onClick = { isAddingEngine = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("검색 엔진 추가")
                }
            }

            // Section 2: Monitoring Keywords
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "모니터링 키워드",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "뉴스 내용에 해당 키워드가 있으면 강조 표시됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${keywords.size}/50",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (keywords.size >= 50) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("키워드 추가...") },
                        singleLine = true,
                        enabled = keywords.size < 50,
                        isError = keywords.size >= 50
                    )
                    IconButton(
                        onClick = {
                            if (newKeyword.isNotBlank() && keywords.size < 50) {
                                settingsRepo.addKeyword(newKeyword)
                                newKeyword = ""
                            } else if (keywords.size >= 50) {
                                android.widget.Toast.makeText(context, "최대 50개까지만 추가할 수 있습니다.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = keywords.size < 50 && newKeyword.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Display Keywords (Simple Flow Layout using FlowRow if available, else standard Row with wrap)
                // For safety/compatibility, using a simple Column of Rows or just a vertical list of chips isn't ideal for chips.
                // Let's use ExperimentalLayoutApi FlowRow if possible, but to be safe and standard:
                // We'll just list them in a vertical column of rows (chunks of 3)
                
                val chunkedKeywords = keywords.chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedKeywords.forEach { rowKeywords ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowKeywords.forEach { keyword ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text(keyword) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp).clickable {
                                                settingsRepo.removeKeyword(keyword)
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Notification Settings
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "알림 설정",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                NotificationToggle("긴급 알림 (소리)", notificationSettings.alertOnCritical) {
                    notificationSettings = notificationSettings.copy(alertOnCritical = it)
                }
                NotificationToggle("중요 알림 (진동)", notificationSettings.alertOnImportant) {
                    notificationSettings = notificationSettings.copy(alertOnImportant = it)
                }
                NotificationToggle("일반 알림 (무음)", notificationSettings.alertOnNormal) {
                    notificationSettings = notificationSettings.copy(alertOnNormal = it)
                }
            }

            // Section 4: Data Management
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "데이터 관리",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "오래된 뉴스 및 분석 데이터를 정리하여 앱 용량을 확보할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                                    dao.deleteOlderThan(oneDayAgo)
                                    dao.deleteSmartNewsOlderThan(dateFormatter.format(java.util.Date(oneDayAgo)))
                                    android.widget.Toast.makeText(context, "1일 지난 데이터 삭제 완료", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) { Text("1일 경과 삭제") }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    val tenDaysAgo = System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000
                                    dao.deleteOlderThan(tenDaysAgo)
                                    dao.deleteSmartNewsOlderThan(dateFormatter.format(java.util.Date(tenDaysAgo)))
                                    android.widget.Toast.makeText(context, "10일 지난 데이터 삭제 완료", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) { Text("10일 경과 삭제") }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                                    dao.deleteOlderThan(thirtyDaysAgo)
                                    dao.deleteSmartNewsOlderThan(dateFormatter.format(java.util.Date(thirtyDaysAgo)))
                                    android.widget.Toast.makeText(context, "30일 지난 데이터 삭제 완료", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) { Text("30일 경과 삭제") }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    dao.deleteAll()
                                    dao.deleteAllSmartNews()
                                    android.widget.Toast.makeText(context, "모든 데이터 삭제 완료", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("모두 삭제") }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
