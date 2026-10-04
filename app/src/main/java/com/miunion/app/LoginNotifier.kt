package com.miunion.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import org.json.JSONObject

/**
 * 登录成功/失败通知：按小米官方“客户端本地创建超级岛”方式，
 * 在通知 extras 写入 miui.focus.param（超级岛/焦点通知数据），
 * 同时按普通通知兜底展示。
 *
 * 报文结构参考小米澎湃OS超级岛开发指南与阿里云《小米超级岛推送指南》。
 */
object LoginNotifier {
    private const val CHANNEL_ID = "login_events"
    private const val BASE_ID = 3101

    private fun label(platform: String) = when (platform) {
        "skland" -> "森空岛"
        "kuro" -> "库街区"
        else -> "米游社"
    }

    private fun colorHex(platform: String) = when (platform) {
        "skland" -> "#17B26A"
        "kuro" -> "#00C8C0"
        else -> "#3482FF"
    }

    private fun platformIconRes(platform: String) = when (platform) {
        "skland" -> R.drawable.skland_icon
        "kuro" -> R.drawable.kuro_icon
        else -> R.drawable.mihoyo_icon
    }

    fun notify(
        context: Context,
        platform: String,
        username: String,
        success: Boolean,
        reason: String = "",
    ) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "登录消息",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "账号登录成功 / 失败提醒"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }

            val name = username.ifBlank { "未知账号" }
            val subTitle = "[${label(platform)}]" + if (success) "登录成功" else "登录失败"
            val bigText = if (!success && reason.isNotBlank()) "$subTitle：$reason" else subTitle
            val iconRes = platformIconRes(platform)

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = launchIntent?.let {
                PendingIntent.getActivity(
                    context, 0, it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

            val focusParam = buildFocusParam(platform, name, subTitle, colorHex(platform))

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(Icon.createWithResource(context, iconRes))
                .setContentTitle(name)
                .setContentText(subTitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
            if (pendingIntent != null) builder.setContentIntent(pendingIntent)

            val notification = builder.build()

            // 超岛参数：主标题=用户名，次标题=[平台]登录成功/失败，图标=平台图标
            notification.extras.putString("miui.focus.param", focusParam)
            val pics = Bundle().apply {
                putParcelable("miui.focus.pic_imageText", Icon.createWithResource(context, iconRes))
                putParcelable("miui.focus.pic_ticker", Icon.createWithResource(context, iconRes))
                putParcelable("miui.focus.pic_aod", Icon.createWithResource(context, iconRes))
            }
            notification.extras.putBundle("miui.focus.pics", pics)

            manager.notify(BASE_ID + (System.currentTimeMillis() % 50_000).toInt(), notification)
        } catch (e: Exception) {
            // 无通知权限或不支持的系统上静默失败，登录流程不受影响
        }
    }

    /** 与 miui.focus.param 协议一致的岛参数（param_v2）。 */
    private fun buildFocusParam(
        platform: String,
        title: String,
        content: String,
        colorTitle: String,
    ): String {
        val textInfo = JSONObject()
            .put("frontTitle", label(platform))
            .put("title", title)
            .put("content", content)
            .put("useHighLight", false)
        val imageText = JSONObject()
            .put("type", 1)
            .put("picInfo", JSONObject().put("type", 1).put("pic", "miui.focus.pic_imageText"))
            .put("miui.focus.paramtextInfo", textInfo)
        val pic = JSONObject().put("type", 1).put("pic", "miui.focus.pic_imageText")
        val island = JSONObject()
            .put("islandProperty", 1)
            .put("bigIslandArea", JSONObject().put("imageTextInfoLeft", imageText).put("picInfo", pic))
            .put("smallIslandArea", JSONObject().put("picInfo", pic))
            .put("shareData", JSONObject().put("title", title))
        val v2 = JSONObject()
            .put("protocol", 1)
            .put("business", "account_login")
            .put("enableFloat", false)
            .put("islandFirstFloat", true)
            .put("updatable", false)
            .put("ticker", title)
            .put("tickerPic", "miui.focus.pic_ticker")
            .put("aodTitle", content)
            .put("aodPic", "miui.focus.pic_aod")
            .put("param_island", island)
            .put(
                "baseInfo",
                JSONObject()
                    .put("title", title)
                    .put("content", content)
                    .put("colorTitle", colorTitle)
                    .put("type", 2),
            )
        return JSONObject().put("param_v2", v2).toString()
    }
}
