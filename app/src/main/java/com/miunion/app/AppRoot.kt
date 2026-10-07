package com.miunion.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.menu.OverlayDropdownMenu
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class UniAccount(
    val platform: String,
    val name: String,
    val uid: String,
    val meta: String,
    val expiresAt: Long = 0L,
    val phone: String = "",
    val remark: String = "",
)

/** 按 sortOrder 展示；旧数据缺失时按当前序补齐。 */
fun orderedSessions(sessions: List<AuthSession>): List<AuthSession> =
    sessions.mapIndexed { i, s -> if (s.sortOrder < 0) s.copy(sortOrder = i) else s }
        .sortedBy { it.sortOrder }

/** 手机号掩码：12345678945 → 123******45（前3后2，中间打星）。 */
fun maskPhone(phone: String): String =
    if (phone.length >= 7) {
        phone.take(3) + "*".repeat(phone.length - 5) + phone.takeLast(2)
    } else {
        "*".repeat(phone.length)
    }

/** 备注长度：中文/全角按 2 计、其余按 1 计，上限 10。 */
fun remarkLength(s: String): Int =
    s.sumOf { c ->
        when (c.code) {
            in 0x2E80..0x9FFF, in 0xF900..0xFAFF, in 0xFF00..0xFFEF, in 0x3000..0x303F -> 2
            else -> 1
        }
    }

data class UniMessage(
    val title: String,
    val time: String,
    val desc: String,
    /** 稳定 id（同步记录文件名）；历史数据加载时回填。 */
    val id: String = "",
)

private const val PREF_NAME = "uni_pass"
private const val KEY_ACCOUNTS = "accounts"
private const val PREF_MESSAGES = "uni_messages"
private const val KEY_MESSAGES = "messages"

class AccountStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun load(): List<UniAccount> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                UniAccount(
                    platform = o.getString("platform"),
                    name = o.getString("name"),
                    uid = o.getString("uid"),
                    meta = o.getString("meta"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(accounts: List<UniAccount>) {
        val arr = JSONArray()
        accounts.forEach { a ->
            arr.put(
                org.json.JSONObject()
                    .put("platform", a.platform)
                    .put("name", a.name)
                    .put("uid", a.uid)
                    .put("meta", a.meta)
            )
        }
        prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply()
    }
}

class MessageStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_MESSAGES, Context.MODE_PRIVATE)

    fun load(): List<UniMessage> {
        val raw = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                UniMessage(
                    title = o.getString("title"),
                    time = o.getString("time"),
                    desc = o.getString("desc"),
                    id = o.optString("id"),
                )
            }
            if (list.any { it.id.isBlank() }) {
                val fixed = list.map {
                    if (it.id.isBlank()) it.copy(id = java.util.UUID.randomUUID().toString()) else it
                }
                save(fixed)
                fixed
            } else {
                list
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(messages: List<UniMessage>) {
        val arr = JSONArray()
        messages.forEach { m ->
            arr.put(
                org.json.JSONObject()
                    .put("id", m.id)
                    .put("title", m.title)
                    .put("time", m.time)
                    .put("desc", m.desc)
            )
        }
        prefs.edit().putString(KEY_MESSAGES, arr.toString()).apply()
    }
}

private fun nowText(): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date())

private fun dateText(ts: Long): String =
    if (ts > 0) SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(ts)) else "未设置"

private fun accountName(session: AuthSession): String =
    session.nickname.ifBlank { "未命名账号" }

private fun platformLabel(platform: String): String = when (platform) {
    "skland" -> "森空岛"
    "kuro" -> "库街区"
    else -> "米游社"
}

private fun platformColor(platform: String): Color = when (platform) {
    "skland" -> Color(0xFF17B26A)
    "kuro" -> Color(0xFF00C8C0)
    else -> Color(0xFF3482FF)
}

private fun detectQrPlatform(raw: String): String? = when {
    raw.contains("KURO_") || raw.contains("G152#") -> "kuro"
    raw.contains("hypergryph://") || raw.contains("scanId=") -> "skland"
    raw.contains("mihoyo.com") || raw.contains("miyoushe.com") ||
        raw.contains("ticket=") || raw.contains("token_types") -> "mihoyo"
    else -> null
}

private fun messagePlatform(desc: String): String =
    if (desc.contains("库街区")) "kuro"
    else if (desc.contains("森空岛")) "skland"
    else if (desc.contains("米游社")) "mihoyo"
    else ""

private fun accountFromSession(session: AuthSession): UniAccount =
    UniAccount(
        platform = session.platform,
        name = accountName(session),
        uid = session.uid,
        meta = platformLabel(session.platform) + " · 长期有效",
        expiresAt = session.expiresAt,
        phone = session.phone,
        remark = session.remark,
    )

@Composable
fun AppContent() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("visual_prefs", Context.MODE_PRIVATE) }
    val systemDark = isSystemInDarkTheme()
    var themeMode by remember {
        mutableStateOf(
            ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme_mode", "SYSTEM") }
                ?: ThemeMode.SYSTEM
        )
    }
    var glassEnabled by remember { mutableStateOf(prefs.getBoolean("glass_enabled", false)) }
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val sessions = remember { AuthStore.load(context).toMutableStateList() }
    val messageStore = remember { MessageStore(context) }
    val messages = remember { messageStore.load().toMutableStateList() }
    val view = LocalView.current
    val window = (context as? Activity)?.window
    SideEffect {
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var unlocked by remember {
        mutableStateOf(!prefs.getBoolean("require_biometric", false))
    }

    MiuixTheme(
        colors = if (dark) darkColorScheme() else lightColorScheme()
    ) {
        if (!unlocked) {
            BiometricGate(onUnlocked = { unlocked = true })
        } else {
            AppRoot(
                themeMode = themeMode,
                onThemeModeChange = {
                    themeMode = it
                    prefs.edit().putString("theme_mode", it.name).apply()
                },
                glassEnabled = glassEnabled,
                onGlassEnabledChange = {
                    glassEnabled = it
                    prefs.edit().putBoolean("glass_enabled", it).apply()
                },
                sessions = sessions,
                context = context,
                messages = messages,
                addMessage = { title, desc ->
                    messages.add(0, UniMessage(title, nowText(), desc, java.util.UUID.randomUUID().toString()))
                    messageStore.save(messages.toList())
                },
                replaceMessages = { list ->
                    messages.clear()
                    messages.addAll(list)
                    messageStore.save(list)
                },
            )
        }
    }
}

/**
 * 打开时指纹验证锁：未解锁前只显示锁屏层，验证通过或无法验证（未录入）时放行。
 */
