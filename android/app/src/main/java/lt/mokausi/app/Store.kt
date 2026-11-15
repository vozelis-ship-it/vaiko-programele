package lt.mokausi.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Bendri duomenys (SharedPreferences) tarp WebView tilto ir sargo paslaugos.
 * Balansas = paskutinis svetainės praneštas balansas − dar nepaimtos žaidimų sekundės (used).
 * Todėl net pasenęs setBalance() iš svetainės nesugadina skaičiavimo.
 */
object Store {
    private const val FILE = "mokausi"
    private fun sp(c: Context): SharedPreferences = c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    @Synchronized fun games(c: Context): Set<String> = sp(c).getStringSet("games", emptySet())!!.toSet()
    @Synchronized fun setGames(c: Context, s: Set<String>) { sp(c).edit().putStringSet("games", HashSet(s)).apply() }

    @Synchronized fun balance(c: Context): Int = maxOf(0, sp(c).getInt("webBalance", 0) - sp(c).getInt("used", 0))
    @Synchronized fun setWebBalance(c: Context, sec: Int) { sp(c).edit().putInt("webBalance", maxOf(0, sec)).apply() }
    @Synchronized fun addUsed(c: Context, sec: Int) { sp(c).edit().putInt("used", sp(c).getInt("used", 0) + sec).apply() }

    /** Grąžina žaidimuose sunaudotas sekundes ir nunulina skaitiklį. */
    @Synchronized fun takeUsed(c: Context): Int {
        val p = sp(c); val used = p.getInt("used", 0)
        p.edit().putInt("webBalance", maxOf(0, p.getInt("webBalance", 0) - used)).putInt("used", 0).apply()
        return used
    }

    @Synchronized fun newApps(c: Context): Set<String> = sp(c).getStringSet("newApps", emptySet())!!.toSet()
    @Synchronized fun markNew(c: Context, pkg: String) { sp(c).edit().putStringSet("newApps", HashSet(newApps(c) + pkg)).apply() }
    @Synchronized fun markSeen(c: Context, pkg: String) { sp(c).edit().putStringSet("newApps", HashSet(newApps(c) - pkg)).apply() }

    @Synchronized fun knownApps(c: Context): Set<String> = sp(c).getStringSet("knownApps", emptySet())!!.toSet()
    @Synchronized fun setKnownApps(c: Context, s: Set<String>) { sp(c).edit().putStringSet("knownApps", HashSet(s)).apply() }
}
