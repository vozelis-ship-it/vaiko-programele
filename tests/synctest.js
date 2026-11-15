// Sinchronizacijos testai su netikru Firestore (be interneto). Paleisti: node tests/synctest.js
// Imituoja du įrenginius (planšetę ir telefoną), kiekvienas su savo localStorage.
const fs = require('fs'), vm = require('vm'), path = require('path');
const html = fs.readFileSync(path.join(__dirname, '..', 'index.html'), 'utf8');
const code = html.split('<script>')[1].split('</script>')[0] +
  '\n;window.__h={H,setView:v=>{view=v},setPlayer:p=>{player=p},setCur:v=>{ytCur=v;ytPl=null},vA:()=>videoAllowed()};';
const DB = {}; let clock = 1e12;
function device() {
  const ls = {}, el = { innerHTML: '', addEventListener() {}, querySelectorAll() { return []; }, classList: { add() {} } };
  const ctx = { console, JSON, Math, Object, Array, String, Set, URL, Uint8Array, Promise, Error, encodeURIComponent, parseInt,
    Date: class extends Date { static now() { return clock += 1000; } },
    crypto: { getRandomValues: a => { for (let i = 0; i < a.length; i++) a[i] = Math.random() * 256 | 0; return a; } },
    localStorage: { getItem: k => ls[k] ?? null, setItem: (k, v) => ls[k] = v, removeItem: k => delete ls[k] },
    document: { getElementById: () => el, addEventListener() {}, querySelector() { return null; }, createElement: () => ({ remove() {}, style: {}, appendChild() {}, querySelector() { return null; } }), head: { appendChild() {} }, body: { appendChild() {} } },
    navigator: { onLine: true }, scrollTo() {}, speechSynthesis: { getVoices: () => [], cancel() {} }, confirm: () => true,
    setTimeout: () => 0, clearTimeout() {}, setInterval: () => 0, clearInterval() {}, addEventListener() {},
    requestAnimationFrame: () => 0, performance: { now: () => 0 },
    fetch: async (u, o) => { const id = decodeURIComponent(u.split('/families/')[1].split('?')[0]);
      if (o && o.method === 'PATCH') { DB[id] = JSON.parse(o.body).fields; return { ok: true, status: 200 }; }
      return DB[id] ? { status: 200, json: async () => ({ fields: DB[id] }) } : { status: 404 }; } };
  ctx.window = ctx; vm.createContext(ctx); vm.runInContext(code, ctx); return ctx;
}
const st = d => d.__vp.getState(), run = (d, s) => vm.runInContext(s, d);
let fails = 0; const ok = (c, m) => { console.log((c ? '✅ ' : '❌ ') + m); if (!c) fails++; };
(async () => {
  // Kodas sukuriamas telefone, planšetė su progresu prisijungia vėliau
  const tab = device(), ph = device();
  Object.assign(st(tab), { stars: 40, balance: 600, log: Array(40).fill({ d: 'x', s: 'math', t: 'add', ok: true }), videos: [{ kind: 'video', id: 'aaaaaaaaaaa', title: 'A' }] });
  run(tab, 'save()');
  run(ph, 'H.newfam()'); await new Promise(r => setImmediate(r));
  const code = run(ph, 'famCode');
  run(tab, `setFam('${code}'); joining = true`); await tab.__vp.sync();
  ok(st(tab).stars === 40 && st(tab).videos.length === 1, 'Prisijungus planšetės progresas ir vaizdo įrašai išliko');
  await ph.__vp.sync(); ok(st(ph).stars === 40 && st(ph).videos.length === 1, 'Telefonas gavo planšetės duomenis');
  // Tėvai prideda +10 min telefone, kol planšetė groja
  tab.__h.setView('ytplay'); st(tab).balance -= 100; run(tab, 'save()');
  run(ph, "H.bonus({ m: '10' })"); await ph.__vp.sync();
  ok(st(ph).balance === 1200, 'Telefone rodomas +10 min.');
  await tab.__vp.sync(); ok(st(tab).balance === 1100, 'Grojanti planšetė gavo +10 min.');
  st(tab).balance -= 50; run(tab, 'save()'); tab.__h.setView('yt'); await tab.__vp.sync();
  await ph.__vp.sync(); ok(st(ph).balance === 1050 && st(tab).balance === 1050, 'Po žiūrėjimo abiejų įrenginių laikas sutampa');
  await tab.__vp.sync(); ok(st(tab).balance === 1050, 'Papildomas laikas nepridedamas dvigubai');
  // YouTube atlygių riba: skaitliukai vaiko dalyje, riba tėvų dalyje
  tab.__h.setView('home');
  run(tab, "st.settings.cap = 240; S = { subj: 'math', qs: Array(10).fill({}), i: 10, correct: 10, locked: false }; finishRound()");
  await tab.__vp.sync(); await ph.__vp.sync();
  ok(st(ph).rewards.math === 1, 'Atlygių skaitliukas sinchronizuojasi į telefoną (vaiko dalis)');
  run(ph, "st.settings.perSubject = 5; save()"); await ph.__vp.sync(); await tab.__vp.sync();
  ok(st(tab).settings.perSubject === 5 && st(tab).rewards.math === 1, 'Ribos nustatymas iš telefono pasiekia planšetę (tėvų dalis), skaitliukai išlieka');
  // Seni nuotoliniai duomenys be naujų laukų
  DB['senaseimakodas1'] = { data: { stringValue: JSON.stringify({ videos: [], settings: { pin: '1234' }, stars: 3, balance: 0, earned: {}, log: [], pts: 1, cts: 1 }) } };
  const old = device(); run(old, "setFam('senaseimakodas1'); joining = true"); await old.__vp.sync();
  ok(st(old).rewards.math === 0 && run(old, 'perSubj()') === 3 && st(old).stars === 3, 'Seni Firestore duomenys be rewards/perSubject nesulūžta');
  // README eiga: kodas kuriamas planšetėje, tuščias telefonas prisijungia
  const t2 = device(), p2 = device();
  Object.assign(st(t2), { stars: 5, log: [1, 2, 3, 4, 5], videos: [{ kind: 'video', id: 'bbbbbbbbbbb' }] }); run(t2, 'save()');
  run(t2, 'H.newfam()'); await t2.__vp.sync(); const c2 = run(t2, 'famCode');
  run(p2, `setFam('${c2}'); joining = true`); await p2.__vp.sync();
  ok(st(p2).stars === 5 && st(p2).videos.length === 1, 'README eiga: telefonas gavo planšetės duomenis');
  // Nepatvirtintų vaizdo įrašų blokavimas
  const t3 = device(); st(t3).videos = [{ kind: 'video', id: 'ccccccccccc' }];
  let vid = 'ccccccccccc';
  t3.__h.setPlayer({ getPlayerState: () => 1, getVideoData: () => ({ video_id: vid }), getVideoUrl: () => '', cueVideoById() {} });
  t3.__h.setCur({ kind: 'video', id: 'ccccccccccc' });
  ok(t3.__h.vA(), 'Patvirtintas vaizdo įrašas leidžiamas'); vid = 'zzzzzzzzzzz'; ok(!t3.__h.vA(), 'Nepatvirtintas vaizdo įrašas blokuojamas');
  console.log(`Klaidų: ${fails}`); process.exit(fails ? 1 : 0);
})();
