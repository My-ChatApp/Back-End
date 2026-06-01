#!/usr/bin/env node
/**
 * Load test: each user signs in, sends N messages via STOMP/SockJS, then GET messages.
 * Tuned for up to 500 concurrent users (ramp-up + concurrency cap).
 *
 * Usage (from Back-End):
 *   npm install --prefix load-test
 *   node load-test/scripts/ws-send-then-get.mjs --users 500 --concurrency 500 --ramp-up-sec 180 --messages 5
 */
import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import WebSocket from 'websocket';
import { parseUsersCsv } from './lib/parseUsersCsv.mjs';
import { connectStomp, disconnectStomp, sendMessagesMeasured } from './lib/stompChat.mjs';

globalThis.WebSocket = WebSocket.w3cwebsocket;

const __dirname = dirname(fileURLToPath(import.meta.url));
const DEFAULT_CSV = resolve(__dirname, '../data/users.csv');

function parseArgs(argv) {
  const opts = {
    host: 'localhost',
    port: 8080,
    csv: DEFAULT_CSV,
    users: 500,
    concurrency: 500,
    rampUpSec: 180,
    messages: 5,
    settleMs: 2000,
    sendGapMs: 0,
    serverWaitMs: 8000,
    outDir: '',
    debug: false,
  };

  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    const next = () => argv[++i];
    if (a === '--host') opts.host = next();
    else if (a === '--port') opts.port = Number.parseInt(next(), 10);
    else if (a === '--csv') opts.csv = resolve(next());
    else if (a === '--users') opts.users = Number.parseInt(next(), 10);
    else if (a === '--concurrency') opts.concurrency = Number.parseInt(next(), 10);
    else if (a === '--ramp-up-sec') opts.rampUpSec = Number.parseInt(next(), 10);
    else if (a === '--messages') opts.messages = Number.parseInt(next(), 10);
    else if (a === '--settle-ms') opts.settleMs = Number.parseInt(next(), 10);
    else if (a === '--send-gap-ms') opts.sendGapMs = Number.parseInt(next(), 10);
    else if (a === '--server-wait-ms') opts.serverWaitMs = Number.parseInt(next(), 10);
    else if (a === '--out-dir') opts.outDir = resolve(next());
    else if (a === '--debug') opts.debug = true;
    else if (a === '-h' || a === '--help') {
      console.log(`Usage: node ws-send-then-get.mjs [options]

Options:
  --host localhost          Gateway host
  --port 8080               Gateway port
  --csv path                users.csv (default load-test/data/users.csv)
  --users 500               Max users from CSV
  --concurrency 500         Max users active at once (500 = "cùng lúc" cap)
  --ramp-up-sec 180         Spread user start over N seconds (like JMeter ramp)
  --messages 5              STOMP sends per user before GET
  --settle-ms 2000          Wait after sends before GET messages
  --send-gap-ms 0           Delay between each send (0 = burst; 150 = think time)
  --server-wait-ms 8000     Max wait for MESSAGE_CREATED after all sends
  --out-dir path            Write summary.json + user-results.jsonl
  --debug                   STOMP debug logs
`);
      process.exit(0);
    }
  }

  if (opts.users < 1 || opts.concurrency < 1 || opts.messages < 1) {
    console.error('users, concurrency, messages must be >= 1');
    process.exit(1);
  }
  return opts;
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function signIn(gatewayBase, email, password) {
  const t0 = performance.now();
  const res = await fetch(`${gatewayBase}/api/auth/signin`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const text = await res.text();
  const ms = Math.round(performance.now() - t0);
  if (!res.ok) {
    throw new Error(`signin ${res.status}: ${text.slice(0, 200)}`);
  }
  let json;
  try {
    json = JSON.parse(text);
  } catch {
    throw new Error('signin invalid JSON');
  }
  if (!json.success || !json.data?.accessToken) {
    throw new Error(`signin failed: ${json.message ?? 'no token'}`);
  }
  return { token: json.data.accessToken, ms };
}

async function getMessages(gatewayBase, token, conversationId, userId) {
  const url = new URL(
    `${gatewayBase}/api/conversations/${conversationId}/messages`
  );
  url.searchParams.set('userId', userId);
  url.searchParams.set('limit', '50');

  const t0 = performance.now();
  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
  });
  const text = await res.text();
  const ms = Math.round(performance.now() - t0);
  if (!res.ok) {
    throw new Error(`getMessages ${res.status}: ${text.slice(0, 200)}`);
  }
  let json;
  try {
    json = JSON.parse(text);
  } catch {
    throw new Error('getMessages invalid JSON');
  }
  const messages = json.data?.messages ?? json.data ?? [];
  const count = Array.isArray(messages) ? messages.length : 0;
  return { count, ms, success: json.success === true };
}

