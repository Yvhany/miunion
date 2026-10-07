---
feature: multi-file-sync-kuro-and-fixes
status: in-progress
updated: 2026-10-04
branch: in-place（原地开发；开源时按用户指令 git init，公开仓库 Yvhany/miunion）
commits:
---

# 多文件云同步、库街区接入与动效修复

## Report

## [S1] Problem

1. 扫码页在切换动画经过时就会启动摄像头（pager 预组合 + `LaunchedEffect(Unit)` 立即绑相机）。要求：仅当扫码页真正停稳且应用前台可交互（settled、非过渡态）时启停相机。
2. 二级页面（视觉、账号与同步、消息详情）进出动画被感知为“斜的”。已定位三个纵向分量：(a) `SyncPage`/`MessageDetailPage` 是 wrap 高度，与满屏子项尺寸不一，`AnimatedContent` 默认 SizeTransform 产生纵向尺寸动画；(b) topBar/bottomBar 槽在切换瞬间整体跳变，内容顶/底边纵向位移；(c) 进出滑移量不对称（进 -100%、出 -25%）。要求改为纯水平推入/推出。
3. 云同步是“单文件整包”（miunion-sync-v1）：两台设备互相覆盖；删除无法传播（本地缺文件 ≠ 从未同步过）；同文件双端修改会静默覆盖。服务端为标准 WebDAV，仅提供上传、下载、删除、列出目录四个能力，无版本号、无变更通知。数据为 `records/<id>.json` 若干独立小文件，需做双向同步：删除正确传播；双端同文件修改不自动覆盖，标记冲突由用户手动处理。
4. 库街区（kurobbs）平台未接入。真实报文已抓取：
   - 扫码确认（第一份 HAR）：`POST /user/qrCode/scan`（qrCode=`G152#KURO_<uuid>`，返回 roleList）→ `POST /user/sms/scanSms`（body `geeTestData=`，返回 geeTest:false）→ `POST /user/auth/scanLogin`（`autoLogin=false&qrCode=..&id=&verifyCode=<6位>`，成功 `code:200,data:true`；无验证码返回 `code:2240 当前用户未进行扫码二次验证`）。请求头：`token`、`Cookie: user_token=..; acw_tc=..`、`User-Agent: okhttp/3.11.0`、form 编码。
   - 应用内登录（第二份 HAR）：`POST /user/getSmsCode`（`mobile=<手机号>&geeTestData=` → `{"geeTest":true}` 需极验）→ 极验 v4 滑块 → 再次 `getSmsCode`（`geeTestData=<urlencoded JSON{captcha_id,lot_number,pass_token,gen_time,captcha_output}>` → `{"geeTest":false}` 发码成功）→ `POST /user/sdkLogin`（`code=<验证码>&devCode=<40字符设备码>&gameList=&mobile=` → 返回 `token`(JWT)、`userId`、`userName`）。登录类请求头：`osVersion/devCode/distinct_id/countryCode/ip/model/source/lang/version=3.4.0/versionCode=30400/channelId=4`、`User-Agent: okhttp/3.11.0`、form 编码。
   - 平台图标来源：`<本地下载的 base.apk>`（路径不入库）。
5. 验证码输入框聚焦时右边框有缺口。像素实测（bd.png）：右侧描边仅在上下圆角弧段存在，直段 y≈1053–1109 无蓝色描边、被与底色同为 #434343 的元素覆盖；左直边连续。根因：Miuix TextField 有 trailingIcon 时布局不做右侧内边距，`获取验证码` TextButton 的不透明底贴住右边缘盖住描边。

## [S2] Design

### D1 摄像头门控
- `ScanPage` 增加 `active: Boolean` 参数；`MainScreen` 计算：
  `active = subPage == null && pagerState.settledPage == 1 && LocalLifecycleOwner.current.lifecycle.currentState == Lifecycle.State.RESUMED`。
