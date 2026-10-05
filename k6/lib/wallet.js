// Thin client for the wallet-service API, shared by all k6 scenarios.
import http from 'k6/http';
import { fail } from 'k6';

export const BASE_URL = __ENV.WALLET_URL || 'http://localhost:8090';
export const REPORT_DIR = __ENV.REPORT_DIR || 'reports';

const PASSWORD = 'k6-scenario-password';

export function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

export function createUser(label) {
  const email = `${label}-${Date.now()}-${Math.floor(Math.random() * 1e9)}@k6.example.com`;
  const registered = http.post(`${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ fullName: `k6 ${label}`, email, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'register' } });
  if (registered.status !== 201) {
    fail(`registration failed: ${registered.status} ${registered.body}`);
  }
  const login = http.post(`${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } });
  if (login.status !== 200) {
    fail(`login failed: ${login.status} ${login.body}`);
  }
  return {
    email,
    userId: registered.json('userId'),
    walletId: registered.json('walletId'),
    token: login.json('accessToken'),
  };
}

function headers(user, idempotencyKey) {
  const h = { 'Content-Type': 'application/json', Authorization: `Bearer ${user.token}` };
  if (idempotencyKey) {
    h['Idempotency-Key'] = idempotencyKey;
  }
  return h;
}

export function deposit(user, amount, idempotencyKey) {
  return http.post(`${BASE_URL}/api/v1/wallets/${user.walletId}/deposits`, JSON.stringify({ amount }),
    { headers: headers(user, idempotencyKey), tags: { name: 'deposit' } });
}

export function withdraw(user, amount, idempotencyKey) {
  return http.post(`${BASE_URL}/api/v1/wallets/${user.walletId}/withdrawals`, JSON.stringify({ amount }),
    { headers: headers(user, idempotencyKey), tags: { name: 'withdraw' } });
}

export function transfer(user, toWalletId, amount, idempotencyKey, description) {
  return http.post(`${BASE_URL}/api/v1/wallets/${user.walletId}/transfers`,
    JSON.stringify({ toWalletId, amount, description }),
    { headers: headers(user, idempotencyKey), tags: { name: 'transfer' } });
}

export function balance(user) {
  const res = http.get(`${BASE_URL}/api/v1/wallets/${user.walletId}`,
    { headers: headers(user), tags: { name: 'balance' } });
  if (res.status !== 200) {
    fail(`balance failed: ${res.status} ${res.body}`);
  }
  return res.json('balance');
}

/** Full transaction history (follows the keyset cursor). */
export function history(user) {
  const items = [];
  let cursor = null;
  do {
    const query = cursor ? `?limit=200&cursor=${cursor}` : '?limit=200';
    const res = http.get(`${BASE_URL}/api/v1/wallets/${user.walletId}/transactions${query}`,
      { headers: headers(user), tags: { name: 'history' } });
    if (res.status !== 200) {
      fail(`history failed: ${res.status} ${res.body}`);
    }
    const page = res.json();
    items.push(...page.items);
    cursor = page.nextCursor;
  } while (cursor);
  return items;
}

export function sum(values) {
  return values.reduce((total, value) => total + value, 0);
}
