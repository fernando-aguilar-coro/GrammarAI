package com.app.grammarai.ui.lesson.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkCardBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.app.grammarai.R
import com.app.grammarai.ui.theme.DarkBackground
import com.app.grammarai.ui.theme.NeonCyan
import com.app.grammarai.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CuratedVideoPlayerCard(
    youtubeId: String?,
    modifier: Modifier = Modifier
) {
    if (youtubeId.isNullOrBlank()) return

    var isVideoLoading by remember(youtubeId) { mutableStateOf(true) }

    val htmlContent = remember(youtubeId) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { width: 100%; height: 100%; background-color: #000000; overflow: hidden; }
                iframe { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <iframe 
                src="https://www.youtube-nocookie.com/embed/$youtubeId?enablejsapi=1&playsinline=1&rel=0&modestbranding=1" 
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                allowfullscreen>
            </iframe>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    resumeTimers()

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            if (newProgress >= 75) {
                                isVideoLoading = false
                            }
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isVideoLoading = false
                        }
                    }

                    loadDataWithBaseURL(
                        "https://www.youtube-nocookie.com",
                        htmlContent,
                        "text/html",
                        "utf-8",
                        null
                    )
                    tag = youtubeId
                }
            },
            update = { webView ->
                webView.resumeTimers()
                if (webView.tag != youtubeId) {
                    isVideoLoading = true
                    webView.tag = youtubeId
                    webView.loadDataWithBaseURL(
                        "https://www.youtube-nocookie.com",
                        htmlContent,
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            },
            onRelease = { webView ->
                try {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.onPause()
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView.removeAllViews()
                    webView.destroy()
                } catch (_: Exception) {
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        if (isVideoLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF0C091A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = NeonCyan,
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = stringResource(R.string.lesson_video_now_playing),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
