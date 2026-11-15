package lt.mokausi.app

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Sargas: kai pirmame plane yra programėlė iš žaidimų sąrašo, kas sekundę (tik įjungus ekraną)
 * skaičiuoja sunaudotą laiką; kai balansas baigiasi — rodo BlockActivity.
 */
class GameGuardService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var current: String? = null
    private var warned = false
    private var lastBlock = 0L
    private var receiverOn = false

    private val tick = object : Runnable {
        override fun run() { onTick(); handler.postDelayed(this, 1000) }
    }

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            val pkg = i.data?.schemeSpecificPart ?: return
            if (c.packageManager.getLaunchIntentForPackage(pkg) != null) Store.markNew(c, pkg)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.removeCallbacks(tick); handler.post(tick)
        val f = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply { addDataScheme("package") }
        ContextCompat.registerReceiver(this, pkgReceiver, f, ContextCompat.RECEIVER_EXPORTED)
        receiverOn = true
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val p = e.packageName?.toString() ?: return
        if (ignored(p)) return
        current = p
        if (p in Store.games(this) && Store.balance(this) <= 0) block()
    }

    private fun ignored(p: String): Boolean {
        if (p == "com.android.systemui") return true
        val imm = getSystemService(InputMethodManager::class.java) ?: return false
        return imm.enabledInputMethodList.any { it.packageName == p }   // klaviatūra
    }

    private fun onTick() {
        val p = current ?: return
        if (!getSystemService(PowerManager::class.java).isInteractive) return
        if (p !in Store.games(this)) return
        val bal = Store.balance(this)
        if (bal <= 0) { block(); return }
        Store.addUsed(this, 1)
        val left = bal - 1
        if (left > 60) warned = false
        else if (!warned && left > 0) { warned = true; Toast.makeText(this, "Liko 1 minutė", Toast.LENGTH_LONG).show() }
        if (left <= 0) block()
    }

    private fun block() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlock < 1500) return
        lastBlock = now
        startActivity(Intent(this, BlockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        if (receiverOn) try { unregisterReceiver(pkgReceiver) } catch (e: Exception) {}
        super.onDestroy()
    }
}