- 相机启停全部收敛到 `LaunchedEffect(active, permissionGranted)`：active 为 false 时解绑并复位 `cameraOn=false`；为 true 且有权限才启动预览/分析。
- 过渡态（animateScrollToPage 进行中 settledPage 未更新、拖到一半回退）不启动相机。

### D2 二级页纯水平转场
- 结构调整：`Scaffold` 的 topBar/bottomBar 归入“主页”子项内部（主页分支仍是 Scaffold + 大标题 + 底栏 + Pager + 渐变遮罩）；二级页子项改为 `Column { SmallTopAppBar(标题, 返回箭头) ; 页面内容(fillMaxSize) }`，并保持与现状一致的沉浸状态栏（标题纵向位置与现版一致，验证标题 y≈46–100）。
- 所有子项尺寸满屏：`SyncPage`、`MessageDetailPage` 的 LazyColumn 改 `fillMaxSize`；`transitionSpec` 追加 `.using(SizeTransform(null))` 禁止尺寸动画。
- 转场规格（对称刚性推入）：
  - 进入：新 `slideInHorizontally(tween(300)) { it } + fadeIn(tween(240))`，旧 `slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(180))`。
  - 返回：新（主页）`slideInHorizontally { -it } + fadeIn`，旧（二级页）`slideOutHorizontally { it } + fadeOut`。
- 顶栏高度变化随各自子项一起滑动，不再发生整页纵向跳变。

### D3 多文件 WebDAV 双向同步（彻底替换整包同步）
- 记录模型：每条数据一个文件 `records/<id>.json`（相对配置的目录地址）。
  - 账号：`id = "account_<platform>"`，内容为 AuthSession JSON（含 token）。
  - 消息：`UniMessage` 新增 `id`（uuid），存量数据首次加载生成并持久化。
  - 内容整体沿用现有 `seal()/open()` 信封（PBKDF2WithHmacSHA256 + AES-GCM），云同步密码与交互不变；云端文件是密文。
- 配置 URL 语义改为“目录地址”：保存时若以 `.json` 结尾则取其上级目录（兼容旧配置），数据存 `目录/records/<id>.json`；`records/` 目录须服务端预先存在（服务端无建目录能力，PUT 409 时明确报错）。
- 传输四操作：`PROPFIND(Depth:1)` 列目录解析 href 得远端 id 集合；`GET` 下载；`PUT` 上传；`DELETE` 删除；均带现有 Basic 认证。
- 本地同步清单（`sync_manifest` prefs）：`id → { lastSyncedSha256, deleted }`，表示“上次同步时的内容哈希”与墓碑标记。本地不存在的 id 只要清单中有记录且非墓碑，即视为“本地已删除”，不得当作从未同步。
- 同步算法（立即同步）：
  1. PROPFIND 取远端集合；逐个 GET 解密得 `remoteHash/payload`；本地构造 `localHash/payload`（账号来自 AuthStore，消息来自 MessageStore）。
  2. 对 id ∈ 本地∪远端∪清单：
     - 双端都活：`localHash==last && remoteHash==last` → 无操作；仅远端变 → 拉取覆盖本地；仅本地变 → 上传；双端都变 → **冲突**（不上传不下载）。
     - 本地活、远端缺：清单有且非墓碑 → 远端被删 → 删除本地并保留墓碑；清单无 → 新增 → 上传；清单为墓碑（本地重建）→ 上传置活。
     - 本地缺、远端活：清单有且非墓碑 → 本地已删 → DELETE 远端（删除传播）；清单无 → 远端新增 → 应用到本地；清单为墓碑 → 删除与重建冲突 → **冲突**。
     - 双端都缺：清单为墓碑保留，其余清理。
  3. 冲突写入 `sync_conflicts` prefs：`id + 本地payload + 远端payload + 时间`；两侧内容均保留在本机内存/云端原样，等用户处理。
  4. 成功后更新 lastSync 与清单。
