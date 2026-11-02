package com.cevier.shinian.ui

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import com.cevier.shinian.data.LlmApiClient
import com.cevier.shinian.data.LlmPrivateStore
import com.cevier.shinian.model.ChatMessage
import com.cevier.shinian.model.LlmConfig
import com.cevier.shinian.model.LlmModel
import com.cevier.shinian.model.LlmProvider
import com.cevier.shinian.model.RecentConversation
import com.cevier.shinian.ui.theme.Blue
import com.cevier.shinian.ui.theme.BlueSoft
import com.cevier.shinian.ui.theme.Ink
import com.cevier.shinian.ui.theme.Line
import com.cevier.shinian.ui.theme.Muted
import com.cevier.shinian.ui.theme.Paper
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun LlmSettingsScreen(
    initialConfig: LlmConfig,
    apiClient: LlmApiClient,
    onSaved: (LlmConfig) -> Unit,
) {
    var provider by rememberSaveable { mutableStateOf(initialConfig.provider) }
    var apiKey by rememberSaveable { mutableStateOf(initialConfig.apiKey) }
    var selectedModelId by rememberSaveable { mutableStateOf(initialConfig.modelId) }
    var selectedModelIds by remember { mutableStateOf(initialConfig.configuredModels.map(LlmModel::id).toSet()) }
    var models by remember { mutableStateOf(initialConfig.configuredModels) }
    var loadingModels by remember { mutableStateOf(false) }
    var modelPickerVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun candidateConfig(): LlmConfig {
        val configured = models.filter { it.id in selectedModelIds }
        val active = configured.firstOrNull { it.id == selectedModelId } ?: configured.firstOrNull()
        return LlmConfig(
            provider = provider,
            apiKey = apiKey.trim(),
            baseUrl = provider.defaultBaseUrl,
            modelId = active?.id.orEmpty(),
            modelName = active?.name.orEmpty(),
            supportsImages = active?.supportsImages ?: false,
            models = configured,
            thinkingEnabled = initialConfig.thinkingEnabled,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Paper, Color(0xFFF1F5FC))))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        ScreenHeader(title = "LLM 配置")

        Text("服务商", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            LlmProvider.entries.forEach { item ->
                Surface(
                    onClick = {
                        if (provider != item) {
                            provider = item
                            selectedModelId = ""
                            selectedModelIds = emptySet()
                            models = emptyList()
                            message = null
                        }
                    },
                    color = if (provider == item) Ink else Color.White,
                    contentColor = if (provider == item) Color.White else Ink,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (provider == item) Ink else Line),
                ) {
                    Text(item.label, modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp), fontSize = 12.sp)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("API Key", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; message = null },
                    placeholder = { Text(if (provider == LlmProvider.DEEPSEEK) "sk-…" else "Vercel AI Gateway Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFBFCFF),
                        unfocusedContainerColor = Color(0xFFFBFCFF),
                    ),
                )
                Text("密钥使用 Android Keystore 加密，仅保存在本机。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("模型", fontWeight = FontWeight.SemiBold)
                        Text("通过 ${provider.defaultBaseUrl}/models 获取", color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        enabled = apiKey.isNotBlank() && !loadingModels,
                        onClick = {
                            loadingModels = true
                            message = null
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) { apiClient.listModels(candidateConfig()) }
                                }.onSuccess { loaded ->
                                    models = loaded
                                    selectedModelIds = selectedModelIds.intersect(loaded.map(LlmModel::id).toSet())
                                        .ifEmpty { loaded.firstOrNull()?.let { setOf(it.id) }.orEmpty() }
                                    selectedModelId = selectedModelId.takeIf { it in selectedModelIds }
                                        ?: selectedModelIds.firstOrNull().orEmpty()
                                    message = if (loaded.isEmpty()) "接口没有返回可用模型" else "已获取 ${loaded.size} 个模型"
                                }.onFailure { error ->
                                    message = error.message ?: "获取模型失败"
                                }
                                loadingModels = false
                            }
                        },
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        if (loadingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(7.dp))
                        }
                        Text(if (loadingModels) "正在获取" else "获取模型", fontSize = 11.sp)
                    }
                }

                Surface(
                    onClick = { if (models.isNotEmpty()) modelPickerVisible = true },
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFFFBFCFF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line),
                    modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        Text(
                            if (selectedModelIds.isEmpty()) "请先获取并选择模型" else "已选择 ${selectedModelIds.size} 个模型",
                            modifier = Modifier.weight(1f),
                            color = if (selectedModelIds.isEmpty()) Muted else Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp,
                        )
                        Icon(Icons.Rounded.ExpandMore, null, tint = Muted)
                    }
                }

                message?.let {
                    Text(it, color = if (it.startsWith("已获取")) Color(0xFF377A4F) else Color(0xFFB4473D), fontSize = 11.sp, modifier = Modifier.padding(top = 9.dp))
                }
            }
        }

        Button(
            enabled = apiKey.isNotBlank() && selectedModelIds.isNotEmpty(),
            onClick = { onSaved(candidateConfig()) },
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            contentPadding = PaddingValues(vertical = 13.dp),
        ) { Text("保存配置") }
        Text("发送对话时，文字和所选图片会传输给当前服务商；普通记录不会被上传。", color = Muted, fontSize = 11.sp, lineHeight = 18.sp, modifier = Modifier.padding(8.dp, 10.dp, 8.dp, 20.dp))
    }

    if (modelPickerVisible) {
        ModelPickerDialog(
            provider = provider,
            models = models,
            selectedIds = selectedModelIds,
            multiple = true,
            onSelect = { model ->
                val checked = model.id in selectedModelIds
                selectedModelIds = if (checked) selectedModelIds - model.id else selectedModelIds + model.id
                if (!checked && selectedModelId.isBlank()) selectedModelId = model.id
                if (checked && selectedModelId == model.id) selectedModelId = (selectedModelIds - model.id).firstOrNull().orEmpty()
            },
            onDismiss = { modelPickerVisible = false },
        )
    }
}

