// Section 5 scenario: a wallet holding 100,000 receives 50 withdrawals of 3,000 in parallel.
// Expected: exactly 33 succeed, 17 fail with INSUFFICIENT_FUNDS, final balance 1,000,
// the balance is never observed negative, and the transaction log matches the balance.
//
// Run: scripts/k6.sh concurrent-withdrawals.js
import { check, sleep } from 'k6';
import { Counter, Gauge, Rate, Trend } from 'k6/metrics';
import { balance, createUser, deposit, history, REPORT_DIR, sum, uuid, withdraw } from './lib/wallet.js';
import { buildReport, reportFiles } from './lib/report.js';

const INITIAL_BALANCE = 100000;
const AMOUNT = 3000;
const REQUESTS = 50;
const EXPECTED_SUCCESS = Math.floor(INITIAL_BALANCE / AMOUNT);
const EXPECTED_REJECTED = REQUESTS - EXPECTED_SUCCESS;
const EXPECTED_FINAL = INITIAL_BALANCE - EXPECTED_SUCCESS * AMOUNT;

const succeeded = new Counter('withdrawals_succeeded');
const rejected = new Counter('withdrawals_rejected_insufficient_funds');
const unexpected = new Counter('withdrawals_unexpected');
const negativeObservations = new Counter('balance_negative_observations');
const observedBalance = new Trend('observed_balance');
const finalBalance = new Gauge('final_balance');
const loggedWithdrawn = new Gauge('history_completed_withdrawals_total');
const loggedCompleted = new Gauge('history_completed_withdrawals');
const loggedFailed = new Gauge('history_failed_withdrawals');
const invariants = new Rate('invariants_hold');

export const options = {
  scenarios: {
    // 50 VUs, one request each, all released at the same moment.
    withdrawals: { executor: 'per-vu-iterations', vus: REQUESTS, iterations: 1, exec: 'withdrawOnce', maxDuration: '1m' },
    // Samples the balance while the withdrawals are in flight.
    balance_observer: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: 'observeBalance', maxDuration: '1m' },
  },
  thresholds: {
    withdrawals_succeeded: [`count==${EXPECTED_SUCCESS}`],
    withdrawals_rejected_insufficient_funds: [`count==${EXPECTED_REJECTED}`],
    withdrawals_unexpected: ['count==0'],
    balance_negative_observations: ['count==0'],
    final_balance: [`value==${EXPECTED_FINAL}`],
    history_completed_withdrawals_total: [`value==${EXPECTED_SUCCESS * AMOUNT}`],
    invariants_hold: ['rate==1'],
  },
};

export function setup() {
  const user = createUser('concurrency');
  const funded = deposit(user, INITIAL_BALANCE, uuid());
  if (funded.status !== 201 || balance(user) !== INITIAL_BALANCE) {
    throw new Error(`could not fund wallet: ${funded.status} ${funded.body}`);
  }
  [succeeded, rejected, unexpected, negativeObservations].forEach((counter) => counter.add(0));
  return { user };
}

export function withdrawOnce(data) {
  const res = withdraw(data.user, AMOUNT, uuid());
  if (res.status === 201) {
    succeeded.add(1);
  } else if (res.status === 422 && res.json('code') === 'INSUFFICIENT_FUNDS') {
    rejected.add(1);
  } else {
    unexpected.add(1);
    console.error(`unexpected response ${res.status}: ${res.body}`);
  }
}

export function observeBalance(data) {
  for (let i = 0; i < 100; i++) {
    const current = balance(data.user);
    observedBalance.add(current);
    if (current < 0) {
      negativeObservations.add(1);
    }
    sleep(0.01);
  }
}

export function teardown(data) {
  const final = balance(data.user);
  finalBalance.add(final);

  const withdrawals = history(data.user).filter((tx) => tx.type === 'WITHDRAWAL');
  const completed = withdrawals.filter((tx) => tx.status === 'COMPLETED');
  const failed = withdrawals.filter((tx) => tx.status === 'FAILED');
  const withdrawn = sum(completed.map((tx) => tx.amount));
  loggedWithdrawn.add(withdrawn);
  loggedCompleted.add(completed.length);
  loggedFailed.add(failed.length);

  const results = check(null, {
    'transaction log: 33 completed withdrawals': () => completed.length === EXPECTED_SUCCESS,
    'transaction log: 17 failed withdrawals (INSUFFICIENT_FUNDS)':
      () => failed.length === EXPECTED_REJECTED && failed.every((tx) => tx.failureReason === 'INSUFFICIENT_FUNDS'),
    'initial - logged withdrawals == final balance': () => INITIAL_BALANCE - withdrawn === final,
    'every ledger balance_after >= 0': () => completed.every((tx) => tx.balanceAfter >= 0),
    'ledger balances strictly decreasing by 3,000': () => {
      const sorted = completed.map((tx) => tx.balanceAfter).sort((a, b) => b - a);
      return sorted.every((b, i) => b === INITIAL_BALANCE - (i + 1) * AMOUNT);
    },
  });
  invariants.add(results);
}

export function handleSummary(data) {
  const report = buildReport(data, 'Concurrent withdrawals scenario',
    [
      `Wallet funded with ${INITIAL_BALANCE}; ${REQUESTS} withdrawals of ${AMOUNT} sent in parallel (one per VU, same start instant).`,
      'A separate VU sampled the balance while the requests were in flight.',
    ],
    [
      { label: 'Successful withdrawals (HTTP 201)', metric: 'withdrawals_succeeded', expected: EXPECTED_SUCCESS },
      { label: 'Rejected with INSUFFICIENT_FUNDS (HTTP 422)', metric: 'withdrawals_rejected_insufficient_funds', expected: EXPECTED_REJECTED },
      { label: 'Unexpected responses', metric: 'withdrawals_unexpected', expected: 0 },
      { label: 'Final balance', metric: 'final_balance', expected: EXPECTED_FINAL },
      { label: 'Negative balance observations', metric: 'balance_negative_observations', expected: 0 },
      { label: 'Sum of COMPLETED withdrawals in history', metric: 'history_completed_withdrawals_total', expected: EXPECTED_SUCCESS * AMOUNT },
      { label: 'COMPLETED withdrawals in history', metric: 'history_completed_withdrawals', expected: EXPECTED_SUCCESS },
      { label: 'FAILED withdrawals in history', metric: 'history_failed_withdrawals', expected: EXPECTED_REJECTED },
      { label: 'Ledger/state invariants', metric: 'invariants_hold', expected: '1 (100%)' },
    ],
    [
      `Balance samples taken during the run: min ${data.metrics.observed_balance ? data.metrics.observed_balance.values.min : 'n/a'}, max ${data.metrics.observed_balance ? data.metrics.observed_balance.values.max : 'n/a'}.`,
    ]);
  return reportFiles(REPORT_DIR, 'concurrent-withdrawals', report, data);
}
