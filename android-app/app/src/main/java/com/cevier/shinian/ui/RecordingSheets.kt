package com.cevier.shinian.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.util.LruCache
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image as ImageIcon
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.cevier.shinian.data.AudioRecorder
import com.cevier.shinian.data.EncryptedNoteStore
import com.cevier.shinian.model.Note
import com.cevier.shinian.model.NoteType
import com.cevier.shinian.model.randomNoteAccent
import com.cevier.shinian.ui.theme.Blue
import com.cevier.shinian.ui.theme.BlueSoft
import com.cevier.shinian.ui.theme.Ink
import com.cevier.shinian.ui.theme.Muted
import com.cevier.shinian.ui.theme.Orange
import com.cevier.shinian.ui.theme.OrangeSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposerSheet(
    store: EncryptedNoteStore,
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val composerScope = rememberCoroutineScope()
    val recorder = remember { AudioRecorder(context) }
    var mediaType by rememberSaveable { mutableStateOf<NoteType?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var mediaPath by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var committed by remember { mutableStateOf(false) }
    var mediaLoading by remember { mutableStateOf(false) }
    var imageImportJob by remember { mutableStateOf<Job?>(null) }

    fun replaceMedia(path: String, type: NoteType) {
        store.deleteMedia(mediaPath)
        mediaPath = path
        mediaType = type
    }

    fun importImage(loader: () -> String) {
        if (mediaLoading) return
        mediaLoading = true
        imageImportJob = composerScope.launch {
            val result = withContext(Dispatchers.IO + NonCancellable) { runCatching(loader) }
            val importedPath = result.getOrNull()
            if (!isActive) {
                withContext(Dispatchers.IO + NonCancellable) { store.deleteMedia(importedPath) }
                return@launch
            }
            mediaLoading = false
            result
                .onSuccess { replaceMedia(it, NoteType.IMAGE) }
                .onFailure { error -> Toast.makeText(context, "读取图片失败：${error.message}", Toast.LENGTH_LONG).show() }
        }
    }

    fun startRecording() {
        runCatching {
            store.deleteMedia(mediaPath)
            mediaPath = null
            mediaType = null
            seconds = 0
            recorder.start()
            recording = true
        }.onFailure { Toast.makeText(context, "无法开始录音：${it.message}", Toast.LENGTH_LONG).show() }
    }

    fun stopRecording() {
        val file = recorder.stop()
        recording = false
        if (file == null) {
            Toast.makeText(context, "录音没有保存成功，请重试", Toast.LENGTH_SHORT).show()
        } else {
            runCatching { store.importMedia(file, "m4a") }
                .onSuccess { replaceMedia(it, NoteType.AUDIO) }
                .onFailure { Toast.makeText(context, "保存录音失败：${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    fun clearMedia() {
        imageImportJob?.cancel()
        imageImportJob = null
        mediaLoading = false
        if (recording) {
            recorder.cancel()
            recording = false
        }
        store.deleteMedia(mediaPath)
        mediaPath = null
        mediaType = null
        seconds = 0
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else Toast.makeText(context, "需要麦克风权限才能录音", Toast.LENGTH_LONG).show()
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            importImage { store.importMedia(it, "jpg") }
        }
    }
    fun dismiss() {
        imageImportJob?.cancel()
        recorder.cancel()
        if (!committed) store.deleteMedia(mediaPath)
        onDismiss()
    }

    DisposableEffect(Unit) {
        onDispose {
            imageImportJob?.cancel()
            recorder.cancel()
            if (!committed) store.deleteMedia(mediaPath)
        }
    }
    LaunchedEffect(recording) {
        while (recording) {
            delay(1_000)
            seconds += 1
        }
    }

    val canSave = (text.isNotBlank() || mediaPath != null) && !mediaLoading && !recording
    val composerScrollState = rememberScrollState()
    var dismissOffsetY by remember { mutableFloatStateOf(0f) }
    var composerHeight by remember { mutableIntStateOf(1) }
    val dismissProgress = (
        dismissOffsetY / (composerHeight * 0.55f).coerceAtLeast(1f)
    ).coerceIn(0f, 1f)
    val dismissConnection = remember(composerScrollState, composerHeight) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val delta = available.y
                val shouldDragSheet =
                    (delta > 0f && composerScrollState.value == 0) || (delta < 0f && dismissOffsetY > 0f)
                if (!shouldDragSheet) return Offset.Zero

                val previous = dismissOffsetY
                dismissOffsetY = (dismissOffsetY + delta).coerceAtLeast(0f)
                return Offset(x = 0f, y = dismissOffsetY - previous)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dismissOffsetY <= 0f) return Velocity.Zero
                val shouldDismiss =
                    dismissOffsetY >= composerHeight * 0.18f || available.y >= 1_500f
                if (shouldDismiss) {
                    animate(
                        initialValue = dismissOffsetY,
                        targetValue = composerHeight.toFloat(),
                        animationSpec = tween(durationMillis = 170),
                    ) { value, _ -> dismissOffsetY = value }
                    dismiss()
                } else {
                    animate(
                        initialValue = dismissOffsetY,
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 190),
                    ) { value, _ -> dismissOffsetY = value }
                }
                return Velocity(x = 0f, y = available.y)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = ::dismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        containerColor = Color.Transparent,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = dismissOffsetY
                    alpha = (1f - dismissProgress).coerceIn(0f, 1f)
                }
                .onSizeChanged { composerHeight = it.height.coerceAtLeast(1) }
                .nestedScroll(dismissConnection)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .navigationBarsPadding()
                .verticalScroll(composerScrollState)
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RecordingSheetHandle()
            }
            RecordingSheetHeader(title = "此刻所想", onClose = ::dismiss)
            if (mediaType == NoteType.IMAGE) {
                mediaPath?.let { path ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(17.dp)),
                    ) {
                        EncryptedImage(store = store, path = path, modifier = Modifier.fillMaxSize())
                        AttachmentCloseButton(
                            onClick = ::clearMedia,
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        )
                    }
                }
            }

            if (recording || mediaType == NoteType.AUDIO) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(17.dp),
                        color = OrangeSoft,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 52.dp, bottom = 12.dp),
                        ) {
                            Icon(
                                if (recording) Icons.Rounded.Stop else Icons.Rounded.KeyboardVoice,
                                contentDescription = null,
                                tint = Orange,
                            )
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(if (recording) "正在录音…" else "录音已就绪", fontWeight = FontWeight.SemiBold)
                                Text(recordingDuration(seconds.coerceAtLeast(if (mediaType == NoteType.AUDIO) 1 else 0)), color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                    AttachmentCloseButton(
                        onClick = ::clearMedia,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    )
                }
            }

            if (mediaPath != null || recording) Spacer(Modifier.height(12.dp))
            RecordTextField(
                value = text,
                onChange = { text = it },
                placeholder = "写下刚刚闪过的想法…",
                height = 118,
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = {
                        if (recording) {
                            recorder.cancel()
                            recording = false
                            seconds = 0
                        }
                        imagePicker.launch("image/*")
                    },
                    enabled = !mediaLoading,
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (mediaType == NoteType.IMAGE) BlueSoft else Color(0xFFF1F3F6),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (mediaLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Blue)
                        } else {
                            Icon(Icons.Rounded.PhotoLibrary, "导入照片", tint = Blue)
                        }
                    }
                }
                Surface(
                    onClick = {
                        if (recording) stopRecording()
                        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    enabled = !mediaLoading,
                    modifier = Modifier.padding(start = 10.dp).size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (recording || mediaType == NoteType.AUDIO) OrangeSoft else Color(0xFFF1F3F6),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (recording) Icons.Rounded.Stop else Icons.Rounded.KeyboardVoice,
                            if (recording) "停止录音" else "录音",
                            tint = Orange,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = canSave,
                    onClick = {
                        val clean = text.trim()
                        val noteType = mediaType ?: NoteType.TEXT
                        val fallback = when (noteType) {
                            NoteType.TEXT -> "文字记录"
                            NoteType.IMAGE -> "图片记录"
                            NoteType.AUDIO -> "音频记录"
                        }
                        committed = true
                        onSave(
                            Note(
                                id = System.currentTimeMillis(),
                                day = "今天",
                                time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                                type = noteType,
                                duration = if (noteType == NoteType.AUDIO) recordingDuration(seconds.coerceAtLeast(1)) else null,
                                title = clean.lineSequence().firstOrNull()?.take(18)?.ifBlank { fallback } ?: fallback,
                                body = clean.ifBlank {
                                    if (noteType == NoteType.IMAGE) "一张本地保存的照片" else "一段本地保存的录音"
                                },
                                accent = randomNoteAccent(),
                                mediaPath = mediaPath,
                                recordedDate = LocalDate.now().toString(),
                            ),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(15.dp),
                ) { Text("保存记录") }
            }
        }
    }
}