- UI（账号与同步页）：
  - “立即同步”执行上述算法，toast 报告 上传N/拉取N/删除N/冲突N。
  - 冲突列表：页面顶部卡片列出冲突条目（id 对应的消息标题/账号名、时间），每条两个按钮「保留本地」「保留云端」，选定后写回清单并可再次同步；无冲突不显示。
  - WebDAV 地址输入提示改为目录地址。
- 旧整包（miunion-sync-v1）的上传/下载入口删除；信封加解密、密码、配置存储复用。

### D4 库街区平台接入
- 平台常量：`platform == "kuro"`；`platformLabel` = 库街区；`platformColor` = 从 APK 图标主色采样；图标资源 `drawable-nodpi/kuro_icon.*`（从 base.apk 提取）。
- 识别与展示：`detectQrPlatform` 优先识别 `raw.contains("KURO_")` → kuro；`messagePlatform` 识别“库街区”；`PlatformIcon`/消息图标/通知大图标与超岛 pics 全部走统一 platformIconRes 分支。
- 会话字段：`uid=userId`、`nickname=userName`、`authToken=token(JWT)`、`savedAt=now`；stoken/ltoken 等保持空。
- AuthApi 新增（全部按抓包原样）：
  - `sendKuroSms(phone)`：`POST /user/getSmsCode`，`mobile=..&geeTestData=`，kuro 登录类头（字段结构按抓包；设备标识值开源版运行时生成，见 D9），返回 `{geeTest}`。
  - `sendKuroSmsWithCaptcha(phone, json)`：同上带 `geeTestData=<urlencoded json>`。
  - `loginKuro(phone, code)`：`POST /user/sdkLogin`，成功解析 token/userId/userName 构造 AuthSession。
  - `confirmKuroQr(raw, session)`：① `POST /user/qrCode/scan`（token+Cookie 头）解析 roleList；② `POST /user/sms/scanSms` 触发二次验证短信；③ 调用方收集 6 位验证码后 `POST /user/auth/scanLogin`（verifyCode）→ 成功。
  - 头策略：按抓包原样发送；若实测被 WAF 拦截则去掉 Cookie 仅保留抓包头（以真机实测为准，不猜新字段）。
- 登录页：新增第三个平台 chip「库街区」；获取验证码时先 `sendKuroSms`，返回 `geeTest:true` 则弹出极验 v4 弹窗，成功回调 JSON 后 `sendKuroSmsWithCaptcha` 发码；`sdkLogin` 走既有登录成功/失败（消息+通知）链路。
- 极验 v4：`GeetestCaptchaDialog` — WebView 加载 GeeTest 官方 v4 JS（captcha_id 用抓包值 `3f7e2d848ce0cb7e7d019d621e556ce2`），回调参数即 `geeTestData` JSON（captcha_id/lot_number/pass_token/gen_time/captcha_output），与抓包结构一致；取消则中止发码。
- 扫码确认页：kuro 二维码在确认 sheet 中额外展示「登录验证码」输入与已自动调用 scanSms 的状态，输满 6 位后点确认执行 scanLogin；成功/失败均写消息 + 超岛通知（复用现有 LoginNotifier 分支）。

### D5 验证码框右边框
- `LoginScreen` 验证码 TextField 的 trailing `TextButton` 加 `modifier = Modifier.padding(end = 18.dp)`（内边距 16dp + 描边 2dp），使按钮底不再覆盖右侧描边。
- 验收像素标准：聚焦态右直边（x≈991–996、y≈1046–1116）蓝色描边连续，与左直边对称。

### D6 构建与验证边界
- 构建：`gradle :app:assembleDebug`（既有 JDK21/GRADLE_USER_HOME 环境）。
- 双设备同步验证：本地 dav_server.py 扩展 PROPFIND/DELETE；以“清空本机同步状态模拟第二台设备”“直接改云端文件制造双端修改”验证拉取/删除/冲突。
- 库街区网络验证：`getSmsCode` 空参数可无副作用验证连通与 geeTest 标志；完整短信登录与扫码二次验证需真实手机号参与，尽力自动化，需人工时在验证记录中注明。
- 屏幕采集注意：模拟器可能将应用置于副屏（dumpsys 查 displayId，`screencap -d <id>` / `input -d <id>`）。

