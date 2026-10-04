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
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher

data class AuthSession(
    val platform: String,
    val uid: String,
    val mid: String,
    val stoken: String = "",
    val ltoken: String = "",
    val cookieToken: String = "",
    val authToken: String = "",
    val cred: String = "",
    val nickname: String = "",
    val expiresAt: Long = 0L,
    val savedAt: Long = 0L,
)

sealed class AuthResult {
    data class Success(val session: AuthSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/** 库街区发短信结果（抓包：getSmsCode 返回 geeTest=true 表示需先过极验，短信未发出）。 */
sealed class SmsResult {
    object NeedCaptcha : SmsResult()
    object Sent : SmsResult()
    data class Error(val message: String) : SmsResult()
}

/** 库街区扫码预检结果（qrCode/scan + scanSms）。 */
sealed class KuroScanResult {
    data class Ready(val roles: List<String>) : KuroScanResult()
    data class Error(val message: String) : KuroScanResult()
}

object AuthStore {
    private const val FILE = "auth_sessions.json"

    fun load(context: Context): MutableList<AuthSession> = try {
        val text = context.openFileInput(FILE).bufferedReader().use { it.readText() }
        val raw = JSONObject(text).opt("sessions")
        val arr = if (raw is JSONArray) raw else JSONArray(raw.toString())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AuthSession(
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
        }.toMutableList()
    } catch (e: Exception) {
        mutableListOf()
    }

    fun save(context: Context, sessions: List<AuthSession>) {
        val array = JSONArray()
        sessions.forEach { s ->
            array.put(
                JSONObject().apply {
                    put("platform", s.platform)
                    put("uid", s.uid)
                    put("mid", s.mid)
                    put("stoken", s.stoken)
                    put("ltoken", s.ltoken)
                    put("cookieToken", s.cookieToken)
                    put("authToken", s.authToken)
                    put("cred", s.cred)
                    put("nickname", s.nickname)
                    put("expiresAt", s.expiresAt)
                    put("savedAt", s.savedAt)
                }
            )
        }
        val root = JSONObject().put("sessions", array)
        context.openFileOutput(FILE, Context.MODE_PRIVATE).bufferedWriter().use { it.write(root.toString()) }
    }
}

object AuthApi {
    private const val MI_HOYO_UA = "Mozilla/5.0 (Linux; Android 13; 22011211C Build/TP1A.220624.014; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/104.0.5112.97 Mobile Safari/537.36 miHoYoBBS/2.81.1"
    private const val MI_HOYO_SALT = "t0qEgfub6cvueAPgR5m9aQWWVciEer7v"
    private const val MI_HOYO_SALT_K2 = "QVu5OdwEWxkq9ygpYBgDprR5tI471HWQ"
    private const val APP_ID = "bll8iq97cem8"
    private const val SKLAND_APP_CODE = "4ca99fa6b56cc2ba"
    private const val SKLAND_SCAN_LOGIN = "https://as.hypergryph.com/user/info/v1/scan_login"
    private const val SKLAND_UPDATE_SCAN_STATUS = "https://as.hypergryph.com/user/info/v1/update_scan_status"

    private const val MI_HOYO_PUB_KEY = "-----BEGIN PUBLIC KEY-----\n" +
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDvekdPMHN3AYhm/vktJT+YJr7\n" +
        "cI5DcsNKqdsx5DZX0gDuWFuIjzdwButrIYPNmRJ1G8ybDIF7oDW2eEpm5sMbL9zs\n" +
        "9ExXCdvqrn51qELbqj0XxtMTIpaCHFSI50PfPpTFV9Xt/hmyVwokoOXFlAEgCn+Q\n" +
        "CgGs52bFoYMtyi+xEQIDAQAB\n" +
        "-----END PUBLIC KEY-----\n"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // 设备标识类头/字段：抓包原值是作者设备的隐私，开源版改为运行时生成（保持原格式），
    // 型号类头取当前设备真实 Build 值；同一次进程内保持一致（头与 body 配对）。
    private fun randomHex(len: Int): String =
        (1..len).joinToString("") { "0123456789abcdef".random().toString() }

    private val deviceFp = randomHex(13)
    private val sklandDeviceId = randomHex(32)
    private val kuroDevCode = randomHex(40)
    private val kuroDistinctId = UUID.randomUUID().toString()
    private val deviceModel: String get() = android.os.Build.MODEL
    private val deviceName: String get() = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

    private fun rsaEncrypt(content: String): String {
        val keyBytes = Base64.decode(
            MI_HOYO_PUB_KEY
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", ""),
            Base64.DEFAULT
        )
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return Base64.encodeToString(cipher.doFinal(content.toByteArray()), Base64.NO_WRAP)
    }

    private fun md5(s: String): String {
        val d = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    private fun ds2(body: JSONObject): String {
        val t = (System.currentTimeMillis() / 1000).toString()
        val r = (100000 + (0..99999).random()).toString()
        val b = body.toString()
        val sign = md5("salt=$MI_HOYO_SALT&t=$t&r=$r&b=$b&q=")
        return "$t,$r,$sign"
    }

    private fun ds1(): String {
        val t = (System.currentTimeMillis() / 1000).toString()
        val r = (0 until 6).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
        val sign = md5("salt=$MI_HOYO_SALT_K2&t=$t&r=$r")
        return "$t,$r,$sign"
    }

    private suspend fun post(url: String, body: JSONObject, headers: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(url)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .build()
            client.newCall(req).execute().use { it.body?.string() ?: "" }
        }

    /** form-urlencoded POST（库街区接口全部使用该编码，报文按抓包原样）。 */
    private suspend fun postForm(url: String, body: String, headers: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaType()))
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .build()
            client.newCall(req).execute().use { it.body?.string() ?: "" }
        }

    private suspend fun get(url: String, headers: Map<String, String>): String =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(url)
                .get()
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .build()
            client.newCall(req).execute().use { it.body?.string() ?: "" }
        }

