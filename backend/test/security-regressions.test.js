const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

function read(rel) {
  return fs.readFileSync(path.join(__dirname, '..', rel), 'utf8');
}

test('production auth has no fixed OTP and stores hashed OTP state', () => {
  const auth = read('src/services/auth.service.js');
  assert.equal(auth.includes("const otp = '123456'"), false);
  assert.match(auth, /randomInt\(100000, 1000000\)/);
  assert.match(auth, /otpCodeHash/);
  assert.match(auth, /otpExpiresAt/);
  assert.match(auth, /OTP_MAX_ATTEMPTS/);
});

test('socket sender identity comes from authenticated JWT', () => {
  const index = read('src/index.js');
  assert.match(index, /socket\.user\.sub/);
  assert.equal(index.includes('saveMessage(data.senderId'), false);
  assert.equal(index.includes('socket.join(userId)'), false);
});

test('payment activation requires signature and gateway verification', () => {
  const payment = read('src/services/payment.service.js');
  assert.equal(payment.includes('Mock verification logic'), false);
  assert.match(payment, /createHmac\('sha256'/);
  assert.match(payment, /client\.payments\.fetch/);
  assert.match(payment, /prisma\.\$transaction/);
});

test('discovery does not fabricate match percentages or human placeholder photos', () => {
  const matching = read('src/services/matching.service.js');
  const search = read('src/services/search.service.js');
  assert.equal(matching.includes('deterministicScore'), false);
  assert.equal(matching.includes('via.placeholder.com'), false);
  assert.equal(search.includes('via.placeholder.com'), false);
  assert.equal(search.includes('match: 90'), false);
});

test('premium does not bypass request-access photo privacy', () => {
  const privacy = read('src/services/privacy.service.js');
  assert.equal(privacy.includes('isMutualMatch || viewerIsPremium'), false);
  assert.match(privacy, /hasExplicitGrant/);
});

test('shortlist uses canonical JWT subject', () => {
  const shortlist = read('src/controllers/shortlist.controller.js');
  assert.equal(shortlist.includes('req.user.userId'), false);
  assert.match(shortlist, /req\.user\.sub/);
});