@Composable
internal fun TemporaryChatScreen(
    config: LlmConfig,
    initialConversation: RecentConversation,
    store: LlmPrivateStore,
    apiClient: LlmApiClient,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onConversationChanged: (RecentConversation) -> Unit,
    onConfigChanged: (LlmConfig) -> Unit,
    timelinePreview: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<ChatMessage>().apply { addAll(initialConversation.messages) } }
    var input by rememberSaveable { mutableStateOf("") }
    var pendingImage by remember { mutableStateOf<String?>(null) }
    var preparingImage by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var streamingReply by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var chatModelPickerVisible by remember { mutableStateOf(false) }
    var requestJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val initialHistoryLastIndex = initialConversation.messages.lastIndex.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialHistoryLastIndex)
    var timelinePullDistance by remember { mutableFloatStateOf(0f) }
    var chatSizePx by remember { mutableStateOf(IntSize.Zero) }
    val timelinePullThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val currentOnBack by rememberUpdatedState(onBack)
    val timelinePageProgress = (
        timelinePullDistance / chatSizePx.height.coerceAtLeast(1)
    ).coerceIn(0f, 1f)
    val timelinePullConnection = remember(listState, timelinePullThreshold) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f || timelinePullDistance <= 0f) {
                    return Offset.Zero
                }
                val consumedY = minOf(available.y, timelinePullDistance / 0.55f)
                timelinePullDistance = (timelinePullDistance - consumedY * 0.55f).coerceAtLeast(0f)
                return Offset(0f, consumedY)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && !listState.canScrollForward && available.y < 0f) {
                    timelinePullDistance = (timelinePullDistance - available.y * 0.55f)
                        .coerceAtMost(maxOf(chatSizePx.height.toFloat(), timelinePullThreshold * 1.25f))
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (timelinePullDistance >= timelinePullThreshold) {
                    animate(
                        initialValue = timelinePullDistance,
                        targetValue = maxOf(chatSizePx.height.toFloat(), timelinePullThreshold),
                        animationSpec = tween(180),
                    ) { value, _ -> timelinePullDistance = value }
                    currentOnBack()
                } else if (timelinePullDistance > 0f) {
                    animate(timelinePullDistance, 0f) { value, _ -> timelinePullDistance = value }
                }
                return Velocity.Zero
            }
        }
    }
    var initialHistoryPositioned by remember {
        mutableStateOf(initialConversation.messages.isEmpty())
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        preparingImage = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { store.prepareImage(uri) } }
                .onSuccess { pendingImage = it }
                .onFailure { Toast.makeText(context, it.message ?: "图片处理失败", Toast.LENGTH_SHORT).show() }
            preparingImage = false
        }
    }

    fun persist() {
        val snapshot = RecentConversation(messages.toList(), System.currentTimeMillis())
        onConversationChanged(snapshot)
    }

    fun sendMessage() {
        if (sending || (input.isBlank() && pendingImage == null)) return
        val userMessage = ChatMessage(
            id = System.currentTimeMillis(),
            role = "user",
            text = input.trim(),
            imageDataUrl = pendingImage,
        )
        messages.add(userMessage)
        val requestHistory = messages.toList()
        input = ""
        pendingImage = null
        errorMessage = null
        streamingReply = ""
        sending = true
        persist()
        requestJob = scope.launch {
            val deltas = Channel<String>(Channel.UNLIMITED)
            val network = async(Dispatchers.IO) {
                runCatching {
                    apiClient.completeStreaming(config, requestHistory) { delta ->
                        deltas.trySend(delta)
                    }
                }.also { deltas.close() }
            }
            for (delta in deltas) streamingReply += delta
            network.await()
                .onSuccess { reply ->
                    messages.add(ChatMessage(System.currentTimeMillis(), "assistant", reply))
                    persist()
                }
                .onFailure { error ->
                    if (streamingReply.isNotBlank()) {
                        messages.add(ChatMessage(System.currentTimeMillis(), "assistant", streamingReply))
                        persist()
                    }
                    errorMessage = error.message ?: "发送失败"
                }
            streamingReply = ""
            sending = false
            requestJob = null
        }
    }

    LaunchedEffect(messages.size, streamingReply.length) {
        val lastItem = messages.lastIndex + if (sending) 1 else 0
        if (lastItem >= 0) {
            if (initialHistoryPositioned) {
                listState.animateScrollToItem(lastItem)
            } else {
                // History should already be at the latest message on the first visible frame.
                // An animated initial scroll makes the whole conversation flash past the user.
                listState.scrollToItem(lastItem)
                initialHistoryPositioned = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .onSizeChanged { chatSizePx = it },
    ) {
        SwipePagePreview(
            page = SwipePreviewPage.TIMELINE,
            edge = SwipePreviewEdge.BOTTOM,
            progress = timelinePageProgress,
            content = timelinePreview,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, -timelinePullDistance.roundToInt()) }
                .background(Color.White)
                .alpha(swipeSourceAlpha(timelinePageProgress)),
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text("AI 对话", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 1.dp)) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (config.isReady) Color(0xFF4FA66A) else Muted))
                    Text(
                        if (config.isReady) config.provider.label else "尚未配置 LLM",
                        color = Muted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }
            Surface(
                onClick = {
                    requestJob?.cancel()
                    requestJob = null
                    messages.clear()
                    pendingImage = null
                    streamingReply = ""
                    sending = false
                    errorMessage = null
                    onConversationChanged(RecentConversation())
                },
                shape = CircleShape,
                color = Paper,
                contentColor = Ink,
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AddComment, "新建对话", modifier = Modifier.size(22.dp))
                }
            }
        }

        if (!config.isReady) {
            Column(modifier = Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(23.dp)).background(BlueSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Blue, modifier = Modifier.size(30.dp))
                }
                Text("先配置 LLM", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
                Text("添加 API Key，并从服务商的 models 接口选择模型。", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
                Button(onClick = onOpenSettings, colors = ButtonDefaults.buttonColors(containerColor = Blue), modifier = Modifier.padding(top = 18.dp)) {
                    Text("前往配置")
                }
            }
            return
        }

        if (messages.isEmpty()) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Blue, modifier = Modifier.size(24.dp))
                }
                Text("有什么想聊的？", fontWeight = FontWeight.SemiBold, fontSize = 17.sp, modifier = Modifier.padding(top = 13.dp))
                Text("支持文字与图片", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(timelinePullConnection),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(messages, key = { it.id }) { message -> ChatBubble(message) }
                    if (sending) {
                        item {
                            if (streamingReply.isBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp, color = Blue)
                                    Text("正在思考…", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 9.dp))
                                }
                            } else {
                                ChatBubble(ChatMessage(Long.MIN_VALUE, "assistant", streamingReply))
                            }
                        }
                    }
                }

            }
        }

        errorMessage?.let {
            Text(it, color = Color(0xFFB4473D), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color.White,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFFE3E6EC)),
            shadowElevation = 7.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
                pendingImage?.let { dataUrl ->
                    Box(modifier = Modifier.padding(start = 4.dp, bottom = 9.dp)) {
                        ChatImage(dataUrl, Modifier.size(76.dp).clip(RoundedCornerShape(15.dp)))
                        Surface(
                            onClick = { pendingImage = null },
                            shape = CircleShape,
                            color = Ink.copy(alpha = 0.88f),
                            modifier = Modifier.align(Alignment.TopEnd).size(23.dp),
                        ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "移除图片", tint = Color.White, modifier = Modifier.size(14.dp)) } }
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("输入消息…", color = Muted) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                    shape = RoundedCornerShape(20.dp),
                    minLines = 2,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Surface(
                        onClick = { imagePicker.launch("image/*") },
                        enabled = !sending && !preparingImage,
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE3E6EC)),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (preparingImage) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Rounded.Add, "添加图片", tint = Ink, modifier = Modifier.size(25.dp))
                        }
                    }

                    Surface(
                        onClick = { chatModelPickerVisible = true },
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE3E6EC)),
                        modifier = Modifier.weight(0.8f).height(44.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 13.dp),
                        ) {
                            Text(
                                config.modelName.ifBlank { config.modelId },
                                modifier = Modifier.weight(1f),
                                color = Ink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.weight(0.2f))

                    val thinkingAccent = Blue.copy(alpha = 0.58f)
                    Surface(
                        onClick = { onConfigChanged(config.copy(thinkingEnabled = !config.thinkingEnabled)) },
                        shape = CircleShape,
                        color = Color.Transparent,
                        contentColor = if (config.thinkingEnabled) thinkingAccent else Muted,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Psychology, "思考开关", modifier = Modifier.size(25.dp))
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 5.dp, bottom = 5.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (config.thinkingEnabled) thinkingAccent else Color(0xFFC6CBD4)),
                            )
                        }
                    }

                    Surface(
                        onClick = { sendMessage() },
                        enabled = !sending && (input.isNotBlank() || pendingImage != null),
                        shape = CircleShape,
                        color = if (!sending && (input.isNotBlank() || pendingImage != null)) Ink else Color(0xFFDCE2EC),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowUpward, "发送", tint = Color.White, modifier = Modifier.size(23.dp))
                        }
                    }
                }
            }
        }
        }
    }

    if (chatModelPickerVisible) {
        ModelPickerDialog(
            provider = config.provider,
            models = config.configuredModels,
            selectedIds = setOf(config.modelId),
            multiple = false,
            onSelect = { model ->
                onConfigChanged(
                    config.copy(
                        modelId = model.id,
                        modelName = model.name,
                        supportsImages = model.supportsImages,
                    ),
                )
                chatModelPickerVisible = false
            },
            onDismiss = { chatModelPickerVisible = false },
        )
    }
}

