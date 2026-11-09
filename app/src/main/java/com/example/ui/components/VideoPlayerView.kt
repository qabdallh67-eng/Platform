package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Lesson
import com.example.ui.theme.BrandPrimary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerView(
    lesson: Lesson,
    onProgressUpdate: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPlaying by remember(lesson.id) { mutableStateOf(true) }
    var currentSpeed by remember(lesson.id) { mutableStateOf("1.0x") }
    var isFullscreen by remember(lesson.id) { mutableStateOf(false) }

    val youtubeId = remember(lesson.videoUrl) {
        extractYoutubeId(lesson.videoUrl)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lesson_video_player_${lesson.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Video Display Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                if (youtubeId != null) {
                    val embedUrl = "https://www.youtube.com/embed/$youtubeId?autoplay=1&controls=1&rel=0&modestbranding=1"
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
                            <style>
                                body { margin: 0; padding: 0; background-color: #000; display: flex; justify-content: center; align-items: center; height: 100vh; overflow: hidden; }
                                iframe { width: 100%; height: 100%; border: none; }
                            </style>
                        </head>
                        <body>
                            <iframe src="$embedUrl" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                        </body>
                        </html>
                    """.trimIndent()

                    AndroidView(
                        factory = { ctx ->
                            try {
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    webChromeClient = WebChromeClient()
                                    webViewClient = WebViewClient()
                                    loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                                }
                            } catch (_: Throwable) {
                                WebView(ctx)
                            }
                        },
                        update = { webView ->
                            try {
                                webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
                            } catch (_: Throwable) {}
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Generic Video Player Fallback Container
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "مشغل الفيديو",
                                tint = BrandPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = lesson.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Controls Bar
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "سرعة التشغيل",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentSpeed,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }

                    // Duration Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "مدة الدرس",
                            tint = BrandPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = lesson.duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Fullscreen indicator
                    IconButton(
                        onClick = { isFullscreen = !isFullscreen },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = "ملء الشاشة",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun extractYoutubeId(url: String): String? {
    if (url.isBlank()) return null
    return when {
        url.contains("v=") -> {
            val part = url.substringAfter("v=")
            part.substringBefore("&").substringBefore("?")
        }
        url.contains("youtu.be/") -> {
            val part = url.substringAfter("youtu.be/")
            part.substringBefore("?").substringBefore("&")
        }
        url.contains("embed/") -> {
            val part = url.substringAfter("embed/")
            part.substringBefore("?").substringBefore("&")
        }
        else -> null
    }
}