@Composable
private fun RecordTextField(value: String, onChange: (String) -> Unit, placeholder: String, height: Int) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth().height(height.dp),
        shape = RoundedCornerShape(17.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF5F2F5),
            unfocusedContainerColor = Color(0xFFF5F2F5),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun AttachmentCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        color = Ink.copy(alpha = 0.78f),
        contentColor = Color.White,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, contentDescription = "取消附件", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun EncryptedImage(
    store: EncryptedNoteStore,
    path: String?,
    modifier: Modifier = Modifier,
    maxPreviewSize: Dp = 420.dp,
) {
    val maxDimensionPx = with(LocalDensity.current) { maxPreviewSize.roundToPx() }.coerceAtLeast(1)
    val bitmap by produceState<Bitmap?>(initialValue = path?.let(ImagePreviewCache::get), key1 = path, key2 = maxDimensionPx) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                path?.let { mediaPath ->
                    ImagePreviewCache.get(mediaPath) ?: runCatching {
                        decodePreviewBitmap(store.loadImagePreview(mediaPath), maxDimensionPx)
                    }.getOrNull()?.also { ImagePreviewCache.put(mediaPath, it) }
                }
            }
        }
    }
    val preview = bitmap
    if (preview == null) {
        Box(modifier.background(BlueSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ImageIcon, "图片不可用", tint = Blue, modifier = Modifier.size(36.dp))
        }
    } else {
        Image(bitmap = preview.asImageBitmap(), contentDescription = "记录图片", contentScale = ContentScale.Crop, modifier = modifier)
    }
}

