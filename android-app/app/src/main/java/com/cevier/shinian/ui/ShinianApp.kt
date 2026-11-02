package com.cevier.shinian.ui

import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp
import com.cevier.shinian.data.AccessManager
import com.cevier.shinian.data.EncryptedNoteStore
import com.cevier.shinian.data.LlmApiClient
import com.cevier.shinian.data.LlmPrivateStore
import com.cevier.shinian.model.LlmConfig
import com.cevier.shinian.model.Note
import com.cevier.shinian.model.NoteType
import com.cevier.shinian.model.RecentConversation
import com.cevier.shinian.ui.theme.Blue
import kotlin.math.roundToInt
import kotlin.math.abs
import com.cevier.shinian.ui.theme.BlueSoft
import com.cevier.shinian.ui.theme.Ink
import com.cevier.shinian.ui.theme.Line
import com.cevier.shinian.ui.theme.Muted
import com.cevier.shinian.ui.theme.Orange
import com.cevier.shinian.ui.theme.OrangeSoft
import com.cevier.shinian.ui.theme.Paper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private enum class Screen {
    LOCK,
    PASSPHRASE,
    SECURITY_UPGRADE,
    SETUP,
    TIMELINE,
    VAULT,
    WEBSITE,
    LLM_SETTINGS,
    CHAT,
}