### D7 首屏始终为主页面（云同步恢复路径）
- 移除 `rootTarget = if (!loggedIn) 0 …` 门控：账号为空时首屏仍是主页面（账号库 4 Tab），不再直接进登录页；`loggedIn` 变量删除。
- 登录页仅作为「添加账号」二级页（`adding=true` → state 1）进入，其返回键一律 `adding = false` 回主页面。
- 动机：新装/清数据后必须能先到「设置 → 账号与同步」配置 WebDAV 与云同步密码、从云端恢复账号与消息，否则要先登录一次才能恢复，形成死循环。
- 验收：`pm clear` 后冷启动直接落在主页面；配置同步后未登录即可恢复全部数据。

### D8 Scaffold 结构修正（内容 padding 与弹窗宿主）
- 主页 Scaffold 内容 Box 应用 `.padding(paddingValues)`（HorizontalPager 上），否则顶栏（不透明大标题）遮挡各 Tab 首屏内容：账号卡片头部不可见、设置页首行「账号与同步」被盖住（“设置页两行之谜”真因）。
- miuix 的弹窗/浮层宿主 `MiuixPopupHost` 仅由 miuix `Scaffold` 挂载：Scaffold 之外组合的 `OverlayDialog`/`OverlayBottomSheet` 状态注册后无人渲染（探针日志证明 onClick/状态/组合全部执行，但屏幕零变化、无新窗口）。
- 修复：`MainScreen` 的二级页分支与登录页分支各包一层空栏 `Scaffold(topBar={}, bottomBar={}, contentWindowInsets=0)`，内容忽略其 padding——只为挂载宿主，布局零变化。
- 验收：同步页 WebDAV/云同步密码弹窗、账号管理退出确认弹窗、登录页极验弹窗均可弹出并交互。

### D9 GitHub 开源（脱敏后上传）
- 用户指令（2026-10-04）：去掉与隐私相关的 Token/设备标识，把工程开源到 GitHub，关于页链接该仓库；版本号统一 1.1.0。
- 脱敏（仅 `AuthApi.kt`，字段结构与格式保持抓包原样，值改为运行时生成或当前设备真实值）：
  - kuro 登录类头：`devCode`（40 hex）与 `distinct_id`（UUID）改为进程级随机；`ip` 头删除（作者内网地址）；`model` 用 `Build.MODEL`；sdkLogin body 的 `devCode` 与头同值。
  - skland 扫码头：`x-deviceid`（32 hex）进程级随机；`x-devicemodel` 用 `Build.MODEL`。
  - mihoyo sdk 头：`device_fp`（13 hex）进程级随机；`device_name/device_model` 用 `Build.MANUFACTURER/MODEL`。
  - 账号凭证（stoken/ltoken/cookieToken/authToken/cred）本就为运行时会话字段，全仓库无硬编码 token 值。
  - 保留：协议公开常量（DS salt、APP_ID、SKLAND_APP_CODE、极验 captcha_id、米游社公钥）与非作者设备的 UA 指纹（`MI_HOYO_UA` 中 22011211C 非作者机型）。
- 风险声明：skland/mihoyo 扫码确认链路的设备头值改为运行时生成，服务端校验行为未知，若被拒按原格式回滚；kuro 扫码链路（qrCode/scan、scanSms、scanLogin）只用 `token+UA`，不经上述头，不受影响。
- 仓库：本地 `git init`；`.gitignore` 排除 build/local.properties/IDE 文件；公开仓库 `Yvhany/miunion`；README 含功能、构建与隐私说明；暂不附 LICENSE（法律决定留给用户）。
- spec 内本地路径（图标来源 apk）以占位符替换。

