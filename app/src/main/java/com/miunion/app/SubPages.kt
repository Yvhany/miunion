package com.miunion.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 通用二次确认弹窗（所有退出登录入口共用）。 */
@Composable
internal fun ConfirmDialog(
    show: Boolean,
    title: String,
    summary: String,
    confirmText: String = "确认",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        title = title,
        summary = summary,
        show = show,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun loginDateText(ts: Long): String =
    if (ts > 0) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(ts))
    } else {
        "未记录"
    }

private fun platformLabelOf(platform: String): String = when (platform) {
    "skland" -> "森空岛"
    "kuro" -> "库街区"
    else -> "米游社"
}

/** 消息详情页（二级页面）。 */
@Composable
internal fun MessageDetailPage(
    message: UniMessage,
    platform: String,
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(16.dp),
            ) {
                Row {
                    if (platform.isBlank()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MiuixTheme.colorScheme.primary, RoundedCornerShape(15.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "!",
                                color = MiuixTheme.colorScheme.onPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(
                                when (platform) {
                                    "skland" -> R.drawable.skland_icon
                                    "kuro" -> R.drawable.kuro_icon
                                    else -> R.drawable.mihoyo_icon
                                }
                            ),
                            contentDescription = platformLabelOf(platform),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(15.dp)),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = message.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = message.time +
                                (if (platform.isNotBlank()) " · " + platformLabelOf(platform) else ""),
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = message.desc,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = "该消息保存在本机，并会随云同步加密备份",
                fontSize = 11.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** “账号与同步”二级页面。 */
@Composable
internal fun SyncPage(
    sessions: List<AuthSession>,
    messages: List<UniMessage>,
    onApplied: (List<AuthSession>, List<UniMessage>) -> Unit,
    onLogout: (String) -> Unit,
    onToast: (String) -> Unit,
    nestedScroll: NestedScrollConnection,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncPrefs = remember { context.getSharedPreferences("visual_prefs", android.content.Context.MODE_PRIVATE) }

    var expanded by remember { mutableStateOf(false) }
    var pendingLogout by remember { mutableStateOf<AuthSession?>(null) }

    var showWebDav by remember { mutableStateOf(false) }
    var urlField by remember { mutableStateOf("") }
    var userField by remember { mutableStateOf("") }
    var passField by remember { mutableStateOf("") }

    // pwdMode: set=首次设置 / change=修改(改后重传) / unlock=解锁云端数据
    var showPwd by remember { mutableStateOf(false) }
    var pwdMode by remember { mutableStateOf("set") }
    var pwdReason by remember { mutableStateOf("") }
    var pwdField by remember { mutableStateOf("") }

    var requireBiometric by remember {
        mutableStateOf(syncPrefs.getBoolean("require_biometric", false))
    }

    var syncing by remember { mutableStateOf(false) }
    var conflicts by remember { mutableStateOf(SyncStore.loadConflicts(context)) }

    val cfg = SyncStore.config(context)

    fun refreshConflicts() {
        conflicts = SyncStore.loadConflicts(context)
    }

    fun startSync() {
        if (syncing) return
        val c = SyncStore.config(context)
        if (c.url.isBlank()) {
            onToast("请先配置 WebDAV 同步账号")
            return
        }
        syncing = true
        scope.launch {
            when (val r = SyncStore.syncMulti(context, sessions, messages)) {
                is SyncOutcome.Done -> onToast(r.message)
                is SyncOutcome.NeedPassword -> {
                    pwdMode = "unlock"
                    pwdReason = r.reason
                    pwdField = ""
                    showPwd = true
                }
                is SyncOutcome.Report -> {
                    onApplied(r.sessions, r.messages)
                    refreshConflicts()
                    val parts = buildList {
                        add("上传 ${r.uploaded}")
                        add("下载 ${r.downloaded}")
                        add("删除 ${r.deleted}")
                        if (r.conflicts > 0) add("冲突 ${r.conflicts}")
                    }
                    onToast("同步完成：" + parts.joinToString(" · "))
                }
                is SyncOutcome.Error -> onToast(r.message)
                is SyncOutcome.ApplyRemote -> Unit
            }
            syncing = false
        }
    }

    fun resolveConflict(item: ConflictItem, keepLocal: Boolean) {
        if (syncing) return
        syncing = true
        scope.launch {
            when (val r = SyncStore.resolveConflict(context, item.id, keepLocal, sessions, messages)) {
                is SyncOutcome.Done -> onToast(r.message)
                is SyncOutcome.ApplyRemote -> {
                    val newSessions = r.session?.let { s ->
                        sessions.filterNot { it.platform == s.platform } + s
                    } ?: sessions
                    val newMessages = r.message?.let { m ->
                        if (messages.any { it.id == m.id }) {
                            messages.map { if (it.id == m.id) m else it }
                        } else {
                            messages + m
                        }
                    } ?: messages
                    onApplied(newSessions, newMessages)
                    onToast("已保留云端版本")
                }
                is SyncOutcome.NeedPassword -> {
                    pwdMode = "unlock"
                    pwdReason = r.reason
                    pwdField = ""
                    showPwd = true
                }
                is SyncOutcome.Error -> onToast(r.message)
                is SyncOutcome.Report -> Unit
            }
            refreshConflicts()
            syncing = false
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
        item {
            if (conflicts.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "云同步冲突（${conflicts.size}）",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
                        )
                        Text(
                            text = "同一数据双端都已修改，请逐条选择保留哪一边",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onBackgroundVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                        )
                        conflicts.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = SyncStore.describeConflict(item),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = loginDateText(item.time),
                                        fontSize = 11.sp,
                                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                                    )
                                }
                                TextButton(
                                    text = "保留本地",
                                    onClick = { resolveConflict(item, true) },
                                )
                                Spacer(Modifier.width(6.dp))
                                TextButton(
                                    text = "保留云端",
                                    onClick = { resolveConflict(item, false) },
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = "账号管理",
                    summary = if (expanded) {
                        "已登录 ${sessions.size} 个账号 · 点击收起"
                    } else {
                        "点击展开已登录账号"
                    },
                    onClick = { expanded = !expanded },
                )
                AnimatedVisibility(visible = expanded) {
                    Column {
                        if (sessions.isEmpty()) {
                            Text(
                                text = "暂无登录的账号",
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                        sessions.forEach { session ->
                            AccountManageRow(
                                session = session,
                                onLogoutClick = { pendingLogout = session },
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = "WebDAV 同步",
                    summary = if (cfg.url.isBlank()) {
                        "未配置，点击填写地址与账号"
                    } else {
                        val host = android.net.Uri.parse(cfg.url).host ?: cfg.url
                        "$host · ${cfg.user.ifBlank { "未填账号" }}"
                    },
                    onClick = {
                        val c = SyncStore.config(context)
                        urlField = c.url
                        userField = c.user
                        passField = c.pass
                        showWebDav = true
                    },
                )
                ArrowPreference(
                    title = "云同步密码",
                    summary = if (SyncStore.hasPassword(context)) {
                        "已设置 · 云端数据已加密"
                    } else {
                        "未设置 · 首次同步时设置"
                    },
                    onClick = {
                        pwdMode = if (SyncStore.hasPassword(context)) "change" else "set"
                        pwdReason = if (pwdMode == "change") {
                            "修改后会立即用新密码重新加密云端数据"
                        } else {
                            "首次同步需设置密码，用于加密云端数据"
                        }
                        pwdField = ""
                        showPwd = true
                    },
                )
                ArrowPreference(
                    title = "立即同步",
                    summary = if (syncing) "同步中…" else "上次同步：${SyncStore.lastSyncText(context)}",
                    onClick = { startSync() },
                )
                SwitchPreference(
                    checked = requireBiometric,
                    onCheckedChange = {
                        requireBiometric = it
                        syncPrefs.edit().putBoolean("require_biometric", it).apply()
                        if (it) onToast("已开启，下次打开 App 时验证指纹")
                    },
                    title = "打开时验证指纹",
                    summary = "启动应用前先验证指纹或锁屏密码",
                )
            }
        }
    }

    // WebDAV 配置弹窗
    if (showWebDav) {
        OverlayDialog(
            title = "WebDAV 同步账号",
            summary = "填写同步目录的完整 URL（数据存入其 records/ 子目录）与 WebDAV 账号",
            show = showWebDav,
            onDismissRequest = { showWebDav = false },
        ) {
            // 隐藏光标水滴手柄，避免戳出输入框外（见登录页说明）
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = Color.Transparent,
                    backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.25f),
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                TextField(
                    value = urlField,
                    onValueChange = { urlField = it },
                    label = "文件 URL",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
                TextField(
                    value = userField,
                    onValueChange = { userField = it },
                    label = "用户名",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
                TextField(
                    value = passField,
                    onValueChange = { passField = it },
                    label = "应用密码 / 密码",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
                TextButton(
                    text = "保存",
                    onClick = {
                        if (!urlField.startsWith("http")) {
                            onToast("请输入以 http 开头的完整 URL")
                        } else {
                            SyncStore.saveConfig(
                                context,
                                WebDavConfig(urlField.trim(), userField.trim(), passField),
                            )
                            showWebDav = false
                            onToast("WebDAV 账号已保存")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
                TextButton(
                    text = "取消",
                    onClick = { showWebDav = false },
                    modifier = Modifier.fillMaxWidth(),
                )
                }
            }
        }
    }

    // 云同步密码弹窗
    if (showPwd) {
        OverlayDialog(
            title = when (pwdMode) {
                "unlock" -> "输入云同步密码"
                "change" -> "修改云同步密码"
                else -> "设置云同步密码"
            },
            summary = pwdReason,
            show = showPwd,
            onDismissRequest = { showPwd = false },
        ) {
            // 隐藏光标水滴手柄，避免戳出输入框外（见登录页说明）
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = Color.Transparent,
                    backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.25f),
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                TextField(
                    value = pwdField,
                    onValueChange = { pwdField = it },
                    label = "云同步密码",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
                TextButton(
                    text = "确认",
                    onClick = {
                        if (pwdField.length < 6) {
                            onToast("密码至少 6 位")
                            return@TextButton
                        }
                        val mode = pwdMode
                        SyncStore.setPassword(context, pwdField)
                        showPwd = false
                        when (mode) {
                            "change" -> {
                                scope.launch {
                                    when (val r = SyncStore.reseed(context, sessions, messages)) {
                                        is SyncOutcome.Done -> onToast(r.message)
                                        is SyncOutcome.Error -> onToast(r.message)
                                        is SyncOutcome.NeedPassword -> onToast(r.reason)
                                        else -> onToast("密码已更新")
                                    }
                                }
                            }
                            else -> {
                                onToast("密码已保存，开始同步")
                                startSync()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
                TextButton(
                    text = "取消",
                    onClick = { showPwd = false },
                    modifier = Modifier.fillMaxWidth(),
                )
                }
            }
        }
    }

    // 退出单个账号的二次确认
    pendingLogout?.let { session ->
        ConfirmDialog(
            show = true,
            title = "退出登录",
            summary = "确定退出${platformLabelOf(session.platform)}账号「${session.nickname.ifBlank { session.uid }}」吗？退出后需要重新登录。",
            confirmText = "退出",
            onConfirm = {
                onLogout(session.platform)
                onToast("已退出${platformLabelOf(session.platform)}账号")
                pendingLogout = null
            },
            onDismiss = { pendingLogout = null },
        )
    }
}

@Composable
private fun AccountManageRow(
    session: AuthSession,
    onLogoutClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.nickname.ifBlank { "未命名账号" },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${platformLabelOf(session.platform)} · ${session.uid}",
                fontSize = 11.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(128.dp),
        ) {
            Text(
                text = "登录日期",
                fontSize = 10.sp,
                color = MiuixTheme.colorScheme.onBackgroundVariant,
            )
            Text(
                text = loginDateText(session.savedAt),
                fontSize = 12.sp,
            )
        }
        TextButton(
            text = "退出",
            onClick = onLogoutClick,
        )
    }
}

/** 开源仓库地址（设置行摘要与关于页跳转共用）。 */
internal const val GITHUB_REPO_URL = "https://github.com/Yvhany/miunion"
internal const val GITHUB_REPO_LABEL = "github.com/Yvhany/miunion"

/** 关于页（二级页面）：应用图标与软件名居中靠上，附 GitHub 开源入口。 */
@Composable
internal fun AboutPage() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = "应用图标",
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "yvhan",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "聚合通行证 v1.1.0 · Miuix",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
        Spacer(Modifier.height(36.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            ArrowPreference(
                title = "GitHub 开源",
                summary = GITHUB_REPO_LABEL,
                onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(GITHUB_REPO_URL),
                        )
                    )
                },
            )
        }
    }
}
