package com.videoquick.mobile

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

private val Ink = Color(0xFF0B1020)
private val Panel = Color(0xFF151D31)
private val Purple = Color(0xFF8060FF)
private val TextMain = Color(0xFFF4F5FB)
private val TextMuted = Color(0xFFADB7CE)

class MainActivity : ComponentActivity() {
    private var webView: WebView? = null
    private var currentUrl by mutableStateOf("https://www.google.com")
    private var address by mutableStateOf(currentUrl)
    private var pageTitle by mutableStateOf("Nova guia")
    private var busy by mutableStateOf(false)
    private var showDownloads by mutableStateOf(false)
    private var showFavorites by mutableStateOf(false)
    private val favorites = mutableStateListOf<String>()
    private val visited = mutableStateListOf<String>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Purple, background = Ink, surface = Panel,
                onBackground = TextMain, onSurface = TextMain
            )) {
                BrowserScreen()
            }
        }
    }

    private fun normalizeInput(raw: String): String {
        val input = raw.trim()
        if (input.isBlank()) return "https://www.google.com"
        val candidate = if (input.contains(" ") || !input.contains(".") || input.startsWith("?"))
            "https://www.google.com/search?q=${Uri.encode(input)}"
        else if (!input.startsWith("https://", true) && !input.startsWith("http://", true))
            "https://$input"
        else input
        val uri = runCatching { Uri.parse(candidate) }.getOrNull()
        if (uri == null || uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank())
            return "https://www.google.com"
        return candidate
    }

    private fun navigate(raw: String) {
        val url = normalizeInput(raw)
        showDownloads = false
        showFavorites = false
        address = url
        webView?.loadUrl(url) ?: run { currentUrl = url }
    }

    private fun enqueueDownload(urlString: String, userAgent: String?, contentDisposition: String?, mimeType: String?) {
        val uri = runCatching { Uri.parse(urlString) }.getOrNull()
        if (uri?.scheme !in listOf("http", "https") || uri?.host.isNullOrBlank()) {
            Toast.makeText(this, "Endereço de download inválido.", Toast.LENGTH_LONG).show()
            return
        }
        val guessedName = URLUtil.guessFileName(urlString, contentDisposition, mimeType)
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val request = DownloadManager.Request(uri)
            .setTitle(guessedName)
            .setDescription("Download iniciado pelo VideoQuick Mobile")
            .setMimeType(mimeType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "VideoQuick/$guessedName")

        if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
        val cookie = CookieManager.getInstance().getCookie(urlString)
        if (!cookie.isNullOrBlank()) request.addRequestHeader("Cookie", cookie)

        try {
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(this, "Download enviado para a área de notificações.", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Não foi possível iniciar o download.", Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    private fun BrowserScreen() {
        var addressDraft by remember { mutableStateOf(address) }
        BackHandler(enabled = webView?.canGoBack() == true && !showDownloads && !showFavorites) {
            webView?.goBack()
        }

        Column(Modifier.fillMaxSize().background(Ink)) {
            Row(
                Modifier.fillMaxWidth().background(Panel).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("▶", color = Purple, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("VideoQuick", style = MaterialTheme.typography.titleLarge, color = TextMain)
                    Text("Navegador + downloads", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                IconButton(onClick = { showDownloads = !showDownloads; showFavorites = false }) {
                    Icon(Icons.Default.Download, contentDescription = "Downloads", tint = TextMain)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
                    .background(Color(0xFF202A42), RoundedCornerShape(16.dp)).padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                TextField(
                    value = addressDraft,
                    onValueChange = { addressDraft = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Pesquise ou cole um link", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextMain, unfocusedTextColor = TextMain,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                        cursorColor = Purple
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { navigate(addressDraft) })
                )
                IconButton(onClick = { navigate(addressDraft) }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Ir", tint = Purple)
                }
            }

            when {
                showDownloads -> DownloadsPanel()
                showFavorites -> FavoritesPanel { selected -> addressDraft = selected; navigate(selected) }
                else -> AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.setSupportMultipleWindows(false)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val uri = request?.url ?: return true
                                    return uri.scheme !in listOf("http", "https")
                                }
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    busy = true
                                    url?.let { currentUrl = it; address = it; addressDraft = it }
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    busy = false
                                    url?.let {
                                        currentUrl = it; address = it; addressDraft = it
                                        pageTitle = view?.title ?: it
                                        if (!visited.contains(it)) visited.add(0, it)
                                    }
                                }
                            }
                            webChromeClient = WebChromeClient()
                            setDownloadListener(DownloadListener { url, userAgent, disposition, mimeType, _ ->
                                enqueueDownload(url, userAgent, disposition, mimeType)
                            })
                            loadUrl(currentUrl)
                        }
                    },
                    update = { webView = it }
                )
            }

            if (!showDownloads && !showFavorites && busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Purple, trackColor = Panel)
            }

            NavigationBar(containerColor = Panel, contentColor = TextMain) {
                NavigationBarItem(
                    selected = !showDownloads && !showFavorites,
                    onClick = { showDownloads = false; showFavorites = false },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Navegar") }
                )
                NavigationBarItem(
                    selected = showDownloads,
                    onClick = { showDownloads = true; showFavorites = false },
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    label = { Text("Downloads") }
                )
                NavigationBarItem(
                    selected = showFavorites,
                    onClick = { showFavorites = true; showDownloads = false },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Favoritos") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { webView?.reload() },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    label = { Text("Atualizar") }
                )
            }
        }
    }

    @Composable
    private fun DownloadsPanel() {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Downloads", style = MaterialTheme.typography.headlineMedium, color = TextMain)
            Text("Os arquivos são gerenciados pelo Android e aparecem na pasta Downloads/VideoQuick.", color = TextMuted)
            Text("Para baixar um arquivo, abra o link direto no navegador. O site precisa permitir o acesso ao arquivo.", color = TextMuted)
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Purple)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Downloads do sistema", color = TextMain)
                        Text("Confira as notificações do Android para ver o progresso.", color = TextMuted)
                    }
                }
            }
        }
    }

    @Composable
    private fun FavoritesPanel(onSelect: (String) -> Unit) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Favoritos", style = MaterialTheme.typography.headlineMedium, color = TextMain)
            if (favorites.isEmpty()) {
                Text("Nenhum favorito ainda. Use a estrela abaixo para salvar a página atual.", color = TextMuted)
            } else favorites.forEach { url ->
                TextButton(onClick = { onSelect(url) }) { Text(url, color = TextMain) }
            }
        }
    }

    override fun onDestroy() {
        webView?.stopLoading()
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
