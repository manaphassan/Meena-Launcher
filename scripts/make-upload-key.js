// Creates the Meena upload key for Google Play (Play App Signing keeps the real app signing key).
// - keystore goes OUTSIDE the repo (default D:\HaNa_Innovation\keys\meena-upload.jks)
// - a random password is written only to keystore.properties (git-ignored), never printed
// - refuses to overwrite an existing keystore or keystore.properties
// usage: node scripts/make-upload-key.js [keystorePath]
// Docs: https://developer.android.com/studio/publish/app-signing#generate-key
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { spawnSync } = require('child_process');

const keytool = process.env.KEYTOOL || 'C:\\Program Files\\Microsoft\\jdk-21.0.12.101-hotspot\\bin\\keytool.exe';
const store = path.resolve(process.argv[2] || 'D:\\HaNa_Innovation\\keys\\meena-upload.jks');
const props = path.resolve('keystore.properties');
const alias = 'upload';

for (const f of [store, props]) {
  if (fs.existsSync(f)) { console.error(`already exists, not touching it: ${f}`); process.exit(1); }
}
fs.mkdirSync(path.dirname(store), { recursive: true });

// 32 characters from an unambiguous alphabet; PKCS12 uses one password for store and key.
const abc = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789';
const password = Array.from(crypto.randomBytes(32), b => abc[b % abc.length]).join('');

const r = spawnSync(keytool, [
  '-genkeypair', '-v',
  '-keystore', store, '-storetype', 'PKCS12',
  '-alias', alias, '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000',
  '-dname', 'CN=HaNa Innovation, O=HaNa Innovation, C=MY',
  '-storepass:env', 'MEENA_KS_PASS', '-keypass:env', 'MEENA_KS_PASS',
], { env: { ...process.env, MEENA_KS_PASS: password }, encoding: 'utf8' });
if (r.status !== 0) { console.error(r.stderr || r.error); process.exit(1); }

fs.writeFileSync(props, [
  '# Meena upload key. Never commit this file. Keep a copy of the password in a password manager.',
  `storeFile=${store.replace(/\\/g, '/')}`,
  `storePassword=${password}`,
  `keyAlias=${alias}`,
  `keyPassword=${password}`,
  '',
].join('\n'));
console.log(`created ${store}`);
console.log(`wrote ${props} (password inside, not shown here)`);
