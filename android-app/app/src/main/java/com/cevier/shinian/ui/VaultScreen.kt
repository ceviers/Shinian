package com.cevier.shinian.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cevier.shinian.model.LlmConfig
import com.cevier.shinian.ui.theme.Blue
import com.cevier.shinian.ui.theme.Ink
import com.cevier.shinian.ui.theme.Line
import com.cevier.shinian.ui.theme.Muted
import com.cevier.shinian.ui.theme.Paper
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
internal fun VaultScreen(
    noteCount: Int,
    biometricEnabled: Boolean,
    hideRecentPreview: Boolean,
    llmConfig: LlmConfig,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onOpenLlmSettings: () -> Unit,
    onBiometricChanged: (Boolean) -> Unit,
    onHideRecentPreviewChanged: (Boolean) -> Unit,
    timelinePreview: @Composable () -> Unit,
) {
    var timelineSwipeDistance by remember { mutableFloatStateOf(0f) }
    var vaultSizePx by remember { mutableStateOf(IntSize.Zero) }
    val timelineSwipeThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    val currentOnBack by rememberUpdatedState(onBack)
    val gestureScope = rememberCoroutineScope()
    val timelineSwipeProgress = (
        timelineSwipeDistance / vaultSizePx.width.coerceAtLeast(1)
    ).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .onSizeChanged { vaultSizePx = it }
            .pointerInput(timelineSwipeThreshold) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val start = down.position
                    var trackingLeftSwipe = false
                    var rejected = false
                    var pointerPressed = true

                    while (pointerPressed) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val deltaX = change.position.x - start.x
                        val deltaY = change.position.y - start.y

                        if (!trackingLeftSwipe && !rejected && maxOf(abs(deltaX), abs(deltaY)) > viewConfiguration.touchSlop) {
                            trackingLeftSwipe = deltaX < 0f && abs(deltaX) > abs(deltaY) * 1.2f
                            rejected = !trackingLeftSwipe
                        }

                        if (trackingLeftSwipe) {
                            change.consume()
                            timelineSwipeDistance = (-deltaX).coerceIn(
                                0f,
                                maxOf(vaultSizePx.width.toFloat(), timelineSwipeThreshold * 1.35f),
                            )
                        }
                        pointerPressed = change.pressed
                    }

                    if (trackingLeftSwipe && timelineSwipeDistance >= timelineSwipeThreshold) {
                        gestureScope.launch {
                            animate(
                                initialValue = timelineSwipeDistance,
                                targetValue = maxOf(vaultSizePx.width.toFloat(), timelineSwipeThreshold),
                                animationSpec = tween(180),
                            ) { value, _ -> timelineSwipeDistance = value }
                            currentOnBack()
                        }
                    } else if (timelineSwipeDistance > 0f) {
                        gestureScope.launch {
                            animate(timelineSwipeDistance, 0f) { value, _ -> timelineSwipeDistance = value }
                        }
                    }
                }
            },
    ) {
        SwipePagePreview(
            page = SwipePreviewPage.TIMELINE,
            edge = SwipePreviewEdge.RIGHT,
            progress = timelineSwipeProgress,
            content = timelinePreview,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(-timelineSwipeDistance.roundToInt(), 0) }
                .background(Paper)
                .alpha(swipeSourceAlpha(timelineSwipeProgress))
                .verticalScroll(rememberScrollState())
                .padding(start = 19.dp, top = 12.dp, end = 19.dp, bottom = 14.dp),
        ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("数据与安全", style = MaterialTheme.typography.headlineMedium)
            }
        }

        SectionLabel("资料库", modifier = Modifier.padding(top = 22.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            SettingRow(
                icon = Icons.Rounded.FolderOpen,
                title = "存储位置",
                subtitle = "App 私有加密目录",
                trailing = {
                    Text("本机", color = Muted, fontSize = 10.sp)
                },
            )
            HorizontalDivider(color = Line)
            SettingRow(
                icon = Icons.Rounded.Backup,
                title = "跨设备恢复",
                subtitle = "通过恢复口令加密备份迁移",
                trailing = {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFE6F4E7)) {
                        Text("已开启", color = Color(0xFF377A4F), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                },
            )
            HorizontalDivider(color = Line)
            SettingRow(
                icon = Icons.Rounded.IosShare,
                title = "导出记录",
                subtitle = "加密备份、PDF 或通用 JSON",
                onClick = onExport,
                trailing = { Icon(Icons.Rounded.ChevronRight, null, tint = Muted) },
            )
        }

        SectionLabel("访问保护", modifier = Modifier.padding(top = 22.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            SettingRow(
                icon = Icons.Rounded.Fingerprint,
                title = "启动时指纹解锁",
                subtitle = "每次启动时重新验证",
                onClick = { onBiometricChanged(!biometricEnabled) },
                trailing = { MiniToggle(checked = biometricEnabled) },
            )
            HorizontalDivider(color = Line)
            SettingRow(
                icon = Icons.Rounded.HideImage,
                title = "隐藏最近任务预览",
                subtitle = "切换应用时遮盖记录内容",
                onClick = { onHideRecentPreviewChanged(!hideRecentPreview) },
                trailing = { MiniToggle(checked = hideRecentPreview) },
            )
        }

        SectionLabel("智能服务", modifier = Modifier.padding(top = 22.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            SettingRow(
                icon = Icons.Rounded.AutoAwesome,
                title = "LLM 配置",
                subtitle = if (llmConfig.isReady) {
                    "${llmConfig.provider.label} · ${llmConfig.configuredModels.size} 个模型"
                } else {
                    "DeepSeek 或 Vercel AI Gateway"
                },
                onClick = onOpenLlmSettings,
                trailing = { Icon(Icons.Rounded.ChevronRight, null, tint = Muted) },
            )
        }

            Text(
                "共 $noteCount 条记录 · 数据仅保存在本机",
                color = Color(0xFF8490A3),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 18.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color(0xFF8F8795),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(13.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF0ECF4)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Blue, modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 11.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        trailing()
    }
}