@Composable
private fun ModelPickerDialog(
    provider: LlmProvider,
    models: List<LlmModel>,
    selectedIds: Set<String>,
    multiple: Boolean,
    onSelect: (LlmModel) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(10.dp)
                    .clickable(interactionSource = interactionSource, indication = null) {},
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD8DDE7)),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (multiple) "选择常用模型" else "切换模型", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${provider.label} · ${if (multiple) "可多选" else "${models.size} 个可用模型"}",
                                color = Muted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Surface(onClick = onDismiss, shape = CircleShape, color = Color(0xFFF2F4F8), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, "关闭", tint = Muted, modifier = Modifier.size(18.dp)) }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(models, key = { it.id }) { model ->
                            val selected = model.id in selectedIds
                            Surface(
                                onClick = { onSelect(model) },
                                shape = RoundedCornerShape(17.dp),
                                color = if (selected) BlueSoft else Color(0xFFF7F8FB),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) Blue.copy(alpha = 0.32f) else Color.Transparent,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (selected) Blue else Color.White),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        else Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFD5DAE4)))
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(horizontal = 11.dp)) {
                                        Text(model.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (model.name != model.id) {
                                            Text(model.id, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    if (model.supportsImages) {
                                        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(34.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Photo, "支持图片", tint = Blue, modifier = Modifier.size(17.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (multiple) {
                        Button(
                            onClick = onDismiss,
                            enabled = selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        ) { Text("完成 · 已选择 ${selectedIds.size} 个") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val user = message.role == "user"
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleMaxWidth = maxWidth * if (user) 0.82f else 0.94f
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
            Surface(
                shape = if (user) RoundedCornerShape(24.dp, 24.dp, 8.dp, 24.dp) else RoundedCornerShape(0.dp),
                color = if (user) Color(0xFFF1F3F6) else Color.Transparent,
                contentColor = Ink,
                shadowElevation = 0.dp,
                modifier = Modifier.widthIn(max = bubbleMaxWidth),
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (user) 16.dp else 3.dp,
                        vertical = if (user) 13.dp else 7.dp,
                    ),
                ) {
                    message.imageDataUrl?.let {
                        ChatImage(it, Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp)))
                        if (message.text.isNotBlank()) Spacer(Modifier.height(9.dp))
                    }
                    if (message.text.isNotBlank()) MarkdownText(message.text)
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Prose(val markdown: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
}

private fun splitTableRow(line: String): List<String> {
    val source = line.trim().removePrefix("|").removeSuffix("|")
    val cells = mutableListOf<String>()
    val cell = StringBuilder()
    var escaped = false
    source.forEach { char ->
        when {
            escaped -> {
                cell.append(char)
                escaped = false
            }
            char == '\\' -> {
                cell.append(char)
                escaped = true
            }
            char == '|' -> {
                cells += cell.toString().trim()
                cell.clear()
            }
            else -> cell.append(char)
        }
    }
    cells += cell.toString().trim()
    return cells
}

private fun isTableSeparator(line: String): Boolean {
    val cells = splitTableRow(line)
    return cells.size >= 2 && cells.all { cell ->
        cell.replace(" ", "").matches(Regex(":?-{3,}:?"))
    }
}

private fun markdownFenceMarker(line: String): String? {
    val trimmed = line.trim()
    return when {
        trimmed.startsWith("```") -> "```"
        trimmed.startsWith("~~~") -> "~~~"
        else -> null
    }
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    val prose = mutableListOf<String>()

    fun flushProse() {
        val value = prose.joinToString("\n").trim('\n')
        if (value.isNotBlank()) blocks += MarkdownBlock.Prose(value)
        prose.clear()
    }

    var index = 0
    while (index < lines.size) {
        val isTableStart = index + 1 < lines.size &&
            lines[index].contains('|') &&
            isTableSeparator(lines[index + 1])
        if (!isTableStart) {
            prose += lines[index]
            index += 1
            continue
        }

        val lastContentIndex = prose.indexOfLast { it.isNotBlank() }
        val fenceMarker = lastContentIndex
            .takeIf { it >= 0 }
            ?.let { markdownFenceMarker(prose[it]) }
        if (fenceMarker != null) {
            while (prose.size > lastContentIndex) prose.removeAt(prose.lastIndex)
        }
        flushProse()
        val headers = splitTableRow(lines[index])
        val rows = mutableListOf<List<String>>()
        index += 2
        while (index < lines.size && lines[index].isNotBlank() && lines[index].contains('|')) {
            val parsed = splitTableRow(lines[index])
            rows += List(headers.size) { cellIndex -> parsed.getOrElse(cellIndex) { "" } }
            index += 1
        }
        blocks += MarkdownBlock.Table(headers, rows)
        if (fenceMarker != null && index < lines.size && lines[index].trim().startsWith(fenceMarker)) {
            index += 1
        }
    }
    flushProse()
    return blocks
}

@Composable
private fun MarkdownText(markdown: String) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Prose -> RawMarkdownText(block.markdown)
                is MarkdownBlock.Table -> MarkdownTable(block)
            }
        }
    }
}

@Composable
private fun MarkdownTable(table: MarkdownBlock.Table) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Blue.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            MarkdownTableRow(table.headers, header = true, alternate = false)
            table.rows.forEachIndexed { index, row ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(1.dp)
                        .background(Line.copy(alpha = 0.08f)),
                )
                MarkdownTableRow(row, header = false, alternate = index % 2 == 1)
            }
        }
    }
}

