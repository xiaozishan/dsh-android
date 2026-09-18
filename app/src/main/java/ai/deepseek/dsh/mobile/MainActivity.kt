package ai.deepseek.dsh.mobile

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * DSH 掌上版 · 主界面
 * 路线 B：Termux 运行 `dsh web`，本应用通过 WebView 加载带一次性 token 的启动地址。
 * 启动地址来源优先级：Intent extra("url") > 本地保存 > 手动粘贴
 */
class MainActivity : Activity() {

    private var webView: WebView? = null
    private val prefs by lazy { getSharedPreferences("dsh", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val url = intent?.getStringExtra("url")
        if (url != null) {
            prefs.edit().putString("last_url", url).apply()
            openWeb(url)
            return
        }
        val saved = prefs.getString("last_url", null)
        if (saved != null) openWeb(saved) else showSetup("首次使用：请按下方步骤在 Termux 中启动 DSH 内核")
    }

    private fun openWeb(url: String) {
        if (webView == null) {
            val wv = WebView(this)
            webView = wv
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
            val s = wv.settings
            s.javaScriptEnabled = true
            s.domStorageEnabled = true
            s.mediaPlaybackRequiresUserGesture = false
            s.loadWithOverviewMode = true
            s.useWideViewPort = true
            s.builtInZoomControls = false
            s.displayZoomControls = false
            wv.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val u: Uri = request.url
                    val host = u.host ?: ""
                    if (host == "127.0.0.1" || host == "localhost") {
                        view.loadUrl(u.toString())
                    } else {
                        // 外部链接（如 OAuth 登录页）交给系统浏览器
                        startActivity(Intent(Intent.ACTION_VIEW, u))
                    }
                    return true
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame && (request.url.host == "127.0.0.1" || request.url.host == "localhost")) {
                        showSetup("连不上 DSH 服务（错误码 ${error.errorCode}）。\n请确认 Termux 里的 dsh web 正在运行，或重新粘贴最新启动地址。")
                    }
                }
            }
        }
        setContentView(webView)
        webView?.loadUrl(url)
    }

    private fun showSetup(status: String) {
        val pad = (20 * resources.displayMetrics.density).toInt()

        val statusView = TextView(this).apply {
            text = status
            setPadding(pad, pad, pad, pad / 2)
            textSize = 15f
        }

        val urlBox = EditText(this).apply {
            hint = "粘贴 http://127.0.0.1:3080/?token=... 启动地址"
            setSingleLine(true)
        }

        val openBtn = Button(this).apply {
            text = "保存并连接"
            setOnClickListener {
                val u = urlBox.text.toString().trim()
                if (u.startsWith("http://127.0.0.1") || u.startsWith("http://localhost")) {
                    prefs.edit().putString("last_url", u).apply()
                    openWeb(u)
                } else {
                    statusView.text = "地址格式不对，应该是 http://127.0.0.1:端口/?token=..."
                }
                hideKeyboard()
            }
        }

        val probeBtn = Button(this).apply {
            text = "检测 DSH 服务状态"
            setOnClickListener {
                statusView.text = "正在检测 127.0.0.1:3080 ..."
                thread {
                    val result = probe("http://127.0.0.1:3080/")
                    runOnUiThread { statusView.text = result }
                }
            }
        }

        val copyBtn = Button(this).apply {
            text = "复制 Termux 一键启动命令"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("dsh", TERMUX_CMD))
                statusView.text = "已复制。打开 Termux 粘贴运行，等它打印出地址后粘到上面输入框。"
            }
        }

        val help = TextView(this).apply {
            text = HELP_TEXT
            setPadding(pad, pad, pad, pad)
            textSize = 13f
            setTextColor(Color.parseColor("#555555"))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(statusView)
            addView(urlBox)
            addView(openBtn)
            addView(probeBtn)
            addView(copyBtn)
            addView(help)
            setPadding(pad / 2, 0, pad / 2, 0)
        }

        val scroll = ScrollView(this).apply { addView(layout) }
        setContentView(scroll)
    }

    /** 探测 DSH 本地服务是否在线（不带 token 也能探测：401/403 说明服务活着） */
    private fun probe(base: String): String {
        return try {
            val conn = URL(base).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.instanceFollowRedirects = false
            val code = conn.responseCode
            conn.disconnect()
            when (code) {
                200, 302 -> "✅ DSH 服务在线（HTTP $code）—— 把 Termux 打印的带 token 地址粘贴后连接"
                401, 403 -> "⚠️ 服务在线但需要令牌（HTTP $code）—— 在 Termux 里复制带 token 的完整地址粘贴到上面"
                else -> "ℹ️ 服务有响应（HTTP $code）"
            }
        } catch (e: Exception) {
            "❌ 连不上 127.0.0.1:3080 —— Termux 里的 dsh web 没在运行？先复制命令启动它"
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    override fun onBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val TERMUX_CMD =
            "pkg install -y nodejs-lts && npm i -g @deepseek-ai/dsh@0.1.5-rc.2 && npx @deepseek-ai/dsh web --no-open"

        private const val HELP_TEXT =
            "使用步骤：\n" +
                "1. 安装 Termux（F-Droid 版，别装 Play 商店版）\n" +
                "2. 点上面按钮复制启动命令，到 Termux 里粘贴运行\n" +
                "3. 首次运行会打印 http://127.0.0.1:3080/?token=... 地址\n" +
                "4. 复制该地址 → 粘贴到上方输入框 → 保存并连接\n\n" +
                "提示：保持 Termux 在后台运行（别划掉）；令牌每次启动都会变，" +
                "连不上时回 Termux 复制最新地址重新粘贴即可。"
    }
}
