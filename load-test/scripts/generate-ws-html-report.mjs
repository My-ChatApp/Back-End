#!/usr/bin/env node
/**
 * Build html-report/index.html from summary.json + user-results.jsonl
 * (dashboard tương tự JMeter, self-contained — không cần asset JMeter).
 */
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';

function parseArgs(argv) {
  let dir = '';
  for (let i = 2; i < argv.length; i++) {
    if (argv[i] === '--dir' && argv[i + 1]) dir = resolve(argv[++i]);
    else if (argv[i] === '-h' || argv[i] === '--help') {
      console.log('Usage: node generate-ws-html-report.mjs --dir load-test/results/ws-...');
      process.exit(0);
    }
  }
  if (!dir) {
    console.error('Missing --dir');
    process.exit(1);
  }
  return { dir };
}

function percentile(sorted, p) {
  if (sorted.length === 0) return 0;
  const idx = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
}

function stats(values) {
  const ok = values.filter((v) => typeof v === 'number' && !Number.isNaN(v));
  if (ok.length === 0) {
    return { count: 0, min: 0, avg: 0, max: 0, p90: 0, p95: 0, p99: 0 };
  }
  const sorted = [...ok].sort((a, b) => a - b);
  const sum = sorted.reduce((a, b) => a + b, 0);
  return {
    count: sorted.length,
    min: sorted[0],
    avg: Math.round(sum / sorted.length),
    max: sorted[sorted.length - 1],
    p90: percentile(sorted, 90),
    p95: percentile(sorted, 95),
    p99: percentile(sorted, 99),
  };
}