/**
 * One virtual user: signin → WS × N → GET messages.
 */
async function runUserScenario(user, opts, gatewayBase, userIndex, totalUsers) {
  const rampDelayMs =
    totalUsers <= 1 ? 0 : Math.floor((userIndex / (totalUsers - 1)) * opts.rampUpSec * 1000);
  if (rampDelayMs > 0) {
    await sleep(rampDelayMs);
  }

  const result = {
    email: user.email,
    userId: user.userId,
    conversationId: user.conversationId,
    ok: false,
    signinMs: 0,
    connectMs: 0,
    sendWallMs: 0,
    sendGapTotalMs: 0,
    sendPublishMs: 0,
    sendMs: 0,
    avgServerRttMs: 0,
    serverRttP95: 0,
    serverRttReceived: 0,
    serverRttMissed: 0,
    getMs: 0,
    messageCount: 0,
    sends: opts.messages,
    error: null,
    startedAt: new Date().toISOString(),
  };

  let client;
  try {
    const { token, ms: signinMs } = await signIn(gatewayBase, user.email, user.password);
    result.signinMs = signinMs;

    const tConnect = performance.now();
    client = await connectStomp({
      gatewayBase,
      token,
      conversationId: user.conversationId,
      debug: opts.debug,
    });
    result.connectMs = Math.round(performance.now() - tConnect);

    const sendMetrics = await sendMessagesMeasured(client, {
      conversationId: user.conversationId,
      senderId: user.userId,
      messageCount: opts.messages,
      sendGapMs: opts.sendGapMs,
      serverWaitMs: opts.serverWaitMs,
    });
    result.sendWallMs = sendMetrics.sendWallMs;
    result.sendGapTotalMs = sendMetrics.sendGapTotalMs;
    result.sendPublishMs = sendMetrics.sendPublishMs;
    result.sendMs = sendMetrics.sendWallMs;
    result.avgServerRttMs = sendMetrics.serverRttAvg;
    result.serverRttP95 = sendMetrics.serverRttP95;
    result.serverRttReceived = sendMetrics.serverRttReceived;
    result.serverRttMissed = sendMetrics.serverRttMissed;

    await disconnectStomp(client);
    client = null;

    if (opts.settleMs > 0) {
      await sleep(opts.settleMs);
    }

    const { count, ms: getMs } = await getMessages(
      gatewayBase,
      token,
      user.conversationId,
      user.userId
    );
    result.getMs = getMs;
    result.messageCount = count;
    result.ok = true;
  } catch (err) {
    result.error = err instanceof Error ? err.message : String(err);
    if (client) await disconnectStomp(client);
  }

  return result;
}

/**
 * Semaphore-limited pool with ramp-up inside each task.
 */
async function runPool(users, opts, gatewayBase) {
  const total = users.length;
  let nextIndex = 0;
  let active = 0;
  const results = [];
  let resolveAll;
  const allDone = new Promise((r) => {
    resolveAll = r;
  });

  function maybeStartMore() {
    while (active < opts.concurrency && nextIndex < total) {
      const userIndex = nextIndex;
      const user = users[nextIndex++];
      active++;
      runUserScenario(user, opts, gatewayBase, userIndex, total)
        .then((r) => results.push(r))
        .catch((e) =>
          results.push({
            email: user.email,
            ok: false,
            error: e instanceof Error ? e.message : String(e),
          })
        )
        .finally(() => {
          active--;
          if (nextIndex >= total && active === 0) {
            resolveAll();
          } else {
            maybeStartMore();
          }
        });
    }
  }

  maybeStartMore();
  await allDone;
  return results;
}

