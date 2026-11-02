package com.cevier.shinian.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cevier.shinian.ui.theme.Blue
import com.cevier.shinian.ui.theme.BlueSoft
import com.cevier.shinian.ui.theme.Ink
import com.cevier.shinian.ui.theme.Line
import com.cevier.shinian.ui.theme.Muted
import com.cevier.shinian.ui.theme.Paper

internal enum class SwipePreviewPage { TIMELINE, VAULT, CHAT }

internal enum class SwipePreviewEdge { LEFT, RIGHT, TOP, BOTTOM }

internal fun swipeSourceAlpha(progress: Float): Float {
    val normalized = progress.coerceIn(0f, 1f)
    if (normalized <= 0.55f) return 1f
    val fadeProgress = ((normalized - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val easedProgress = fadeProgress * fadeProgress * (3f - 2f * fadeProgress)
    return 1f - easedProgress
}

@Composable
internal fun SwipePagePreview(
    page: SwipePreviewPage,
    edge: SwipePreviewEdge,
    progress: Float,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    val normalized = progress.coerceIn(0f, 1f)
    var widthPx by remember { mutableIntStateOf(1) }
    var heightPx by remember { mutableIntStateOf(1) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                widthPx = it.width.coerceAtLeast(1)
                heightPx = it.height.coerceAtLeast(1)
            }
            .background(if (page == SwipePreviewPage.CHAT) Color.White else Paper),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val remaining = 1f - normalized
                    translationX = when (edge) {
                        SwipePreviewEdge.LEFT -> -widthPx * remaining
                        SwipePreviewEdge.RIGHT -> widthPx * remaining
                        else -> 0f
                    }
                    translationY = when (edge) {
                        SwipePreviewEdge.TOP -> -heightPx * remaining
                        SwipePreviewEdge.BOTTOM -> heightPx * remaining
                        else -> 0f
                    }
                },
        ) {
            if (content != null) {
                content()
            } else {
                when (page) {
                    SwipePreviewPage.TIMELINE -> TimelinePreview()
                    SwipePreviewPage.VAULT -> VaultPreview()
                    SwipePreviewPage.CHAT -> ChatPreview()
                }
            }
        }
    }
}

@Composable
private fun TimelinePreview() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 19.dp, vertical = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            PreviewPill("全部", selected = true)
            PreviewPill("文字")
            PreviewPill("图片")
            PreviewPill("音频")
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Text("⌕", color = Ink, fontSize = 22.sp)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("今天", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("最近记录", color = Muted, fontSize = 12.sp)
        }
        PreviewTimelineCard(Color(0xFF3D7AF3), "图片记录", "一张本地保存的照片", tall = true)
        Spacer(Modifier.height(11.dp))
        PreviewTimelineCard(Color(0xFFFF9966), "此刻所想", "刚刚记下的想法")
        Spacer(Modifier.height(11.dp))
        PreviewTimelineCard(Color(0xFF49B383), "音频记录", "一段本地保存的录音")
    }
}

@Composable
private fun PreviewPill(label: String, selected: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = if (selected) Ink else Color.Transparent,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Text(
            label,
            color = if (selected) Color.White else Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun PreviewTimelineCard(accent: Color, title: String, body: String, tall: Boolean = false) {
    Surface(shape = RoundedCornerShape(19.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(modifier = Modifier.fillMaxWidth().height(if (tall) 176.dp else 108.dp)) {
            Box(Modifier.width(3.dp).fillMaxSize().background(accent))
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (tall) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BlueSoft),
                    )
                }
                Text(body, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 9.dp))
            }
        }
    }
}

@Composable
private fun VaultPreview() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 19.dp, vertical = 12.dp)) {
        Text("数据与安全", color = Ink, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        PreviewSectionLabel("资料库")
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column {
                PreviewSettingRow(Icons.Rounded.FolderOpen, "存储位置", "App 私有加密目录")
                PreviewDivider()
                PreviewSettingRow(Icons.Rounded.IosShare, "导出记录", "加密备份、PDF 或 JSON")
            }
        }
        PreviewSectionLabel("访问保护")
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column {
                PreviewSettingRow(Icons.Rounded.Fingerprint, "启动时指纹解锁", "每次启动时重新验证")
                PreviewDivider()
                PreviewSettingRow(Icons.Rounded.HideImage, "隐藏最近任务预览", "切换应用时遮盖记录内容")
            }
        }
        PreviewSectionLabel("智能服务")
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            PreviewSettingRow(Icons.Rounded.AutoAwesome, "LLM 配置", "DeepSeek · 已配置模型")
        }
    }
}

@Composable
private fun PreviewSectionLabel(text: String) {
    Text(text, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp))
}

@Composable
private fun PreviewSettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF0ECF4)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Blue, modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.padding(start = 11.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PreviewDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@Composable
private fun ChatPreview() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("AI 对话", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF4FA66A)))
                    Text(" DeepSeek", color = Muted, fontSize = 10.sp)
                }
            }
            Box(Modifier.size(42.dp).clip(CircleShape).background(Paper), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AddComment, null, tint = Ink, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.weight(0.23f))
        Surface(shape = RoundedCornerShape(22.dp), color = Paper, modifier = Modifier.align(Alignment.End)) {
            Text("记录下刚才想到的内容", color = Ink, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        }
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.padding(top = 14.dp)) {
            Text("当然，我们可以从这里继续。", color = Ink, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
        }
        Spacer(Modifier.weight(1f))
        Surface(
            modifier = Modifier.fillMaxWidth().height(154.dp),
            color = Color.White,
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3E6EC)),
            shadowElevation = 5.dp,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("输入消息…", color = Muted, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Paper), contentAlignment = Alignment.Center) {
                        Text("+", color = Ink, fontSize = 24.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(45.dp).clip(CircleShape).background(Ink))
                }
            }
        }
    }
}