@Composable
fun ShinianApp(
    store: EncryptedNoteStore,
    requestBiometric: (onSuccess: () -> Unit, onUnavailable: () -> Unit) -> Unit,
    onHideRecentPreviewChanged: (Boolean) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.LOCK) }
    val notes = remember { mutableStateListOf<Note>().apply { addAll(store.load()) } }
    val accessManager = remember { AccessManager(context.applicationContext) }
    val llmStore = remember { LlmPrivateStore(context.applicationContext) }
    val llmApiClient = remember { LlmApiClient() }
    var llmConfig by remember { mutableStateOf(llmStore.loadConfig()) }
    var recentConversation by remember { mutableStateOf(llmStore.loadConversation()) }
    var llmSettingsReturnScreen by remember { mutableStateOf(Screen.VAULT) }
    val persistenceScope = rememberCoroutineScope()
    val persistenceMutex = remember { Mutex() }
    var initialized by remember { mutableStateOf(accessManager.hasLocalUsage(notes.isNotEmpty())) }
    var biometricEnabled by remember { mutableStateOf(accessManager.biometricEnabled) }
    var hideRecentPreview by remember { mutableStateOf(accessManager.hideRecentPreview) }
    var composerVisible by rememberSaveable { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var exportVisible by rememberSaveable { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        restoreUri = uri
    }

    fun openRecovery() {
        screen = if (accessManager.hasPassphrase) Screen.PASSPHRASE else Screen.SECURITY_UPGRADE
    }

    fun unlock() {
        if (biometricEnabled) {
            requestBiometric(
                { screen = Screen.TIMELINE },
                ::openRecovery,
            )
        } else {
            openRecovery()
        }
    }

    LaunchedEffect(initialized) {
        if (initialized && screen == Screen.LOCK) {
            unlock()
        }
    }

    fun persistNotes(snapshot: List<Note>, mediaToDelete: String? = null) {
        persistenceScope.launch {
            persistenceMutex.withLock {
                withContext(Dispatchers.IO) {
                    store.deleteMedia(mediaToDelete)
                    store.save(snapshot)
                }
            }
        }
    }

    fun deleteNote(note: Note) {
        notes.removeAll { it.id == note.id }
        persistNotes(notes.toList(), note.mediaPath)
        if (selectedNote?.id == note.id) selectedNote = null
    }

    fun updateNote(updated: Note) {
        val index = notes.indexOfFirst { it.id == updated.id }
        if (index < 0) return
        notes[index] = updated
        persistNotes(notes.toList())
        if (selectedNote?.id == updated.id) selectedNote = updated
    }

    BackHandler(enabled = screen != Screen.LOCK || composerVisible || selectedNote != null || exportVisible) {
        when {
            composerVisible -> composerVisible = false
            selectedNote != null -> selectedNote = null
            exportVisible -> exportVisible = false
            screen == Screen.SETUP -> screen = Screen.LOCK
            screen == Screen.PASSPHRASE -> screen = Screen.LOCK
            screen == Screen.SECURITY_UPGRADE -> screen = Screen.LOCK
            screen == Screen.VAULT -> screen = Screen.TIMELINE
            screen == Screen.WEBSITE -> screen = Screen.TIMELINE
            screen == Screen.LLM_SETTINGS -> screen = llmSettingsReturnScreen
            screen == Screen.CHAT -> screen = Screen.TIMELINE
            screen == Screen.TIMELINE -> onExit()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
        when (screen) {
            Screen.LOCK -> LockScreen(
                firstUse = !initialized,
                biometricEnabled = biometricEnabled,
                passphraseAvailable = accessManager.hasPassphrase,
                onUnlock = ::unlock,
                onPasscode = { screen = Screen.PASSPHRASE },
                onSecurityUpgrade = { screen = Screen.SECURITY_UPGRADE },
                onCreateVault = { screen = Screen.SETUP },
                onRestoreBackup = { restoreLauncher.launch(arrayOf("*/*")) },
            )

            Screen.PASSPHRASE -> PassphraseScreen(
                onBack = { screen = Screen.LOCK },
                onVerify = { passphrase ->
                    accessManager.verifyPassphrase(passphrase).also { verified ->
                        if (verified) screen = Screen.TIMELINE
                    }
                },
            )

            Screen.SECURITY_UPGRADE -> CreatePassphraseScreen(
                onBack = { screen = Screen.LOCK },
                onComplete = { passphrase ->
                    accessManager.completeSetup(passphrase, biometricEnabled)
                    initialized = true
                    screen = Screen.TIMELINE
                },
            )

            Screen.SETUP -> SetupScreen(
                onBack = { screen = Screen.LOCK },
                onComplete = { passphrase, enableBiometric ->
                    accessManager.completeSetup(passphrase, enableBiometric)
                    initialized = true
                    biometricEnabled = enableBiometric
                    screen = Screen.TIMELINE
                },
            )

            Screen.TIMELINE -> TimelineScreen(
                notes = notes,
                store = store,
                onCompose = { composerVisible = true },
                onOpen = { selectedNote = it },
                onDelete = ::deleteNote,
                onOpenVault = { screen = Screen.VAULT },
                onOpenChat = { screen = Screen.CHAT },
                vaultPreview = {
                    VaultScreen(
                        noteCount = notes.size,
                        biometricEnabled = biometricEnabled,
                        hideRecentPreview = hideRecentPreview,
                        llmConfig = llmConfig,
                        onBack = {},
                        onExport = {},
                        onOpenLlmSettings = {},
                        onBiometricChanged = {},
                        onHideRecentPreviewChanged = {},
                        timelinePreview = {},
                    )
                },
                chatPreview = {
                    TemporaryChatScreen(
                        config = llmConfig,
                        initialConversation = recentConversation,
                        store = llmStore,
                        apiClient = llmApiClient,
                        onBack = {},
                        onOpenSettings = {},
                        onConversationChanged = {},
                        onConfigChanged = {},
                        timelinePreview = {},
                    )
                },
            )

            Screen.VAULT -> VaultScreen(
                noteCount = notes.size,
                biometricEnabled = biometricEnabled,
                hideRecentPreview = hideRecentPreview,
                llmConfig = llmConfig,
                onBack = { screen = Screen.TIMELINE },
                onExport = { exportVisible = true },
                onOpenLlmSettings = {
                    llmSettingsReturnScreen = Screen.VAULT
                    screen = Screen.LLM_SETTINGS
                },
                onBiometricChanged = { enabled ->
                    biometricEnabled = enabled
                    accessManager.setBiometricEnabled(enabled)
                },
                onHideRecentPreviewChanged = { hidden ->
                    hideRecentPreview = hidden
                    accessManager.setHideRecentPreview(hidden)
                    onHideRecentPreviewChanged(hidden)
                },
                timelinePreview = {
                    TimelineScreen(
                        notes = notes,
                        store = store,
                        onCompose = {},
                        onOpen = {},
                        onDelete = {},
                        onOpenVault = {},
                        onOpenChat = {},
                        vaultPreview = {},
                        chatPreview = {},
                    )
                },
            )

            Screen.WEBSITE -> WebsiteScreen(onBack = { screen = Screen.TIMELINE })

            Screen.LLM_SETTINGS -> LlmSettingsScreen(
                initialConfig = llmConfig,
                apiClient = llmApiClient,
                onSaved = { config ->
                    llmConfig = config
                    persistenceScope.launch(Dispatchers.IO) { llmStore.saveConfig(config) }
                    screen = llmSettingsReturnScreen
                },
            )

            Screen.CHAT -> TemporaryChatScreen(
                config = llmConfig,
                initialConversation = recentConversation,
                store = llmStore,
                apiClient = llmApiClient,
                onBack = { screen = Screen.TIMELINE },
                onOpenSettings = {
                    llmSettingsReturnScreen = Screen.CHAT
                    screen = Screen.LLM_SETTINGS
                },
                onConversationChanged = { conversation ->
                    recentConversation = conversation
                    persistenceScope.launch(Dispatchers.IO) {
                        if (conversation.messages.isEmpty()) llmStore.clearConversation()
                        else llmStore.saveConversation(conversation)
                    }
                },
                onConfigChanged = { config ->
                    llmConfig = config
                    persistenceScope.launch(Dispatchers.IO) { llmStore.saveConfig(config) }
                },
                timelinePreview = {
                    TimelineScreen(
                        notes = notes,
                        store = store,
                        onCompose = {},
                        onOpen = {},
                        onDelete = {},
                        onOpenVault = {},
                        onOpenChat = {},
                        vaultPreview = {},
                        chatPreview = {},
                    )
                },
            )
        }
    }

    if (composerVisible) {
        ComposerSheet(
            store = store,
            onDismiss = { composerVisible = false },
            onSave = { note ->
                notes.add(0, note)
                composerVisible = false
                persistNotes(notes.toList())
            },
        )
    }

    selectedNote?.let { note ->
        NoteDetailSheet(
            note = note,
            store = store,
            onDismiss = { selectedNote = null },
            onUpdate = ::updateNote,
        )
    }

    if (exportVisible) {
        ExportSheet(
            notes = notes,
            store = store,
            verifyPassphrase = accessManager::verifyPassphrase,
            onDismiss = { exportVisible = false },
        )
    }

    restoreUri?.let { uri ->
        RestoreBackupSheet(
            uri = uri,
            store = store,
            onDismiss = { restoreUri = null },
            onRestored = { importedNotes, passphrase ->
                notes.clear()
                notes.addAll(importedNotes)
                accessManager.completeSetup(passphrase, biometricEnabled = true)
                initialized = true
                biometricEnabled = true
                restoreUri = null
                screen = Screen.TIMELINE
            },
        )
    }
}

@Composable
private fun LockScreen(
    firstUse: Boolean,
    biometricEnabled: Boolean,
    passphraseAvailable: Boolean,
    onUnlock: () -> Unit,
    onPasscode: () -> Unit,
    onSecurityUpgrade: () -> Unit,
    onCreateVault: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFFFAF6), Color(0xFFFBFCFF), Color(0xFFEEF5FF)),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Blue.copy(alpha = 0.13f),
                radius = size.minDimension * 0.58f,
                center = Offset(size.width * 1.07f, size.height * 0.05f),
                style = Stroke(width = 1.4f),
            )
            drawCircle(
                color = Orange.copy(alpha = 0.18f),
                radius = size.minDimension * 0.42f,
                center = Offset(-size.width * 0.08f, size.height * 0.95f),
                style = Stroke(width = 1.4f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .shadow(30.dp, RoundedCornerShape(28.dp, 28.dp, 14.dp, 28.dp))
                    .size(80.dp)
                    .clip(RoundedCornerShape(28.dp, 28.dp, 14.dp, 28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF4F89EF), Color(0xFF285FC9)))),
                contentAlignment = Alignment.Center,
            ) {
                Text("拾", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(25.dp))
            Text(
                "拾念 · 随手记录",
                color = Color(0xFFD96C20),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(18.dp))
            Text(if (firstUse) "第一次见面" else "欢迎回来", style = MaterialTheme.typography.displaySmall, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text(if (firstUse) "创建只属于你的本地记录空间" else "让每一个闪念都有迹可循", color = Muted, fontSize = 15.sp)
            Spacer(Modifier.weight(0.95f))

            if (firstUse) {
                Button(
                    onClick = onCreateVault,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(17.dp),
                ) {
                    Text("首次使用 · 创建资料库", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRestoreBackup,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp),
                    shape = RoundedCornerShape(17.dp),
                ) {
                    Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp))
                    Text("从备份恢复", modifier = Modifier.padding(start = 7.dp), fontWeight = FontWeight.SemiBold)
                }
                Text("所有记录都只保存在你的设备上", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            } else {
                if (biometricEnabled) {
                    Surface(
                        onClick = onUnlock,
                        modifier = Modifier
                            .size(88.dp)
                            .semantics { contentDescription = "使用生物识别解锁" },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.78f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue.copy(alpha = 0.42f)),
                        shadowElevation = 13.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Fingerprint, null, tint = Blue, modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("触摸以使用指纹解锁", color = Color(0xFF4F5F75), fontSize = 14.sp)
                }
                if (passphraseAvailable) {
                    TextButton(onClick = onPasscode) { Text("改用恢复口令") }
                } else {
                    TextButton(onClick = onSecurityUpgrade) { Text("为现有资料库设置恢复口令") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CreatePassphraseScreen(
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
) {
    var passphrase by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    val valid = passphrase.length >= 8 && passphrase == confirmation

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
        }
        Spacer(Modifier.weight(0.55f))
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(24.dp, 24.dp, 12.dp, 24.dp)).background(Color(0xFFFFF0D8)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Key, null, tint = Color(0xFFB67B26), modifier = Modifier.size(35.dp))
        }
        Text("完成安全升级", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 24.dp))
        Text("旧版本没有保存恢复口令。请为现有资料库设置一个口令，之后需要验证才能进入。", color = Muted, lineHeight = 21.sp, modifier = Modifier.padding(top = 7.dp, bottom = 22.dp))
        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("新恢复口令") },
            placeholder = { Text("至少输入 8 个字符") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = confirmation,
            onValueChange = { confirmation = it },
            label = { Text("再次确认") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = confirmation.isNotEmpty() && confirmation != passphrase,
            supportingText = { if (confirmation.isNotEmpty() && confirmation != passphrase) Text("两次输入不一致") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        Button(
            onClick = { onComplete(passphrase) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("保存口令并进入")
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PassphraseScreen(
    onBack: () -> Unit,
    onVerify: (String) -> Boolean,
) {
    var passphrase by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
        }
        Spacer(Modifier.weight(0.7f))
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(24.dp, 24.dp, 12.dp, 24.dp)).background(Color(0xFFFFF0D8)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Key, null, tint = Color(0xFFB67B26), modifier = Modifier.size(35.dp))
        }
        Text("使用恢复口令", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 24.dp))
        Text("输入创建资料库时设置的口令", color = Muted, modifier = Modifier.padding(top = 7.dp, bottom = 22.dp))
        OutlinedTextField(
            value = passphrase,
            onValueChange = {
                passphrase = it
                invalid = false
            },
            label = { Text("恢复口令") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = invalid,
            supportingText = { if (invalid) Text("恢复口令不正确") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        Button(
            onClick = { invalid = !onVerify(passphrase) },
            enabled = passphrase.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("验证并进入")
        }
        Spacer(Modifier.weight(1f))
        Text("口令仅在本机校验，不会发送到网络", color = Muted, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun SetupScreen(onBack: () -> Unit, onComplete: (String, Boolean) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var passphrase by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var biometricEnabled by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { if (step == 1) onBack() else step -= 1 }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Text(
                "创建资料库",
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
            Text("$step / 2", color = Muted, fontSize = 12.sp, modifier = Modifier.width(48.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 18.dp)) {
            repeat(2) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (index < step) Blue else Color(0xFFE2DDE5)),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            when (step) {
                1 -> SetupPassphraseStep(
                    passphrase = passphrase,
                    confirmation = confirmation,
                    onPassphrase = { passphrase = it },
                    onConfirmation = { confirmation = it },
                )

                2 -> SetupBiometricStep(
                    enabled = biometricEnabled,
                    onToggle = { biometricEnabled = !biometricEnabled },
                )
            }
        }

        Button(
            onClick = { if (step < 2) step += 1 else onComplete(passphrase, biometricEnabled) },
            enabled = step != 1 || (passphrase.length >= 8 && passphrase == confirmation),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (step == 2) "创建并进入拾念" else "继续")
        }
    }
}

@Composable
private fun SetupPassphraseStep(
    passphrase: String,
    confirmation: String,
    onPassphrase: (String) -> Unit,
    onConfirmation: (String) -> Unit,
) {
    SetupHeading(
        icon = { Icon(Icons.Rounded.Key, null, tint = Color(0xFFB67B26), modifier = Modifier.size(36.dp)) },
        iconBackground = Color(0xFFFFF0D8),
        eyebrow = "第一步 · 恢复方式",
        title = "设置恢复口令",
        description = "重新安装 App 或更换设备时，需要用它打开导出的加密资料库。",
    )
    OutlinedTextField(
        value = passphrase,
        onValueChange = onPassphrase,
        label = { Text("恢复口令") },
        placeholder = { Text("至少输入 8 个字符") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = confirmation,
        onValueChange = onConfirmation,
        label = { Text("再次确认") },
        placeholder = { Text("再次输入恢复口令") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        isError = confirmation.isNotEmpty() && confirmation != passphrase,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    )
    InfoNote("拾念无法替你找回恢复口令，请把它保存在安全且容易找到的地方。", warning = true)
}

@Composable
private fun SetupBiometricStep(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    SetupHeading(
        icon = { Icon(Icons.Rounded.Fingerprint, null, tint = Color(0xFF4D8A5E), modifier = Modifier.size(38.dp)) },
        iconBackground = Color(0xFFE3F2DF),
        eyebrow = "第二步 · 快速访问",
        title = "启用指纹解锁",
        description = "以后每次启动拾念，验证指纹后即可进入你的时间线。",
    )
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(15.dp)) {
            Icon(Icons.Rounded.Fingerprint, null, tint = Color(0xFF4D8A5E), modifier = Modifier.size(34.dp))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("启动时验证指纹", fontWeight = FontWeight.SemiBold)
                Text("使用 Android 系统生物识别", color = Muted, fontSize = 12.sp)
            }
            MiniToggle(checked = enabled)
        }
    }
    Spacer(Modifier.height(15.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        SummaryRow(Icons.Rounded.FolderOpen, "存储位置", "App 私有加密目录")
        HorizontalDivider(color = Line)
        SummaryRow(Icons.Rounded.Key, "恢复方式", "恢复口令已设置")
    }
}

@Composable
private fun SetupHeading(
    icon: @Composable () -> Unit,
    iconBackground: Color,
    eyebrow: String,
    title: String,
    description: String,
) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(RoundedCornerShape(28.dp, 28.dp, 14.dp, 28.dp))
            .background(iconBackground),
        contentAlignment = Alignment.Center,
    ) { icon() }
    Spacer(Modifier.height(22.dp))
    Text(eyebrow, color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(6.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(description, color = Muted, lineHeight = 22.sp, modifier = Modifier.padding(bottom = 22.dp))
}

@Composable
private fun InfoNote(text: String, warning: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 15.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (warning) Color(0xFFFFF1DF) else Color(0xFFF0EDF3))
            .padding(13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(21.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (warning) "!" else "i", color = if (warning) Color(0xFFBD7924) else Blue, fontWeight = FontWeight.Bold)
        }
        Text(text, modifier = Modifier.padding(start = 9.dp), color = Color(0xFF746C79), fontSize = 12.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun TimelineScreen(
    notes: List<Note>,
    store: EncryptedNoteStore,
    onCompose: () -> Unit,
    onOpen: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onOpenVault: () -> Unit,
    onOpenChat: () -> Unit,
    vaultPreview: @Composable () -> Unit,
    chatPreview: @Composable () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("全部") }
    var query by rememberSaveable { mutableStateOf("") }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var revealedNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var revealedCardBounds by remember { mutableStateOf<Rect?>(null) }
    var timelineOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    var timelineSizePx by remember { mutableStateOf(IntSize.Zero) }
    val cardBoundsById = remember { mutableMapOf<Long, Rect>() }

    val visible = notes.filter { note ->
        val typeMatches = when (filter) {
            "文字" -> note.type == NoteType.TEXT
            "图片" -> note.type == NoteType.IMAGE
            "音频" -> note.type == NoteType.AUDIO
            else -> true
        }
        typeMatches && (query.isBlank() || "${note.title}${note.body}".contains(query, ignoreCase = true))
    }
    LaunchedEffect(visible, revealedNoteId) {
        if (revealedNoteId != null && visible.none { it.id == revealedNoteId }) {
            revealedNoteId = null
            revealedCardBounds = null
        }
    }
    val listState = rememberLazyListState()
    var chatPullDistance by remember { mutableFloatStateOf(0f) }
    val chatPullThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val currentOnOpenChat by rememberUpdatedState(onOpenChat)
    val currentOnOpenVault by rememberUpdatedState(onOpenVault)
    var securitySwipeDistance by remember { mutableFloatStateOf(0f) }
    val securitySwipeThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    val navigationGestureScope = rememberCoroutineScope()
    val chatPullConnection = remember(listState, chatPullThreshold) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y >= 0f || chatPullDistance <= 0f) {
                    return Offset.Zero
                }
                val consumedY = maxOf(available.y, -chatPullDistance / 0.55f)
                chatPullDistance = (chatPullDistance + consumedY * 0.55f).coerceAtLeast(0f)
                return Offset(0f, consumedY)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && !listState.canScrollBackward && available.y > 0f) {
                    chatPullDistance = (chatPullDistance + available.y * 0.55f)
                        .coerceAtMost(maxOf(timelineSizePx.height.toFloat(), chatPullThreshold * 1.25f))
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (chatPullDistance >= chatPullThreshold) {
                    animate(
                        initialValue = chatPullDistance,
                        targetValue = maxOf(timelineSizePx.height.toFloat(), chatPullThreshold),
                        animationSpec = tween(180),
                    ) { value, _ -> chatPullDistance = value }
                    currentOnOpenChat()
                } else if (chatPullDistance > 0f) {
                    animate(chatPullDistance, 0f) { value, _ -> chatPullDistance = value }
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .onGloballyPositioned {
                timelineOriginInWindow = it.positionInWindow()
                timelineSizePx = it.size
            }
            .pointerInput(revealedNoteId, revealedCardBounds, timelineOriginInWindow) {
                if (revealedNoteId != null) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val downInWindow = down.position + timelineOriginInWindow
                        val tappedOutsideRevealedCard = revealedCardBounds?.contains(downInWindow) != true

                        if (tappedOutsideRevealedCard) {
                            down.consume()
                            var pointerPressed = true
                            while (pointerPressed) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                change.consume()
                                pointerPressed = change.pressed
                            }
                            revealedNoteId = null
                            revealedCardBounds = null
                        }
                    }
                }
            }
            .pointerInput(securitySwipeThreshold, revealedNoteId) {
                if (revealedNoteId == null) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val start = down.position
                        var trackingRightSwipe = false
                        var rejected = false
                        var pointerPressed = true

                        while (pointerPressed) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val deltaX = change.position.x - start.x
                            val deltaY = change.position.y - start.y

                            if (!trackingRightSwipe && !rejected && maxOf(abs(deltaX), abs(deltaY)) > viewConfiguration.touchSlop) {
                                trackingRightSwipe = deltaX > 0f && abs(deltaX) > abs(deltaY) * 1.2f
                                rejected = !trackingRightSwipe
                            }

                            if (trackingRightSwipe) {
                                change.consume()
                                securitySwipeDistance = deltaX.coerceIn(
                                    0f,
                                    maxOf(timelineSizePx.width.toFloat(), securitySwipeThreshold * 1.35f),
                                )
                            }
                            pointerPressed = change.pressed
                        }

                        if (trackingRightSwipe && securitySwipeDistance >= securitySwipeThreshold) {
                            navigationGestureScope.launch {
                                animate(
                                    initialValue = securitySwipeDistance,
                                    targetValue = maxOf(timelineSizePx.width.toFloat(), securitySwipeThreshold),
                                    animationSpec = tween(180),
                                ) { value, _ -> securitySwipeDistance = value }
                                currentOnOpenVault()
                            }
                        } else if (securitySwipeDistance > 0f) {
                            navigationGestureScope.launch {
                                animate(securitySwipeDistance, 0f) { value, _ -> securitySwipeDistance = value }
                            }
                        }
                    }
                }
            },
    ) {
        val pageTransitionProgress = maxOf(
            securitySwipeDistance / timelineSizePx.width.coerceAtLeast(1),
            chatPullDistance / timelineSizePx.height.coerceAtLeast(1),
        ).coerceIn(0f, 1f)
        SwipePagePreview(
            page = SwipePreviewPage.VAULT,
            edge = SwipePreviewEdge.LEFT,
            progress = securitySwipeDistance / timelineSizePx.width.coerceAtLeast(1),
            modifier = Modifier.alpha(if (securitySwipeDistance > 0f) 1f else 0f),
            content = vaultPreview,
        )
        SwipePagePreview(
            page = SwipePreviewPage.CHAT,
            edge = SwipePreviewEdge.TOP,
            progress = chatPullDistance / timelineSizePx.height.coerceAtLeast(1),
            modifier = Modifier.alpha(if (chatPullDistance > 0f) 1f else 0f),
            content = chatPreview,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(chatPullConnection)
                .offset {
                    IntOffset(
                        securitySwipeDistance.roundToInt(),
                        chatPullDistance.roundToInt(),
                    )
                }
                .background(Paper)
                .alpha(swipeSourceAlpha(pageTransitionProgress)),
            contentPadding = PaddingValues(start = 19.dp, end = 19.dp, top = 12.dp, bottom = 104.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        listOf("全部", "文字", "图片", "音频").forEach { item ->
                            FilterPill(item, selected = filter == item) { filter = item }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) query = ""
                        },
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (searchVisible) Blue else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (searchVisible) Blue else Line),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Search, "搜索", tint = if (searchVisible) Color.White else Ink)
                        }
                    }
                }

                AnimatedVisibility(visible = searchVisible) {
                    Column(modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            placeholder = { Text("搜索记录内容") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                            ),
                        )
                        Text(
                            if (query.isBlank()) "搜索只在本机完成" else "找到 ${visible.size} 条记录",
                            color = Muted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                        )
                    }
                }
            }

            visible.groupBy { it.recordedDate ?: it.day }.forEach { (date, dayNotes) ->
                if (dayNotes.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(formatSectionDate(date), fontWeight = FontWeight.Bold)
                            Text("${dayNotes.size} 条记录", color = Muted, fontSize = 12.sp)
                        }
                    }
                    items(dayNotes, key = { it.id }) { note ->
                        TimelineCard(
                            note = note,
                            store = store,
                            revealed = revealedNoteId == note.id,
                            onRevealChanged = { isRevealed ->
                                if (isRevealed) {
                                    revealedNoteId = note.id
                                    revealedCardBounds = cardBoundsById[note.id]
                                } else if (revealedNoteId == note.id) {
                                    revealedNoteId = null
                                    revealedCardBounds = null
                                }
                            },
                            onBoundsChanged = { bounds ->
                                cardBoundsById[note.id] = bounds
                                if (revealedNoteId == note.id) revealedCardBounds = bounds
                            },
                            onClick = { onOpen(note) },
                            onDelete = {
                                if (revealedNoteId == note.id) {
                                    revealedNoteId = null
                                    revealedCardBounds = null
                                }
                                cardBoundsById.remove(note.id)
                                onDelete(note)
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 70.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(21.dp, 21.dp, 10.dp, 21.dp)).background(BlueSoft),
                            contentAlignment = Alignment.Center,
                        ) { Icon(if (notes.isEmpty()) Icons.Rounded.Add else Icons.Rounded.Search, null, tint = Blue, modifier = Modifier.size(30.dp)) }
                        Spacer(Modifier.height(13.dp))
                        Text(if (notes.isEmpty()) "还没有记录" else "没有找到相关记录", fontWeight = FontWeight.Bold)
                        Text(if (notes.isEmpty()) "文字、照片和声音，都可以从这里开始" else "换一个关键词，或调整上方分类", color = Muted, fontSize = 12.sp)
                        if (notes.isEmpty()) {
                            Button(onClick = onCompose, modifier = Modifier.padding(top = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                                Text("写下第一条记录")
                            }
                        }
                    }
                }
            }
        }

        val composeButtonShape = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = 6.dp,
            bottomStart = 18.dp,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset {
                    IntOffset(
                        securitySwipeDistance.roundToInt(),
                        chatPullDistance.roundToInt(),
                    )
                }
                .navigationBarsPadding()
                .padding(20.dp)
                .size(56.dp)
                .alpha(swipeSourceAlpha(pageTransitionProgress))
                .clip(composeButtonShape)
                .background(Color(0xFFFF9966))
                .clickable(onClick = onCompose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "新建记录", tint = Color.White)
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) Ink else Color.Transparent,
        contentColor = if (selected) Color.White else Muted,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp), fontSize = 12.sp)
    }
}