### D10 设置「关于」二级页
- `SubPage.About` 新增；设置页「关于聚合通行证」行由 toast 改为进入二级页（标题「关于」，转场复用 D2 纯水平推拉）。
- `AboutPage`（SubPages.kt）：`ic_launcher` 96dp 居中靠上（顶距 64dp）→ 软件名「yvhan」26sp Bold → 副行「聚合通行证 v1.1.0 · Miuix」→ 卡片行「GitHub 开源」（summary `github.com/Yvhany/miunion`），点击 `ACTION_VIEW` 打开仓库地址（常量 `GITHUB_REPO_URL`/`GITHUB_REPO_LABEL`）；设置行 summary 为「yvhan · v1.1.0」。
- Manifest 增加 `<queries>`（ACTION_VIEW + https scheme）满足 Android 11+ 包可见性，保证跳转浏览器可解析。

### D11 账号卡改版（展开角色区 + 右下按钮行）
- 视觉参照用户设计图；用户决策（2026-10-04）：先做 UI、抓包后再接角色 API；按钮单开一行放右下角（尺寸/间距由实现定）；头部保留原样式「平台图标 + 昵称 + 彩色胶囊平台名 + uid·meta 小字」。
- 结构（AccountCard）：
  - 头部整行可点击：原样式内容 + 右侧 20dp 箭头（`MiuixIcons.ChevronForward`，默认收起=▷ 尖口朝右，展开顺时针旋转 90°=∨ 朝下，`animateFloatAsState` 240ms 过渡）；默认收起。
  - 展开区（`AnimatedVisibility` 纵向展开）：与昵称对齐的缩进列（图标 46dp + 间距 12dp = 58dp），角色行为「游戏名（weight 1）+ 等级（weight 1，左对齐，起点≈内容区中点）」两列布局；本轮无接口 → 空态占位「暂无角色数据 · 待接口接入」。
  - 卡片不放操作按钮（用户决策 2026-10-04 追加）：「扫码登录」入口=底栏扫码页，「退出」入口=设置页退出登录；AccountCard/AccountsPage 相应回调与退出确认死代码已删。
  - `items` key 改为 `"$platform:$uid"`（防同平台多账号键冲突）。
- 后续 T15：用户提供森空岛/米游社（及库街区）角色列表接口 HAR → `UniAccount.roles` 模型 + 拉取缓存 + 角色行真实渲染（游戏名+等级）。

### D12 角色数据接入（森空岛 + 米游社）
- 报文依据：用户 2026-10-07 HAR + 公开签名实现交叉验证；两链路均本机实测 retcode=0。
- 米游社：GET game_record/app/card/wapi/getGameRecordCard?uid=stuid；Cookie=stuid/stoken/mid/ltoken/ltuid/cookie_token（AuthSession 全有）；DS= salt xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs + t,r(100001..200000) + md5(salt&t&r&b=&q=uid=..)；client_type=5 WebView 头组（样本形态，设备头用 Build 真实值）。解析 data.list[]：game_name/level/region_name/nickname。
- 森空岛：① GET /api/v1/auth/refresh（cred+基础头，无 sign，保留 is_new_tiger）→ data.token；② GET /api/v1/user/center（cred+新鲜 timestamp+sign=md5(HMAC-SHA256(token, path+ts+compactJson))，compactJson={platform,timestamp,dId,vName} 紧凑序固定；**请求头必须去掉 is_new_tiger**（实测带=网关 405），xsm/wtoken 不发（实测可省）。解析 data.gameCardList[]：外卡 name=游戏名，内嵌含 level 的对象取 level 与 name（游戏昵称）。
- 脱敏：did/rid/x-rpc-device_id 运行时随机，device_fp 用进程随机值，型号头用 Build 真实值，UA 设备段用 Build——不落 HAR 中的作者设备标识。
- 展示（账号卡展开区）：行1=游戏名 + Lv.xx（两列各占半，level 起点≈内容区中点，贴用户设计图）；行2=区服 · 游戏昵称（11sp 灰）。加载态「正在获取角色数据…」；空/失败「暂无角色数据」；kuro 按用户决策不接入。
- 拉取时机：账号库进入时对 skland/mihoyo 账号后台拉取（内存态，失败静默空态）。

