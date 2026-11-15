# Mokausi ir žaidžiu — vaiko mokymosi programėlė

1 klasės užduotys (matematika, lietuvių, anglų) + „YouTube“ tik tėvų patvirtintiems vaizdo įrašams, kurį laiką vaikas užsidirba išsprendęs užduotis.
Savininkas: Valdas (ne programuotojas). Atsakyk lietuviškai, paprastai, trumpai.

## Struktūra
- `index.html` — visa programa vienu failu (CSS + JS, be bibliotekų ir be build).
- `manifest.json`, `icon*.png/svg` — PWA (įdiegimas planšetėje).
- `firestore.rules` — Firebase saugumo taisyklės (kopija; tikros veikia Firebase konsolėje).
- `netlify.toml` — Netlify publikavimas (tik statiniai failai).
- `tests/` — `test.html` (logika: užduotys, nuorodos, YouTube atlygių riba ir ciklas, seni duomenys), `test2.html` (naudotojo veiksmai: raundai, kortelės, tėvų zona, šuniukas), `synctest.js` (Node, du įrenginiai + netikras Firestore), `run.sh` (paleidžia viską).

## Svarbiausios taisyklės
- Po kiekvieno pakeitimo padidink `VERSION` (index.html viršuje) — Valdas pagal ją planšetėje mato, ar atsinaujino.
- Neperrašinėk veikiančios logikos be reikalo; keisk minimaliai.
- Būsena: localStorage raktas `vp_v1`; šeimos kodas atskirai `vp_family`.
- Sinchronizacija: Firestore REST (be SDK), dokumentas `families/{kodas}`, laukas `data` (JSON tekstas). Tėvų dalis (`P_F`: videos, settings, bonusTotal, cheer; `pts`) ir vaiko dalis (`C_F`: stars, balance, earned, log, bonusApplied, rounds, levels, streak, badges, rewards; `cts`) sulyginamos atskirai pagal laiko žymą (naujesnis laimi). Naujus laukus dėk į `DEFAULTS` (ir į `P_F`/`C_F`), kad seni duomenys nesulūžtų.
- Tėvų +5/+10 min. keliauja kaip skirtumas `bonusTotal − bonusApplied` (nedingsta sinchronizuojant). Prisijungiant prie kodo (`joining`) laimi įrenginys su tikrais duomenimis.
- YouTube atlygių riba: dalykas per ciklą duoda ne daugiau kaip `settings.perSubject` (1–10, numatyta 3) atlygių; skaitliukai `rewards` (vaiko dalis). Atlygis skaičiuojamas tik kai raundas iš tikro davė minučių. Kai visi dalykai pasiekia ribą — `cycleCheck()` nunulina visus (naujas ciklas). Per naktį ciklas nenusinulina; dienos riba `cap` galioja papildomai.
- Laikas skaičiuojamas tik kai YouTube grotuvo būsena = 1 (groja).
- Firebase projektas: `lukrecijos-programele` (Spark, nemokamas). `apiKey` web programoms yra viešas; apsaugą užtikrina `firestore.rules` ir slaptas šeimos kodas.
- Numatytasis PIN 1234 — keisti tėvų zonoje. PIN tik užraktas nuo vaiko, ne tikra apsauga.

## Testai prieš kiekvieną pakeitimą
- Viskas vienu kartu: `bash tests/run.sh` (reikia Chrome ir Node; turi baigtis „VISI TESTAI PRAĖJO“).
- Arba rankiniu būdu: `python3 -m http.server 8765` → `http://localhost:8765/tests/test.html` ir `tests/test2.html` (#out: „Klaidų: 0“); `node tests/synctest.js`.
- Testai išvalo svetainės localStorage — leisti tik vietiniame serveryje.

## Dar neužbaigta
- Tikrinti planšetėje: lietuviškas TTS balsas, YouTube įterpimas, Family Link „Always allowed“ PWA atveju (jei neveiks — reikės natyvaus APK).
