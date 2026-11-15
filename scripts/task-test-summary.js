// Summarises the Meena task test (docs/task-test-plan.md) and checks the pass rule.
// usage: node scripts/task-test-summary.js <results.csv> <A first-screen count> <B first-screen count>
const fs = require('fs');

const [file, countA, countB] = process.argv.slice(2);
if (!file) { console.error('usage: node scripts/task-test-summary.js <results.csv> [countA countB]'); process.exit(2); }

const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/).map(l => l.trim()).filter(Boolean);
const head = lines.shift().split(',');
const rows = lines.map((l, i) => {
  const cells = l.split(',');
  if (cells.length !== head.length) throw new Error(`line ${i + 2}: expected ${head.length} columns, got ${cells.length}`);
  const r = Object.fromEntries(head.map((h, j) => [h, cells[j]]));
  for (const k of ['seconds', 'taps', 'scrolls', 'errors', 'success']) {
    r[k] = Number(r[k]);
    if (!Number.isFinite(r[k])) throw new Error(`line ${i + 2}: ${k} is not a number`);
  }
  return r;
});
if (!rows.length) { console.log('no results yet'); process.exit(0); }

const median = xs => {
  if (!xs.length) return NaN;
  const s = [...xs].sort((a, b) => a - b), m = s.length >> 1;
  return s.length % 2 ? s[m] : (s[m - 1] + s[m]) / 2;
};
const pick = (variant, task) => rows.filter(r => r.variant === variant && r.task === task);
const tasks = [...new Set(rows.map(r => r.task))].sort();
const people = new Set(rows.map(r => r.participant));

console.log(`${people.size} people, ${rows.length} rows`);
console.log('task  home  runs  median s  median taps  median scrolls  errors  done');
for (const t of tasks) {
  for (const v of ['A', 'B']) {
    const rs = pick(v, t);
    if (!rs.length) continue;
    const sum = k => rs.reduce((a, r) => a + r[k], 0);
    console.log([t.padEnd(4), v.padEnd(4), String(rs.length).padStart(4),
      String(median(rs.map(r => r.seconds))).padStart(9),
      String(median(rs.map(r => r.taps))).padStart(12),
      String(median(rs.map(r => r.scrolls))).padStart(15),
      String(sum('errors')).padStart(7), `${sum('success')}/${rs.length}`.padStart(5)].join('  '));
  }
}

const results = [];
const check = (name, ok) => results.push([name, ok]);
const t1A = pick('A', 'T1'), t1B = pick('B', 'T1');
if (t1A.length && t1B.length) {
  check('T1 median time lower on B', median(t1B.map(r => r.seconds)) < median(t1A.map(r => r.seconds)));
}
if (t1B.length) check('every B run of T1 has 0 scrolls', t1B.every(r => r.scrolls === 0));
if (countA !== undefined && countB !== undefined) check(`first-screen count lower on B (${countB} vs ${countA})`, Number(countB) < Number(countA));
for (const t of tasks) {
  const a = pick('A', t), b = pick('B', t);
  if (a.length && b.length) {
    const ea = a.reduce((s, r) => s + r.errors, 0), eb = b.reduce((s, r) => s + r.errors, 0);
    check(`${t} errors on B no higher than A (${eb} vs ${ea})`, eb <= ea);
  }
}
console.log('');
for (const [name, ok] of results) console.log(`${ok ? 'pass' : 'FAIL'}  ${name}`);
if (people.size < 5) console.log(`note: only ${people.size} people; the plan needs 5 or more`);
process.exit(results.every(([, ok]) => ok) ? 0 : 1);