@Composable
private fun BiometricGate(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var attempt by remember { mutableStateOf(0) }

    fun authenticate() {
        if (activity == null) {
            onUnlocked()
            return
        }
        val manager = BiometricManager.from(activity)
        val can = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            // 未录入指纹或锁屏密码：无法验证时放行，避免把用户锁死
            onUnlocked()
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        // 用户主动取消：留在锁屏层，可重新验证或退出
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        -> Unit
                        else -> onUnlocked()
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("验证解锁聚合通行证")
            .setSubtitle("使用指纹或锁屏密码继续")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        try {
            prompt.authenticate(info)
        } catch (e: Exception) {
            onUnlocked()
        }
    }

    LaunchedEffect(attempt) { authenticate() }

    BackHandler {
        (context as? Activity)?.finish()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
                Text(
                    text = "聚合通行证已锁定",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "验证指纹后进入应用",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        text = "重新验证",
                        onClick = { attempt++ },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                    TextButton(
                        text = "退出应用",
                        onClick = { (context as? Activity)?.finish() },
                    )
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    glassEnabled: Boolean,
    onGlassEnabledChange: (Boolean) -> Unit,
    sessions: androidx.compose.runtime.snapshots.SnapshotStateList<AuthSession>,
    context: android.content.Context,
    messages: androidx.compose.runtime.snapshots.SnapshotStateList<UniMessage>,
    addMessage: (String, String) -> Unit,
    replaceMessages: (List<UniMessage>) -> Unit,
) {
    var accounts by remember { mutableStateOf(orderedSessions(sessions).map(::accountFromSession)) }
    var adding by remember { mutableStateOf(false) }
    var scanPlatform by remember { mutableStateOf("mihoyo") }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        sessions.toList().forEach { s ->
            if (s.nickname.isBlank() || s.expiresAt > 0) {
                val fresh = AuthApi.refreshNickname(s)
                val index = sessions.indexOf(s)
                if (index >= 0) {
                    sessions[index] = fresh
                    AuthStore.save(context, sessions.toList())
                }
            }
        }
    }

    LaunchedEffect(sessions.toList()) {
        accounts = orderedSessions(sessions).map(::accountFromSession)
    }

    fun showToast(msg: String) {
        toast = msg
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2000)
            toast = null
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            BackHandler(enabled = adding) {
                adding = false
            }

            // 有云同步能力时首屏必须是主页面（4 Tab）：账号为空也要能进「账号与同步」恢复数据，
            // 登录页仅作为「添加账号」二级页（state=1）进入
            val rootTarget = if (adding) 1 else 2
            AnimatedContent(
                targetState = rootTarget,
                transitionSpec = {
                    when {
                        // 从二级登录页返回：该页向右滑出，来页从左滑入
                        initialState == 1 && targetState != 1 ->
                            (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(280)) { it } + fadeOut(tween(200)))
                        // 进入添加账号登录页：从右往左滑入
                        targetState == 1 ->
                            (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(280))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(280)) { -it / 4 } + fadeOut(tween(200)))
                        else ->
                            fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                    }
                },
                label = "root_nav",
            ) { state ->
                if (state <= 1) {
                    // 同样包空栏 Scaffold：登录页的极验 OverlayDialog 需要宿主才可见
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                        topBar = {},
                        bottomBar = {},
                    ) { _ ->
                        LoginScreen(
                        onLoggedIn = { session ->
                            // 一平台多账号：同 uid 覆盖，不同 uid 追加
                            sessions.removeAll {
                                it.platform == session.platform && it.uid == session.uid
                            }
                            sessions.add(session)
                            AuthStore.save(context, sessions.toList())
                            addMessage(
                                "账号登录成功",
                                "${platformLabel(session.platform)}账号「${accountName(session)}」已通过手机验证码登录，凭证已保存在本机，长期有效。"
                            )
                            LoginNotifier.notify(
                                context,
                                session.platform,
                                accountName(session),
                                success = true,
                            )
                            adding = false
                            showToast("已登录${platformLabel(session.platform)}账号，凭证已保存")
                        },
                        onLoginFailed = { platform, rawName, reason ->
                            val masked = if (rawName.length == 11) {
                                rawName.take(3) + "****" + rawName.takeLast(4)
                            } else {
                                rawName.ifBlank { "账号" }
                            }
                            addMessage(
                                "登录失败",
                                "[${platformLabel(platform)}]账号登录失败：$reason"
                            )
                            LoginNotifier.notify(
                                context,
                                platform,
                                masked,
                                success = false,
                                reason = reason,
                            )
                        },
                        onToast = ::showToast,
                        onBack = {
                            adding = false
                        },
                        )
                    }
                } else {
                    MainScreen(
                        accounts = accounts,
                        scanPlatform = scanPlatform,
                        onScanPlatformChange = { scanPlatform = it },
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        glassEnabled = glassEnabled,
                        onGlassEnabledChange = onGlassEnabledChange,
                        onToast = ::showToast,
                        onAddAccount = { adding = true },
                        onLogout = { platform, uid ->
                            if (platform == null) {
                                accounts = emptyList()
                                sessions.clear()
                                AuthStore.save(context, emptyList())
                                scanPlatform = "mihoyo"
                            } else {
                                sessions.removeAll {
                                    it.platform == platform && (uid == null || it.uid == uid)
                                }
                                AuthStore.save(context, sessions.toList())
                                accounts = if (uid == null) {
                                    accounts.filterNot { it.platform == platform }
                                } else {
                                    accounts.filterNot { it.platform == platform && it.uid == uid }
                                }
                                if (accounts.isEmpty()) scanPlatform = "mihoyo"
                            }
                        },
                        onSyncApplied = { syncedSessions, syncedMessages ->
                            sessions.clear()
                            sessions.addAll(syncedSessions)
                            AuthStore.save(context, sessions.toList())
                            replaceMessages(syncedMessages)
                        },
                        onReorderSessions = { reordered ->
                            sessions.clear()
                            sessions.addAll(reordered)
                            AuthStore.save(context, sessions.toList())
                        },
                        onUpdatePhone = { platform, uid, phone ->
                            val i = sessions.indexOfFirst {
                                it.platform == platform && it.uid == uid
                            }
                            if (i >= 0) {
                                sessions[i] = sessions[i].copy(phone = phone)
                                AuthStore.save(context, sessions.toList())
                            }
                        },
                        onUpdateRemark = { platform, uid, remark ->
                            val i = sessions.indexOfFirst {
                                it.platform == platform && it.uid == uid
                            }
                            if (i >= 0) {
                                sessions[i] = sessions[i].copy(remark = remark)
                                AuthStore.save(context, sessions.toList())
                            }
                        },
                        messages = messages,
                        sessions = sessions,
                        onMessage = addMessage,
                    )
                }
            }

            AnimatedVisibility(
                visible = toast != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xE6222222), RoundedCornerShape(50))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = toast.orEmpty(),
                            color = Color.White,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

/** 主页之上的二级页面。 */
private sealed class SubPage {
    object Visual : SubPage()
    object Sync : SubPage()
    object About : SubPage()
    data class Msg(val message: UniMessage, val platform: String) : SubPage()
    data class GameDetail(val row: GameRoleRow, val session: AuthSession?) : SubPage()
}

