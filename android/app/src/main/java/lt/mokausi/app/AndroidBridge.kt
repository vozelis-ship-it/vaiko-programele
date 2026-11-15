package lt.mokausi.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.util.Base64
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** window.AndroidBridge — kviečiamas iš svetainės (index.html). */
class AndroidBridge(private val activity: Activity) {
    private val ctx = activity.applicationContext

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

    @JavascriptInterface
    fun getAppVersion(): String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
}