## [S3] Out of Scope
- 消息的删除/编辑入口（同步机制支持删除传播，但本版无消息删除 UI；账号退出即账号记录删除）。
- 云端旧单文件 `miunion-sync-v1` 数据迁移（彻底替换，旧文件忽略）。
- WebDAV 自动建目录（MKCOL 不在服务端四能力内）。
- 库街区发帖、签到等非登录功能。
- 应用桌面图标、模拟器三键导航白区问题。

## Tasks
- [x] T1: 摄像头 active 门控 — acceptance: 页面过渡/未停稳时 logcat 无 camera bind，停稳扫码页预览与识别正常 (covers: D1)
- [x] T2: 二级页纯水平转场重构 — acceptance: 进出视觉/账号与同步/消息详情逐帧内容纵向位置零位移，返回为从右滑出 (covers: D2; depends: T1)
- [x] T3: WebDAV 多文件同步核心（记录/清单/墓碑/四操作/算法） — acceptance: 实测 上传→清本地→拉回、远端删除→本地删除、双端改→产生冲突不覆盖 (covers: D3)
- [x] T4: 同步页改造（多文件立即同步 + 冲突列表逐条保留） — acceptance: 冲突条目可选保留本地/保留云端并可再次同步成功，WebDAV 地址提示为目录 (covers: D3; depends: T3)
- [x] T5: 库街区图标提取与平台常量 — acceptance: base.apk 图标入包，账号库/消息/详情/通知/扫码 pill 五处显示 kuro 图标与“库街区”文案 (covers: D4/D6)
- [ ] T6: 库街区扫码确认链路 — acceptance: KURO_ 二维码走 qrCode/scan→scanSms→短信码→scanLogin，头体与抓包一致，成功/失败入消息与通知 (covers: D4; depends: T5)
- [ ] T7: 库街区应用内登录（发码+极验+sdkLogin） — acceptance: 登录页第三个 chip，geeTest:true 时弹官方极验 v4，通过后发码，sdkLogin 成功入库并触发登录成功通知 (covers: D4; depends: T5)
- [x] T8: 验证码框右边框修复 — acceptance: 聚焦态右直边描边连续无缺口、与左侧对称 (covers: D5)
- [ ] T9: 构建、端到端验证、输出 APK — acceptance: T1–T8 验收项全部通过并记录命令结果，APK 输出 E:\Creative\Android\output\ (covers: D1-D6; depends: T1,T2,T3,T4,T5,T6,T7,T8)
- [x] T10: 首屏始终为主页面 — acceptance: pm clear 后冷启动直接进主页面 4 Tab；不登录配置同步可恢复账号与消息 (covers: D7)
- [x] T11: Scaffold 结构修正（padding 遮挡 + 弹窗宿主） — acceptance: 账号卡片与设置三行完整可见；同步页 WebDAV/密码/退出确认弹窗与登录页极验弹窗可弹出 (covers: D8)
- [x] T12: GitHub 开源上传（脱敏 + git init + 公开仓库推送） — acceptance: 全仓库无作者设备标识/凭据残留；https://github.com/Yvhany/miunion 公开可访问 (covers: D9)
- [x] T13: 设置「关于」二级页 — acceptance: 设置→关于进入二级页，图标居中靠上、显示 yvhan、v1.1.0；GitHub 行点击打开仓库页 (covers: D10; depends: T12)
- [x] T14: 账号卡改版 UI（默认收起 + 旋转箭头角色区） — acceptance: 头部原样式保留；默认收起箭头▷、展开旋转90°变∨且角色区空态出现；卡片无操作按钮；折叠/展开流畅（像素级验证） (covers: D11)
- [ ] T15: 角色数据接口接入 — acceptance: 森空岛/米游社角色列表实测入库，账号卡展开显示真实游戏名+等级+区服昵称 (covers: D12; depends: T14, 用户 HAR)