private object ImagePreviewCache {
    private val cacheSizeKb = (Runtime.getRuntime().maxMemory() / 1024L / 8L)
        .coerceIn(8L * 1024L, 48L * 1024L)
        .toInt()
    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    fun get(path: String): Bitmap? = cache.get(path)

    fun put(path: String, bitmap: Bitmap) {
        cache.put(path, bitmap)
    }
}

private const val FULLSCREEN_IMAGE_MAX_DIMENSION = 3072

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteDetailSheet(
    note: Note,
    store: EncryptedNoteStore,
    onDismiss: () -> Unit,
    onUpdate: (Note) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    var title by rememberSaveable(note.id) { mutableStateOf(note.title) }
    var body by rememberSaveable(note.id) { mutableStateOf(note.body) }
    var fullImageVisible by rememberSaveable(note.id) { mutableStateOf(false) }
    var dismissOffsetY by remember(note.id) { mutableFloatStateOf(0f) }
    var sheetHeight by remember(note.id) { mutableIntStateOf(1) }
    val cleanTitle = title.trim()
    val cleanBody = body.trim()
    val dismissProgress = (
        dismissOffsetY / (sheetHeight * 0.55f).coerceAtLeast(1f)
    ).coerceIn(0f, 1f)
    val dismissConnection = remember(note.id, scrollState, sheetHeight) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val delta = available.y
                val shouldDragSheet =
                    (delta > 0f && scrollState.value == 0) || (delta < 0f && dismissOffsetY > 0f)
                if (!shouldDragSheet) return Offset.Zero

                val previous = dismissOffsetY
                dismissOffsetY = (dismissOffsetY + delta).coerceAtLeast(0f)
                return Offset(x = 0f, y = dismissOffsetY - previous)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dismissOffsetY <= 0f) return Velocity.Zero
                val shouldDismiss =
                    dismissOffsetY >= sheetHeight * 0.18f || available.y >= 1_500f
                if (shouldDismiss) {
                    animate(
                        initialValue = dismissOffsetY,
                        targetValue = sheetHeight.toFloat(),
                        animationSpec = tween(durationMillis = 170),
                    ) { value, _ -> dismissOffsetY = value }
                    onDismiss()
                } else {
                    animate(
                        initialValue = dismissOffsetY,
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 190),
                    ) { value, _ -> dismissOffsetY = value }
                }
                return Velocity(x = 0f, y = available.y)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        containerColor = Color.Transparent,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = dismissOffsetY
                    alpha = (1f - dismissProgress).coerceIn(0f, 1f)
                }
                .onSizeChanged { sheetHeight = it.height.coerceAtLeast(1) }
                .nestedScroll(dismissConnection)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(52.dp))
                Text("${note.day} ${note.time}", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        onUpdate(note.copy(title = cleanTitle, body = cleanBody))
                        onDismiss()
                    },
                    enabled = cleanTitle.isNotEmpty() && (cleanTitle != note.title || cleanBody != note.body),
                ) { Text("保存") }
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
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
            )
            if (note.type == NoteType.IMAGE) {
                EncryptedImage(
                    store,
                    note.mediaPath,
                    Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { fullImageVisible = true },
                )
            }
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("内容") },
                minLines = 3,
                maxLines = 8,
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
            )
            if (note.type == NoteType.AUDIO && note.mediaPath != null) {
                AudioPlayer(note, store)
            }
        }
    }

    if (fullImageVisible && note.mediaPath != null) {
        FullscreenImageDialog(
            store = store,
            path = note.mediaPath,
            onDismiss = { fullImageVisible = false },
        )
    }
}

