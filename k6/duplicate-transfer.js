// Section 5, second scenario: one transfer request with identical content (same Idempotency-Key)
// is sent 5 times at exactly the same moment. Expected: the money moves exactly once on both
// sides; the other 4 requests get the original answer without re-applying it.
//
// Run: scripts/k6.sh duplicate-transfer.js
import { check } from 'k6';
import { Counter, Gauge, Rate } from 'k6/metrics';
import { balance, createUser, deposit, history, REPORT_DIR, transfer, uuid } from './lib/wallet.js';
import { buildReport, reportFiles } from './lib/report.js';

const SENDER_BALANCE = 50000;
const AMOUNT = 12345;
const DUPLICATES = 5;

const applied = new Counter('transfer_applied');
const replayed = new Counter('transfer_replayed');
const unexpected = new Counter('transfer_unexpected');
const senderFinal = new Gauge('sender_final_balance');
const receiverFinal = new Gauge('receiver_final_balance');
const senderTransfers = new Gauge('sender_transfers_in_history');
const receiverTransfers = new Gauge('receiver_transfers_in_history');
const invariants = new Rate('invariants_hold');

export const options = {
  scenarios: {
    duplicates: { executor: 'per-vu-iterations', vus: DUPLICATES, iterations: 1, maxDuration: '1m' },
  },
  thresholds: {
    transfer_applied: ['count==1'],
    transfer_replayed: [`count==${DUPLICATES - 1}`],
    transfer_unexpected: ['count==0'],
    sender_final_balance: [`value==${SENDER_BALANCE - AMOUNT}`],
    receiver_final_balance: [`value==${AMOUNT}`],
    sender_transfers_in_history: ['value==1'],
    receiver_transfers_in_history: ['value==1'],
    invariants_hold: ['rate==1'],
  },
};

export function setup() {
  const sender = createUser('dup-sender');
  const receiver = createUser('dup-receiver');
  deposit(sender, SENDER_BALANCE, uuid());
  [applied, replayed, unexpected].forEach((counter) => counter.add(0));
  return { sender, receiver, key: uuid() };
}

export default function (data) {
  const res = transfer(data.sender, data.receiver.walletId, AMOUNT, data.key, 'k6 duplicate transfer');
  if (res.status === 201 && res.headers['Idempotent-Replayed'] === 'true') {
    replayed.add(1);
  } else if (res.status === 201) {
    applied.add(1);
  } else {
    unexpected.add(1);
    console.error(`unexpected response ${res.status}: ${res.body}`);
    return;
  }
  console.log(`VU ${__VU}: HTTP ${res.status} transactionId=${res.json('transactionId')} replayed=${res.headers['Idempotent-Replayed'] === 'true'}`);
}

export function teardown(data) {
  senderFinal.add(balance(data.sender));
  receiverFinal.add(balance(data.receiver));
  const sent = history(data.sender).filter((tx) => tx.type === 'TRANSFER');
  const received = history(data.receiver).filter((tx) => tx.type === 'TRANSFER');
  senderTransfers.add(sent.length);
  receiverTransfers.add(received.length);
  invariants.add(check(null, {
    'sender and receiver see the same single transaction':
      () => sent.length === 1 && received.length === 1 && sent[0].transactionId === received[0].transactionId,
    'sender entry is a DEBIT of the amount': () => sent[0].direction === 'DEBIT' && sent[0].amount === AMOUNT,
    'receiver entry is a CREDIT of the amount': () => received[0].direction === 'CREDIT' && received[0].amount === AMOUNT,
  }));
}

export function handleSummary(data) {
  const report = buildReport(data, 'Duplicate transfer scenario',
    [`The same transfer of ${AMOUNT} (one Idempotency-Key, identical body) was sent ${DUPLICATES} times simultaneously.`],
    [
      { label: 'Requests that applied the transfer', metric: 'transfer_applied', expected: 1 },
      { label: 'Requests answered from the stored result (Idempotent-Replayed: true)', metric: 'transfer_replayed', expected: DUPLICATES - 1 },
      { label: 'Unexpected responses', metric: 'transfer_unexpected', expected: 0 },
      { label: 'Sender final balance', metric: 'sender_final_balance', expected: SENDER_BALANCE - AMOUNT },
      { label: 'Receiver final balance', metric: 'receiver_final_balance', expected: AMOUNT },
      { label: 'Transfers in sender history', metric: 'sender_transfers_in_history', expected: 1 },
      { label: 'Transfers in receiver history', metric: 'receiver_transfers_in_history', expected: 1 },
      { label: 'Ledger invariants', metric: 'invariants_hold', expected: '1 (100%)' },
    ]);
  return reportFiles(REPORT_DIR, 'duplicate-transfer', report, data);
}
