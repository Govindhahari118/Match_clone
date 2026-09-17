const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..', '..');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    console.error(`FAIL: ${message}`);
    process.exitCode = 1;
  } else {
    console.log(`PASS: ${message}`);
  }
}

const authService = read('backend/src/services/auth.service.js');
const paymentService = read('backend/src/services/payment.service.js');
const server = read('backend/src/index.js');
const messageService = read('backend/src/services/message.service.js');
const matchingService = read('backend/src/services/matching.service.js');
const supportService = read('backend/src/services/support.service.js');
const photoController = read('backend/src/controllers/photo.controller.js');
const otpPage = read('frontend/src/app/(auth)/otp/page.js');
const pricingPage = read('frontend/src/app/(main)/pricing/page.js');
const interestsPage = read('frontend/src/app/(main)/interests/page.js');
const chatPage = read('frontend/src/app/(main)/chat/page.js');

assert(!authService.includes("'123456'") && !authService.includes('"123456"'), 'OTP service does not use a fixed 123456 code');
assert(authService.includes('randomInt') || authService.includes('randomBytes'), 'OTP generation uses cryptographic randomness');
assert(!otpPage.includes('Demo OTP') && !otpPage.includes('<strong>123456</strong>'), 'OTP UI does not advertise a universal test code');

assert(!paymentService.includes('pay_mock_') && !paymentService.includes('ord_mock'), 'Payment service contains no mock payment identifiers');
assert(!pricingPage.includes('pay_mock_') && !pricingPage.includes('Demo Mode'), 'Pricing UI cannot simulate successful payment');
assert(paymentService.includes('idempotencyKey'), 'Payment creation is idempotency-aware');
assert(paymentService.includes('timingSafeEqual') || paymentService.includes('createHmac'), 'Payment verification checks a cryptographic signature');

assert(server.includes('jwt.verify'), 'Realtime connection authenticates JWTs');
assert(server.includes('const authenticatedUserId = socket.user.sub'), 'Realtime sender identity comes from authenticated JWT');
assert(!server.includes('const { senderId, receiverId'), 'Realtime send path does not trust a client-supplied senderId');
assert(server.includes("socket.on('message_delivered'"), 'Realtime delivery requires recipient acknowledgement');
assert(messageService.includes('clientMessageId'), 'Messages support client idempotency identifiers');
assert(messageService.includes('Sender cannot acknowledge own delivery'), 'Delivery receipts are validated against the recipient');

assert(matchingService.includes('hardFields'), 'Matching enforces user-declared hard preference fields');
assert(matchingService.includes('cooldownUntil') || matchingService.includes('exposure'), 'Matching accounts for prior exposure/repetition');
assert(matchingService.includes('searchStatus'), 'Matching suppresses unavailable profile lifecycle states');

assert(supportService.includes('closeTicket'), 'Support has an explicit ticket closure path');
assert(supportService.includes('resolved'), 'Support distinguishes resolved from closed state');

assert(!photoController.includes('randomuser.me'), 'Photo upload path never substitutes a synthetic person');
assert(!interestsPage.includes('MOCK_RECEIVED') && !interestsPage.includes('MOCK_SENT'), 'Authenticated interest center has no fabricated engagement inventory');
assert(chatPage.includes('Guest preview only'), 'Guest chat preview is explicitly labelled');
assert(chatPage.includes('profile-placeholder.svg'), 'Chat uses a neutral placeholder instead of a synthetic person photo');

if (process.exitCode) {
  console.error('Trust regression checks failed.');
  process.exit(process.exitCode);
}
console.log('All trust regression checks passed.');
