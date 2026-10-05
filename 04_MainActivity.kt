package com.sankalp.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Base64
import java.util.UUID
import kotlin.math.roundToInt

private data class MediaItem(val id: String, val name: String, val uri: String, val type: String)
private enum class Page { HOME, MANTRA, AUDIO, VIDEO, IMAGES, BROWSER, SETTINGS, MORE }

private val Saffron = Color(0xFFD86B27)
private val Brown = Color(0xFF4A2A1A)
private val Cream = Color(0xFFFFF8EF)
private val Rose = Color(0xFFB94A68)

class MainActivity : ComponentActivity() {
    private var lastBack = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SankalpRoot(this) }
    }
    fun doubleBackExit() {
        val now = System.currentTimeMillis()
        if (now - lastBack < 1800) finish() else {
            lastBack = now
            android.widget.Toast.makeText(this, "Back again to exit Sankalp", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SankalpRoot(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("sankalp", Context.MODE_PRIVATE) }
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var sankalp by rememberSaveable { mutableStateOf(prefs.getString("sankalp", "आज का मेरा संकल्प — मैं शांत रहूँगा, सजग रहूँगा और अपने कर्मों में श्रद्धा रखूँगा।") ?: "") }
    var count by rememberSaveable { mutableIntStateOf(prefs.getInt("count", 0)) }
    var target by rememberSaveable { mutableIntStateOf(prefs.getInt("target", 108)) }
    var media by remember { mutableStateOf(loadMedia(prefs)) }
    var browserUrl by rememberSaveable { mutableStateOf("https://www.google.com") }
    var pickerType by remember { mutableStateOf("audio") }
    var dark by rememberSaveable { mutableStateOf(prefs.getBoolean("dark", false)) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val added = uris.map { uri ->
            try { activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            MediaItem(UUID.randomUUID().toString(), uri.lastPathSegment ?: "Sankalp Media", uri.toString(), pickerType)
        }
        val updated = media + added
        media = updated
        saveMedia(prefs, updated)
    }

    BackHandler { if (page != Page.HOME) page = Page.HOME else activity.doubleBackExit() }

    val scheme = if (dark) darkColorScheme(primary = Color(0xFFFF9B62), secondary = Color(0xFFFF9AB2), background = Color(0xFF17110E), surface = Color(0xFF241A16), onSurface = Color(0xFFFFEEE5)) else lightColorScheme(primary = Saffron, onPrimary = Color.White, secondary = Rose, background = Cream, surface = Color.White, onSurface = Brown)
    MaterialTheme(colorScheme = scheme) {
    Scaffold(
        containerColor = if (dark) Color(0xFF17110E) else Cream,
        topBar = { SankalpTopBar(page, onHome = { page = Page.HOME }) },
        bottomBar = { BottomNav(page) { page = it } }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (page) {
                Page.HOME -> HomeScreen(sankalp, { sankalp = it; prefs.edit().putString("sankalp", it).apply() }, count, target, { addCount(activity, prefs, count) { count = it } }, { count = 0; prefs.edit().putInt("count", 0).apply() }, { page = Page.MANTRA })
                Page.MANTRA -> MantraScreen(count, target, { addCount(activity, prefs, count) { count = it } }, { count = 0; prefs.edit().putInt("count", 0).apply() }, { target = it; count = 0; prefs.edit().putInt("target", it).putInt("count", 0).apply() })
                Page.AUDIO -> MediaScreen("Audio Sadhana", "audio", media, { pickerType = "audio"; picker.launch(arrayOf("audio/*")) }, { item -> val updated = media.filterNot { it.id == item.id }; media = updated; saveMedia(prefs, updated) }, activity)
                Page.VIDEO -> MediaScreen("Video Sadhana", "video", media, { pickerType = "video"; picker.launch(arrayOf("video/*")) }, { item -> val updated = media.filterNot { it.id == item.id }; media = updated; saveMedia(prefs, updated) }, activity)
                Page.IMAGES -> MediaScreen("Divya Images", "image", media, { pickerType = "image"; picker.launch(arrayOf("image/*")) }, { item -> val updated = media.filterNot { it.id == item.id }; media = updated; saveMedia(prefs, updated) }, activity)
                Page.BROWSER -> BrowserScreen(browserUrl) { browserUrl = it }
                Page.SETTINGS -> SettingsScreen(dark, { dark = it; prefs.edit().putBoolean("dark", it).apply() }, media.size, { sankalp = ""; prefs.edit().putString("sankalp", "").apply() })
                Page.MORE -> MoreScreen { page = it }
            }
        }
    }
    }
}

private fun addCount(activity: Context, prefs: android.content.SharedPreferences, old: Int, set: (Int) -> Unit) {
    val target = prefs.getInt("target", 108)
    val next = (old + 1).coerceAtMost(target)
    set(next); prefs.edit().putInt("count", next).apply()
    try {
        val v = if (android.os.Build.VERSION.SDK_INT >= 31) (activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator else activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(VibrationEffect.createOneShot(22, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SankalpTopBar(page: Page, onHome: () -> Unit) {
    TopAppBar(
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Spa, null, tint = Saffron); Spacer(Modifier.width(8.dp)); Text(if (page == Page.HOME) "Sankalp" else page.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold) } },
        navigationIcon = { if (page != Page.HOME) IconButton(onClick = onHome) { Icon(Icons.Default.ArrowBack, "Back") } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable private fun BottomNav(page: Page, set: (Page) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        val items = listOf(Page.HOME to (Icons.Default.Home to "Home"), Page.MANTRA to (Icons.Default.Favorite to "Mantra"), Page.AUDIO to (Icons.Default.MusicNote to "Audio"), Page.IMAGES to (Icons.Default.Image to "Images"), Page.MORE to (Icons.Default.MoreHoriz to "More"))
        items.forEach { (p, pair) -> NavigationBarItem(selected = page == p, onClick = { set(p) }, icon = { Icon(pair.first, null) }, label = { Text(pair.second) }) }
    }
}

@Composable private fun MoreScreen(set: (Page) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("More", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Text("Sankalp के बाकी साधना tools", color = Color.Gray) }
        item { MoreCard(Icons.Default.PlayCircle, "Video Sadhana", "भक्ति और साधना videos खोलें") { set(Page.VIDEO) } }
        item { MoreCard(Icons.Default.Public, "Google / Browser", "बिना app छोड़े web पर खोजें") { set(Page.BROWSER) } }
        item { MoreCard(Icons.Default.Settings, "Settings", "Theme और app preferences") { set(Page.SETTINGS) } }
    }
}

@Composable private fun MoreCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, click: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = click), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFFFEEE4)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Saffron) }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(subtitle, color = Color.Gray) }; Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