@Composable
private fun TimelineCard(
    note: Note,
    store: EncryptedNoteStore,
    revealed: Boolean,
    onRevealChanged: (Boolean) -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirming by rememberSaveable(note.id) { mutableStateOf(false) }
    val maxRevealPx = with(LocalDensity.current) { 104.dp.toPx() }
    var dragOffsetPx by rememberSaveable(note.id) { mutableFloatStateOf(0f) }
    val gestureScope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val revealProgress = (-dragOffsetPx / maxRevealPx).coerceIn(0f, 1f)
    val endCorner = (19f * (1f - revealProgress)).dp

    fun settleCard(targetOffsetPx: Float) {
        settleJob?.cancel()
        val startOffsetPx = dragOffsetPx
        settleJob = gestureScope.launch {
            animate(startOffsetPx, targetOffsetPx) { value, _ ->
                dragOffsetPx = value
            }
        }
    }

    LaunchedEffect(revealed) {
        if (!revealed && dragOffsetPx < 0f) {
            confirming = false
            settleCard(0f)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { onBoundsChanged(it.boundsInWindow()) },
        verticalAlignment = Alignment.Top,
    ) {
        Text(note.time, color = Color(0xFF99919F), fontSize = 10.sp, modifier = Modifier.width(43.dp).padding(top = 17.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(19.dp, 19.dp, 19.dp, 8.dp)),
        ) {
            Box(modifier = Modifier.matchParentSize()) {
                Surface(
                    onClick = {
                        if (confirming) onDelete() else confirming = true
                    },
                    modifier = Modifier.align(Alignment.CenterEnd).width(104.dp).fillMaxHeight(),
                    color = if (confirming) Color(0xFFC73E32) else Color(0xFFE96A5B),
                    contentColor = Color.White,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 18.dp),
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(20.dp))
                        Text(if (confirming) "确认删除" else "删除", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            ElevatedCard(
                onClick = {
                    if (revealed) {
                        onRevealChanged(false)
                        confirming = false
                        settleCard(0f)
                    } else {
                        onClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
                    .pointerInput(note.id, maxRevealPx, revealed) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                settleJob?.cancel()
                            },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                dragOffsetPx = (dragOffsetPx + amount).coerceIn(-maxRevealPx, 0f)
                            },
                            onDragEnd = {
                                val shouldReveal = dragOffsetPx <= -maxRevealPx * 0.35f
                                onRevealChanged(shouldReveal)
                                if (!shouldReveal) {
                                    confirming = false
                                }
                                settleCard(if (shouldReveal) -maxRevealPx else 0f)
                            },
                            onDragCancel = {
                                settleCard(if (revealed) -maxRevealPx else 0f)
                            },
                        )
                    },
                shape = RoundedCornerShape(topStart = 19.dp, topEnd = endCorner, bottomEnd = endCorner, bottomStart = 8.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(Color(note.accent)))
                    Column(modifier = Modifier.padding(14.dp).weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                note.title,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (note.type == NoteType.AUDIO) {
                                Box(modifier = Modifier.alpha(1f - revealProgress)) {
                                    TypeChip(note)
                                }
                            }
                        }
                        when (note.type) {
                            NoteType.AUDIO -> AudioCardWaveform(
                                note = note,
                                store = store,
                                modifier = Modifier.padding(top = 13.dp),
                            )
                            NoteType.IMAGE -> EncryptedImage(
                                store = store,
                                path = note.mediaPath,
                                modifier = Modifier.fillMaxWidth().height(192.dp).padding(top = 12.dp).clip(RoundedCornerShape(12.dp)),
                            )
                            NoteType.TEXT -> Unit
                        }
                        Text(note.body, color = Color(0xFF68616E), fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 9.dp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioCardWaveform(
    note: Note,
    store: EncryptedNoteStore,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var player by remember(note.id) { mutableStateOf<MediaPlayer?>(null) }
    var tempFile by remember(note.id) { mutableStateOf<File?>(null) }
    var playing by remember(note.id) { mutableStateOf(false) }
    var loading by remember(note.id) { mutableStateOf(false) }

    DisposableEffect(note.id, note.mediaPath) {
        onDispose {
            player?.release()
            tempFile?.delete()
        }
    }

    fun togglePlayback() {
        if (loading) return
        val current = player
        if (current != null) {
            runCatching {
                if (current.isPlaying) {
                    current.pause()
                    playing = false
                } else {
                    current.start()
                    playing = true
                }
            }.onFailure {
                Toast.makeText(context, "无法播放录音", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val mediaPath = note.mediaPath ?: return
        loading = true
        scope.launch {
            var preparedFile: File? = null
            var preparedPlayer: MediaPlayer? = null
            runCatching {
                preparedFile = withContext(Dispatchers.IO) {
                    store.createPlaybackFile(mediaPath, "m4a")
                }
                val readyPlayer = withContext(Dispatchers.IO) {
                    MediaPlayer().apply {
                        setDataSource(requireNotNull(preparedFile).absolutePath)
                        prepare()
                    }
                }
                preparedPlayer = readyPlayer
                readyPlayer.setOnCompletionListener {
                    playing = false
                    runCatching { it.seekTo(0) }
                }
                tempFile = preparedFile
                player = readyPlayer
                readyPlayer.start()
                playing = true
            }.onFailure {
                preparedPlayer?.release()
                preparedFile?.delete()
                Toast.makeText(context, "无法播放录音", Toast.LENGTH_SHORT).show()
            }
            loading = false
        }
    }

    Waveform(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = ::togglePlayback)
            .semantics {
                contentDescription = when {
                    loading -> "正在准备录音"
                    playing -> "暂停录音"
                    else -> "播放录音"
                }
            }
            .padding(horizontal = 4.dp),
        compact = true,
        active = playing,
        loading = loading,
    )
}

private fun formatSectionDate(value: String): String {
    val date = runCatching { LocalDate.parse(value) }.getOrNull() ?: return value
    return when (date) {
        LocalDate.now() -> "今天"
        LocalDate.now().minusDays(1) -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.SIMPLIFIED_CHINESE))
    }
}

@Composable
private fun TypeChip(note: Note) {
    val (icon, label) = when (note.type) {
        NoteType.TEXT -> Icons.AutoMirrored.Rounded.TextSnippet to "文本"
        NoteType.IMAGE -> Icons.Rounded.Image to "图片"
        NoteType.AUDIO -> Icons.Rounded.KeyboardVoice to (note.duration ?: "音频")
    }
    Surface(shape = CircleShape, color = Color(0xFFF2EFF4)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)) {
            Icon(icon, null, modifier = Modifier.size(13.dp), tint = Color(0xFF776E7E))
            Text(label, fontSize = 10.sp, color = Color(0xFF776E7E), modifier = Modifier.padding(start = 3.dp))
        }
    }
}

@Composable
internal fun Waveform(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    active: Boolean = false,
    loading: Boolean = false,
) {
    val values = listOf(7, 11, 16, 9, 19, 13, 7, 17, 12, 15, 8, 13, 16, 9, 6)
    Row(
        modifier = modifier.height(if (compact) 24.dp else 62.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp),
    ) {
        values.forEachIndexed { index, height ->
            Box(
                Modifier
                    .width(if (compact) 2.dp else 3.dp)
                    .height((if (compact) height else height * 2).dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            loading -> Blue.copy(alpha = 0.35f + (index % 3) * 0.16f)
                            active && index % 2 == 0 -> Orange
                            else -> Blue
                        },
                    ),
            )
        }
    }
}

@Composable
private fun PhotoStrip(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .weight(1f)
                .height(76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF788694), Color(0xFFE5B97D), Color(0xFF576A68)))),
        )
        Box(
            Modifier
                .weight(1f)
                .height(76.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFD7C3A5), Color(0xFFF2E5CC), Color(0xFF6A7B70)))),
        )
    }
}

@Composable
internal fun MiniToggle(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(22.dp)
            .clip(CircleShape)
            .background(if (checked) Blue else Color(0xFFCCC6D0))
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun SummaryRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
        Icon(icon, null, tint = Blue)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 11.dp)) {
            Text(label, color = Muted, fontSize = 11.sp)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Icon(Icons.Rounded.Check, null, tint = Color(0xFF4D8A5E), modifier = Modifier.size(18.dp))
    }
}