    private fun mihoyoHeaders(body: JSONObject?, cookie: String = ""): Map<String, String> {
        val h = mutableListOf<Pair<String, String>>(
            "User-Agent" to MI_HOYO_UA,
            "Accept" to "application/json",
            "Content-Type" to "application/json",
            "Referer" to "https://app.mihoyo.com",
            "x-rpc-app_version" to "2.81.1",
            "x-rpc-app_id" to APP_ID,
            "x-rpc-client_type" to "2",
            "x-rpc-game_biz" to "bbs_cn",
        )
        if (cookie.isNotBlank()) h.add("Cookie" to cookie)
        h.add("DS" to if (body != null) ds2(body) else ds1())
        return h.toMap()
    }

    private fun mihoyoSdkHeaders(device: String): Map<String, String> = mapOf(
        "Accept" to "application/json",
        "User-Agent" to "okhttp/4.9.3",
        "Content-Type" to "application/json",
        "x-rpc-app_id" to APP_ID,
        "x-rpc-client_type" to "2",
        "x-rpc-device_id" to device,
        "x-rpc-device_fp" to deviceFp,
        "x-rpc-device_name" to deviceName,
        "x-rpc-device_model" to deviceModel,
        "x-rpc-sys_version" to "17",
        "x-rpc-game_biz" to "bbs_cn",
        "x-rpc-app_version" to "2.116.0",
        "x-rpc-sdk_version" to "2.44.0",
        "x-rpc-lifecycle_id" to UUID.randomUUID().toString(),
        "x-rpc-account_version" to "2.44.0",
    )

    private fun mihoyoScanHeaders(body: JSONObject, cookie: String, device: String): Map<String, String> =
        mihoyoSdkHeaders(device) + mapOf(
            "Cookie" to cookie,
            "DS" to ds2(body),
        )

