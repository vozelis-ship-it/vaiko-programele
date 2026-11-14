# Mokausi ir žaidžiu — vaiko mokymosi programėlė

1 klasės užduotys (matematika, lietuvių, anglų) + „YouTube“ tik tėvų patvirtintiems vaizdo įrašams, kurį laiką vaikas užsidirba išsprendęs užduotis.
Savininkas: Valdas (ne programuotojas). Atsakyk lietuviškai, paprastai, trumpai.

## Struktūra
- `index.html` — visa programa vienu failu (CSS + JS, be bibliotekų ir be build).
- `manifest.json`, `icon*.png/svg` — PWA (įdiegimas planšetėje).
- `firestore.rules` — Firebase saugumo taisyklės (kopija; tikros veikia Firebase konsolėje).
- `netlify.toml` — Netlify publikavimas (tik statiniai failai).
- `tests/` — `test.html`, `test2.html` (atidaryti per `python3 -m http.server`, žr. `#out`), `synctest.py` (Playwright, netikras Firestore).

## Svarbiausios taisyklės
- Po kiekvieno pakeitimo padidink `VERSION` (index.html viršuje) — Valdas pagal ją planšetėje mato, ar atsinaujino.
- Neperrašinėk veikiančios logikos be reikalo; keisk minimaliai.
- Būsena: localStorage raktas `vp_v1`; šeimos kodas atskirai `vp_family`.
- Sinchronizacija: Firestore REST (be SDK), dokumentas `families/{kodas}`, laukas `data` (JSON tekstas). Tėvų dalis (videos+settings, `pts`) ir vaiko dalis (stars, balance, earned, log, `cts`) sulyginamos atskirai pagal laiko žymą (naujesnis laimi).
- Laikas skaičiuojamas tik kai YouTube grotuvo būsena = 1 (groja).
- Firebase projektas: `lukrecijos-programele` (Spark, nemokamas). `apiKey` web programoms yra viešas; apsaugą užtikrina `firestore.rules` ir slaptas šeimos kodas.
- Numatytasis PIN 1234 — keisti tėvų zonoje. PIN tik užraktas nuo vaiko, ne tikra apsauga.

## Testai prieš kiekvieną pakeitimą
1. `python3 -m http.server 8765` ir atidaryti `tests/test.html`, `tests/test2.html` (klaidų turi būti 0).
2. `python3 tests/synctest.py` (reikia `pip install playwright`, Chrome).

## Dar neužbaigta
- Tikrinti planšetėje: lietuviškas TTS balsas, YouTube įterpimas, Family Link „Always allowed“ PWA atveju (jei neveiks — reikės natyvaus APK).