@Composable
private fun MainScreen(
    accounts: List<UniAccount>,
    scanPlatform: String,
    onScanPlatformChange: (String) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    glassEnabled: Boolean,
    onGlassEnabledChange: (Boolean) -> Unit,
    onToast: (String) -> Unit,
    onAddAccount: () -> Unit,
    onLogout: (String?, String?) -> Unit,
    messages: List<UniMessage>,
    sessions: List<AuthSession>,
    onMessage: (String, String) -> Unit,
    onSyncApplied: (List<AuthSession>, List<UniMessage>) -> Unit,
    onReorderSessions: (List<AuthSession>) -> Unit,
    onUpdatePhone: (String, String, String) -> Unit,
    onUpdateRemark: (String, String, String) -> Unit,
) {
    var subPage by remember { mutableStateOf<SubPage?>(null) }
    // 账号卡展开状态上提到主页层：进游戏详情等二级页再返回时保持展开
    var expandedAccountKeys by remember { mutableStateOf(setOf<String>()) }
    // 手机号显示开关（右上角眼睛）：仅本次会话有效，不记忆，重启恢复为隐藏
    var showPhone by remember { mutableStateOf(false) }
    // 排序模式上提主页层：排序期间锁定底栏/横滑/眼睛等页面切换
    var reorderMode by remember { mutableStateOf(false) }
    var workingOrder by remember { mutableStateOf<List<UniAccount>?>(null) }
    // 角色信息刷新（标题栏刷新按钮 → AccountsPage 重新拉取并覆盖缓存）
    var rolesRefreshTick by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = subPage != null) {
        subPage = null
    }

    val scrollBehavior = MiuixScrollBehavior(state = rememberTopAppBarState())
    val pageTitle = when (pagerState.currentPage) {
        0 -> "账号库"
        1 -> "扫码登录"
        2 -> "消息"
        else -> "设置"
    }
    // 仅当扫码页停稳（settled）、处于主页层且应用前台 RESUMED 时才允许启动相机
    val scanActive = subPage == null &&
        pagerState.settledPage == 1 &&
        lifecycleState == Lifecycle.State.RESUMED

    AnimatedContent(
        targetState = subPage,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val push = if (targetState == null) {
                // 返回：二级页整屏向右滑出，主页从左滑入（刚性水平推拉）
                (slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(tween(180)))
            } else {
                // 进入二级页面：从右往左滑入并渐显，主页整屏向左滑出
                (slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(tween(180)))
            }
            push
        },
        label = "sub_page",
    ) { current ->
        if (current == null) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                topBar = {
                    TopAppBar(
                        title = pageTitle,
                        largeTitle = pageTitle,
                        scrollBehavior = scrollBehavior,
                        actions = {
                            // 刷新角色信息（仅账号库页显示）
                            if (pagerState.currentPage == 0) {
                                IconButton(
                                    onClick = {
                                        if (!reorderMode) {
                                            rolesRefreshTick++
                                            onToast("正在刷新角色信息")
                                        }
                                    },
                                    modifier = Modifier.graphicsLayer {
                                        translationY = (1f - scrollBehavior.state.collapsedFraction) *
                                            52.dp.toPx()
                                    },
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Refresh,
                                        contentDescription = "刷新角色信息",
                                    )
                                }
                            }
                            // 眼睛跟随标题：展开（collapsedFraction=0）时下移到大标题行，
                            // 收起（=1）回小标题行，两种状态都与标题对齐
                            val titleShift = (1f - scrollBehavior.state.collapsedFraction)
                            IconButton(
                                onClick = { if (!reorderMode) showPhone = !showPhone },
                                modifier = Modifier.graphicsLayer {
                                    translationY = titleShift * 52.dp.toPx()
                                },
                            ) {
                                Icon(
                                    imageVector = if (showPhone) MiuixIcons.Show else MiuixIcons.Hide,
                                    contentDescription = if (showPhone) "隐藏手机号" else "显示手机号",
                                )
                            }
                        },
                    )
                },
                bottomBar = {
                    if (glassEnabled) {
                        FloatingNavigationBar(
                            shadowElevation = 8.dp,
                        ) {
                            listOf("账号库", "扫码登录", "消息", "设置").forEachIndexed { index, label ->
                                FloatingNavigationBarItem(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        if (!reorderMode) {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                    },
                                    icon = listOf(
                                        MiuixIcons.Community,
                                        MiuixIcons.Scan,
                                        MiuixIcons.Messages,
                                        MiuixIcons.Settings,
                                    )[index],
                                    label = label,
                                )
                            }
                        }
                    } else {
                        MainBottomBar(
                            selected = pagerState.currentPage,
                            onSelect = {
                                if (!reorderMode) {
                                    scope.launch { pagerState.animateScrollToPage(it) }
                                }
                            },
                        )
                    }
                },
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = !reorderMode,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                    ) { page ->
                        when (page) {
                            0 -> AccountsPage(
                                accounts = accounts,
                                sessions = sessions,
                                onAdd = onAddAccount,
                                nestedScroll = scrollBehavior.nestedScrollConnection,
                                onOpenGameDetail = { account, row ->
                                    subPage = SubPage.GameDetail(
                                        row,
                                        sessions.find {
                                            it.platform == account.platform && it.uid == account.uid
                                        },
                                    )
                                },
                                expandedKeys = expandedAccountKeys,
                                onToggleExpand = { key ->
                                    expandedAccountKeys = if (key in expandedAccountKeys) {
                                        expandedAccountKeys - key
                                    } else {
                                        expandedAccountKeys + key
                                    }
                                },
                                onReorder = onReorderSessions,
                                showPhone = showPhone,
                                reorderMode = reorderMode,
                                workingOrder = workingOrder,
                                onEnterReorder = {
                                    reorderMode = true
                                    workingOrder = accounts
                                    expandedAccountKeys = emptySet()
                                },
                                onDragReorder = { order -> workingOrder = order },
                                onExitReorder = {
                                    reorderMode = false
                                    workingOrder = null
                                },
                                onUpdateRemark = onUpdateRemark,
                                rolesRefreshTick = rolesRefreshTick,
                            )
                            1 -> ScanPage(
                                accounts = accounts,
                                sessions = sessions,
                                scanPlatform = scanPlatform,
                                onScanPlatformChange = onScanPlatformChange,
                                onToast = onToast,
                                onMessage = onMessage,
                                active = scanActive,
                                nestedScroll = scrollBehavior.nestedScrollConnection,
                            )
                            2 -> MessagesPage(
                                accounts = accounts,
                                messages = messages,
                                nestedScroll = scrollBehavior.nestedScrollConnection,
                                onOpenMessage = { msg ->
                                    subPage = SubPage.Msg(msg, messagePlatform(msg.desc))
                                },
                            )
                            else -> SettingsPage(
                                accounts = accounts,
                                onOpenVisual = { subPage = SubPage.Visual },
                                onOpenSync = { subPage = SubPage.Sync },
                                onOpenAbout = { subPage = SubPage.About },
                                onToast = onToast,
                                onLogout = onLogout,
                                nestedScroll = scrollBehavior.nestedScrollConnection,
                            )
                        }
                    }

                    TopFadeScrim(
                        scrollBehavior = scrollBehavior,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(paddingValues),
                    )

                    // 旧版（明文存储）升级提示：仅弹一次，确认=迁移加密，取消=保持明文
                    val mainContext = LocalContext.current
                    var showKsUpgrade by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        val prefs = mainContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                        if (!prefs.contains("ks_migration") &&
                            SyncStore.passwordMode(mainContext) == "plain"
                        ) {
                            showKsUpgrade = true
                        }
                    }
                    if (showKsUpgrade) {
                        ConfirmDialog(
                            show = true,
                            title = "升级为加密存储",
                            summary = "检测到云同步密钥当前以明文存储在本机。升级后将改用系统级加密（Keystore）保存，同步功能与密钥本身不受影响。",
                            confirmText = "升级",
                            onConfirm = {
                                showKsUpgrade = false
                                mainContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                                    .edit().putString("ks_migration", "1").apply()
                                val pwd = SyncStore.password(mainContext)
                                if (pwd.isNotBlank()) {
                                    try {
                                        SyncStore.setPasswordKs(mainContext, pwd)
                                        onToast("密钥已升级为加密存储")
                                    } catch (e: Exception) {
                                        onToast("升级失败，仍为明文存储")
                                    }
                                }
                            },
                            onDismiss = {
                                showKsUpgrade = false
                                mainContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                                    .edit().putString("ks_migration", "1").apply()
                            },
                        )
                    }
                }
            }
        } else {
            // 二级页面：自带小标题栏，整屏作为刚性单元参与水平转场。
            // 外面包一层空栏 Scaffold：miuix 的弹窗/浮层宿主（MiuixPopupHost）只在
            // Scaffold 内挂载，否则本页的 OverlayDialog 状态注册后无人渲染（弹窗看不见）。
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                topBar = {},
                bottomBar = {},
            ) { _ ->
                Column(modifier = Modifier.fillMaxSize()) {
                    SmallTopAppBar(
                        title = when (current) {
                            SubPage.Visual -> "视觉"
                            SubPage.Sync -> "账号与同步"
                            SubPage.About -> "关于"
                            is SubPage.Msg -> "消息详情"
                            is SubPage.GameDetail -> current.row.gameName
                        },
                        navigationIcon = {
                            IconButton(onClick = { subPage = null }) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = "返回",
                                )
                            }
                        },
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        when (current) {
                            SubPage.Visual -> VisualPage(
                                themeMode = themeMode,
                                onThemeModeChange = onThemeModeChange,
                                glassEnabled = glassEnabled,
                                onGlassEnabledChange = onGlassEnabledChange,
                                nestedScroll = scrollBehavior.nestedScrollConnection,
                            )
                            SubPage.Sync -> SyncPage(
                                sessions = orderedSessions(sessions),
                                messages = messages,
                                onApplied = onSyncApplied,
                                onLogout = { platform, uid -> onLogout(platform, uid) },
                                onUpdatePhone = onUpdatePhone,
                                onToast = onToast,
                                nestedScroll = scrollBehavior.nestedScrollConnection,
                            )
                            SubPage.About -> AboutPage()
                            is SubPage.Msg -> MessageDetailPage(
                                message = current.message,
                                platform = current.platform,
                                nestedScroll = scrollBehavior.nestedScrollConnection,
                            )
                            is SubPage.GameDetail -> GameDetailPage(
                                row = current.row,
                                session = current.session,
                                nestedScroll = scrollBehavior.nestedScrollConnection,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopFadeScrim(
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val surface = MiuixTheme.colorScheme.surface
    Box(
        modifier = modifier
            .height(28.dp)
            .drawBehind {
                val fraction = scrollBehavior.state.collapsedFraction
                if (fraction > 0.01f) {
                    val alpha = (fraction * 2f).coerceIn(0f, 1f)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                surface.copy(alpha = alpha),
                                surface.copy(alpha = 0f),
                            ),
                        ),
                    )
                }
            }
    )
}

@Composable
private fun MainBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val labels = listOf("账号库", "扫码登录", "消息", "设置")
    val icons = listOf(
        MiuixIcons.Community,
        MiuixIcons.Scan,
        MiuixIcons.Messages,
        MiuixIcons.Settings,
    )
    NavigationBar {
        labels.forEachIndexed { index, label ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = icons[index],
                label = label,
            )
        }
    }
}

