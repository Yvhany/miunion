package com.miunion.app

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class WebDavConfig(
    val url: String,
    val user: String,
    val pass: String,
)

/** 同步结果。Report 携带“应用后的最终本地状态”，由 UI 写回会话与消息列表。 */
sealed class SyncOutcome {
    data class NeedPassword(val reason: String) : SyncOutcome()
    data class Error(val message: String) : SyncOutcome()
    data class Report(
        val sessions: List<AuthSession>,
        val messages: List<UniMessage>,
        val uploaded: Int,
        val downloaded: Int,
        val deleted: Int,
        val conflicts: Int,
    ) : SyncOutcome()

    /** 保留云端：需要 UI 合并到本地列表（session/message 至少一个非空）。 */
    data class ApplyRemote(
        val session: AuthSession?,
        val message: UniMessage?,
    ) : SyncOutcome()

    data class Done(val message: String) : SyncOutcome()
}

/** 一条未决冲突：同一 id 双端都改过，等待用户手动选择保留哪边。 */
data class ConflictItem(
    val id: String,
    val localPayload: String,
    val remotePayload: String,
    val time: Long,
)

/**
 * WebDAV 多文件双向同步。
 *
 * - 数据按条拆分为 records/<id>.json（账号 account_<platform>、消息 uuid）。
 * - 每个文件独立用云同步密码加密（PBKDF2 + AES-GCM 信封，云端只有密文）。
 * - 本地清单 sync_manifest 记录“上次同步哈希”；本地缺文件 + 清单有记录 = 待删除（传播删除），
 *   本地缺文件 + 清单无记录 = 从未同步（远端新增则拉取），不会误判。
 * - 双端同 id 都变更 → 写入 sync_conflicts，等用户在 UI 逐条选择保留本地/云端。
 * - 服务端仅四操作：PROPFIND 列目录 / GET / PUT / DELETE。
 */
object SyncStore {
    private const val PREFS = "sync_prefs"
    private const val MANIFEST = "sync_manifest"
    private const val CONFLICTS = "sync_conflicts"
    private const val FORMAT = "miunion-sync-v1"
    private const val DEFAULT_ITER = 200_000

    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val xmlType = "application/xml; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- WebDAV 账号 ----

    fun config(context: Context): WebDavConfig {
        val p = prefs(context)
        return WebDavConfig(
            url = p.getString("url", "").orEmpty(),
            user = p.getString("user", "").orEmpty(),
            pass = p.getString("pass", "").orEmpty(),
        )
    }

    fun saveConfig(context: Context, cfg: WebDavConfig) {
        prefs(context).edit()
            .putString("url", cfg.url.trim())
            .putString("user", cfg.user.trim())
            .putString("pass", cfg.pass)
            .apply()
    }

    // ---- 云同步密码（本机保存；云端只存密文）----

    fun password(context: Context): String =
        prefs(context).getString("password", "").orEmpty()

    fun setPassword(context: Context, password: String) {
        prefs(context).edit().putString("password", password).apply()
    }

    fun hasPassword(context: Context): Boolean = password(context).isNotBlank()

    fun lastSyncText(context: Context): String {
        val ts = prefs(context).getLong("last_sync", 0L)
        return if (ts > 0) dateText(ts) else "尚未同步"
    }

    private fun markSynced(context: Context) {
        prefs(context).edit().putLong("last_sync", System.currentTimeMillis()).apply()
    }

    // ---- 加解密信封（与旧整包同步一致）----

