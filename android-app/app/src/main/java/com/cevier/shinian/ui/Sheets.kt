package com.cevier.shinian.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cevier.shinian.data.EncryptedNoteStore
import com.cevier.shinian.model.Note
import com.cevier.shinian.model.NoteType
import com.cevier.shinian.model.randomNoteAccent
import com.cevier.shinian.ui.theme.Blue
import com.cevier.shinian.ui.theme.BlueSoft
import com.cevier.shinian.ui.theme.Ink
import com.cevier.shinian.ui.theme.Line
import com.cevier.shinian.ui.theme.Muted
import com.cevier.shinian.ui.theme.Orange
import com.cevier.shinian.ui.theme.OrangeSoft
import com.cevier.shinian.ui.theme.Paper
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrototypeComposerSheet(
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by rememberSaveable { mutableStateOf(NoteType.TEXT) }
    var text by rememberSaveable { mutableStateOf("") }
    var recording by rememberSaveable { mutableStateOf(false) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(recording) {
        while (recording) {
            delay(1000)
            seconds += 1
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { SheetHandle() },
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            SheetHeader(kicker = "新建记录", title = "捕捉这一刻", onClose = onDismiss)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 18.dp)) {
                CaptureTab("文字", Icons.AutoMirrored.Rounded.TextSnippet, mode == NoteType.TEXT, Modifier.weight(1f)) { mode = NoteType.TEXT }
                CaptureTab("图片", Icons.Rounded.Image, mode == NoteType.IMAGE, Modifier.weight(1f)) { mode = NoteType.IMAGE }
                CaptureTab("音频", Icons.Rounded.KeyboardVoice, mode == NoteType.AUDIO, Modifier.weight(1f)) { mode = NoteType.AUDIO }
            }

            when (mode) {
                NoteType.TEXT -> TextCapture(text = text, onTextChange = { text = it })
                NoteType.IMAGE -> ImageCapture(text = text, onTextChange = { text = it })
                NoteType.AUDIO -> AudioCapture(
                    recording = recording,
                    seconds = seconds,
                    onToggleRecording = { recording = !recording },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Color(0xFF5F976F), modifier = Modifier.size(16.dp))
                Text("已加密", color = Color(0xFF5F976F), fontSize = 11.sp, modifier = Modifier.padding(start = 5.dp).weight(1f))
                Button(
                    onClick = {
                        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                        val fallback = when (mode) {
                            NoteType.TEXT -> "未命名记录"
                            NoteType.IMAGE -> "这一刻的画面"
                            NoteType.AUDIO -> "刚刚录下的想法"
                        }
                        val clean = text.trim()
                        onSave(
                            Note(
                                id = System.currentTimeMillis(),
                                day = "今天",
                                time = now,
                                type = mode,
                                duration = if (mode == NoteType.AUDIO) formatDuration(seconds.coerceAtLeast(1)) else null,
                                title = clean.lineSequence().firstOrNull()?.take(18)?.ifBlank { fallback } ?: fallback,
                                body = clean.ifBlank {
                                    when (mode) {
                                        NoteType.TEXT -> "刚刚记录下来的一个念头。"
                                        NoteType.IMAGE -> "为这张画面留下当时的感受。"
                                        NoteType.AUDIO -> "声音已经安全保存在本机。"
                                    }
                                },
                                accent = randomNoteAccent(),
                            ),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Text("保存记录")
                }
            }
        }
    }
}

@Composable
private fun CaptureTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = if (selected) Blue else Color(0xFFEEF2F8),
        contentColor = if (selected) Color.White else Ink,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, modifier = Modifier.size(22.dp))
            Text(label, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun TextCapture(text: String, onTextChange: (String) -> Unit) {
    Column {
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("写下刚刚闪过的想法…") },
            modifier = Modifier.fillMaxWidth().height(230.dp),
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F2F5),
                unfocusedContainerColor = Color(0xFFF5F2F5),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
            Text("今天 ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("${text.length} 字", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ImageCapture(text: String, onTextChange: (String) -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF9FA8B0), Color(0xFFE9C389), Color(0xFF687A6D), Color(0xFF394D48)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("示例取景画面", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.PhotoLibrary, null, tint = Blue)
            Text("从相册选择", fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
            Spacer(Modifier.width(38.dp))
            Box(Modifier.size(48.dp).clip(CircleShape).background(Blue).padding(7.dp).clip(CircleShape).background(Color.White))
        }
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("补充当时的想法…") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(17.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F2F5),
                unfocusedContainerColor = Color(0xFFF5F2F5),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun AudioCapture(recording: Boolean, seconds: Int, onToggleRecording: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().height(290.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (recording) "正在聆听…" else "用声音保存还没成形的想法", color = Muted, fontSize = 13.sp)
        Text(formatDuration(seconds), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
        Waveform(modifier = Modifier.padding(vertical = 23.dp), compact = false)
        Surface(
            onClick = onToggleRecording,
            modifier = Modifier.size(66.dp),
            shape = CircleShape,
            color = OrangeSoft,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (recording) {
                    Icon(Icons.Rounded.Stop, "停止录音", tint = Color(0xFFE75E32), modifier = Modifier.size(34.dp))
                } else {
                    Icon(Icons.Rounded.KeyboardVoice, "开始录音", tint = Orange, modifier = Modifier.size(34.dp))
                }
            }
        }
        Text(if (recording) "点击停止录音" else "点击开始 · 音频只保存在本机", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrototypeNoteDetailSheet(note: Note, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { SheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("收起") }
                Text("${note.day} ${note.time}", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                TextButton(onClick = {}) { Text("•••") }
            }
            Surface(shape = CircleShape, color = BlueSoft, contentColor = Blue) {
                Text(
                    when (note.type) {
                        NoteType.TEXT -> "文字记录"
                        NoteType.IMAGE -> "图片记录"
                        NoteType.AUDIO -> "音频记录 · ${note.duration}"
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(note.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 18.dp))
            Text(note.body, color = Color(0xFF4F5B6D), lineHeight = 25.sp, modifier = Modifier.padding(top = 12.dp))

            if (note.type == NoteType.AUDIO) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF3FB)),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(13.dp)) {
                        Surface(shape = CircleShape, color = Blue, modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PlayArrow, "播放", tint = Color.White)
                            }
                        }
                        Waveform(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), compact = true)
                        Text(note.duration ?: "00:00", color = Muted, fontSize = 11.sp)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            ) {
                Row(
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(BlueSoft, OrangeSoft)))
                        .padding(15.dp),
                ) {
                    Text("✦", color = Orange, fontSize = 20.sp)
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text("AI 整理 · 未来功能", color = Blue, fontSize = 11.sp)
                        Text("以后可在这里生成摘要、主题与关联记录", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                Text("编辑这条记录")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExportSheet(
    notes: List<Note>,
    store: EncryptedNoteStore,
    verifyPassphrase: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var format by rememberSaveable { mutableStateOf("加密备份") }
    var passphrase by remember { mutableStateOf("") }
    var passphraseError by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var pendingFormat by remember { mutableStateOf<String?>(null) }
    var pendingPassphrase by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri == null) {
            pendingFormat = null
            pendingPassphrase = null
        } else {
            val selectedFormat = pendingFormat ?: format
            val selectedPassphrase = pendingPassphrase
            exporting = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { store.export(context, uri, selectedFormat, notes, selectedPassphrase) }
                }
                exporting = false
                result
                    .onSuccess {
                        passphrase = ""
                        Toast.makeText(context, "$selectedFormat 已创建", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                    .onFailure { Toast.makeText(context, "导出失败：${it.message}", Toast.LENGTH_LONG).show() }
                pendingFormat = null
                pendingPassphrase = null
            }
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!exporting) onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { SheetHandle() },
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 30.dp)) {
            SheetHeader(kicker = "数据导出", title = "带走你的全部记录", onClose = { if (!exporting) onDismiss() })
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 18.dp)) {
                listOf(
                    "加密备份" to "使用恢复口令加密，可在新设备完整恢复",
                    "PDF 文档" to "适合阅读和打印",
                    "通用 JSON" to "适合迁移至其他工具",
                ).forEach { (label, description) ->
                    Card(
                        onClick = { format = label },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (format == label) Color(0xFFF3F7FF) else Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (format == label) Blue else Line),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                            RadioButton(selected = format == label, onClick = { format = label })
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(label, fontWeight = FontWeight.SemiBold)
                                Text(description, color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            if (format == "加密备份") {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = {
                        passphrase = it
                        passphraseError = null
                    },
                    label = { Text("当前恢复口令") },
                    supportingText = {
                        Text(passphraseError ?: "新手机导入时需要输入同一个恢复口令")
                    },
                    isError = passphraseError != null,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !exporting,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(15.dp),
                )
            }
            HorizontalDivider(color = Line)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                Text("导出范围", color = Muted, modifier = Modifier.weight(1f))
                Text("全部 ${notes.size} 条记录", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = {
                    if (format == "加密备份" && !verifyPassphrase(passphrase)) {
                        passphraseError = "恢复口令不正确"
                        return@Button
                    }
                    val fileName = when (format) {
                        "加密备份" -> "拾念备份.shinian"
                        "PDF 文档" -> "拾念记录.pdf"
                        else -> "拾念记录.json"
                    }
                    pendingFormat = format
                    pendingPassphrase = passphrase.takeIf { format == "加密备份" }
                    launcher.launch(fileName)
                },
                enabled = !exporting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
            ) {
                if (exporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("创建$format")
                }
            }
            Text(
                "导出过程完全在本机完成，不会上传任何内容。",
                color = Muted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RestoreBackupSheet(
    uri: android.net.Uri,
    store: EncryptedNoteStore,
    onDismiss: () -> Unit,
    onRestored: (notes: List<Note>, passphrase: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var passphrase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var restoring by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { if (!restoring) onDismiss() },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { SheetHandle() },
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 30.dp)) {
            SheetHeader(kicker = "迁移到新设备", title = "从加密备份恢复", onClose = { if (!restoring) onDismiss() })
            Text(
                "请输入旧手机导出备份时使用的恢复口令。验证成功后，记录与图片、音频会使用这台手机的新密钥重新加密。",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
            OutlinedTextField(
                value = passphrase,
                onValueChange = {
                    passphrase = it
                    errorMessage = null
                },
                label = { Text("恢复口令") },
                supportingText = { errorMessage?.let { Text(it) } },
                isError = errorMessage != null,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !restoring,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                shape = RoundedCornerShape(15.dp),
            )
            Button(
                onClick = {
                    if (passphrase.length < 8) {
                        errorMessage = "恢复口令至少需要 8 个字符"
                        return@Button
                    }
                    val enteredPassphrase = passphrase
                    restoring = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { store.importPortableBackup(context, uri, enteredPassphrase) }
                        }
                        restoring = false
                        result
                            .onSuccess { importedNotes ->
                                passphrase = ""
                                onRestored(importedNotes, enteredPassphrase)
                            }
                            .onFailure { error ->
                                errorMessage = error.message ?: "备份恢复失败"
                            }
                    }
                },
                enabled = !restoring,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
            ) {
                if (restoring) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("验证并恢复")
                }
            }
            Text(
                "恢复过程完全在本机完成，不会上传备份内容。",
                color = Muted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 14.dp)
            .width(40.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(Color(0xFFD4CFD8)),
    )
}

@Composable
private fun SheetHeader(kicker: String, title: String, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(kicker, color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = onClose, modifier = Modifier.clip(CircleShape).background(Color(0xFFEFEAF1))) {
            Icon(Icons.Rounded.Close, contentDescription = "关闭")
        }
    }
}

private fun formatDuration(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)
