package lt.mokausi.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * Mokymosi puslapiai (EDUKA, Oxford ir kt.) atidaromi PAČIOJE programėlėje, ne Chrome —
 * todėl Chrome ribojimai jų neliečia. Pramogų svetainės (YouTube ir pan.) čia neatidaromos.
 */
class EduWebActivity : Activity() {
    companion object {
        const val EXTRA_URL = "url"
        private val BLOCKED = listOf("youtube.com", "youtu.be", "tiktok.com", "facebook.com", "instagram.com", "netflix.com", "twitch.tv", "roblox.com")
        fun isBlocked(u: Uri): Boolean { val h = (u.host ?: "").lowercase(); return BLOCKED.any { h == it || h.endsWith(".$it") } }
        fun isWeb(u: Uri) = u.scheme == "http" || u.scheme == "https"
    }

    private lateinit var web: WebView
    private lateinit var root: FrameLayout
    private lateinit var title: TextView
    private lateinit var progress: ProgressBar
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = { v: Int -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt() }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#4f46e5")); setPadding(dp(8), dp(6), dp(12), dp(6))
        }
        bar.addView(Button(this).apply {
            text = "← Į Mokausi"; textSize = 18f; isAllCaps = false; setTextColor(Color.parseColor("#4f46e5"))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(14).toFloat() }
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener { finish() }
        })
        title = TextView(this).apply {
            textSize = 16f; setTextColor(Color.WHITE); setPadding(dp(14), 0, 0, 0); maxLines = 1
        }
        bar.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        web = WebView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)))
        col.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root = FrameLayout(this); root.addView(col)
        setContentView(root)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)   // prisijungimas išlieka
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true; useWideViewPort = true
            builtInZoomControls = true; displayZoomControls = false
            setSupportMultipleWindows(true); javaScriptCanOpenWindowsAutomatically = true
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean = handle(req.url)
            override fun onPageFinished(view: WebView, url: String) { CookieManager.getInstance().flush() }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, p: Int) { progress.progress = p; progress.visibility = if (p >= 100) View.GONE else View.VISIBLE }
            override fun onReceivedTitle(view: WebView, t: String?) { title.text = t ?: "" }
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customView?.let { callback.onCustomViewHidden(); return }
                customView = view; customCallback = callback
                root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
            override fun onHideCustomView() { exitFullscreen() }
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                // Puslapio naujus langus (pvz. užduoties atidarymas) rodome tame pačiame lange
                val tmp = WebView(this@EduWebActivity)
                tmp.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                        if (!handle(r.url)) web.loadUrl(r.url.toString())
                        v.destroy(); return true
                    }
                }
                (resultMsg.obj as WebView.WebViewTransport).webView = tmp
                resultMsg.sendToTarget(); return true
            }
        }
        val url = intent.getStringExtra(EXTRA_URL)
        if (savedInstanceState != null) web.restoreState(savedInstanceState) else if (url != null) web.loadUrl(url) else finish()
    }

    /** true = puslapis čia NEkraunamas. */
    private fun handle(u: Uri): Boolean {
        if (!isWeb(u)) {                       // mailto:, intent: ir pan. — atidarome sistemoje
            try { startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {}
            return true
        }
        if (isBlocked(u)) return true          // pramogos mokymosi lange neatidaromos
        return false
    }

    private fun exitFullscreen() {
        customView?.let { root.removeView(it) }
        customView = null; customCallback?.onCustomViewHidden(); customCallback = null
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); web.saveState(outState) }
    override fun onResume() { super.onResume(); web.onResume() }
    override fun onPause() { web.onPause(); CookieManager.getInstance().flush(); super.onPause() }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            customView != null -> exitFullscreen()
            web.canGoBack() -> web.goBack()
            else -> finish()
        }
    }

    override fun onDestroy() { web.destroy(); super.onDestroy() }
}