@Composable
private fun FullscreenImageDialog(
    store: EncryptedNoteStore,
    path: String,
    onDismiss: () -> Unit,
) {
    val bitmap by produceState<Bitmap?>(initialValue = ImagePreviewCache.get(path), key1 = path) {
        val fullResolution = withContext(Dispatchers.IO) {
            runCatching {
                decodePreviewBitmap(store.loadMedia(path), FULLSCREEN_IMAGE_MAX_DIMENSION)
            }.getOrNull()
        }
        if (fullResolution != null) value = fullResolution
    }
    var scale by remember(path) { mutableFloatStateOf(1f) }
    var offset by remember(path) { mutableStateOf(Offset.Zero) }
    var dismissOffsetY by remember(path) { mutableFloatStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val dismissAlpha = (
        1f - abs(dismissOffsetY) / (viewportSize.height * 0.55f).coerceAtLeast(1f)
    ).coerceIn(0f, 1f)

    fun updateTransform(nextScale: Float, pan: Offset = Offset.Zero) {
        val boundedScale = nextScale.coerceIn(1f, 5f)
        val maxOffsetX = viewportSize.width * (boundedScale - 1f) / 2f
        val maxOffsetY = viewportSize.height * (boundedScale - 1f) / 2f
        val nextOffset = if (boundedScale == 1f) Offset.Zero else offset + pan
        scale = boundedScale
        if (boundedScale > 1.01f) dismissOffsetY = 0f
        offset = Offset(
            nextOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
            nextOffset.y.coerceIn(-maxOffsetY, maxOffsetY),
        )
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        updateTransform(scale * zoomChange, panChange)
    }
    val dismissDragState = rememberDraggableState { delta ->
        if (scale <= 1.01f) dismissOffsetY += delta
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dismissAlpha))
                .onSizeChanged { viewportSize = it }
                .pointerInput(path) {
                    detectTapGestures(
                        onDoubleTap = {
                            updateTransform(if (scale > 1f) 1f else 2.5f)
                        },
                    )
                }
                .transformable(transformState)
                .draggable(
                    state = dismissDragState,
                    orientation = Orientation.Vertical,
                    enabled = scale <= 1.01f,
                    onDragStopped = { velocity ->
                        val dismissDistance = viewportSize.height * 0.18f
                        val shouldDismiss = abs(dismissOffsetY) >= dismissDistance || abs(velocity) >= 1_700f
                        if (shouldDismiss) {
                            val direction = when {
                                dismissOffsetY > 0f -> 1f
                                dismissOffsetY < 0f -> -1f
                                velocity >= 0f -> 1f
                                else -> -1f
                            }
                            val target = direction * viewportSize.height.coerceAtLeast(1).toFloat()
                            animate(
                                initialValue = dismissOffsetY,
                                targetValue = target,
                                animationSpec = tween(durationMillis = 160),
                            ) { value, _ -> dismissOffsetY = value }
                            onDismiss()
                        } else {
                            animate(
                                initialValue = dismissOffsetY,
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 180),
                            ) { value, _ -> dismissOffsetY = value }
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val fullImage = bitmap
            if (fullImage == null) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Image(
                    bitmap = fullImage.asImageBitmap(),
                    contentDescription = "完整图片",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y + dismissOffsetY
                            alpha = dismissAlpha
                        },
                )
            }

            Surface(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(44.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer { alpha = dismissAlpha }) {
                    Icon(Icons.Rounded.Close, "关闭全屏图片")
                }
            }
        }
    }
}

