# Mokausi ir žaidžiu

Vaiko mokymosi programėlė planšetei: 1 klasės matematika, lietuvių ir anglų kalbos užduotys; už sėkmingus raundus vaikas gauna laiko žiūrėti tėvų patvirtintus „YouTube“ vaizdo įrašus. Yra nuorodos į EDUKA ir Oxford.

## Kaip paleisti
Statinė svetainė — jokio serverio nereikia.
- Vietoje: `python3 -m http.server 8765` ir atidaryti http://localhost:8765
- Internete: Netlify (žr. žemiau)

## Publikavimas per GitHub + Netlify
1. Sukurk GitHub saugyklą (gali būti privati) ir įkelk šio aplanko turinį.
2. Netlify → Add new site → Import from Git → pasirink saugyklą. Build command palik tuščią, publish directory: `.`
3. Kiekvienas `git push` pats atnaujina svetainę.

## Planšetėje
Chrome → atidaryti svetainės adresą → ⋮ → „Pridėti į pagrindinį ekraną“. Tėvų zona: PIN (numatytasis 1234 — pakeisk).

## Duomenų sinchronizacija
Tėvų zonoje → „Sinchronizacija“ → „Sukurti naują kodą“. Tą patį kodą įvesk telefone — vaizdo įrašus ir taisykles galėsi keisti iš ten. Duomenys saugomi Firebase Firestore (projektas `lukrecijos-programele`). Kodas veikia kaip slaptažodis.

## Testai
Žr. `CLAUDE.md`.
