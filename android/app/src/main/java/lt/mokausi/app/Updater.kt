package lt.mokausi.app

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Atnaujinimai per GitHub Releases. Bet kokia klaida — programėlė tiesiog veikia toliau. */
object Updater {
    private const val API = "https://api.github.com/repos/vozelis-ship-it/vaiko-programele/releases/latest"
    private const val APK = "app-release.apk"

    /** v1.0.3 → 10003 */
    fun codeOf(tag: String): Int? = try {
        val p = tag.trim().removePrefix("v").split(".").map { it.toInt() }
        if (p.size != 3) null else p[0] * 10000 + p[1] * 100 + p[2]
    } catch (e: Exception) { null }

    fun check(a: Activity) {
        Thread {
            try {
                val c = URL(API).openConnection() as HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 8000
                c.setRequestProperty("Accept", "application/vnd.github+json")
                c.setRequestProperty("User-Agent", "Mokausi-app")
                if (c.responseCode != 200) return@Thread
                val j = JSONObject(c.inputStream.bufferedReader().readText())
                val code = codeOf(j.optString("tag_name")) ?: return@Thread
                if (code <= BuildConfig.VERSION_CODE) return@Thread
                val assets = j.optJSONArray("assets") ?: return@Thread
                val url = (0 until assets.length()).map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name") == APK }?.optString("browser_download_url") ?: return@Thread
                a.runOnUiThread {
                    if (a.isFinishing) return@runOnUiThread
                    AlertDialog.Builder(a)
                        .setTitle("Mokausi")
                        .setMessage("Yra nauja versija (${j.optString("tag_name")}). Atnaujinti?")
                        .setPositiveButton("Atnaujinti") { _, _ -> download(a, url) }
                        .setNegativeButton("Vėliau", null)
                        .show()
                }
            } catch (e: Exception) { /* be interneto ar GitHub klaida — nieko nedarome */ }
        }.start()
    }

    private fun download(a: Activity, url: String) {
        try {
            if (!a.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(a, "Leisk „Mokausi“ diegti programas ir paspausk „Atnaujinti“ dar kartą.", Toast.LENGTH_LONG).show()
                a.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${a.packageName}")))
                return
            }
            val dir = a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
            File(dir, APK).delete()
            val dm = a.getSystemService(DownloadManager::class.java)
            val id = dm.enqueue(DownloadManager.Request(Uri.parse(url))
                .setTitle("Mokausi atnaujinimas")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(a, Environment.DIRECTORY_DOWNLOADS, APK))
            Toast.makeText(a, "Siunčiama nauja versija…", Toast.LENGTH_SHORT).show()
            val ctx = a.applicationContext
            ContextCompat.registerReceiver(ctx, object : BroadcastReceiver() {
                override fun onReceive(c: Context, i: Intent) {
                    if (i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return
                    try { ctx.unregisterReceiver(this) } catch (e: Exception) {}
                    install(ctx, File(dir, APK))
                }
            }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            Toast.makeText(a, "Nepavyko atsisiųsti atnaujinimo.", Toast.LENGTH_LONG).show()
        }
    }

    private fun install(c: Context, f: File) {
        try {
            if (!f.exists() || f.length() == 0L) return
            val uri = FileProvider.getUriForFile(c, "${c.packageName}.fileprovider", f)
            c.startActivity(Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {}
    }
}
