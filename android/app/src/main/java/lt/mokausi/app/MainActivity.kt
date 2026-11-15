package lt.mokausi.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Pilno ekrano WebView su mūsų svetaine + window.AndroidBridge. */
class MainActivity : Activity() {
    companion object { const val SITE = "https://mokausi-ir-zaidziu.netlify.app/" }

    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private lateinit var offline: LinearLayout
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private var pendingMic: PermissionRequest? = null

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        web = WebView(this)
        offline = buildOfflineView()
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(offline, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        hideStatusBar()

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)          // window.open (EDUKA, Oxford, YouTube) → atskirai
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        web.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                val u = req.url
                if (u.host == Uri.parse(SITE).host) return false
                openExternal(u); return true
            }
            override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                if (req.isForMainFrame) showOffline(true)
            }
            override fun onPageFinished(view: WebView, url: String) {
                if (offline.visibility != View.VISIBLE) web.visibility = View.VISIBLE
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {   // pilno ekrano video
                customView?.let { callback.onCustomViewHidden(); return }
                customView = view; customCallback = callback
                root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                web.visibility = View.GONE
            }
            override fun onHideCustomView() { exitFullscreenVideo() }
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                // Naują langą „pagauname“ ir jo adresą atidarome naršyklėje / programėlėje
                val tmp = WebView(this@MainActivity)
                tmp.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                        openExternal(r.url); v.destroy(); return true
                    }
                }
                (resultMsg.obj as WebView.WebViewTransport).webView = tmp
                resultMsg.sendToTarget(); return true
            }
            override fun onPermissionRequest(request: PermissionRequest) {     // mikrofonas Bobiui
                runOnUiThread {
                    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE !in request.resources) { request.deny(); return@runOnUiThread }
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                        request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                    else { pendingMic = request; requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 7) }
                }
            }
        }
        if (savedInstanceState != null) web.restoreState(savedInstanceState) else load()
        Updater.check(this)
    }

    private fun load() { showOffline(false); web.loadUrl(SITE) }

    private fun openExternal(u: Uri) {
        try { startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {}
    }

    private fun showOffline(show: Boolean) {
        offline.visibility = if (show) View.VISIBLE else View.GONE
        web.visibility = if (show) View.INVISIBLE else View.VISIBLE
    }

    private fun buildOfflineView(): LinearLayout {
        val dp = { v: Int -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt() }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; visibility = View.GONE
            setBackgroundColor(Color.parseColor("#eef2ff")); setPadding(dp(32), dp(32), dp(32), dp(32))
            addView(TextView(context).apply { text = "📡"; textSize = 80f; gravity = Gravity.CENTER })
            addView(TextView(context).apply {
                text = "Nėra interneto.\nPaprašyk tėvų įjungti Wi-Fi ir pabandyk dar kartą."
                textSize = 24f; setTextColor(Color.parseColor("#1e1b4b")); gravity = Gravity.CENTER; setPadding(0, dp(16), 0, dp(28))
            })
            addView(Button(context).apply {
                text = "Bandyti dar kartą"; textSize = 22f; isAllCaps = false; setTextColor(Color.WHITE)
                background = GradientDrawable().apply { setColor(Color.parseColor("#4f46e5")); cornerRadius = dp(18).toFloat() }
                setPadding(dp(36), dp(16), dp(36), dp(16))
                setOnClickListener { load() }
            })
        }
    }

    private fun exitFullscreenVideo() {
        customView?.let { root.removeView(it) }
        customView = null; customCallback?.onCustomViewHidden(); customCallback = null
        web.visibility = View.VISIBLE
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        val req = pendingMic ?: return
        pendingMic = null
        if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) req.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) else req.deny()
    }

    override fun onResume() {
        super.onResume(); web.onResume(); hideStatusBar()
        // Svetainė pasiima žaidimuose sunaudotą laiką (dubliuoja visibilitychange)
        web.evaluateJavascript("window.__vpAndroidResume && window.__vpAndroidResume()", null)
    }

    override fun onPause() { web.onPause(); super.onPause() }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); web.saveState(outState) }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            customView != null -> exitFullscreenVideo()
            web.canGoBack() -> web.goBack()
            else -> web.evaluateJavascript("(function(){ if (typeof view !== 'undefined' && view !== 'home') { H.home ? H.home() : go('home'); return 1; } return 0; })()") { r ->
                if (r != "1") moveTaskToBack(true)
            }
        }
    }

    override fun onDestroy() { web.destroy(); super.onDestroy() }
}
