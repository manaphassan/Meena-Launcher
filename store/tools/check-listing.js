// Checks store/listing.md: block lengths against Play limits, banned words and non-ASCII characters.
// usage: node store/tools/check-listing.js
const t = require('fs').readFileSync('store/listing.md', 'utf8');
const b = [...t.matchAll(/```\r?\n([\s\S]*?)\r?\n```/g)].map(m => m[1].replace(/\r/g, ''));
const limits = [80, 4000];
const bad = /telemetry|\bram\b|ltpo|realtime|permission|intent|package|canvas|proactive|context|\bbest\b|#1|\btop\b|\bnew\b|\bfree\b|download|install now|[^\x00-\x7F]/gi;
let ok = true;
['short', 'full'].forEach((name, i) => {
  const hits = b[i].match(bad) || [];
  const fits = b[i].length <= limits[i];
  console.log(`${name}: ${b[i].length}/${limits[i]} ${fits ? 'ok' : 'TOO LONG'}${hits.length ? ' banned: ' + hits.join(', ') : ''}`);
  ok = ok && fits && !hits.length;
});
process.exit(ok ? 0 : 1);
