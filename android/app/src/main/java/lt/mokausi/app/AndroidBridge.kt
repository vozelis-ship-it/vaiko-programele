package lt.mokausi.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale

/** window.AndroidBridge — kviečiamas iš svetainės (index.html). */
class AndroidBridge(private val activity: Activity) {
    private val ctx = activity.applicationContext

    // ---------- Balsas: WebView neturi speechSynthesis, todėl skaitome Android TextToSpeech ----------
    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false
    @Volatile private var pending: Triple<String, String, Float>? = null

    init { ensureTts() }

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(ctx) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            pending?.let { pending = null; if (ttsReady) doSpeak(it.first, it.second, it.third) }
        }
    }

    private fun localeFor(t: TextToSpeech, lang: String): Locale? {
        val want = Locale.forLanguageTag(lang)
        if (t.isLanguageAvailable(want) >= TextToSpeech.LANG_AVAILABLE) return want
        if (lang.startsWith("en")) listOf(Locale.UK, Locale.US, Locale.ENGLISH).firstOrNull { t.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }?.let { return it }
        return null
    }

    private fun doSpeak(text: String, lang: String, pitch: Float): String {
        val t = tts ?: return "error"
        val loc = localeFor(t, lang) ?: return "missing"
        t.language = loc
        t.setPitch(if (pitch > 0f) pitch else 1f)
        t.setSpeechRate(0.9f)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mokausi")
        return "ok"
    }

    /** Perskaito tekstą. Grąžina "ok", "missing" (nėra tos kalbos balso), "init" (dar ruošiamasi — perskaitys, kai bus pasiruošęs). */
    @JavascriptInterface
    fun speak(text: String, lang: String, pitch: Float): String {
        ensureTts()
        if (!ttsReady) { pending = Triple(text, lang, pitch); return "init" }
        return doSpeak(text, lang, pitch)
    }

    @JavascriptInterface
    fun stopSpeak() { pending = null; try { tts?.stop() } catch (e: Exception) {} }

    /** "ok" — kalbos balsas yra; "missing" — nėra; "init" — dar tikrinama. */
    @JavascriptInterface
    fun ttsStatus(lang: String): String {
        ensureTts()
        val t = tts ?: return "missing"
        if (!ttsReady) return "init"
        return if (localeFor(t, lang) != null) "ok" else "missing"
    }

    fun shutdown() { try { tts?.shutdown() } catch (e: Exception) {}; tts = null; ttsReady = false }

    /** Visos programėlės su paleidimo ikona (be „Mokausi“ ir Nustatymų): [{pkg, name, icon, isNew}] */
    @JavascriptInterface
    fun getApps(): String {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val skip = setOf(ctx.packageName, "com.android.settings")
        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName !in skip }
            .distinctBy { it.activityInfo.packageName }
        val current = apps.map { it.activityInfo.packageName }.toSet()
        // Naujos programėlės: tokios, kurių anksčiau nematėme (pirmą kartą — nė viena nelaikoma nauja)
        val known = Store.knownApps(ctx)
        if (known.isNotEmpty()) (current - known).forEach { Store.markNew(ctx, it) }
        Store.setKnownApps(ctx, current)
        val newApps = Store.newApps(ctx)
        val arr = JSONArray()
        apps.map { it to it.loadLabel(pm).toString() }
            .sortedBy { it.second.lowercase() }
            .forEach { (ri, name) ->
                val pkg = ri.activityInfo.packageName
                arr.put(JSONObject().put("pkg", pkg).put("name", name).put("icon", iconBase64(ri.loadIcon(pm))).put("isNew", pkg in newApps))
            }
        return arr.toString()
    }

    private fun iconBase64(d: android.graphics.drawable.Drawable): String = try {
        val bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); d.setBounds(0, 0, 48, 48); d.draw(c)
        val out = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) { "" }

    @JavascriptInterface
    fun setGames(json: String) {
        val arr = try { JSONArray(json) } catch (e: Exception) { return }
        Store.setGames(ctx, (0 until arr.length()).map { arr.getString(it) }.toSet())
    }

    @JavascriptInterface
    fun getGames(): String = JSONArray(Store.games(ctx).toList()).toString()

    @JavascriptInterface
    fun markSeen(pkg: String) = Store.markSeen(ctx, pkg)

    @JavascriptInterface
    fun setBalance(sec: Int) = Store.setWebBalance(ctx, sec)

    @JavascriptInterface
    fun takeUsedSeconds(): Int = Store.takeUsed(ctx)

    @JavascriptInterface
    fun isGuardOn(): Boolean {
        val me = ComponentName(ctx, GameGuardService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { it.equals(me, ignoreCase = true) || it.equals(ComponentName(ctx, GameGuardService::class.java).flattenToShortString(), ignoreCase = true) }
    }

    @JavascriptInterface
    fun openGuardSettings() {
        activity.runOnUiThread {
            try { activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (e: Exception) {}
        }
    }

    /** Mokymosi puslapis (EDUKA, Oxford…) — atidaromas programėlės viduje; pramogos — išorėje. */
    @JavascriptInterface
    fun openLearning(url: String) {
        activity.runOnUiThread {
            val u = Uri.parse(url)
            try {
                if (EduWebActivity.isWeb(u) && !EduWebActivity.isBlocked(u))
                    activity.startActivity(Intent(activity, EduWebActivity::class.java).putExtra(EduWebActivity.EXTRA_URL, url))
                else activity.startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {}
        }
    }

    @JavascriptInterface
    fun getAppVersion(): String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
}
