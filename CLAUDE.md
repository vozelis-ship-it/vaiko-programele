# Mokausi ir žaidžiu — vaiko mokymosi programėlė

1 klasės užduotys (matematika, lietuvių, anglų) + „YouTube“ tik tėvų patvirtintiems vaizdo įrašams, kurį laiką vaikas užsidirba išsprendęs užduotis.
Savininkas: Valdas (ne programuotojas). Atsakyk lietuviškai, paprastai, trumpai.

## Struktūra
- `index.html` — visa programa vienu failu (CSS + JS, be bibliotekų ir be build).
- `manifest.json`, `icon*.png/svg` — PWA (įdiegimas planšetėje).
- `firestore.rules` — Firebase saugumo taisyklės (kopija; tikros veikia Firebase konsolėje).
- `netlify.toml` — Netlify publikavimas (tik statiniai failai). Svetainė: https://mokausi-ir-zaidziu.netlify.app/
- `android/` — Android programėlė „Mokausi“ (Kotlin, minSdk 26, targetSdk 35, paketas `lt.mokausi.app`): WebView su svetaine + sargas žaidimams. Versija `android/version.txt`.
- `.github/workflows/android.yml` — pasirašytas APK + GitHub Release. `docs/ANDROID.md` — instrukcija tėvams.
- `tests/` — `test.html` (logika: užduotys, nuorodos, YouTube atlygių riba ir ciklas, seni duomenys), `test2.html` (naudotojo veiksmai: raundai, kortelės, tėvų zona, šuniukas), `test3.html` (Android tiltas su netikru AndroidBridge), `synctest.js` (Node, du įrenginiai + netikras Firestore), `run.sh` (paleidžia viską).

## Svarbiausios taisyklės
- Po kiekvieno pakeitimo padidink `VERSION` (index.html viršuje) — Valdas pagal ją planšetėje mato, ar atsinaujino.
- Neperrašinėk veikiančios logikos be reikalo; keisk minimaliai.
- Būsena: localStorage raktas `vp_v1`; šeimos kodas atskirai `vp_family`.
- Sinchronizacija: Firestore REST (be SDK), dokumentas `families/{kodas}`, laukas `data` (JSON tekstas). Tėvų dalis (`P_F`: videos, settings, bonusTotal, cheer; `pts`) ir vaiko dalis (`C_F`: stars, balance, earned, log, bonusApplied, rounds, levels, streak, badges, rewards; `cts`) sulyginamos atskirai pagal laiko žymą (naujesnis laimi). Naujus laukus dėk į `DEFAULTS` (ir į `P_F`/`C_F`), kad seni duomenys nesulūžtų.
- Tėvų +5/+10 min. keliauja kaip skirtumas `bonusTotal − bonusApplied` (nedingsta sinchronizuojant). Prisijungiant prie kodo (`joining`) laimi įrenginys su tikrais duomenimis.
- Lygiai 1–3 (`levels`): kyla po `PERFECT_TO_LEVEL` (3) raundų be klaidų (10/10) tame lygyje, skaičiuojama sumiškai (`perfects`, vaiko dalis); 0–4 teisingi → žemyn. Pasikeitus lygiui `perfects` nusinulina.
- YouTube atlygių riba: dalykas per ciklą duoda ne daugiau kaip `settings.perSubject` (1–10, numatyta 3) atlygių; skaitliukai `rewards` (vaiko dalis). Atlygis skaičiuojamas tik kai raundas iš tikro davė minučių. Kai visi dalykai pasiekia ribą — `cycleCheck()` nunulina visus (naujas ciklas). Per naktį ciklas nenusinulina; dienos riba `cap` galioja papildomai.
- Laikas skaičiuojamas tik kai YouTube grotuvo būsena = 1 (groja).
- Firebase projektas: `lukrecijos-programele` (Spark, nemokamas). `apiKey` web programoms yra viešas; apsaugą užtikrina `firestore.rules` ir slaptas šeimos kodas.
- Numatytasis PIN 1234 — keisti tėvų zonoje. PIN tik užraktas nuo vaiko, ne tikra apsauga.

## Testai prieš kiekvieną pakeitimą
- Viskas vienu kartu: `bash tests/run.sh` (reikia Chrome ir Node; turi baigtis „VISI TESTAI PRAĖJO“).
- Arba rankiniu būdu: `python3 -m http.server 8765` → `http://localhost:8765/tests/test.html` ir `tests/test2.html` (#out: „Klaidų: 0“); `node tests/synctest.js`.
- Testai išvalo svetainės localStorage — leisti tik vietiniame serveryje.

## Android dalis
- Failai `android/app/src/main/java/lt/mokausi/app/`: `MainActivity` (WebView, offline ekranas, pilno ekrano video, window.open → išorėje, mikrofonas Bobiui), `AndroidBridge` (JS tiltas), `Store` (SharedPreferences), `GameGuardService` (AccessibilityService), `BlockActivity`, `Updater` (GitHub Releases).
- `window.AndroidBridge`: `getApps()` [{pkg,name,icon(48px base64),isNew}], `setGames(json)`, `getGames()`, `markSeen(pkg)`, `setBalance(sec)`, `takeUsedSeconds()`, `isGuardOn()`, `openGuardSettings()`, `getAppVersion()`.
- Balansas programėlėje = paskutinis `setBalance` − dar nepaimtos žaidimų sekundės (todėl pasenęs setBalance nesugadina). Svetainė: `abPull()` startuojant ir grįžus (visibilitychange + `window.__vpAndroidResume`) atima `takeUsedSeconds()` iš `st.balance`, įrašo `{s:'games', t:'games', sec}` į log; `abPushBalance()` po `save()` ir kas 5 s.
- index.html: VISI Android kvietimai per `abCall()` ir tik jei `AB` (window.AndroidBridge) yra — naršyklėje nieko nekeičia. Žaidimų įrašai log'e neskaičiuojami kaip užduotys.
- Kompiliuoti vietoje: reikia JDK 17 ir Android SDK (35); `cd android && ./gradlew assembleDebug` (sdk.dir į `android/local.properties`, jis .gitignore).
- **Nauja Android versija**: padidink `android/version.txt` (pvz. 1.0.0 → 1.0.1), commit + push → GitHub Actions sukuria Release `v1.0.1` su `app-release.apk` → programėlė pasiūlo atnaujinti.
- Raktas: `~/Documents/Mokausi-raktas/` (NE repo). Secrets: ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, ANDROID_KEY_PASSWORD.
- ⚠️ Repo privatus: GitHub API/Releases be prisijungimo nepasiekiami — atnaujinimų tikrinimas ir APK atsisiuntimas planšetėje veiks tik viešame repo (arba reikės kitos vietos APK).

## Užduotys pagal pratybas
- Pratybų nuotraukos: `/Users/v.ozelis/Desktop/Claude darbai/TEMPLLATES/Vaiko programele/<data>/` — NEdėti į šį aplanką (Netlify publikuoja viską, būtų vieša).
- 2026-11-15 pagal pratybas (skaičiai iki 20, tekstiniai uždaviniai, atkarpos cm, „į“, vardai, Ką? nosinė) pridėta: `numname`, `neighbor`, `ruler`, `rulercmp`, `story` su schema (1 lygis), `nosine` (+ `ACC` formos), `caps`, `ijword`. Užduotis kurti savas, ne kopijuoti pratybų.

## Dar neužbaigta
- Tikrinti planšetėje: lietuviškas TTS balsas, YouTube įterpimas, Family Link „Always allowed“ PWA atveju (jei neveiks — reikės natyvaus APK).