@Composable
private fun AccountsPage(
    accounts: List<UniAccount>,
    sessions: List<AuthSession>,
    onAdd: () -> Unit,
    nestedScroll: NestedScrollConnection,
    onOpenGameDetail: (UniAccount, GameRoleRow) -> Unit,
    expandedKeys: Set<String>,
    onToggleExpand: (String) -> Unit,
    onReorder: (List<AuthSession>) -> Unit,
    showPhone: Boolean,
    reorderMode: Boolean,
    workingOrder: List<UniAccount>?,
    onEnterReorder: () -> Unit,
    onDragReorder: (List<UniAccount>) -> Unit,
    onExitReorder: () -> Unit,
    onUpdateRemark: (String, String, String) -> Unit,
    rolesRefreshTick: Int,
) {
    val appContext = LocalContext.current
    var rolesMap by remember { mutableStateOf(mapOf<String, List<GameRoleRow>>()) }
    var rolesLoading by remember { mutableStateOf(true) }
    // 拖拽排序：按住三横杠把手拖动（等高卡假设；进入排序时自动收起所有卡）
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var rowHeightPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    // 排序模式下单击卡片 → 编辑备注
    var remarkTarget by remember { mutableStateOf<UniAccount?>(null) }
    var remarkField by remember { mutableStateOf("") }

    val roleCache = remember { RoleCache.load(appContext) }

    LaunchedEffect(accounts.map { "${it.platform}:${it.uid}" }, rolesRefreshTick) {
        val keys = accounts.map { "${it.platform}:${it.uid}" }.toSet()
        // 1) 先显示缓存（没有缓存即首次才显示“获取中”）
        val cached = roleCache.filterKeys { it in keys }
        if (cached.isNotEmpty()) {
            rolesMap = cached
            rolesLoading = false
        } else {
            rolesLoading = true
        }
        // 2) 后台拉取；按账号合并：只有拉到数据的账号覆盖缓存，
        // 失败/为空的账号保留旧缓存（否则一个平台成功会把其他平台的缓存整体抹掉）
        val result = mutableMapOf<String, List<GameRoleRow>>()
        for (account in accounts) {
            val key = "${account.platform}:${account.uid}"
            val session = sessions.find { it.platform == account.platform && it.uid == account.uid }
                ?: sessions.find { it.platform == account.platform }
            val fresh = when {
                session == null -> emptyList()
                account.platform == "skland" -> AuthApi.fetchSklandRoles(session)
                account.platform == "mihoyo" -> AuthApi.fetchMihoyoGameRecord(session)
                else -> emptyList()
            }
            result[key] = if (fresh.isNotEmpty()) fresh else (cached[key] ?: fresh)
        }
        if (result.values.any { it.isNotEmpty() }) {
            rolesMap = result
            RoleCache.save(appContext, result)
        } else if (cached.isEmpty()) {
            rolesMap = result
        }
        rolesLoading = false
    }

    val displayAccounts = workingOrder ?: accounts

    fun handleDragStart(key: String) {
        if (!reorderMode) onEnterReorder()
        dragKey = key
        dragOffsetY = 0f
    }

    fun handleDrag(dy: Float) {
        val key = dragKey ?: return
        dragOffsetY += dy
        val rowH = rowHeightPx
        if (rowH <= 0f) return
        val list = workingOrder ?: return
        val from = list.indexOfFirst { "${it.platform}:${it.uid}" == key }
        if (from < 0) return
        val target = (from + Math.round(dragOffsetY / rowH)).coerceIn(0, list.lastIndex)
        if (target != from) {
            val m = list.toMutableList()
            val item = m.removeAt(from)
            m.add(target, item)
            dragOffsetY -= (target - from) * rowH
            onDragReorder(m)
        }
    }

    fun handleDragEnd() {
        dragKey = null
        dragOffsetY = 0f
    }

    fun saveOrder() {
        val order = workingOrder ?: return
        val byKey = sessions.associateBy { "${it.platform}:${it.uid}" }
        val reordered = order.mapIndexedNotNull { i, acc ->
            byKey["${acc.platform}:${acc.uid}"]?.copy(sortOrder = i)
        }
        val rest = sessions
            .filter { s -> order.none { it.platform == s.platform && it.uid == s.uid } }
            .mapIndexed { i, s -> s.copy(sortOrder = order.size + i) }
        onReorder(reordered + rest)
        onExitReorder()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScroll)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (reorderMode) {
            item {
                Text(
                    text = "按住三横杠拖动排序 · 点击卡片编辑备注",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (accounts.isEmpty()) {
            item {
                Text(
                    text = "还没有登录的账号\n请分别登录米游社 / 森空岛",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                )
            }
        } else {
            items(displayAccounts, key = { "${it.platform}:${it.uid}" }) { account ->
                val key = "${account.platform}:${account.uid}"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            rowHeightPx = it.size.height.toFloat() +
                                with(density) { 12.dp.toPx() }
                        }
                        // 位移动画只在排序模式下启用：拖拽换位需要它让相邻卡滑开；
                        // 普通模式下卡片展开会逐帧改变下方布局，弹簧追帧会让下方卡片
                        // 滞后于底部按钮（无动画），产生不同步的视觉异常。
                        // 且被拖拽卡的手动 dragOffsetY 会与弹簧动画叠加导致跳变。
                        .then(
                            if (reorderMode && dragKey != key) {
                                Modifier.animateItem()
                            } else {
                                Modifier
                            }
                        ),
                ) {
                    AccountCard(
                        account = account,
                        roles = rolesMap[key],
                        rolesLoading = rolesLoading,
                        expanded = key in expandedKeys && !reorderMode,
                        onToggle = { onToggleExpand(key) },
                        onOpenGame = { row -> onOpenGameDetail(account, row) },
                        reorderMode = reorderMode,
                        onEnterReorder = { onEnterReorder() },
                        onRemark = {
                            remarkTarget = account
                            remarkField = account.remark
                        },
                        onHandleDragStart = { handleDragStart(key) },
                        onHandleDrag = { dy -> handleDrag(dy) },
                        onHandleDragEnd = { handleDragEnd() },
                        dragTranslationY = if (dragKey == key) dragOffsetY else null,
                        showPhone = showPhone,
                    )
                }
            }
        }
        item {
            if (reorderMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { saveOrder() },
                    ) {
                        Text("保存顺序")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onExitReorder() },
                    ) {
                        Text("取消")
                    }
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAdd,
                ) {
                    Text("添加其他平台账号")
                }
            }
        }
    }

    // 排序模式下单击卡片 → 编辑备注（展示在手机号后面）
    remarkTarget?.let { acc ->
        OverlayDialog(
            title = "编辑备注",
            summary = "显示在手机号后面，最多 10 个字符（中文按 2 个计）",
            show = true,
            onDismissRequest = { remarkTarget = null },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextField(
                    value = remarkField,
                    onValueChange = { new ->
                        if (remarkLength(new) <= 10) remarkField = new
                    },
                    label = "备注",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
                Text(
                    text = "${remarkLength(remarkField)}/10",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.align(Alignment.End),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onUpdateRemark(acc.platform, acc.uid, remarkField.trim())
                            remarkTarget = null
                        },
                    ) {
                        Text("确认")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { remarkTarget = null },
                    ) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

/** 三横杠拖动把手：按住可拖动排序。 */
@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .size(24.dp)
            .semantics { contentDescription = "拖动排序" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .background(
                        MiuixTheme.colorScheme.onBackgroundVariant,
                        RoundedCornerShape(1.dp),
                    ),
            )
            if (it < 2) Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AccountCard(
    account: UniAccount,
    roles: List<GameRoleRow>?,
    rolesLoading: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenGame: (GameRoleRow) -> Unit,
    reorderMode: Boolean = false,
    onEnterReorder: () -> Unit = {},
    onRemark: () -> Unit = {},
    onHandleDragStart: () -> Unit = {},
    onHandleDrag: (Float) -> Unit = {},
    onHandleDragEnd: () -> Unit = {},
    dragTranslationY: Float? = null,
    showPhone: Boolean = false,
) {
    // 默认收起；箭头朝右（▷），展开时顺时针旋转 90° 朝下（∨）
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(240),
        label = "account_chevron",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (dragTranslationY != null) {
                    Modifier.graphicsLayer { translationY = dragTranslationY }
                } else {
                    Modifier
                },
            ),
        insideMargin = PaddingValues(16.dp),
    ) {
        // 头部：图标 + 昵称 + uid + 平台胶囊；点按展开，排序模式单击编辑备注，
        // 右侧三横杠把手按住拖动排序
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (reorderMode) onRemark() else onToggle()
                    },
                    onLongClick = { if (!reorderMode) onEnterReorder() },
                ),
        ) {
            AccountAvatar(account)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = account.uid,
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                    if (!reorderMode) {
                        Spacer(Modifier.width(8.dp))
                        PlatformBadge(account.platform)
                    }
                }
                Spacer(Modifier.height(4.dp))
                // 第二行：手机号（默认掩码，眼睛睁开显示全文）+ 备注；无手机号时显示原 meta
                val phoneDisplay = if (account.phone.isNotBlank()) {
                    if (showPhone) account.phone else maskPhone(account.phone)
                } else {
                    account.meta
                }
                Text(
                    text = if (account.remark.isNotBlank()) {
                        "$phoneDisplay · ${account.remark}"
                    } else {
                        phoneDisplay
                    },
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (reorderMode) {
                // 排序模式：三横杠拖动把手（平常不显示）。
                // rememberUpdatedState：pointerInput 只在 key 变化时重启，
                // 需要把最新回调引进手势闭包，否则拖动读到的是旧 workingOrder。
                val dragStart by rememberUpdatedState(onHandleDragStart)
                val dragMove by rememberUpdatedState(onHandleDrag)
                val dragEnd by rememberUpdatedState(onHandleDragEnd)
                DragHandle(
                    modifier = Modifier.pointerInput(account.platform + account.uid) {
                        detectDragGestures(
                            onDragStart = { dragStart() },
                            onDrag = { _, amount -> dragMove(amount.y) },
                            onDragEnd = { dragEnd() },
                            onDragCancel = { dragEnd() },
                        )
                    },
                )
            } else {
                Icon(
                    imageVector = MiuixIcons.ChevronForward,
                    contentDescription = if (expanded) "收起角色" else "展开角色",
                    tint = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation),
                )
            }
        }

        // 展开区：每个游戏一张大卡片（左侧图标），点进详情页；区分加载/空态/数据
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    roles == null && rolesLoading -> Text(
                        text = "正在获取角色数据…",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                    roles.isNullOrEmpty() -> Text(
                        text = "暂无角色数据",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                    else -> roles.forEach { row ->
                        GameCardItem(row = row, onClick = { onOpenGame(row) })
                    }
                }
            }
        }
    }
}

