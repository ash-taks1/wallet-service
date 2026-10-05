// Section 7 scenario: perform one transfer and record its trace id, so that every log line of that
// request (API, DB commit, outbox relay, consumer) can be extracted with scripts/trace.sh.
// Run: scripts/trace-demo.sh (wraps this script and the log extraction)
import { fail } from 'k6';
import { createUser, deposit, REPORT_DIR, transfer, uuid } from './lib/wallet.js';

export const options = { vus: 1, iterations: 1 };

export function setup() {
  const sender = createUser('trace-sender');
  const receiver = createUser('trace-receiver');
  deposit(sender, 10000, uuid());
  const res = transfer(sender, receiver.walletId, 2500, uuid(), 'trace demo transfer');
  if (res.status !== 201) {
    fail(`transfer failed: ${res.status} ${res.body}`);
  }
  return {
    traceId: res.headers['X-Trace-Id'],
    transactionId: res.json('transactionId'),
    fromWalletId: sender.walletId,
    toWalletId: receiver.walletId,
    amount: 2500,
  };
}

export default function () {}

export function handleSummary(data) {
  const result = data.setup_data;
  return {
    [`${REPORT_DIR}/trace-demo.json`]: JSON.stringify(result, null, 2),
    stdout: `\nTransfer ${result.transactionId} completed with trace id ${result.traceId}\n`,
  };
}