@Composable
private fun MarkdownTableRow(cells: List<String>, header: Boolean, alternate: Boolean) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    header -> BlueSoft.copy(alpha = 0.72f)
                    alternate -> Paper.copy(alpha = 0.76f)
                    else -> Color.White
                },
            )
            .padding(horizontal = 13.dp, vertical = if (header) 12.dp else 11.dp),
    ) {
        cells.forEachIndexed { index, cell ->
            Box(Modifier.weight(1f)) {
                RawMarkdownText(cell, textSize = 13f, bold = header)
            }
            if (index < cells.lastIndex) Spacer(Modifier.width(10.dp))
        }
    }
}

@Composable
private fun RawMarkdownText(markdown: String, textSize: Float = 14f, bold: Boolean = false) {
    val context = LocalContext.current
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .build()
    }
    AndroidView(
        factory = { viewContext ->
            TextView(viewContext).apply {
                setTextColor(Ink.toArgb())
                setLinkTextColor(Blue.toArgb())
                this.textSize = textSize
                if (bold) setTypeface(typeface, Typeface.BOLD)
                includeFontPadding = false
                setLineSpacing(2f, 1.22f)
                setPadding(0, 0, 0, 0)
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
            }
        },
        update = { textView -> markwon.setMarkdown(textView, markdown) },
    )
}

@Composable
private fun ChatImage(dataUrl: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, dataUrl) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val encoded = dataUrl.substringAfter("base64,", "")
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }
    Box(modifier = modifier.background(Color(0xFFEEF1F6)), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}