function summarize(results, opts, elapsedMs) {
  const ok = results.filter((r) => r.ok);
  const fail = results.filter((r) => !r.ok);
  const avg = (arr, key) => {
    const vals = arr.map((r) => r[key]).filter((n) => typeof n === 'number');
    return vals.length ? Math.round(vals.reduce((a, b) => a + b, 0) / vals.length) : 0;
  };

  return {
    config: {
      host: opts.host,
      port: opts.port,
      usersRequested: opts.users,
      usersRun: results.length,
      concurrency: opts.concurrency,
      rampUpSec: opts.rampUpSec,
      messagesPerUser: opts.messages,
      settleMs: opts.settleMs,
      sendGapMs: opts.sendGapMs,
      serverWaitMs: opts.serverWaitMs,
    },
    elapsedMs,
    success: ok.length,
    failed: fail.length,
    errorRatePct: results.length ? Math.round((fail.length / results.length) * 10000) / 100 : 0,
    avgSigninMs: avg(ok, 'signinMs'),
    avgConnectMs: avg(ok, 'connectMs'),
    avgSendWallMs: avg(ok, 'sendWallMs'),
    avgSendGapTotalMs: avg(ok, 'sendGapTotalMs'),
    avgSendPublishMs: avg(ok, 'sendPublishMs'),
    avgServerRttMs: avg(ok, 'avgServerRttMs'),
    avgServerRttP95: avg(ok, 'serverRttP95'),
    totalServerRttMissed: ok.reduce((s, r) => s + (r.serverRttMissed ?? 0), 0),
    avgSendMs: avg(ok, 'sendWallMs'),
    avgGetMs: avg(ok, 'getMs'),
    avgMessageCount: ok.length
      ? Math.round(ok.reduce((s, r) => s + (r.messageCount ?? 0), 0) / ok.length)
      : 0,
    minMessageCount: ok.length ? Math.min(...ok.map((r) => r.messageCount ?? 0)) : 0,
    maxMessageCount: ok.length ? Math.max(...ok.map((r) => r.messageCount ?? 0)) : 0,
    sampleErrors: fail.slice(0, 10).map((r) => ({ email: r.email, error: r.error })),
  };
}

async function main() {
  const opts = parseArgs(process.argv);
  const gatewayBase = `http://${opts.host}:${opts.port}`;

  const allUsers = await parseUsersCsv(opts.csv, opts.users);
  if (allUsers.length < opts.users) {
    console.warn(
      `Warning: CSV has only ${allUsers.length} users, requested ${opts.users}`
    );
  }
  const users = allUsers.slice(0, opts.users);

  const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  const outDir =
    opts.outDir || resolve(__dirname, `../results/ws-${timestamp.replace(/T/g, '-')}`);
  await mkdir(outDir, { recursive: true });

  console.log('MyChatApp WS + GET load test');
  console.log(`  Target:       ${gatewayBase}`);
  console.log(`  Users:        ${users.length}`);
  console.log(`  Concurrency:  ${opts.concurrency} (max active at once)`);
  console.log(`  Ramp-up:      ${opts.rampUpSec}s (stagger user start)`);
  console.log(`  Messages/user:${opts.messages} via STOMP (+ server RTT), then GET`);
  console.log(`  Send gap:     ${opts.sendGapMs}ms between publishes (0 = burst)`);
  console.log(`  CSV:          ${opts.csv}`);
  console.log(`  Output:       ${outDir}`);
  console.log('');

  const t0 = performance.now();
  const results = await runPool(users, opts, gatewayBase);
  const elapsedMs = Math.round(performance.now() - t0);

  const summary = summarize(results, opts, elapsedMs);
  await writeFile(join(outDir, 'summary.json'), JSON.stringify(summary, null, 2));
  const jsonl = results.map((r) => JSON.stringify(r)).join('\n') + '\n';
  await writeFile(join(outDir, 'user-results.jsonl'), jsonl);

  const reportScript = join(__dirname, 'generate-ws-html-report.mjs');
  const { spawn } = await import('node:child_process');
  await new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [reportScript, '--dir', outDir], {
      stdio: 'inherit',
      cwd: dirname(reportScript),
    });
    child.on('exit', (code) => (code === 0 ? resolve() : reject(new Error(`HTML report exit ${code}`))));
    child.on('error', reject);
  });

  console.log('Summary');
  console.log(`  Elapsed:      ${(elapsedMs / 1000).toFixed(1)}s`);
  console.log(`  Success:      ${summary.success}/${results.length}`);
  console.log(`  Failed:       ${summary.failed}`);
  console.log(`  Avg connect:  ${summary.avgConnectMs}ms`);
  console.log(`  Avg send wall: ${summary.avgSendWallMs}ms (gap ${summary.avgSendGapTotalMs}ms, publish ${summary.avgSendPublishMs}ms)`);
  console.log(`  Avg server RTT: ${summary.avgServerRttMs}ms (P95 ${summary.avgServerRttP95}ms), missed ${summary.totalServerRttMissed}`);
  console.log(`  Avg GET:      ${summary.avgGetMs}ms`);
  console.log(`  Avg messages: ${summary.avgMessageCount} (min ${summary.minMessageCount}, max ${summary.maxMessageCount})`);
  if (summary.failed > 0) {
    console.log('  Sample errors:');
    for (const e of summary.sampleErrors) {
      console.log(`    - ${e.email}: ${e.error}`);
    }
  }
  console.log('');
  console.log(`Wrote ${join(outDir, 'summary.json')}`);
  console.log(`HTML report: ${join(outDir, 'html-report', 'index.html')}`);

  process.exit(summary.failed > 0 ? 1 : 0);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
