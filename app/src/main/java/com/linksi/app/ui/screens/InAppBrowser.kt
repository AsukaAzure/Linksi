package com.linksi.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppBrowser(
    url: String,
    title: String = "",
    onScrollChanged: (Int) -> Unit = {},
    onDrag: (Float) -> Unit = {},      // add
    onDragEnd: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var currentUrl by remember { mutableStateOf(url) }
    var currentTitle by remember { mutableStateOf(title) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    BackHandler {
        if (canGoBack) webView?.goBack()
        else onDismiss()
    }

    Scaffold(
        topBar = {
            Column {
                // Drag handle only at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { onDragEnd() },
                                onVerticalDrag = { _, dragAmount ->
                                    onDrag(dragAmount)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp),
                        shape = RoundedCornerShape(2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    ) {}
                }

                // Progress bar
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Back
                    IconButton(
                        onClick = { webView?.goBack() },
                        enabled = canGoBack
                    ) {
                        Icon(Icons.Outlined.ArrowBack, "Back")
                    }

                    // Forward
                    IconButton(
                        onClick = { webView?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.Outlined.ArrowForward, "Forward")
                    }

                    // URL bar in center
                    val context = LocalContext.current

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp)) // match card shape
                            .clickable {
                                // Open current page in external browser
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                context.startActivity(intent)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Lock, null,
                                Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                currentUrl
                                    .removePrefix("https://")
                                    .removePrefix("http://")
                                    .removePrefix("www.")
                                    .substringBefore("/"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Outlined.OpenInBrowser, null,
                                Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Reload / Stop
                    IconButton(onClick = {
                        if (isLoading) webView?.stopLoading()
                        else webView?.reload()
                    }) {
                        Icon(
                            if (isLoading) Icons.Outlined.Close
                            else Icons.Outlined.Refresh,
                            "Refresh"
                        )
                    }

                    // Close browser
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, "Close")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        WebViewContent(
            url = url,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onWebViewCreated = { webView = it },
            onPageStarted = { pageUrl, pageTitle ->
                currentUrl = pageUrl
                currentTitle = pageTitle ?: ""
                isLoading = true
            },
            onPageFinished = { pageUrl, pageTitle ->
                currentUrl = pageUrl
                currentTitle = pageTitle ?: ""
                isLoading = false
                canGoBack = webView?.canGoBack() ?: false
                canGoForward = webView?.canGoForward() ?: false
            },
            onProgressChanged = { progress = it },
            onScrollChanged = onScrollChanged
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContent(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: (String, String?) -> Unit,
    onPageFinished: (String, String?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onScrollChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                }
                // Override scroll change to report position
                setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    onScrollChanged(scrollY)
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        onPageStarted(url, view.title)
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        onPageFinished(url, view.title)
                    }
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val url = request.url.toString()
                        val scheme = request.url.scheme ?: ""

                        return when {
                            // Normal web URLs — let WebView handle
                            scheme == "http" || scheme == "https" -> {
                                view.loadUrl(url)
                                true
                            }
                            // Special schemes — open in external app
                            scheme == "intent" -> {
                                try {
                                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                    // Try to open the app
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        // App not installed — try fallback URL
                                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                        if (fallbackUrl != null) {
                                            view.loadUrl(fallbackUrl)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Can't parse intent — ignore
                                }
                                true
                            }
                            // Maps, geo, tel, mailto etc — open in external app
                            scheme in listOf("geo", "maps", "tel", "mailto", "market", "whatsapp") -> {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // No app to handle it
                                }
                                true
                            }
                            // Anything else unknown — try external app
                            else -> {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    }
                                } catch (e: Exception) { }
                                true
                            }
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        onProgressChanged(newProgress)
                    }
                    override fun onReceivedTitle(view: WebView, title: String) {
                        onPageFinished(view.url ?: url, title)
                    }
                }
                loadUrl(url)
                onWebViewCreated(this)
            }
        },
        modifier = modifier
    )
}