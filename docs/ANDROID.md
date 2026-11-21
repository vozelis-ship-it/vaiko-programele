# „Mokausi“ Android programėlė — instrukcija tėvams

Programėlė rodo tą pačią svetainę (užduotys, YouTube, šuniukas Bobis), o papildomai **užblokuoja pasirinktas programėles** (žaidimus, YouTube, Chrome ir kt.), kai baigiasi užduotimis užsidirbtas laikas.

---

## a) Leisti diegti programą ne iš Play parduotuvės (Family Link)
1. Savo telefone atidaryk **Family Link** → pasirink vaiką.
2. **Valdikliai** → **Įrenginiai** → vaiko planšetė.
3. **Programos iš nežinomų šaltinių** → **Įjungti**.

## b) Įdiegti programėlę planšetėje
1. Planšetėje atidaryk **Chrome** ir eik į:
   `https://github.com/vozelis-ship-it/vaiko-programele/releases/latest`
2. Skiltyje **Assets** paspausk **app-release.apk** → **Atsisiųsti**.
3. Atidaryk atsisiųstą failą → **Įdiegti**. (Jei Chrome paklaus — leisk jam diegti programas.)
4. Pagrindiniame ekrane atsiras **„Mokausi“** ikona.

## c) Įjungti sargą
1. Atidaryk „Mokausi“ → ⚙️ → įvesk PIN → kortelė **„Programėlės planšetėje“** → **Įjungti**.
2. Atsidarys: **Nustatymai → Pritaikymas neįgaliesiems → Mokausi** → **Įjungti** → **Leisti**.
3. Jei Android rašo **„Ribojamas nustatymas“**: **Nustatymai → Programėlės → Mokausi → ⋮** (viršuje dešinėje) → **„Leisti apribotus nustatymus“**, tada pakartok 2 žingsnį.
4. Grįžus į „Mokausi“ turi būti parašyta **„Sargas įjungtas ✅“**.

## d) Pažymėti, kurias programėles riboti
Toje pačioje kortelėje prie kiekvienos programėlės yra jungiklis **„Riboti laiką (žaidimas)“**. Pažymėk žaidimus, **YouTube**, **Chrome** ir viską, kur vaikas gali „pražūti“.
- Kai laikas baigiasi, atsidaro ekranas „⏰ Laikas baigėsi“ su mygtuku **„Į užduotis“**.
- Likus 1 min. parodomas pranešimas „Liko 1 minutė“.
- Naujai įdiegtos programėlės sąrašo viršuje pažymėtos **NAUJA** — nepamiršk jų peržiūrėti.
- Laikas bendras: tas pats balansas ir YouTube „Mokausi“ viduje, ir žaidimams. Dienos riba (pvz. 20 min.) galioja viskam kartu.

### EDUKA ir Oxford
EDUKA ir Oxford (anglų) puslapiai atsidaro **pačioje „Mokausi“ programėlėje** (viršuje mygtukas „← Į Mokausi“), ne Chrome — todėl Chrome ribojimai jiems negalioja. Prisijungimas išsaugomas. YouTube, TikTok ir kitos pramogos ten neatsidaro.

## e) Family Link nustatymai
1. **Mokausi** → **Visada leidžiama** (kad bendras limitas jos neužrakintų — vaikas turi galėti spręsti užduotis).
2. **Išjunk bendrą dienos limitą** — laiką dabar skaičiuoja „Mokausi“.
3. **Miego laiką palik** — naktį planšetė vis tiek užsirakins.
4. Nereikalingas programėles (kurių nenori, kad vaikas naudotų išvis) **užblokuok** Family Link.

## f) Atnaujinimai
- **Svetainės dalis** (užduotys, šuniukas ir pan.) atsinaujina automatiškai po kiekvieno pakeitimo GitHub — nieko daryti nereikia.
- **Android dalis**: atidarius „Mokausi“, jei yra nauja versija, pasirodys **„Yra nauja versija. Atnaujinti?“** → **Atnaujinti** → **Įdiegti**. Pirmą kartą Android paprašys leisti „Mokausi“ diegti programas — leisk.

---

### Vienkartinis paruošimas GitHub (tėvams, kompiuteryje)
Kad GitHub galėtų pats sukurti pasirašytą APK, reikia įdėti 4 slaptus nustatymus (Secrets):
1. Atidaryk `https://github.com/vozelis-ship-it/vaiko-programele` → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**.
2. Sukurk 4 įrašus (reikšmės — faile `~/Documents/Mokausi-raktas/SLAPTAZODZIAI.txt`):
   - `ANDROID_KEYSTORE_BASE64` — visas failo `ANDROID_KEYSTORE_BASE64.txt` turinys (atidaryk TextEdit → Cmd+A → Cmd+C → įklijuok);
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS` (`mokausi`)
   - `ANDROID_KEY_PASSWORD`
3. **Actions** → **Android** → **Run workflow**. Po ~5 min. atsiras **Releases** → `app-release.apk`.

⚠️ **Raktas** (`mokausi-release.jks` ir slaptažodžiai) — be jo nebus galima atnaujinti įdiegtos programėlės. Laikyk atsarginę kopiją (pvz. USB raktelyje ir slaptažodžių tvarkyklėje). Į GitHub jo nekelk.