/** 展开区的游戏大卡片：左侧游戏图标 + 游戏名/区服昵称 + 等级 + 右箭头，点击进详情。 */
@Composable
private fun GameCardItem(
    row: GameRoleRow,
    onClick: () -> Unit,
) {
    // 用 miuix 交互版 Card：按压反馈（水波/缩放）跟随卡片圆角；
    // 外层 clickable 的 ripple 是矩形，会露出不带圆角的“阴影”。
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            GameIcon(
                url = row.iconUrl,
                name = row.gameName,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.gameName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (row.subText.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = row.subText,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
            }
            if (row.levelText.isNotBlank()) {
                Text(
                    text = row.levelText,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = "查看详情",
                tint = MiuixTheme.colorScheme.onBackgroundVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** 游戏图标（官方 icon URL）；无图时用游戏名首字兜底。 */
@Composable
private fun GameIcon(
    url: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    if (url.isNotBlank()) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(10.dp)),
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}

/** 游戏详情页（二级页面）：头部卡片 + 统计网格（体力/活跃天数/已解锁角色…）。 */
@Composable
private fun GameDetailPage(
    row: GameRoleRow,
    session: AuthSession?,
    nestedScroll: NestedScrollConnection,
) {
    // 角色列表（森空岛 card/detail）：进入页面拉取，空=不支持或无数据
    var chars by remember(row) { mutableStateOf<List<GameCharRow>?>(null) }
    LaunchedEffect(row, session) {
        if (session == null || row.platform != "skland" || row.gameKey.isBlank()) {
            chars = emptyList()
        } else {
            chars = AuthApi.fetchSklandCharList(session, row)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScroll)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 头部：图标 + 游戏名 + 等级 + 区服·昵称
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameIcon(
                        url = row.iconUrl,
                        name = row.gameName,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = row.gameName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (row.levelText.isNotBlank()) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = row.levelText,
                                    fontSize = 13.sp,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                )
                            }
                        }
                        if (row.subText.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = row.subText,
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                            )
                        }
                    }
                }
            }
        }
        // 统计网格：两列
        if (row.stats.isNotEmpty()) {
            item {
                Text(
                    text = "概览",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            items(row.stats.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { stat ->
                        StatCard(stat = stat, modifier = Modifier.weight(1f))
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "暂无统计数据",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        // 角色列表（森空岛 card/detail）：加载中 / 空态不显示 / 列表
        when {
            chars == null -> item {
                Text(
                    text = "正在获取角色列表…",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            !chars.isNullOrEmpty() -> {
                item {
                    Text(
                        text = "角色列表 · ${chars!!.size}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                items(chars!!, key = { it.name }) { c ->
                    CharRowItem(c)
                }
            }
        }
    }
}

/** 详情页角色行：头像 + 名字 + 稀有度 + 职业/属性 + 等级。 */
@Composable
private fun CharRowItem(c: GameCharRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GameIcon(
                url = c.avatarUrl,
                name = c.name,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = c.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (c.rarity > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "★".repeat(c.rarity.coerceAtMost(6)),
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                    }
                }
                val sub = listOf(c.profession, c.property).filter(String::isNotBlank)
                    .joinToString(" · ")
                if (sub.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = sub,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
            }
            if (c.level > 0) {
                Text(
                    text = "Lv.${c.level}",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    stat: GameStat,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Text(
                text = stat.label,
                fontSize = 11.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stat.value,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AccountAvatar(account: UniAccount) {
    PlatformIcon(
        platform = account.platform,
        modifier = Modifier.size(46.dp),
    )
}

@Composable
private fun PlatformIcon(platform: String, modifier: Modifier = Modifier) {
    val resId = when (platform) {
        "skland" -> R.drawable.skland_icon
        "kuro" -> R.drawable.kuro_icon
        else -> R.drawable.mihoyo_icon
    }
    Image(
        painter = painterResource(resId),
        contentDescription = platformLabel(platform),
        modifier = modifier.clip(RoundedCornerShape(15.dp)),
    )
}

@Composable
private fun PlatformPill(platform: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(platformColor(platform).copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        PlatformIcon(platform, Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = platformLabel(platform),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = platformColor(platform),
        )
    }
}

@Composable
private fun PlatformBadge(platform: String) {
    val color = platformColor(platform)
    Text(
        text = platformLabel(platform),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun ScanPage(
    accounts: List<UniAccount>,
    sessions: List<AuthSession>,
    scanPlatform: String,
    onScanPlatformChange: (String) -> Unit,
    onToast: (String) -> Unit,
    onMessage: (String, String) -> Unit,
    active: Boolean,
    nestedScroll: NestedScrollConnection,
) {
    var scannedRaw by remember { mutableStateOf<String?>(null) }
    var cameraOn by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    // 多账号时选择用哪个账号确认换端（单账号自动选中）
    var selectedScanUid by remember { mutableStateOf<String?>(null) }
    // 库街区扫码二次验证状态：idle=未开始，sms=已发码等待输入
    var kuroStage by remember { mutableStateOf("idle") }
    var kuroCode by remember { mutableStateOf("") }
    var kuroRoles by remember { mutableStateOf(listOf<String>()) }

    fun resetScanSheet() {
        scannedRaw = null
        cameraOn = true
        kuroStage = "idle"
        kuroCode = ""
        kuroRoles = emptyList()
        selectedScanUid = null
    }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) cameraOn = true
    }

    fun handleScanned(raw: String) {
        val detected = detectQrPlatform(raw)
        if (detected == null) {
            onToast("无法识别二维码平台")
            scannedRaw = null
            cameraOn = false
        } else if (!accounts.any { it.platform == detected }) {
            onToast("未登录${platformLabel(detected)}账号，请先到账号库登录")
            scannedRaw = null
            cameraOn = false
        } else {
            scannedRaw = raw
            cameraOn = false
            onScanPlatformChange(detected)
            // 单账号直接预选；多账号等待用户在弹窗里选择
            selectedScanUid = accounts
                .filter { it.platform == detected }
                .singleOrNull()
                ?.uid
        }
    }

    val galleryScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val image = withContext(Dispatchers.IO) {
                    try {
                        InputImage.fromFilePath(context, uri)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (image != null) {
                    galleryScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val raw = barcodes.firstOrNull { it.rawValue?.contains("token_types") == true }
                                ?.rawValue
                                ?: barcodes.firstOrNull()?.rawValue
                            if (raw != null) handleScanned(raw) else onToast("图片中没有识别到二维码")
                        }
                        .addOnFailureListener { onToast("相册图片识别失败") }
                } else {
                    onToast("无法读取相册图片")
                }
            }
        }
    }

    // 只有扫码页停稳且应用处于 RESUMED 前台可交互状态时才启动相机；
    // 页面切换过渡/预组合经过扫码页时不绑相机，离开时解绑
    LaunchedEffect(active, permissionGranted) {
        when {
            !active -> cameraOn = false
            permissionGranted -> cameraOn = true
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(accounts, scanPlatform) {
        if (!accounts.any { it.platform == scanPlatform }) {
            onScanPlatformChange(accounts.firstOrNull()?.platform ?: "mihoyo")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScroll)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            if (accounts.isEmpty()) {
                item {
                    Text(
                        text = "请先到「账号库」登录平台账号",
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                item {
                    Text(
                        text = "融合扫码：自动识别二维码平台",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        accounts.forEach { PlatformPill(it.platform) }
                    }
                }
                item {
                    if (cameraOn && scannedRaw == null) {
                        QrCameraPreview(
                            active = cameraOn && scannedRaw == null,
                            onQrScanned = { handleScanned(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                        )
                    } else {
                        ScannerFrame()
                    }
                }
                item {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        onClick = {
                            when {
                                scannedRaw != null -> resetScanSheet()
                                else -> galleryLauncher.launch("image/*")
                            }
                        },
                    ) {
                        Text(
                            if (scannedRaw != null) "重新扫码" else "使用相册"
                        )
                    }
                }
                item {
                    Text(
                        text = "将米游社、森空岛或库街区的登录二维码对准取景框，确认后完成扫码登录。",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // 同平台候选账号：多账号时按用户选择，单账号默认选中
        val scanCandidates = accounts.filter { it.platform == scanPlatform }
        val scannedAccount = scannedRaw?.let {
            scanCandidates.firstOrNull { a -> a.uid == selectedScanUid }
                ?: scanCandidates.singleOrNull()
        }
        val deviceName = when (scanPlatform) {
            "skland" -> "森空岛网站 · 扫码登录"
            "kuro" -> "扫码登录游戏"
            else -> AuthApi.mihoyoQrAppName(scannedRaw.orEmpty())
                .ifBlank { "另一台设备" } + " · 扫码登录"
        }
        val kuroSmsStage = scanPlatform == "kuro" && kuroStage == "sms"

        OverlayBottomSheet(
            show = scannedRaw != null && scannedAccount != null && !confirming,
            title = if (kuroSmsStage) "短信二次验证" else "确认扫码登录",
            onDismissRequest = { resetScanSheet() },
        ) {
            // 底部留 ~3 行文字的余量，避免按钮贴着手势小白条
            Column(modifier = Modifier.padding(bottom = 48.dp)) {
                Text(
                    text = "${platformLabel(scanPlatform)} · $deviceName",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                Spacer(Modifier.height(14.dp))
                if (kuroSmsStage) {
                    if (kuroRoles.isNotEmpty()) {
                        Text(
                            text = "游戏角色：" + kuroRoles.joinToString("、"),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = "验证码已发送至账号绑定手机号",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    TextField(
                        value = kuroCode,
                        onValueChange = { kuroCode = it.take(6) },
                        label = "短信验证码",
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(
                            text = "取消",
                            onClick = { resetScanSheet() },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = "确认登录",
                            enabled = kuroCode.length == 6 && !confirming,
                            onClick = {
                                val session = scannedAccount?.let { acc ->
                                    sessions.find {
                                        it.platform == acc.platform && it.uid == acc.uid
                                    }
                                } ?: sessions.firstOrNull { it.platform == "kuro" }
                                val raw = scannedRaw ?: return@TextButton
                                if (session == null) {
                                    onToast("未找到库街区账号，请先登录")
                                    return@TextButton
                                }
                                confirming = true
                                scope.launch {
                                    val result = AuthApi.kuroConfirmScan(raw, session, kuroCode)
                                    if (result is AuthResult.Success) {
                                        onMessage(
                                            "扫码登录成功",
                                            "库街区账号「${accountName(session)}」已确认扫码登录游戏。"
                                        )
                                        LoginNotifier.notify(
                                            context,
                                            "kuro",
                                            accountName(session),
                                            success = true,
                                        )
                                        onToast("已确认，另一台设备即将登录")
                                        resetScanSheet()
                                    } else {
                                        val reason =
                                            (result as? AuthResult.Error)?.message ?: "确认登录失败"
                                        onMessage(
                                            "扫码登录失败",
                                            "[库街区]扫码确认登录失败：$reason"
                                        )
                                        LoginNotifier.notify(
                                            context,
                                            "kuro",
                                            accountName(session),
                                            success = false,
                                            reason = reason,
                                        )
                                        onToast(reason)
                                    }
                                    confirming = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                } else {
                    if (scanCandidates.size > 1) {
                        Text(
                            text = "选择登录账号",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        scanCandidates.forEach { cand ->
                            val selected = cand.uid == selectedScanUid
                            val radioColor = if (selected) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onBackgroundVariant
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedScanUid = cand.uid }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                            ) {
                                // 单选圆点
                                Box(
                                    modifier = Modifier.size(20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .border(1.5.dp, radioColor, CircleShape),
                                    )
                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(radioColor, CircleShape),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = cand.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = cand.uid,
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    } else {
                        Text(
                            text = "登录账号",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = scannedAccount?.name.orEmpty() + " · " + scannedAccount?.uid.orEmpty(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(
                            text = "拒绝",
                            onClick = { resetScanSheet() },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = "同意登录",
                            // 防重复提交 + 多账号时必须先选择
                            enabled = !confirming && (scanCandidates.size == 1 || selectedScanUid != null),
                            onClick = {
                                val session = scannedAccount?.let { acc ->
                                    sessions.find {
                                        it.platform == acc.platform && it.uid == acc.uid
                                    }
                                } ?: sessions.firstOrNull { it.platform == scanPlatform }
                                val raw = scannedRaw ?: return@TextButton
                                confirming = true
                                if (session == null) {
                                    onToast("未找到可确认的账号，请重新登录")
                                    confirming = false
                                    scannedRaw = null
                                    return@TextButton
                                }
                                when (session.platform) {
                                    "kuro" -> {
                                        // 库街区：qrCode/scan + scanSms 预检，成功后弹短信码输入（保留 sheet）
                                        scope.launch {
                                            when (val r = AuthApi.kuroScanReady(raw, session)) {
                                                is KuroScanResult.Ready -> {
                                                    kuroRoles = r.roles
                                                    kuroStage = "sms"
                                                    confirming = false
                                                    onToast("二次验证短信已发送")
                                                }
                                                is KuroScanResult.Error -> {
                                                    onToast(r.message)
                                                    confirming = false
                                                }
                                            }
                                        }
                                    }
                                    "mihoyo" -> {
                                        scannedRaw = null
                                        scope.launch {
                                            val result = AuthApi.confirmMihoyoQr(raw, session)
                                            if (result is AuthResult.Success) {
                                                onMessage(
                                                    "扫码登录成功",
                                                    "${platformLabel("mihoyo")}账号「${accountName(session)}」已确认另一台设备扫码登录，凭证保存在本机，长期有效。"
                                                )
                                                LoginNotifier.notify(
                                                    context,
                                                    "mihoyo",
                                                    accountName(session),
                                                    success = true,
                                                )
                                                onToast("已确认，另一台设备即将登录")
                                            } else {
                                                val reason =
                                                    (result as? AuthResult.Error)?.message ?: "确认登录失败"
                                                onMessage(
                                                    "扫码登录失败",
                                                    "[${platformLabel("mihoyo")}]扫码确认登录失败：$reason"
                                                )
                                                LoginNotifier.notify(
                                                    context,
                                                    "mihoyo",
                                                    accountName(session),
                                                    success = false,
                                                    reason = reason,
                                                )
                                                onToast(reason)
                                            }
                                            confirming = false
                                        }
                                    }
                                    else -> {
                                        scannedRaw = null
                                        scope.launch {
                                            val result = AuthApi.confirmSklandQr(raw, session)
                                            if (result is AuthResult.Success) {
                                                onMessage(
                                                    "扫码登录成功",
                                                    "${platformLabel("skland")}账号「${accountName(session)}」已确认另一台设备扫码登录。"
                                                )
                                                LoginNotifier.notify(
                                                    context,
                                                    "skland",
                                                    accountName(session),
                                                    success = true,
                                                )
                                                onToast("已确认，另一台设备即将登录")
                                            } else {
                                                val reason =
                                                    (result as? AuthResult.Error)?.message ?: "确认登录失败"
                                                onMessage(
                                                    "扫码登录失败",
                                                    "[${platformLabel("skland")}]扫码确认登录失败：$reason"
                                                )
                                                LoginNotifier.notify(
                                                    context,
                                                    "skland",
                                                    accountName(session),
                                                    success = false,
                                                    reason = reason,
                                                )
                                                onToast(reason)
                                            }
                                            confirming = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            }
        }

        if (confirming) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "正在确认登录",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun QrCameraPreview(
    active: Boolean,
    onQrScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    val analysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    var consumed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        providerFuture.addListener({
            provider = providerFuture.get()
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            analysis.clearAnalyzer()
        }
    }

    DisposableEffect(active, provider, consumed) {
        if (provider == null || !active || consumed) return@DisposableEffect onDispose {}
        val cameraProvider = provider ?: return@DisposableEffect onDispose {}
        val imageAnalysisUse = analysis
        imageAnalysisUse.setAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { imageProxy: ImageProxy ->
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return@setAnalyzer
            }
            val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(input)
                .addOnSuccessListener { barcodes ->
                    val raw = barcodes.firstOrNull { it.rawValue?.contains("token_types") == true }
                        ?.rawValue
                        ?: barcodes.firstOrNull()?.rawValue
                    if (raw != null) {
                        consumed = true
                        onQrScanned(raw)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
        val previewUse = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUse,
                imageAnalysisUse,
            )
        } catch (e: Exception) {
            // 模拟器无可用后摄时保持友好提示
        }
        onDispose {
            cameraProvider.unbind(previewUse, imageAnalysisUse)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF15161A)),
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp)),
        )
        ViewfinderMask()
    }
}

@Composable
private fun SelectChip(
    text: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    val bg = if (selected) color.copy(alpha = 0.14f) else Color.Transparent
    val border = if (selected) color else MiuixTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) color else MiuixTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ScannerFrame() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFF15161A), RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        ViewfinderMask()
    }
}

@Composable
private fun ViewfinderMask() {
    var scanOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            scanOffset = 0f
            delay(30)
            for (i in 0..100) {
                scanOffset = i / 100f
                delay(22)
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
    ) {
        val side = minOf(size.width, size.height) * 0.72f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val radius = 20.dp.toPx()
        val hole = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, left + side, top + side),
                    cornerRadius = CornerRadius(radius, radius),
                )
            )
        }
        clipPath(hole, ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.55f))
        }

        val stroke = 3.dp.toPx()
        drawRoundRect(
            color = Color(0xFF3482FF),
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke),
        )
        val lineY = top + 24.dp.toPx() + (side - 48.dp.toPx()) * scanOffset
        drawLine(
            color = Color(0xCC3482FF),
            start = Offset(left + 24.dp.toPx(), lineY),
            end = Offset(left + side - 24.dp.toPx(), lineY),
            strokeWidth = 2.dp.toPx(),
        )
    }
}

@Composable
private fun MessagesPage(
    accounts: List<UniAccount>,
    messages: List<UniMessage>,
    nestedScroll: NestedScrollConnection,
    onOpenMessage: (UniMessage) -> Unit,
) {
    val now = System.currentTimeMillis()
    val realExpiry = accounts.filter { it.expiresAt > 0 }
    val longTermAccounts = accounts.filter { it.expiresAt <= 0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScroll)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (messages.isEmpty() && realExpiry.isEmpty() && longTermAccounts.isEmpty()) {
            item {
                Text(
                    text = "暂无真实消息\n登录或扫码后，这里会显示账号与设备动态",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                )
            }
        }
        items(messages) { message ->
            MessageCard(
                title = message.title,
                time = message.time,
                desc = message.desc,
                platform = messagePlatform(message.desc),
                onClick = { onOpenMessage(message) },
            )
        }
        longTermAccounts.forEach { account ->
            val synthetic = UniMessage(
                title = "会话状态",
                time = "实时",
                desc = "${platformLabel(account.platform)}账号「${account.name}」凭证已保存在本机，长期有效；如平台会话失效，登录页会提示重新登录。",
            )
            item(key = "long_term_${account.platform}") {
                MessageCard(
                    title = synthetic.title,
                    time = synthetic.time,
                    desc = synthetic.desc,
                    platform = account.platform,
                    onClick = { onOpenMessage(synthetic) },
                )
            }
        }
        realExpiry.forEach { account ->
            val daysLeft = ((account.expiresAt - now) / 86400000L).toInt().coerceAtLeast(0)
            val synthetic = UniMessage(
                title = if (daysLeft <= 7) "会话即将过期" else "会话保活提醒",
                time = "实时",
                desc = "${platformLabel(account.platform)}账号「${account.name}」会话有效期至 ${dateText(account.expiresAt)}，剩余约 $daysLeft 天，过期后需要重新登录。",
            )
            item(key = "expiry_${account.platform}") {
                MessageCard(
                    title = synthetic.title,
                    time = synthetic.time,
                    desc = synthetic.desc,
                    platform = account.platform,
                    onClick = { onOpenMessage(synthetic) },
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    time: String,
    desc: String,
    platform: String = "",
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
        showIndication = true,
        onClick = onClick,
    ) {
        Row {
            if (platform.isBlank()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MiuixTheme.colorScheme.primary, RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "!",
                        color = MiuixTheme.colorScheme.onPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                PlatformIcon(platform, Modifier.size(40.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = time,
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
        }
    }
}

@Composable
private fun VisualPage(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    glassEnabled: Boolean,
    onGlassEnabledChange: (Boolean) -> Unit,
    nestedScroll: NestedScrollConnection,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScroll)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                OverlayDropdownMenu(
                    title = "皮肤",
                    summary = when (themeMode) {
                        ThemeMode.LIGHT -> "亮色皮肤"
                        ThemeMode.DARK -> "暗色皮肤"
                        ThemeMode.SYSTEM -> "跟随系统皮肤"
                    },
                    entry = DropdownEntry(
                        items = ThemeMode.entries.map { mode ->
                            DropdownItem(
                                text = when (mode) {
                                    ThemeMode.LIGHT -> "亮色皮肤"
                                    ThemeMode.DARK -> "暗色皮肤"
                                    ThemeMode.SYSTEM -> "跟随系统皮肤"
                                },
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                            )
                        }
                    ),
                )
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                SwitchPreference(
                    checked = glassEnabled,
                    onCheckedChange = onGlassEnabledChange,
                    title = "启用液态玻璃",
                    summary = "底栏和面板使用澎湃 OS 风格玻璃效果",
                )
            }
        }
    }
}

@Composable
private fun SettingsPage(
    accounts: List<UniAccount>,
    onOpenVisual: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenAbout: () -> Unit,
    onToast: (String) -> Unit,
    onLogout: (String?, String?) -> Unit,
    nestedScroll: NestedScrollConnection,
) {
    var showLogout by remember { mutableStateOf(false) }
    // null=未选择, Pair(null,null)=退出全部, Pair(platform,name)=退出单个
    var pendingLogout by remember { mutableStateOf<Triple<String?, String, String>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScroll)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ArrowPreference(
                        title = "账号与同步",
                        summary = "账号管理、WebDAV 同步、指纹验证",
                        onClick = onOpenSync,
                    )
                    ArrowPreference(
                        title = "视觉",
                        summary = "皮肤、液态玻璃",
                        onClick = onOpenVisual,
                    )
                    ArrowPreference(
                        title = "关于聚合通行证",
                        summary = "yvhan · v1.1.0",
                        onClick = onOpenAbout,
                    )
                }
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showLogout = true },
                ) {
                    Text("退出登录")
                }
            }
        }

        OverlayDialog(
            title = "退出登录",
            summary = "选择要退出的账号",
            show = showLogout,
            onDismissRequest = { showLogout = false },
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                accounts.forEach { account ->
                    ArrowPreference(
                        title = account.name,
                        summary = "${platformLabel(account.platform)} · ${account.uid}",
                        onClick = {
                            showLogout = false
                            pendingLogout = Triple(account.platform, account.uid, account.name)
                        },
                    )
                }
            }
            if (accounts.size > 1) {
                TextButton(
                    text = "退出全部账号",
                    onClick = {
                        showLogout = false
                        pendingLogout = Triple(null, "", "全部账号")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            TextButton(
                text = "取消",
                onClick = { showLogout = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 第二步：二次确认
        pendingLogout?.let { (platform, uid, name) ->
            ConfirmDialog(
                show = true,
                title = "退出登录",
                summary = if (platform == null) {
                    "确定退出全部账号吗？退出后所有平台都需要重新登录。"
                } else {
                    "确定退出「$name」的账号吗？退出后需要重新登录。"
                },
                confirmText = "退出",
                onConfirm = {
                    onLogout(platform, uid.ifBlank { null })
                    onToast(if (platform == null) "已退出全部账号" else "已退出 $name")
                    pendingLogout = null
                },
                onDismiss = { pendingLogout = null },
            )
        }
    }
}

@Composable
private fun LoginScreen(
    onLoggedIn: (AuthSession) -> Unit,
    onLoginFailed: (platform: String, rawName: String, reason: String) -> Unit,
    onToast: (String) -> Unit,
    onBack: () -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("mihoyo") }
    var countdown by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    // 库街区：待完成极验的手机号（非空时显示极验弹窗）
    var geeTestFor by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    // 隐藏输入框的光标拖拽水滴手柄（新版 Compose 会在框外画出手柄，形成“框外缺口”）
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = Color.Transparent,
            backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.25f),
        )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp),
            contentPadding = PaddingValues(top = 36.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Back,
                        contentDescription = "返回",
                    )
                }
            }
        }
        item {
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = "聚合通行证图标",
                modifier = Modifier.size(60.dp),
            )
        }
        item {
            Text(
                text = "聚合通行证",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item {
            Text(
                text = "米游社 · 森空岛 统一账号入口\n分别登录账号，扫码时选择对应账号换端",
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                textAlign = TextAlign.Center,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    value = phone,
                    onValueChange = { phone = it.take(11) },
                    label = "手机号",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
                TextField(
                    value = code,
                    onValueChange = { code = it.take(6) },
                    label = "验证码",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    trailingIcon = {
                        TextButton(
                            text = if (countdown > 0) "${countdown}s" else "获取验证码",
                            enabled = countdown == 0,
                            // 让按钮底离开右侧描边，避免聚焦时盖住右边框直段
                            modifier = Modifier.padding(end = 18.dp),
                            onClick = {
                                if (phone.length != 11) {
                                    onToast("请输入正确的 11 位手机号")
                                } else if (platform == "kuro") {
                                    scope.launch {
                                        when (val r = AuthApi.sendKuroSms(phone)) {
                                            SmsResult.NeedCaptcha -> geeTestFor = phone
                                            SmsResult.Sent -> {
                                                countdown = 60
                                                onToast("验证码已发送")
                                            }
                                            is SmsResult.Error -> onToast(r.message)
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        when (val r = AuthApi.sendSms(platform, phone)) {
                                            is AuthResult.Success -> {
                                                countdown = 60
                                                onToast("验证码已发送")
                                            }
                                            is AuthResult.Error -> onToast(r.message)
                                        }
                                    }
                                }
                            },
                        )
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SelectChip(
                    text = "米游社",
                    selected = platform == "mihoyo",
                    color = platformColor("mihoyo"),
                    onClick = { platform = "mihoyo" },
                )
                SelectChip(
                    text = "森空岛",
                    selected = platform == "skland",
                    color = platformColor("skland"),
                    onClick = { platform = "skland" },
                )
                SelectChip(
                    text = "库街区",
                    selected = platform == "kuro",
                    color = platformColor("kuro"),
                    onClick = { platform = "kuro" },
                )
            }
        }
        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
                onClick = {
                    when {
                        phone.length != 11 -> onToast("请输入正确的 11 位手机号")
                        code.length != 6 -> onToast("请输入 6 位验证码")
                        else -> {
                            busy = true
                            scope.launch {
                                when (val r = AuthApi.login(platform, phone, code)) {
                                    is AuthResult.Success -> onLoggedIn(r.session)
                                    is AuthResult.Error -> {
                                        onLoginFailed(platform, phone, r.message)
                                        onToast(r.message)
                                    }
                                }
                                busy = false
                            }
                        }
                    }
                },
            ) {
                Text(if (busy) "登录中…" else "登 录")
            }
        }
        item {
            Text(
                text = "验证码会发送到该手机号，登录后凭证保存在本机",
                fontSize = 11.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
        }
    }

    geeTestFor?.let { pendingPhone ->
        GeetestCaptchaDialog(
            onResult = { json ->
                geeTestFor = null
                scope.launch {
                    when (val r = AuthApi.sendKuroSms(pendingPhone, json)) {
                        SmsResult.Sent -> {
                            countdown = 60
                            onToast("验证码已发送")
                        }
                        SmsResult.NeedCaptcha -> onToast("验证未通过，请重试")
                        is SmsResult.Error -> onToast(r.message)
                    }
                }
            },
            onError = { msg ->
                geeTestFor = null
                onToast(msg)
            },
            onDismiss = { geeTestFor = null },
        )
    }
}

/** 极验 v4 滑块弹窗：WebView 加载官方 JS，回调 JSON 即抓包中的 geeTestData 结构。 */
@Composable
private fun GeetestCaptchaDialog(
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var handled by remember { mutableStateOf(false) }
    OverlayDialog(
        title = "安全验证",
        summary = "完成滑块验证后将自动发送短信验证码",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                AndroidView(
                    // 必须 fillMaxSize：否则 WebView 按内容 wrap（高~48px），
                    // viewport 高度为 0，极验验证窗口会缩成异常小窗
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // 不用 useWideViewPort/loadWithOverviewMode：与 meta viewport 组合
                            // 会把极验弹窗缩到约一半尺寸（viewport 度量异常）
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onSuccess(json: String) {
                                    if (handled) return
                                    handled = true
                                    post {
                                        onResult(json)
                                    }
                                }

                                @JavascriptInterface
                                fun onError(msg: String) {
                                    if (handled) return
                                    handled = true
                                    post {
                                        onError(msg.ifBlank { "安全验证失败，请重试" })
                                    }
                                }
                            }, "AndroidBridge")
                            loadDataWithBaseURL(
                                "https://kurobbs.com/",
                                GEETEST_HTML,
                                "text/html",
                                "utf-8",
                                null,
                            )
                        }
                    },
                )
            }
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val GEETEST_HTML = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<!-- 官方 v4 JS 地址见 docs.geetest.com gt4 Web 部署文档（旧 captcha_v4.js 路径已 404） -->
<script src="https://static.geetest.com/v4/gt4.js" onerror="window.__gt4loadFail=true"></script>
</head>
<body style="margin:0;padding:8px;background:transparent;">
<div id="captcha"></div>
<script>
function emit(o){ try{ AndroidBridge.onSuccess(JSON.stringify(o)); }catch(e){} }
function fail(m){ try{ AndroidBridge.onError(String(m)); }catch(e){} }
window.onerror = function(m){ fail(m); };
function boot(attempt){
  // WebView 首次 layout 前 innerHeight=0，极验按 0 高度计算会把验证窗口缩成 ~155px 小窗；
  // 等 viewport 高度就绪再初始化
  if (window.__gt4loadFail || typeof initGeetest4 !== "function") {
    fail("验证组件加载失败，请检查网络后重试");
    return;
  }
  if (window.innerHeight < 50) {
    if (attempt > 100) { fail("验证组件加载超时，请重试"); return; }
    setTimeout(function(){ boot(attempt + 1); }, 100);
    return;
  }
  initGeetest4({captchaId:"3f7e2d848ce0cb7e7d019d621e556ce2", product:"popup"}, function(captcha){
    captcha.appendTo("#captcha");
    captcha.onSuccess(function(){
      var v = captcha.getValidate();
      if (v && v.lot_number){ emit(v); return; }
      fail("验证结果不完整，请重试");
    });
    try{ captcha.onError(function(){ fail("验证错误，请重试"); }); }catch(e){}
  });
}
try{ boot(0); }catch(e){ fail(e); }
</script>
</body>
</html>
"""