@Composable private fun HomeScreen(sankalp: String, setSankalp: (String) -> Unit, count: Int, target: Int, tap: () -> Unit, reset: () -> Unit, mantra: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Brown)) {
                Column(Modifier.padding(22.dp)) {
                    Text("शुभ प्रभात", color = Color(0xFFFFD6A8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp)); Text("आज का संकल्प", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(10.dp)); Text("अपने मन को एक दिशा दें। एक छोटा संकल्प, एक सुंदर दिन।", color = Color(0xFFFFEDE0))
                }
            }
        }
        item {
            Text("मेरा आज का संकल्प", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            OutlinedTextField(value = sankalp, onValueChange = setSankalp, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), minLines = 3, placeholder = { Text("आज मैं क्या संकल्प ले रहा हूँ?") })
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("मंत्र साधना", fontWeight = FontWeight.Bold, fontSize = 19.sp); Text("$count / $target", color = Saffron, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold) }
                        Box(Modifier.size(72.dp).clip(CircleShape).background(Saffron), contentAlignment = Alignment.Center) { Text("ॐ", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(14.dp)); LinearProgressIndicator(progress = { if (target == 0) 0f else count.toFloat() / target }, modifier = Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(9.dp)))
                    Spacer(Modifier.height(14.dp)); Button(onClick = tap, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) { Icon(Icons.Default.TouchApp, null); Spacer(Modifier.width(8.dp)); Text("मंत्र जप करें") }
                    TextButton(onClick = reset, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Reset") }
                }
            }
        }
        item { OutlinedButton(onClick = mantra, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("पूरी मंत्र साधना खोलें") } }
        item { Text("साधना के लिए", fontWeight = FontWeight.Bold, fontSize = 19.sp) }
        item { QuickCards() }
    }
}

@Composable private fun QuickCards() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("🔔" to "शांति", "🪷" to "ध्यान", "✨" to "श्रद्धा").forEach { (emoji, text) -> Card(Modifier.weight(1f), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(emoji, fontSize = 26.sp); Text(text, fontWeight = FontWeight.SemiBold) } } }
    }
}

@Composable private fun MantraScreen(count: Int, target: Int, tap: () -> Unit, reset: () -> Unit, setTarget: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(10.dp)); Text("मंत्र साधना", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text("हर स्पर्श में एकाग्रता", color = Color.Gray); Spacer(Modifier.height(30.dp))
        Box(Modifier.size(230.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("ॐ", fontSize = 68.sp, color = Saffron, fontWeight = FontWeight.Bold); Text("$count", fontSize = 46.sp, fontWeight = FontWeight.ExtraBold); Text("of $target", color = Color.Gray) }
        }
        Spacer(Modifier.height(24.dp)); LinearProgressIndicator(progress = { if (target == 0) 0f else count.toFloat() / target }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape))
        Spacer(Modifier.height(22.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf(108, 1001).forEach { n -> FilterChip(selected = target == n, onClick = { setTarget(n) }, label = { Text("$n जप") }) } }
        Spacer(Modifier.height(20.dp)); Button(onClick = tap, modifier = Modifier.fillMaxWidth().height(110.dp), shape = RoundedCornerShape(32.dp)) { Text("ॐ  TAP", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold) }
        TextButton(onClick = reset) { Text("साधना फिर से शुरू करें") }
    }
}