    private fun deriveKey(password: String, salt: ByteArray, iter: Int, kdf: String): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iter, 256)
        val factory = try {
            SecretKeyFactory.getInstance(kdf)
        } catch (e: Exception) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun seal(password: String, plain: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        var kdf = "PBKDF2WithHmacSHA256"
        val key = try {
            deriveKey(password, salt, DEFAULT_ITER, kdf)
        } catch (e: Exception) {
            kdf = "PBKDF2WithHmacSHA1"
            deriveKey(password, salt, DEFAULT_ITER, kdf)
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val data = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return JSONObject()
            .put("format", FORMAT)
            .put("kdf", kdf)
            .put("iter", DEFAULT_ITER)
            .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(data, Base64.NO_WRAP))
            .toString()
    }

    fun open(password: String, envelope: String): String? = try {
        val o = JSONObject(envelope)
        val salt = Base64.decode(o.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(o.getString("iv"), Base64.NO_WRAP)
        val data = Base64.decode(o.getString("data"), Base64.NO_WRAP)
        val iter = o.optInt("iter", DEFAULT_ITER)
        val kdf = o.optString("kdf", "PBKDF2WithHmacSHA256")
        val key = deriveKey(password, salt, iter, kdf)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        String(cipher.doFinal(data), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    // ---- 记录（每条一个 JSON 文件）----

    private fun sessionToJson(s: AuthSession): JSONObject = JSONObject()
        .put("platform", s.platform)
        .put("uid", s.uid)
        .put("mid", s.mid)
        .put("stoken", s.stoken)
        .put("ltoken", s.ltoken)
        .put("cookieToken", s.cookieToken)
        .put("authToken", s.authToken)
        .put("cred", s.cred)
        .put("nickname", s.nickname)
        .put("expiresAt", s.expiresAt)
        .put("savedAt", s.savedAt)

    private fun sessionFromJson(o: JSONObject): AuthSession = AuthSession(
        platform = o.optString("platform"),
        uid = o.optString("uid"),
        mid = o.optString("mid"),
        stoken = o.optString("stoken"),
        ltoken = o.optString("ltoken"),
        cookieToken = o.optString("cookieToken"),
        authToken = o.optString("authToken"),
        cred = o.optString("cred"),
        nickname = o.optString("nickname"),
        expiresAt = o.optLong("expiresAt"),
        savedAt = o.optLong("savedAt"),
    )

    private fun messageToJson(m: UniMessage): JSONObject = JSONObject()
        .put("id", m.id)
        .put("title", m.title)
        .put("time", m.time)
        .put("desc", m.desc)

    private fun messageFromJson(o: JSONObject): UniMessage = UniMessage(
        title = o.optString("title"),
        time = o.optString("time"),
        desc = o.optString("desc"),
        id = o.optString("id"),
    )

    /** id → 明文载荷（账号 account_<platform>；消息为 uuid，id 为空的历史消息跳过）。 */
    fun buildLocalRecords(
        sessions: List<AuthSession>,
        messages: List<UniMessage>,
    ): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        sessions.forEach { map["account_${it.platform}"] = sessionToJson(it).toString() }
        messages.forEach { m ->
            if (m.id.isNotBlank()) map[m.id] = messageToJson(m).toString()
        }
        return map
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun labelOf(platform: String): String = when (platform) {
        "skland" -> "森空岛"
        "kuro" -> "库街区"
        else -> "米游社"
    }

    /** 冲突条目的展示文案：“账号 · 库街区「昵称」” / “消息 · 标题”。 */
    fun describeConflict(item: ConflictItem): String = try {
        val src = item.localPayload.ifBlank { item.remotePayload }
        val o = JSONObject(src)
        if (o.has("platform")) {
            val name = o.optString("nickname").ifBlank { o.optString("uid") }
            "账号 · ${labelOf(o.optString("platform"))}「$name」"
        } else {
            "消息 · ${o.optString("title")}"
        }
    } catch (e: Exception) {
        item.id
    }

    // ---- 清单（上次同步哈希；条目存在 = 曾同步过）----

    private data class ManifestEntry(val hash: String)

    private fun loadManifest(context: Context): MutableMap<String, ManifestEntry> {
        val raw = prefs(context).getString(MANIFEST, null) ?: return mutableMapOf()
        return try {
            val o = JSONObject(raw)
            val map = mutableMapOf<String, ManifestEntry>()
            for (key in o.keys()) {
                val e = o.optJSONObject(key) ?: continue
                map[key] = ManifestEntry(e.optString("h"))
            }
            map
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun saveManifest(context: Context, map: Map<String, ManifestEntry>) {
        val o = JSONObject()
        map.forEach { (id, e) -> o.put(id, JSONObject().put("h", e.hash)) }
        prefs(context).edit().putString(MANIFEST, o.toString()).apply()
    }

    // ---- 冲突集合 ----

    fun loadConflicts(context: Context): List<ConflictItem> {
        val raw = prefs(context).getString(CONFLICTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ConflictItem(
                    id = o.optString("id"),
                    localPayload = o.optString("local"),
                    remotePayload = o.optString("remote"),
                    time = o.optLong("time"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveConflicts(context: Context, list: List<ConflictItem>) {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("local", c.localPayload)
                    .put("remote", c.remotePayload)
                    .put("time", c.time)
            )
        }
        prefs(context).edit().putString(CONFLICTS, arr.toString()).apply()
    }

    // ---- URL ----

    /** 配置既接受目录地址，也兼容旧的“整文件地址”（取其上级目录）。 */
    private fun baseDir(cfg: WebDavConfig): String {
        var u = cfg.url.trim().trimEnd('/')
        if (u.endsWith(".json")) u = u.substringBeforeLast('/')
        return u
    }

    private fun recordsUrl(cfg: WebDavConfig): String = baseDir(cfg) + "/records"

    private fun fileUrl(cfg: WebDavConfig, id: String): String =
        recordsUrl(cfg) + "/" + id + ".json"

    // ---- 传输四操作 ----

    private fun authHeader(cfg: WebDavConfig): String {
        val raw = "${cfg.user}:${cfg.pass}"
        return "Basic " + Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private sealed class Remote {
        data class Found(val body: String) : Remote()
        object Missing : Remote()
        data class Fail(val message: String) : Remote()
    }

    private suspend fun download(cfg: WebDavConfig, url: String): Remote =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader(cfg))
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    when {
                        resp.code == 200 -> Remote.Found(resp.body?.string().orEmpty())
                        resp.code == 404 -> Remote.Missing
                        resp.code == 401 -> Remote.Fail("WebDAV 账号或密码错误")
                        else -> Remote.Fail("下载失败 HTTP ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                Remote.Fail("网络异常：${e.message ?: "连接失败"}")
            }
        }

    private suspend fun upload(cfg: WebDavConfig, url: String, body: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader(cfg))
                    .put(body.toRequestBody(jsonType))
                    .build()
                client.newCall(req).execute().use { resp ->
                    when {
                        resp.code in 200..204 -> null
                        resp.code == 401 -> "WebDAV 账号或密码错误"
                        resp.code == 409 -> "云端目录不存在（请确认 URL 目录及 records/ 子目录已存在）"
                        else -> "上传失败 HTTP ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                "网络异常：${e.message ?: "连接失败"}"
            }
        }

    private suspend fun delete(cfg: WebDavConfig, url: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("Authorization", authHeader(cfg))
                    .delete()
                    .build()
                client.newCall(req).execute().use { resp ->
                    when {
                        resp.code in 200..204 -> null
                        resp.code == 404 -> null
                        resp.code == 401 -> "WebDAV 账号或密码错误"
                        else -> "删除失败 HTTP ${resp.code}"
                    }
                }
            } catch (e: Exception) {
                "网络异常：${e.message ?: "连接失败"}"
            }
        }

    private sealed class ListResult {
        data class Ok(val ids: Set<String>) : ListResult()
        data class Fail(val message: String) : ListResult()
    }

    /** PROPFIND(Depth:1) 列出 records/ 下的 *.json → id 集合。 */
    private suspend fun listIds(cfg: WebDavConfig): ListResult =
        withContext(Dispatchers.IO) {
            try {
                val xml =
                    """<?xml version="1.0" encoding="utf-8"?><d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/></d:prop></d:propfind>"""
                val req = Request.Builder()
                    .url(recordsUrl(cfg))
                    .header("Authorization", authHeader(cfg))
                    .header("Depth", "1")
                    .method("PROPFIND", xml.toRequestBody(xmlType))
                    .build()
                client.newCall(req).execute().use { resp ->
                    when {
                        resp.code == 207 -> {
                            val body = resp.body?.string().orEmpty()
                            val hrefs = Regex(
                                "(?i)<[^>]*href[^>]*>\\s*([^<\\s]+)\\s*</[^>]*href[^>]*>"
                            ).findAll(body).map { it.groupValues[1] }.toSet()
                            val ids = hrefs.mapNotNull { href ->
                                val name = href.substringAfterLast('/')
                                    .substringBefore('?')
                                val decoded = try {
                                    URLDecoder.decode(name, "UTF-8")
                                } catch (e: Exception) {
                                    name
                                }
                                decoded.removeSuffix("/").removeSuffix(".json")
                                    .takeIf { decoded.endsWith(".json") }
                            }.filter { it.isNotBlank() }.toSet()
                            ListResult.Ok(ids)
                        }
                        resp.code == 404 -> ListResult.Ok(emptySet()) // records/ 尚无内容
                        resp.code == 401 -> ListResult.Fail("WebDAV 账号或密码错误")
                        resp.code == 405 -> ListResult.Fail("服务端不支持列目录（PROPFIND）")
                        else -> ListResult.Fail("列目录失败 HTTP ${resp.code}")
                    }
                }
            } catch (e: Exception) {
                ListResult.Fail("网络异常：${e.message ?: "连接失败"}")
            }
        }

    // ---- 主同步流程 ----

    /**
     * 双向同步。返回 Report（含应用后的最终本地状态），由 UI 写回。
     * 不做任何自动覆盖：同 id 双端变更进冲突列表。
     */
    suspend fun syncMulti(
        context: Context,
        sessions: List<AuthSession>,
        messages: List<UniMessage>,
    ): SyncOutcome {
        val cfg = config(context)
        if (cfg.url.isBlank()) return SyncOutcome.Error("请先配置 WebDAV 账号")

        val remoteIds = when (val r = listIds(cfg)) {
            is ListResult.Ok -> r.ids
            is ListResult.Fail -> return SyncOutcome.Error(r.message)
        }

        val password = password(context)
        if (password.isBlank()) {
            return SyncOutcome.NeedPassword(
                if (remoteIds.isNotEmpty()) {
                    "首次从云端同步，请输入云同步密码解密数据"
                } else {
                    "首次同步，请设置云同步密码（用于加密云端数据）"
                }
            )
        }

        // 下载并解密远端全部记录
        val remote = LinkedHashMap<String, String>()
        for (id in remoteIds) {
            when (val d = download(cfg, fileUrl(cfg, id))) {
                is Remote.Found -> {
                    val plain = open(password, d.body)
                        ?: return SyncOutcome.Error("云同步密码错误或云端数据损坏")
                    remote[id] = plain
                }
                Remote.Missing -> Unit // 列表与下载之间被删，按不存在处理
                is Remote.Fail -> return SyncOutcome.Error(d.message)
            }
        }

        val local = buildLocalRecords(sessions, messages)
        val manifest = loadManifest(context)
        val conflicts = loadConflicts(context).toMutableList()
        val conflictIds = conflicts.map { it.id }.toSet()

        val toUpload = LinkedHashMap<String, String>()
        val downloaded = LinkedHashMap<String, String>()
        val deleteLocalIds = mutableSetOf<String>()
        val deleteRemoteIds = mutableSetOf<String>()
        val newManifest = manifest.toMutableMap()
        var conflictCount = 0

        fun markLive(id: String, payload: String) {
            newManifest[id] = ManifestEntry(sha256(payload))
        }

        val allIds = linkedSetOf<String>().apply {
            addAll(local.keys)
            addAll(remote.keys)
            addAll(manifest.keys)
        }

        for (id in allIds) {
            if (id in conflictIds) {
                conflictCount++ // 已有未决冲突，等待人工处理
                continue
            }
            val lp = local[id]
            val rp = remote[id]
            val me = manifest[id]
            when {
                lp != null && rp != null -> {
                    val lh = sha256(lp)
                    val rh = sha256(rp)
                    if (lh == rh) {
                        markLive(id, lp)
                        continue
                    }
                    val last = me?.hash
                    val localChanged = last == null || lh != last
                    val remoteChanged = last == null || rh != last
                    when {
                        !localChanged -> {
                            downloaded[id] = rp
                            markLive(id, rp)
                        }
                        !remoteChanged -> {
                            toUpload[id] = lp
                            markLive(id, lp)
                        }
                        else -> {
                            conflicts.add(ConflictItem(id, lp, rp, System.currentTimeMillis()))
                            conflictCount++
                        }
                    }
                }
                lp != null && rp == null -> if (me == null) {
                    // 从未同步过的新本地记录 → 上传
                    toUpload[id] = lp
                    markLive(id, lp)
                } else {
                    // 曾同步过、远端缺失 → 另一台删除了它 → 删除本地（删除传播）
                    deleteLocalIds.add(id)
                    newManifest.remove(id)
                }
                lp == null && rp != null -> if (me == null) {
                    // 从未同步过的远端新记录 → 拉取
                    downloaded[id] = rp
                    markLive(id, rp)
                } else {
                    // 曾同步过、本地缺失 → 本机已删除 → 删除远端（删除传播）
                    deleteRemoteIds.add(id)
                    newManifest.remove(id)
                }
                else -> {
                    // 双端都没有 → 清理清单残留
                    if (me != null) newManifest.remove(id)
                }
            }
        }

        // 冲突不参与自动传输
        saveConflicts(context, conflicts)

        // 执行上传
        for ((id, payload) in toUpload) {
            upload(cfg, fileUrl(cfg, id), seal(password, payload))?.let {
                return SyncOutcome.Error(it)
            }
        }
        // 执行远端删除
        for (id in deleteRemoteIds) {
            delete(cfg, fileUrl(cfg, id))?.let {
                return SyncOutcome.Error(it)
            }
        }

        // 计算最终本地状态
        var finalSessions = sessions.toList()
        var finalMessages = messages.toList()
        for (id in deleteLocalIds) {
            if (id.startsWith("account_")) {
                val platform = id.removePrefix("account_")
                finalSessions = finalSessions.filterNot { it.platform == platform }
            } else {
                finalMessages = finalMessages.filterNot { it.id == id }
            }
        }
        for ((id, payload) in downloaded) {
            try {
                val o = JSONObject(payload)
                if (o.has("platform")) {
                    val s = sessionFromJson(o)
                    finalSessions = finalSessions.filterNot { it.platform == s.platform } + s
                } else {
                    val m = messageFromJson(o)
                    finalMessages = if (finalMessages.any { it.id == m.id }) {
                        finalMessages.map { if (it.id == m.id) m else it }
                    } else {
                        finalMessages + m
                    }
                }
                markLive(id, payload)
            } catch (e: Exception) {
                return SyncOutcome.Error("记录格式不正确：$id")
            }
        }

        // 清单收尾：两端都不存在的 id 清除；上传/下载的记录已 markLive
        val finalLocalIds = buildLocalRecords(finalSessions, finalMessages).keys
        val finalRemoteIds = (remoteIds - deleteRemoteIds) + toUpload.keys
        newManifest.keys.retainAll { it in finalLocalIds || it in finalRemoteIds }
        // （retainAll 中仅一侧存在也保留：另一侧待传播）
        saveManifest(context, newManifest)
        markSynced(context)

        return SyncOutcome.Report(
            sessions = finalSessions,
            messages = finalMessages,
            uploaded = toUpload.size,
            downloaded = downloaded.size,
            deleted = deleteLocalIds.size + deleteRemoteIds.size,
            conflicts = conflictCount,
        )
    }

    /** 修改云同步密码后：以新密码全量重传本地、删除远端多余文件（本地优先）。 */
    suspend fun reseed(
        context: Context,
        sessions: List<AuthSession>,
        messages: List<UniMessage>,
    ): SyncOutcome {
        val cfg = config(context)
        if (cfg.url.isBlank()) return SyncOutcome.Error("请先配置 WebDAV 账号")
        val password = password(context)
        if (password.isBlank()) return SyncOutcome.NeedPassword("请先设置云同步密码")

        val remoteIds = when (val r = listIds(cfg)) {
            is ListResult.Ok -> r.ids
            is ListResult.Fail -> return SyncOutcome.Error(r.message)
        }
        val local = buildLocalRecords(sessions, messages)
        for ((id, payload) in local) {
            upload(cfg, fileUrl(cfg, id), seal(password, payload))?.let {
                return SyncOutcome.Error(it)
            }
        }
        for (id in remoteIds) {
            if (id !in local.keys) {
                delete(cfg, fileUrl(cfg, id))?.let {
                    return SyncOutcome.Error(it)
                }
            }
        }
        saveManifest(
            context,
            local.mapValues { (_, payload) -> ManifestEntry(sha256(payload)) }
        )
        saveConflicts(context, emptyList())
        markSynced(context)
        return SyncOutcome.Done("已用新密码重新加密上传（本地数据为准）")
    }

    /**
     * 处理一条冲突。
     * keepLocal=true：上传当前本地版本覆盖云端；否则采用云端版本并返回 ApplyRemote 供 UI 合并。
     */
    suspend fun resolveConflict(
        context: Context,
        id: String,
        keepLocal: Boolean,
        sessions: List<AuthSession>,
        messages: List<UniMessage>,
    ): SyncOutcome {
        val cfg = config(context)
        if (cfg.url.isBlank()) return SyncOutcome.Error("请先配置 WebDAV 账号")
        val password = password(context)
        if (password.isBlank()) return SyncOutcome.NeedPassword("请先设置云同步密码")
        val conflicts = loadConflicts(context).toMutableList()
        val entry = conflicts.firstOrNull { it.id == id }
            ?: return SyncOutcome.Error("该冲突已不存在")

        if (keepLocal) {
            val payload = buildLocalRecords(sessions, messages)[id] ?: entry.localPayload
            upload(cfg, fileUrl(cfg, id), seal(password, payload))?.let {
                return SyncOutcome.Error(it)
            }
            val manifest = loadManifest(context)
            manifest[id] = ManifestEntry(sha256(payload))
            saveManifest(context, manifest)
            saveConflicts(context, conflicts.filterNot { it.id == id })
            markSynced(context)
            return SyncOutcome.Done("已保留本地版本并上传")
        }

        // 保留云端
        val plain = entry.remotePayload
        val manifest = loadManifest(context)
        manifest[id] = ManifestEntry(sha256(plain))
        saveManifest(context, manifest)
        saveConflicts(context, conflicts.filterNot { it.id == id })
        markSynced(context)
        return try {
            val o = JSONObject(plain)
            if (o.has("platform")) {
                SyncOutcome.ApplyRemote(session = sessionFromJson(o), message = null)
            } else {
                SyncOutcome.ApplyRemote(session = null, message = messageFromJson(o))
            }
        } catch (e: Exception) {
            SyncOutcome.Error("云端数据格式不正确")
        }
    }

    private fun dateText(ts: Long): String =
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
            .format(java.util.Date(ts))
}
