package com.miunion.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    onLogout: (String, String) -> Unit,
    onUpdatePhone: (String, String, String) -> Unit,
    onToast: (String) -> Unit,
    nestedScroll: NestedScrollConnection,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncPrefs = remember { context.getSharedPreferences("visual_prefs", android.content.Context.MODE_PRIVATE) }

    var expanded by remember { mutableStateOf(false) }
    var pendingLogout by remember { mutableStateOf<AuthSession?>(null) }
    // 补填/修改手机号
    var editPhoneSession by remember { mutableStateOf<AuthSession?>(null) }
    var phoneField by remember { mutableStateOf("") }

    var showWebDav by remember { mutableStateOf(false) }
    var urlField by remember { mutableStateOf("") }
    var userField by remember { mutableStateOf("") }
    var passField by remember { mutableStateOf("") }

    // pwdMode: set=设置二级密码 / set_plain=明文存储(二次确认后) / change=修改(改后重传) / unlock=解锁云端数据
    var showPwd by remember { mutableStateOf(false) }
    var pwdMode by remember { mutableStateOf("set") }
    var pwdReason by remember { mutableStateOf("") }
    var pwdField by remember { mutableStateOf("") }
    // 不设二级密码的两级确认链：警告 → 二次确认 → 明文设置
    var warnPlain by remember { mutableStateOf(false) }
    var confirmPlain by remember { mutableStateOf(false) }

    var requireBiometric by remember {
        mutableStateOf(syncPrefs.getBoolean("require_biometric", false))
    }

    var syncing by remember { mutableStateOf(false) }
    var conflicts by remember { mutableStateOf(SyncStore.loadConflicts(context)) }

    // 本地备份导入：信封文本 + 密钥弹窗（本地密钥解不开时才弹）
    var importEnvelope by remember { mutableStateOf("") }
    var showImportPwd by remember { mutableStateOf(false) }
    var importPwdField by remember { mutableStateOf("") }

    fun applyBackup(b: BackupStore.Backup) {
        val (mergedS, mergedM) = BackupStore.merge(sessions, messages, b)
        onApplied(mergedS, mergedM)
        onToast("已导入 ${b.sessions.size} 个账号、${b.messages.size} 条消息")
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val pwd = SyncStore.password(context)
        if (pwd.isBlank()) {
            onToast("请先设置密钥，导出文件将用它加密")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                val envelope = withContext(Dispatchers.IO) {
                    SyncStore.seal(pwd, BackupStore.plain(sessions, messages))
                }
                val ok = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(envelope.toByteArray(Charsets.UTF_8))
                        true
                    } ?: false
                }
                if (ok) {
                    onToast("已导出 ${sessions.size} 个账号、${messages.size} 条消息")
                } else {
                    onToast("写入文件失败")
                }
            } catch (e: Exception) {
                onToast("导出失败：${e.message ?: "未知错误"}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                } catch (e: Exception) {
                    null
                }
            }
            if (text.isNullOrBlank()) {
                onToast("读取文件失败")
                return@launch
            }
            importEnvelope = text
            // 先用本地密钥尝试解密；失败再弹输入框
            val localPwd = SyncStore.password(context)
            if (localPwd.isNotBlank()) {
                val b = withContext(Dispatchers.IO) { BackupStore.parse(localPwd, text) }
                if (b != null) {
                    applyBackup(b)
                    return@launch
                }
            }
            importPwdField = ""
            showImportPwd = true
        }
    }

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
                    pwdMode = "set"
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
                    pwdMode = "set"
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
                                onPhoneClick = {
                                    editPhoneSession = session
                                    phoneField = session.phone
                                },
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
                    title = "密钥",
                    summary = when (SyncStore.passwordMode(context)) {
                        "ks" -> "已设置 · 加密存储"
                        "plain" -> "已设置 · 明文存储（建议改用加密存储）"
                        else -> "未设置 · 首次同步时设置"
                    },
                    onClick = {
                        pwdMode = if (SyncStore.hasPassword(context)) "change" else "set"
                        pwdReason = if (pwdMode == "change") {
                            "修改后会立即用新密钥重新加密云端数据（本机加密保存）"
                        } else {
                            "首次同步需设置密钥，用于加密云端数据"
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
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = "导出到文件",
                    summary = if (SyncStore.hasPassword(context)) {
                        "加密导出全部账号与消息，可用「导入」恢复"
                    } else {
                        "需先设置密钥（导出文件用它加密）"
                    },
                    onClick = {
                        if (!SyncStore.hasPassword(context)) {
                            onToast("请先在上方「密钥」中设置")
                        } else {
                            val name = "miunion-backup-" +
                                SimpleDateFormat("yyyyMMdd-HHmm", Locale.CHINA).format(Date()) +
                                ".json"
                            exportLauncher.launch(name)
                        }
                    },
                )
                ArrowPreference(
                    title = "导入备份文件",
                    summary = "选择导出的加密文件恢复，同账号以备份为准",
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
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

    // 密钥弹窗（set=首次设置 / set_plain=明文存储(二次确认后) / change=修改(改后重传)）
    if (showPwd) {
        OverlayDialog(
            title = when (pwdMode) {
                "change" -> "修改密钥"
                else -> "设置密钥"
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
                    label = "密钥",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
                TextButton(
                    text = if (pwdMode == "set") "设置密钥" else "确认",
                    onClick = {
                        if (pwdField.length < 6) {
                            onToast("密钥至少 6 位")
                            return@TextButton
                        }
                        val mode = pwdMode
                        if (mode == "set_plain") {
                            SyncStore.setPasswordPlain(context, pwdField)
                        } else {
                            // set/change 一律以 Keystore 加密方式落盘
                            SyncStore.setPasswordKs(context, pwdField)
                        }
                        showPwd = false
                        when (mode) {
                            "change" -> {
                                scope.launch {
                                    when (val r = SyncStore.reseed(context, sessions, messages)) {
                                        is SyncOutcome.Done -> onToast(r.message)
                                        is SyncOutcome.Error -> onToast(r.message)
                                        is SyncOutcome.NeedPassword -> onToast(r.reason)
                                        else -> onToast("密钥已更新")
                                    }
                                }
                            }
                            "set_plain" -> {
                                onToast("密钥已保存（明文存储），开始同步")
                                startSync()
                            }
                            "set" -> {
                                onToast("密钥已保存（本机加密存储），开始同步")
                                startSync()
                            }
                            else -> {
                                onToast("密钥已保存，开始同步")
                                startSync()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
                if (pwdMode == "set") {
                    TextButton(
                        text = "改用明文存储",
                        onClick = {
                            showPwd = false
                            warnPlain = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(
                    text = "取消",
                    onClick = { showPwd = false },
                    modifier = Modifier.fillMaxWidth(),
                )
                }
            }
        }
    }

    // 明文存储：第一级警告
    if (warnPlain) {
        ConfirmDialog(
            show = true,
            title = "改用明文存储？",
            summary = "不加密保存时，密钥将以明文存储在本机，拿到手机的人可以直接读取并解密你的云端数据。建议加密保存在本机。",
            confirmText = "仍要明文存储",
            onConfirm = {
                warnPlain = false
                confirmPlain = true
            },
            onDismiss = {
                warnPlain = false
                pwdMode = "set"
                pwdReason = "设置后密钥将以加密方式保存在本机"
                pwdField = ""
                showPwd = true
            },
        )
    }

    // 明文存储：第二级确认
    if (confirmPlain) {
        ConfirmDialog(
            show = true,
            title = "确定明文存储？",
            summary = "确定后密钥将明文保存在本机。可随时在 账号与同步 → 密钥 中改用加密存储。",
            confirmText = "确定",
            onConfirm = {
                confirmPlain = false
                pwdMode = "set_plain"
                pwdReason = "密钥将明文保存在本机。可随时在 账号与同步 → 密钥 中改用加密存储。"
                pwdField = ""
                showPwd = true
            },
            onDismiss = { confirmPlain = false },
        )
    }

    // 导入备份：本地密钥解不开时，输入导出时用的密钥
    if (showImportPwd) {
        OverlayDialog(
            title = "导入备份",
            summary = "输入导出时使用的密钥以解密文件",
            show = true,
            onDismissRequest = { showImportPwd = false },
        ) {
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
                        value = importPwdField,
                        onValueChange = { importPwdField = it },
                        label = "密钥",
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    TextButton(
                        text = "解密导入",
                        onClick = {
                            if (importPwdField.isBlank()) {
                                onToast("请输入密钥")
                                return@TextButton
                            }
                            val envelope = importEnvelope
                            scope.launch {
                                val b = withContext(Dispatchers.IO) {
                                    BackupStore.parse(importPwdField, envelope)
                                }
                                if (b == null) {
                                    onToast("密钥错误或不是有效的备份文件")
                                } else {
                                    showImportPwd = false
                                    applyBackup(b)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                    TextButton(
                        text = "取消",
                        onClick = { showImportPwd = false },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // 手机号弹窗（补填入口）
    editPhoneSession?.let { s ->
        OverlayDialog(
            title = "手机号",
            summary = "用于账号库展示：默认掩码显示（如 123******45），右上角眼睛可展开全文",
            show = true,
            onDismissRequest = { editPhoneSession = null },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextField(
                    value = phoneField,
                    onValueChange = { phoneField = it.filter { c -> c.isDigit() }.take(11) },
                    label = "11 位手机号",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
                TextButton(
                    text = "保存",
                    onClick = {
                        if (phoneField.length != 11) {
                            onToast("请输入 11 位手机号")
                            return@TextButton
                        }
                        onUpdatePhone(s.platform, s.uid, phoneField)
                        editPhoneSession = null
                        onToast("手机号已保存")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
                TextButton(
                    text = "取消",
                    onClick = { editPhoneSession = null },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                onLogout(session.platform, session.uid)
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
    onPhoneClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // 行1：昵称 + 退出
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = session.nickname.ifBlank { "未命名账号" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "退出",
                    onClick = onLogoutClick,
                )
            }
            // 行2：平台 · uid + 登录日期
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${platformLabelOf(session.platform)} · ${session.uid}",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "登录于 ${loginDateText(session.savedAt)}",
                    fontSize = 10.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            // 行3：手机号。绑定时已自动获取手机号的账号只展示，不放修改入口；
            // 旧版本绑定（无手机号）的账号在“未绑定手机号”下方提供“补填”
            if (session.phone.isNotBlank()) {
                Text(
                    text = maskPhone(session.phone),
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
            } else {
                Text(
                    text = "未绑定手机号",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                )
                TextButton(
                    text = "补填",
                    onClick = onPhoneClick,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                )
            }
        }
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