@Composable private fun MediaScreen(title: String, type: String, media: List<MediaItem>, add: () -> Unit, delete: (MediaItem) -> Unit, ctx: Context) {
    val list = media.filter { it.type == type }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); FilledTonalButton(onClick = add) { Icon(Icons.Default.Add, null); Text("Add") } }
        Spacer(Modifier.height(6.dp)); Text("आपकी निजी साधना सामग्री", color = Color.Gray); Spacer(Modifier.height(14.dp))
        if (list.isEmpty()) EmptyMedia(type, add) else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(list, key = { it.id }) { item -> MediaRow(item, delete, ctx) } }
    }
}

@Composable private fun EmptyMedia(type: String, add: () -> Unit) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(if (type == "audio") Icons.Default.MusicNote else if (type == "video") Icons.Default.PlayCircle else Icons.Default.Image, null, modifier = Modifier.size(52.dp), tint = Saffron); Spacer(Modifier.height(12.dp)); Text("अभी कुछ नहीं है", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("अपने फोन से सामग्री जोड़ें और यहीं से खोलें।", color = Color.Gray); Spacer(Modifier.height(14.dp)); Button(onClick = add) { Text("सामग्री जोड़ें") } } } }

@Composable private fun MediaRow(item: MediaItem, delete: (MediaItem) -> Unit, ctx: Context) {
    Card(shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (item.type == "image") AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageURI(Uri.parse(item.uri)) } }, modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp))) else Box(Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFEEE4)), contentAlignment = Alignment.Center) { Icon(if (item.type == "audio") Icons.Default.MusicNote else Icons.Default.PlayArrow, null, tint = Saffron) }
        Spacer(Modifier.width(12.dp)); Text(item.name, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.uri))) } catch (_: Exception) {} }) { Icon(Icons.Default.PlayArrow, "Open") }
        IconButton(onClick = { delete(item) }) { Icon(Icons.Default.DeleteOutline, "Delete") }
    } }
}

@Composable private fun BrowserScreen(url: String, setUrl: (String) -> Unit) {
    var text by remember(url) { mutableStateOf(url) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(text, { text = it }, Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(16.dp), placeholder = { Text("Search Google") }); Spacer(Modifier.width(8.dp)); IconButton(onClick = { setUrl(if (text.startsWith("http")) text else "https://www.google.com/search?q=${Uri.encode(text)}") }) { Icon(Icons.Default.Search, "Search") } }
        AndroidView(factory = { c -> WebView(c).apply { webViewClient = WebViewClient(); settings.javaScriptEnabled = true; settings.domStorageEnabled = true; loadUrl(url) } }, update = { it.loadUrl(url) }, modifier = Modifier.fillMaxSize())
    }
}

@Composable private fun SettingsScreen(dark: Boolean, setDark: (Boolean) -> Unit, mediaCount: Int, clearSankalp: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold); Text("Sankalp को अपने तरीके से रखें", color = Color.Gray) }
        item { SettingRow(Icons.Default.DarkMode, "Dark mode", "आंखों के लिए शांत थीम", dark, setDark) }
        item { Card(shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text("आपका डेटा", fontWeight = FontWeight.Bold, fontSize = 19.sp); Text("$mediaCount media items saved locally", color = Color.Gray); Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = clearSankalp) { Text("Daily Sankalp साफ करें") } } } }
        item { Card(shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text("Sankalp", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("Version 2.0.0", color = Color.Gray); Spacer(Modifier.height(8.dp)); Text("एक सरल, सुंदर और निजी साधना साथी।", color = Brown) } } }
    }
}

@Composable private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, set: (Boolean) -> Unit) { Card(shape = RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Saffron); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray, fontSize = 13.sp) }; Switch(checked, set) } } }

private fun loadMedia(p: android.content.SharedPreferences): List<MediaItem> = p.getStringSet("media", emptySet()).orEmpty().mapNotNull { raw -> val a = raw.split("~", limit = 4); if (a.size == 4) try { MediaItem(a[0], String(Base64.getDecoder().decode(a[1])), String(Base64.getDecoder().decode(a[2])), a[3]) } catch (_: Exception) { null } else null }
private fun saveMedia(p: android.content.SharedPreferences, items: List<MediaItem>) { p.edit().putStringSet("media", items.map { "${it.id}~${Base64.getEncoder().encodeToString(it.name.toByteArray())}~${Base64.getEncoder().encodeToString(it.uri.toByteArray())}~${it.type}" }.toSet()).apply() }