@Composable
private fun AudioPlayer(note: Note, store: EncryptedNoteStore) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var tempFile by remember { mutableStateOf<File?>(null) }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(note.mediaPath) {
        onDispose {
            player?.release()
            tempFile?.delete()
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF3FB)), modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(13.dp)) {
            Surface(
                modifier = Modifier.size(42.dp).clickable {
                    val current = player
                    if (current != null) {
                        if (current.isPlaying) {
                            current.pause()
                            playing = false
                        } else {
                            current.start()
                            playing = true
                        }
                    } else {
                        runCatching {
                            val file = store.createPlaybackFile(requireNotNull(note.mediaPath), "m4a")
                            tempFile = file
                            MediaPlayer().apply {
                                setDataSource(file.absolutePath)
                                prepare()
                                setOnCompletionListener { playing = false }
                                start()
                            }.also {
                                player = it
                                playing = true
                            }
                        }.onFailure { Toast.makeText(context, "无法播放录音", Toast.LENGTH_SHORT).show() }
                    }
                },
                shape = CircleShape,
                color = Blue,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, if (playing) "暂停" else "播放", tint = Color.White)
                }
            }
            Waveform(Modifier.weight(1f).padding(horizontal = 12.dp), compact = true)
            Text(note.duration ?: "00:00", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RecordingSheetHandle() {
    Box(
        Modifier.padding(top = 10.dp).width(38.dp).height(4.dp).clip(CircleShape).background(Color(0xFFD8D3DA)),
    )
}

@Composable
private fun RecordingSheetHeader(title: String, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        Surface(onClick = onClose, shape = CircleShape, color = Color(0xFFF1EEF2), modifier = Modifier.size(38.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "关闭") }
        }
    }
}

private fun recordingDuration(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

private fun decodePreviewBitmap(bytes: ByteArray, maxDimensionPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sampleSize = 1
    while (bounds.outWidth / sampleSize > maxDimensionPx || bounds.outHeight / sampleSize > maxDimensionPx) sampleSize *= 2
    return BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize.coerceAtLeast(1) },
    )
}