function esc(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function barRow(label, st, maxScale) {
  const pct = maxScale > 0 ? Math.min(100, (st.avg / maxScale) * 100) : 0;
  return `
    <tr>
      <td>${esc(label)}</td>
      <td>${st.min}</td>
      <td><strong>${st.avg}</strong></td>
      <td>${st.p90}</td>
      <td>${st.p95}</td>
      <td>${st.p99}</td>
      <td>${st.max}</td>
      <td class="bar-cell"><div class="bar" style="width:${pct.toFixed(1)}%"></div></td>
    </tr>`;
}

function buildHtml(summary, rows, computed) {
  const c = summary.config ?? {};
  const generated = new Date().toISOString();
  const maxBar = Math.max(
    computed.signin.max,
    computed.connect.max,
    computed.sendWall.max,
    computed.sendPublish.max,
    computed.serverRtt.max,
    computed.get.max,
    1
  );

  const errorRows = rows
    .filter((r) => !r.ok)
    .slice(0, 50)
    .map(
      (r) =>
        `<tr><td>${esc(r.email)}</td><td>${esc(r.userId ?? '')}</td><td>${esc(r.error ?? '')}</td></tr>`
    )
    .join('');

  return `<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>MyChatApp WS Load Test — Dashboard</title>
  <style>
    :root {
      --bg: #0f1419;
      --card: #1a2332;
      --text: #e7ecf3;
      --muted: #8b9cb3;
      --accent: #3b82f6;
      --ok: #22c55e;
      --err: #ef4444;
      --border: #2d3a4f;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "Segoe UI", system-ui, sans-serif;
      background: var(--bg);
      color: var(--text);
      line-height: 1.5;
    }
    header {
      padding: 1.25rem 2rem;
      border-bottom: 1px solid var(--border);
      background: linear-gradient(135deg, #1e3a5f 0%, #0f1419 100%);
    }
    header h1 { margin: 0 0 0.25rem; font-size: 1.5rem; }
    header p { margin: 0; color: var(--muted); font-size: 0.9rem; }
    main { padding: 1.5rem 2rem 3rem; max-width: 1200px; margin: 0 auto; }
    .cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }
    .card {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 1rem 1.1rem;
    }
    .card .label { font-size: 0.75rem; color: var(--muted); text-transform: uppercase; letter-spacing: 0.04em; }
    .card .value { font-size: 1.75rem; font-weight: 700; margin-top: 0.25rem; }
    .card.ok .value { color: var(--ok); }
    .card.err .value { color: var(--err); }
    section {
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 1.25rem 1.5rem;
      margin-bottom: 1.5rem;
    }
    section h2 { margin: 0 0 1rem; font-size: 1.1rem; }
    table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
    th, td { padding: 0.55rem 0.75rem; text-align: left; border-bottom: 1px solid var(--border); }
    th { color: var(--muted); font-weight: 600; font-size: 0.75rem; text-transform: uppercase; }
    .bar-cell { width: 28%; }
    .bar {
      height: 8px;
      background: var(--accent);
      border-radius: 4px;
      min-width: 2px;
    }
    .config dt { color: var(--muted); font-size: 0.85rem; }
    .config dd { margin: 0 0 0.5rem 1rem; }
    .note { color: var(--muted); font-size: 0.85rem; margin-top: 1rem; }
    a { color: var(--accent); }
  </style>
</head>
<body>
  <header>
    <h1>MyChatApp — WebSocket Load Test Dashboard</h1>
    <p>Signin → STOMP × ${c.messagesPerUser ?? '?'} → GET messages · Generated ${esc(generated)}</p>
  </header>
  <main>
    <div class="cards">
      <div class="card"><div class="label">Users</div><div class="value">${computed.usersRun}</div></div>
      <div class="card ok"><div class="label">Success</div><div class="value">${computed.success}</div></div>
      <div class="card ${computed.failed > 0 ? 'err' : ''}"><div class="label">Failed</div><div class="value">${computed.failed}</div></div>
      <div class="card"><div class="label">Error %</div><div class="value">${computed.errorRatePct}%</div></div>
      <div class="card"><div class="label">Elapsed</div><div class="value">${(computed.elapsedMs / 1000).toFixed(1)}s</div></div>
      <div class="card"><div class="label">Avg messages</div><div class="value">${computed.avgMessageCount}</div></div>
      <div class="card"><div class="label">Server RTT avg</div><div class="value">${computed.serverRtt.avg}ms</div></div>
      <div class="card ${computed.serverRttMissedTotal > 0 ? 'err' : ''}"><div class="label">RTT missed</div><div class="value">${computed.serverRttMissedTotal}</div></div>
    </div>

    <section>
      <h2>Response times (ms)</h2>
      <table>
        <thead>
          <tr>
            <th>Step</th><th>Min</th><th>Avg</th><th>P90</th><th>P95</th><th>P99</th><th>Max</th><th></th>
          </tr>
        </thead>
        <tbody>
          ${barRow('POST signin', computed.signin, maxBar)}
          ${barRow('STOMP connect', computed.connect, maxBar)}
          ${barRow('Send wall (×' + (c.messagesPerUser ?? 5) + ')', computed.sendWall, maxBar)}
          ${barRow('Send gap (sleep only)', computed.sendGap, maxBar)}
          ${barRow('Send publish (wall − gap)', computed.sendPublish, maxBar)}
          ${barRow('Server RTT (MESSAGE_CREATED)', computed.serverRtt, maxBar)}
          ${barRow('GET messages', computed.get, maxBar)}
        </tbody>
      </table>
      <p class="note">
        <strong>Send wall</strong> = toàn bộ phase gửi (gồm chờ gap + chờ MESSAGE_CREATED từng tin). 
        <strong>Send publish</strong> ≈ thời gian client chủ động (không tính sleep giữa các tin). 
        <strong>Server RTT</strong> = publish → nhận event trên <code>/topic/conversation/{id}</code>.
        Throughput ≈ ${computed.throughputUsersPerSec} users/s.
      </p>
    </section>

    <section>
      <h2>Messages returned (GET)</h2>
      <table>
        <tr><th>Min</th><td>${computed.msgMin}</td></tr>
        <tr><th>Avg</th><td>${computed.avgMessageCount}</td></tr>
        <tr><th>Max</th><td>${computed.msgMax}</td></tr>
      </table>
    </section>

    <section>
      <h2>Configuration</h2>
      <dl class="config">
        <dt>Target</dt><dd>http://${esc(c.host ?? 'localhost')}:${c.port ?? 8080}</dd>
        <dt>Concurrency</dt><dd>${c.concurrency ?? '—'}</dd>
        <dt>Ramp-up</dt><dd>${c.rampUpSec ?? '—'} s</dd>
        <dt>Messages / user</dt><dd>${c.messagesPerUser ?? '—'}</dd>
        <dt>Settle before GET</dt><dd>${c.settleMs ?? '—'} ms</dd>
        <dt>Gap between sends</dt><dd>${c.sendGapMs ?? '—'} ms (0 = burst)</dd>
        <dt>Server wait after sends</dt><dd>${c.serverWaitMs ?? '—'} ms</dd>
      </dl>
    </section>

    ${
      computed.failed > 0
        ? `<section>
      <h2>Errors (first 50)</h2>
      <table>
        <thead><tr><th>Email</th><th>User ID</th><th>Error</th></tr></thead>
        <tbody>${errorRows}</tbody>
      </table>
    </section>`
        : ''
    }

    <p class="note">Raw data: <a href="../user-results.jsonl">user-results.jsonl</a> · <a href="../summary.json">summary.json</a></p>
  </main>
</body>
</html>`;
}

async function main() {
  const { dir } = parseArgs(process.argv);
  const summaryPath = join(dir, 'summary.json');
  const jsonlPath = join(dir, 'user-results.jsonl');

  const summary = JSON.parse(await readFile(summaryPath, 'utf8'));
  const lines = (await readFile(jsonlPath, 'utf8')).split(/\r?\n/).filter(Boolean);
  const rows = lines.map((l) => JSON.parse(l));
  const okRows = rows.filter((r) => r.ok);

  const computed = {
    usersRun: rows.length,
    success: okRows.length,
    failed: rows.length - okRows.length,
    errorRatePct:
      rows.length > 0
        ? Math.round(((rows.length - okRows.length) / rows.length) * 10000) / 100
        : 0,
    elapsedMs: summary.elapsedMs ?? 0,
    avgMessageCount: okRows.length
      ? Math.round(okRows.reduce((s, r) => s + (r.messageCount ?? 0), 0) / okRows.length)
      : 0,
    msgMin: okRows.length ? Math.min(...okRows.map((r) => r.messageCount ?? 0)) : 0,
    msgMax: okRows.length ? Math.max(...okRows.map((r) => r.messageCount ?? 0)) : 0,
    signin: stats(okRows.map((r) => r.signinMs)),
    connect: stats(okRows.map((r) => r.connectMs)),
    sendWall: stats(okRows.map((r) => r.sendWallMs ?? r.sendMs ?? 0)),
    sendGap: stats(okRows.map((r) => r.sendGapTotalMs ?? 0)),
    sendPublish: stats(
      okRows.map((r) =>
        r.sendPublishMs != null
          ? r.sendPublishMs
          : Math.max(0, (r.sendWallMs ?? r.sendMs ?? 0) - (r.sendGapTotalMs ?? 0))
      )
    ),
    serverRtt: stats(
      okRows.map((r) => r.avgServerRttMs).filter((v) => typeof v === 'number' && v > 0)
    ),
    serverRttMissedTotal: okRows.reduce((s, r) => s + (r.serverRttMissed ?? 0), 0),
    get: stats(okRows.map((r) => r.getMs)),
    throughputUsersPerSec:
      summary.elapsedMs > 0
        ? (rows.length / (summary.elapsedMs / 1000)).toFixed(2)
        : '0',
  };

  const htmlDir = join(dir, 'html-report');
  await mkdir(htmlDir, { recursive: true });
  const indexPath = join(htmlDir, 'index.html');
  await writeFile(indexPath, buildHtml(summary, rows, computed), 'utf8');
  console.log(`Wrote ${indexPath}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