    suspend fun sendMihoyoSms(phone: String): AuthResult {
        val body = JSONObject()
            .put("area_code", rsaEncrypt("+86"))
            .put("mobile", rsaEncrypt(phone))
        val resp = post("https://passport-api.mihoyo.com/account/ma-cn-verifier/verifier/createLoginCaptcha", body, mihoyoHeaders(body))
        return parseRetcode(resp)
    }

    suspend fun loginMihoyo(phone: String, code: String): AuthResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("area_code", rsaEncrypt("+86"))
            .put("mobile", rsaEncrypt(phone))
            .put("action_type", "login_by_mobile_captcha")
            .put("captcha", code)
        val raw = post("https://passport-api.mihoyo.com/account/ma-cn-passport/app/loginByMobileCaptcha", body, mihoyoHeaders(body))
        return@withContext try {
            val obj = JSONObject(raw)
            if (obj.optInt("retcode") != 0) {
                AuthResult.Error(obj.optString("message", "登录失败"))
            } else {
                val data = obj.getJSONObject("data")
                val tokenObj = data.optJSONObject("token")
                val userInfo = data.optJSONObject("user_info")
                val stoken = tokenObj?.optString("token").orEmpty()
                val aid = userInfo?.optString("aid").orEmpty()
                val mid = userInfo?.optString("mid").orEmpty()
                var cookieToken = ""
                var ltoken = ""
                if (stoken.isNotBlank()) {
                    cookieToken = getMihoyoCookieToken(stoken, mid)
                    ltoken = getMihoyoLToken(stoken, mid)
                }
                val nickname = if (stoken.isNotBlank()) {
                    getMihoyoNickname(aid, mid, stoken, cookieToken, ltoken)
                } else {
                    ""
                }
                AuthResult.Success(
                    AuthSession(
                        platform = "mihoyo",
                        uid = aid,
                        mid = mid,
                        stoken = stoken,
                        ltoken = ltoken,
                        cookieToken = cookieToken,
                        nickname = nickname,
                        expiresAt = 0L,
                        savedAt = System.currentTimeMillis(),
                    )
                )
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "解析失败")
        }
    }

    private suspend fun getMihoyoCookieToken(stoken: String, mid: String): String {
        val cookie = "stoken=$stoken; mid=$mid"
        val resp = get(
            "https://passport-api.mihoyo.com/account/auth/api/getCookieAccountInfoBySToken?stoken=$stoken",
            mihoyoHeaders(null, cookie)
        )
        return try {
            val o = JSONObject(resp)
            if (o.optInt("retcode") == 0) o.getJSONObject("data").optString("cookie_token") else ""
        } catch (e: Exception) { "" }
    }

    private suspend fun getMihoyoLToken(stoken: String, mid: String): String {
        val cookie = "stoken=$stoken; mid=$mid"
        val resp = get(
            "https://passport-api.mihoyo.com/account/auth/api/getLTokenBySToken",
            mihoyoHeaders(null, cookie)
        )
        return try {
            val o = JSONObject(resp)
            if (o.optInt("retcode") == 0) o.getJSONObject("data").optString("ltoken") else ""
        } catch (e: Exception) { "" }
    }

    private suspend fun getMihoyoNickname(
        uid: String,
        mid: String,
        stoken: String,
        cookieToken: String,
        ltoken: String,
    ): String {
        if (uid.isBlank() || mid.isBlank() || stoken.isBlank()) return ""
        val cookie = buildString {
            append("stoken=$stoken; mid=$mid")
            if (cookieToken.isNotBlank()) {
                append("; cookie_token=$cookieToken; account_id=$uid")
            }
            if (ltoken.isNotBlank()) {
                append("; ltoken=$ltoken; ltuid=$uid")
            }
        }
        val resp = get(
            "https://bbs-api.miyoushe.com/user/wapi/getUserFullInfo?uid=$uid",
            mihoyoHeaders(null, cookie)
        )
        return try {
            val o = JSONObject(resp)
            if (o.optInt("retcode") == 0) {
                val info = o.optJSONObject("data")?.optJSONObject("user_info")
                info?.optString("nickname").orEmpty()
                    .ifBlank { info?.optString("screen_name").orEmpty() }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun sendSklandSms(phone: String): AuthResult {
        val body = JSONObject().put("phone", phone).put("type", 2)
        val resp = post("https://as.hypergryph.com/general/v1/send_phone_code", body, sklandHeaders())
        return parseRetcode(resp)
    }

    suspend fun loginSkland(phone: String, code: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("phone", phone).put("code", code)
            val raw = post("https://as.hypergryph.com/user/auth/v2/token_by_phone_code", body, sklandHeaders())
            val obj = JSONObject(raw)
            if (obj.optInt("status") != 0) {
                return@withContext AuthResult.Error(obj.optString("msg", "登录失败"))
            }
            val token = obj.getJSONObject("data").optString("token")

            val grantBody = JSONObject().put("token", token).put("appCode", SKLAND_APP_CODE).put("type", 0)
            val grantRaw = post("https://as.hypergryph.com/user/oauth2/v2/grant", grantBody, sklandHeaders())
            val grantObj = JSONObject(grantRaw)
            if (grantObj.optInt("status") != 0) {
                return@withContext AuthResult.Error(grantObj.optString("msg", "OAuth 授权失败"))
            }
            val grantData = grantObj.getJSONObject("data")
            val oauthCode = grantData.optString("code")
            val uid = grantData.optString("uid")

            val credBody = JSONObject().put("kind", 1).put("code", oauthCode)
            val credRaw = post("https://zonai.skland.com/api/v1/user/auth/generate_cred_by_code", credBody, sklandHeaders())
            val credObj = JSONObject(credRaw)
            if (credObj.optInt("code") != 0) {
                return@withContext AuthResult.Error(credObj.optString("message", "获取 cred 失败"))
            }
            val credData = credObj.getJSONObject("data")
            val cred = credData.optString("cred")
            val nickname = getSklandNickname(token, cred)
            AuthResult.Success(
                AuthSession(
                    platform = "skland",
                    uid = credData.optString("userId").ifBlank { uid },
                    mid = uid,
                    authToken = token,
                    cred = cred,
                    nickname = nickname,
                    expiresAt = 0L,
                    savedAt = System.currentTimeMillis(),
                )
            )
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "网络异常")
        }
    }

    private suspend fun getSklandNickname(authToken: String, cred: String): String {
        if (authToken.isNotBlank()) {
            try {
                val resp = get(
                    "https://as.hypergryph.com/user/info/v1/basic?token=$authToken",
                    sklandHeaders()
                )
                val o = JSONObject(resp)
                if (o.optInt("status") == 0) {
                    val d = o.optJSONObject("data")
                    val user = d?.optJSONObject("user") ?: d
                    val name = user?.optString("name").orEmpty()
                        .ifBlank { user?.optString("nickname").orEmpty() }
                    if (name.isNotBlank()) return name
                }
            } catch (e: Exception) {
                // 继续用 cred 兜底拉取
            }
        }
        if (cred.isNotBlank()) {
            try {
                val resp = get(
                    "https://zonai.skland.com/api/v1/user/check",
                    mapOf(
                        "Cred" to cred,
                        "User-Agent" to "Skland/2.0.0 Android",
                        "Accept" to "application/json",
                    )
                )
                val o = JSONObject(resp)
                if (o.optInt("code") == 0) {
                    val d = o.optJSONObject("data")
                    val user = d?.optJSONObject("user") ?: d
                    return user?.optString("userName").orEmpty()
                        .ifBlank { user?.optString("nickname").orEmpty() }
                }
            } catch (e: Exception) {
                // 忽略，昵称留空也不会阻断登录
            }
        }
        return ""
    }

    private fun sklandHeaders(): Map<String, String> = mapOf(
        "User-Agent" to "Skland/2.0.0 Android",
        "Content-Type" to "application/json",
        "Accept" to "application/json",
    )

    private fun sklandScanHeaders(): Map<String, String> = mapOf(
        "User-Agent" to "okhttp/4.12.0",
        "Accept-Encoding" to "gzip",
        "Content-Type" to "application/json; charset=utf-8",
        "x-devicemodel" to deviceModel,
        "x-devicetype" to "1",
        "x-osver" to "17",
        "x-deviceid" to sklandDeviceId,
    )

    private fun sklandScanError(raw: String): String? = try {
        val o = JSONObject(raw)
        if (o.optInt("status") == 0 || o.optInt("code") == 0) {
            null
        } else {
            o.optString("msg", o.optString("message", "请求失败"))
        }
    } catch (e: Exception) {
        "响应解析失败"
    }

    private fun parseRetcode(resp: String): AuthResult =
        try {
            val o = JSONObject(resp)
            if (o.optInt("retcode") != 0 && o.optInt("status") != 0 && o.optInt("code") != 0) {
                AuthResult.Error(o.optString("message", o.optString("msg", "请求失败")))
            } else {
                AuthResult.Success(AuthSession("", "", ""))
            }
        } catch (e: Exception) {
            AuthResult.Error("响应解析失败")
        }

    private fun qrParam(raw: String, key: String, terminators: String): String {
        val marker = "$key="
        val start = raw.indexOf(marker)
        if (start < 0) return ""
        val valueStart = start + marker.length
        var valueEnd = raw.length
        for (i in valueStart until raw.length) {
            if (terminators.indexOf(raw[i]) >= 0) {
                valueEnd = i
                break
            }
        }
        return raw.substring(valueStart, valueEnd)
    }

    private fun qrAppName(raw: String): String {
        val encoded = qrParam(raw, "app_name", "&")
        if (encoded.isBlank()) return ""
        return try {
            URLDecoder.decode(encoded, "UTF-8")
        } catch (e: Exception) {
            encoded
        }
    }

    private fun gameQrBase(appId: Int, bizKey: String): String = when {
        appId == 8 || bizKey == "hkrpg_cn" -> "https://hkrpg-sdk.mihoyo.com/hkrpg_cn"
        appId == 4 || bizKey == "hk4e_cn" -> "https://api-sdk.mihoyo.com/hk4e_cn"
        appId == 12 || bizKey == "nap_cn" -> "https://api-sdk.mihoyo.com/nap_cn"
        appId == 1 || bizKey == "bh3_cn" -> "https://api-sdk.mihoyo.com/bh3_cn"
        else -> ""
    }

    fun mihoyoQrAppName(raw: String): String = qrAppName(raw)

    suspend fun confirmMihoyoQr(raw: String, session: AuthSession): AuthResult = withContext(Dispatchers.IO) {
        val ticket = qrParam(raw, "tk", "&").ifBlank { qrParam(raw, "ticket", "&") }
        var tokenTypes = qrParam(raw, "token_types", "#")
        val appId = qrParam(raw, "app_id", "&").toIntOrNull() ?: 0
        val bizKey = qrParam(raw, "biz_key", "&")
        val gameBase = gameQrBase(appId, bizKey)
        val isGameQr = ticket.isNotBlank() && (appId > 0 || bizKey.isNotBlank()) && gameBase.isNotBlank()
        if (ticket.isBlank() || (!isGameQr && tokenTypes.isBlank())) {
            return@withContext AuthResult.Error("不是有效的米游社登录二维码")
        }
        if (session.stoken.isBlank() || session.mid.isBlank()) {
            return@withContext AuthResult.Error("米游社账号凭证缺失，请重新登录")
        }
        val device = UUID.randomUUID().toString().replace("-", "").take(16)

        var loginTicket = ticket
        if (isGameQr) {
            val scanBody = JSONObject()
                .put("passport_app_id", APP_ID)
                .put("ticket", ticket)
                .put("app_id", appId)
                .put("device", device)
                .put("ts", System.currentTimeMillis() / 1000)
            val scanRaw = post("$gameBase/combo/panda/qrcode/scan", scanBody, mihoyoSdkHeaders(device))
            loginTicket = try {
                val o = JSONObject(scanRaw)
                if (o.optInt("retcode") != 0) {
                    return@withContext AuthResult.Error(o.optString("message", "扫码失败"))
                }
                val url = o.optJSONObject("data")?.optString("passport_qr_url").orEmpty()
                qrParam(url, "tk", "&").ifBlank { qrParam(url, "ticket", "&") }
                    .also {
                        val types = qrParam(url, "token_types", "&#")
                        if (types.isNotBlank()) tokenTypes = types
                    }
            } catch (e: Exception) {
                return@withContext AuthResult.Error("扫码响应解析失败")
            }
            if (loginTicket.isBlank()) {
                return@withContext AuthResult.Error("扫码响应缺少登录凭证")
            }
        }

        val body = JSONObject()
            .put("ticket", loginTicket)
            .put("token_types", JSONArray().put(tokenTypes))
        val cookie = "stoken=${session.stoken}; mid=${session.mid}"
        val headers = mihoyoScanHeaders(body, cookie, device)
        val scanRaw = post(
            "https://passport-api.mihoyo.com/account/ma-cn-passport/app/scanQRLogin",
            body,
            headers,
        )
        val scanError = try {
            val o = JSONObject(scanRaw)
            if (o.optInt("retcode") == 0) "" else o.optString("message", "扫码失败")
        } catch (e: Exception) {
            "扫码响应解析失败"
        }
        if (scanError.isNotBlank()) return@withContext AuthResult.Error(scanError)

        val confirmRaw = post(
            "https://passport-api.mihoyo.com/account/ma-cn-passport/app/confirmQRLogin",
            body,
            headers,
        )
        val confirmError = try {
            val o = JSONObject(confirmRaw)
            if (o.optInt("retcode") == 0) "" else o.optString("message", "确认登录失败")
        } catch (e: Exception) {
            "确认响应解析失败"
        }
        if (confirmError.isNotBlank()) return@withContext AuthResult.Error(confirmError)

        return@withContext AuthResult.Success(AuthSession("", "", ""))
    }

    suspend fun confirmSklandQr(raw: String, session: AuthSession): AuthResult = withContext(Dispatchers.IO) {
        val scanId = qrParam(raw, "scanId", "&")
            .ifBlank { qrParam(raw, "scan_id", "&") }
        if (scanId.isBlank()) {
            return@withContext AuthResult.Error("不是有效的森空岛登录二维码")
        }
        if (session.authToken.isBlank()) {
            return@withContext AuthResult.Error("森空岛账号凭证缺失，请重新登录")
        }
        val headers = sklandScanHeaders()
        val scanBody = JSONObject()
            .put("token", session.authToken)
            .put("appCode", SKLAND_APP_CODE)
            .put("scanId", scanId)
        val scanMsg = sklandScanError(post(SKLAND_SCAN_LOGIN, scanBody, headers))
        if (scanMsg != null) {
            return@withContext AuthResult.Error(scanMsg)
        }
        val updateBody = JSONObject()
            .put("token", session.authToken)
            .put("scanId", scanId)
            .put("rememberLogin", false)
        val updateMsg = sklandScanError(post(SKLAND_UPDATE_SCAN_STATUS, updateBody, headers))
        if (updateMsg != null) {
            return@withContext AuthResult.Error(updateMsg)
        }
        AuthResult.Success(AuthSession("", "", ""))
    }

    // ---- 库街区（kurobbs，报文全部来自抓包） ----

    private const val KURO_SEND_SMS = "https://api.kurobbs.com/user/getSmsCode"
    private const val KURO_SDK_LOGIN = "https://api.kurobbs.com/user/sdkLogin"
    private const val KURO_QR_SCAN = "https://api.kurobbs.com/user/qrCode/scan"
    private const val KURO_SCAN_SMS = "https://api.kurobbs.com/user/sms/scanSms"
    private const val KURO_SCAN_LOGIN = "https://api.kurobbs.com/user/auth/scanLogin"

    /** 登录类接口请求头（getSmsCode/sdkLogin；字段结构按抓包，设备标识值开源版运行时生成，未登录时无 user_token 故不带 Cookie）。 */
    private fun kuroLoginHeaders(): Map<String, String> = mapOf(
        "User-Agent" to "okhttp/3.11.0",
        "osVersion" to "37",
        "devCode" to kuroDevCode,
        "distinct_id" to kuroDistinctId,
        "countryCode" to "CN",
        "model" to deviceModel,
        "source" to "android",
        "lang" to "zh-Hans",
        "version" to "3.4.0",
        "versionCode" to "30400",
        "channelId" to "4",
    )

    /**
     * 扫码确认类接口请求头（qrCode/scan、scanSms、scanLogin 抓包原样：token + Cookie）。
     * 抓包 Cookie 含 acw_tc（阿里云 WAF 动态会话，无法复用固定值）；实测携带固定 Cookie
     * 被服务端以 500「服务器内部错误」拦截，按 spec fallback 去掉 Cookie 仅保留抓包头。
     */
    private fun kuroSessionHeaders(session: AuthSession): Map<String, String> = mapOf(
        "User-Agent" to "okhttp/3.11.0",
        "token" to session.authToken,
    )

    private fun formEncode(params: List<Pair<String, String>>): String =
        params.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }

    /**
     * 发送库街区短信验证码。
     * @param geeTestData 极验 v4 结果 JSON（首次为空；服务端返回 geeTest=true 时需先过滑块）。
     */
    suspend fun sendKuroSms(phone: String, geeTestData: String = ""): SmsResult =
        withContext(Dispatchers.IO) {
            try {
                val body = formEncode(listOf("mobile" to phone, "geeTestData" to geeTestData))
                val resp = postForm(KURO_SEND_SMS, body, kuroLoginHeaders())
                val o = JSONObject(resp)
                if (o.optInt("code") != 200) {
                    SmsResult.Error(o.optString("msg", "发送验证码失败"))
                } else if (o.optJSONObject("data")?.optBoolean("geeTest") == true) {
                    SmsResult.NeedCaptcha
                } else {
                    SmsResult.Sent
                }
            } catch (e: Exception) {
                SmsResult.Error(e.message ?: "网络异常")
            }
        }

    /** 库街区手机号验证码登录（sdkLogin）。 */
    suspend fun loginKuro(phone: String, code: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val body = formEncode(
                listOf(
                    "code" to code,
                    "devCode" to kuroDevCode,
                    "gameList" to "",
                    "mobile" to phone,
                )
            )
            val resp = postForm(KURO_SDK_LOGIN, body, kuroLoginHeaders())
            val o = JSONObject(resp)
            if (o.optInt("code") != 200) {
                AuthResult.Error(o.optString("msg", "登录失败"))
            } else {
                val d = o.optJSONObject("data") ?: JSONObject()
                AuthResult.Success(
                    AuthSession(
                        platform = "kuro",
                        uid = d.optString("userId"),
                        mid = "",
                        authToken = d.optString("token"),
                        nickname = d.optString("userName"),
                        expiresAt = 0L,
                        savedAt = System.currentTimeMillis(),
                    )
                )
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "网络异常")
        }
    }

    /**
     * 库街区扫码预检：qrCode/scan 解析角色列表 → scanSms 触发二次验证短信。
     * 之后由 UI 收集短信码调用 [kuroConfirmScan]。
     */
    suspend fun kuroScanReady(raw: String, session: AuthSession): KuroScanResult =
        withContext(Dispatchers.IO) {
            try {
                val headers = kuroSessionHeaders(session)
                val scanResp = postForm(
                    KURO_QR_SCAN,
                    formEncode(listOf("qrCode" to raw)),
                    headers,
                )
                android.util.Log.d("KuroScan", "qrCode/scan resp: $scanResp")
                val o = JSONObject(scanResp)
                if (o.optInt("code") != 200) {
                    return@withContext KuroScanResult.Error(o.optString("msg", "扫码识别失败"))
                }
                val roles = o.optJSONObject("data")?.optJSONArray("roleList")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        arr.optJSONObject(i)?.optString("gameName")?.takeIf { it.isNotBlank() }
                    }
                } ?: emptyList()
                val smsResp = postForm(
                    KURO_SCAN_SMS,
                    formEncode(listOf("geeTestData" to "")),
                    headers,
                )
                android.util.Log.d("KuroScan", "scanSms resp: $smsResp")
                val so = JSONObject(smsResp)
                if (so.optInt("code") != 200) {
                    return@withContext KuroScanResult.Error(so.optString("msg", "发送二次验证短信失败"))
                }
                KuroScanResult.Ready(roles)
            } catch (e: Exception) {
                KuroScanResult.Error(e.message ?: "网络异常")
            }
        }

    /** 库街区扫码最终确认（scanLogin + verifyCode，二次验证短信码）。 */
    suspend fun kuroConfirmScan(raw: String, session: AuthSession, smsCode: String): AuthResult =
        withContext(Dispatchers.IO) {
            try {
                val body = formEncode(
                    listOf(
                        "autoLogin" to "false",
                        "qrCode" to raw,
                        "id" to "",
                        "verifyCode" to smsCode,
                    )
                )
                val resp = postForm(KURO_SCAN_LOGIN, body, kuroSessionHeaders(session))
                val o = JSONObject(resp)
                android.util.Log.d(
                    "KuroScan",
                    "scanLogin code=${o.optInt("code")} msg=${o.optString("msg")}"
                )
                if (o.optInt("code") != 200) {
                    AuthResult.Error(o.optString("msg", "确认登录失败"))
                } else {
                    AuthResult.Success(AuthSession("", "", ""))
                }
            } catch (e: Exception) {
                AuthResult.Error(e.message ?: "网络异常")
            }
        }

    suspend fun sendSms(platform: String, phone: String): AuthResult =
        if (platform == "skland") sendSklandSms(phone)
        else if (platform == "kuro") {
            // 库街区走专用实现（需极验），这里保留签名兼容
            when (val r = sendKuroSms(phone)) {
                SmsResult.Sent -> AuthResult.Success(AuthSession("", "", ""))
                SmsResult.NeedCaptcha -> AuthResult.Error("需要完成安全验证")
                is SmsResult.Error -> AuthResult.Error(r.message)
            }
        } else sendMihoyoSms(phone)

    suspend fun refreshNickname(session: AuthSession): AuthSession {
        val nickname = when (session.platform) {
            "mihoyo" -> getMihoyoNickname(
                session.uid,
                session.mid,
                session.stoken,
                session.cookieToken,
                session.ltoken
            )
            "skland" -> getSklandNickname("", session.cred)
            else -> session.nickname
        }
        return session.copy(
            nickname = nickname.ifBlank { session.nickname },
            expiresAt = 0L,
        )
    }

    suspend fun login(platform: String, phone: String, code: String): AuthResult = when (platform) {
        "skland" -> loginSkland(phone, code)
        "kuro" -> loginKuro(phone, code)
        else -> loginMihoyo(phone, code)
    }
}