import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

/**
 * Connect STOMP over SockJS (same as Front-End / test-stomp-send.mjs).
 */
export function connectStomp({ gatewayBase, token, debug = false }) {
  const wsUrl = `${gatewayBase.replace(/\/$/, '')}/ws?access_token=${encodeURIComponent(token)}`;

  const client = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 0,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: debug ? (msg) => console.log('[STOMP]', msg) : () => {},
  });

  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      client.deactivate();
      reject(new Error('STOMP connect timeout'));
    }, 30000);

    client.onStompError = (frame) => {
      clearTimeout(timeout);
      client.deactivate();
      reject(new Error(frame.headers?.message || frame.body || 'STOMP error'));
    };

    client.onWebSocketError = (err) => {
      clearTimeout(timeout);
      reject(err instanceof Error ? err : new Error(String(err)));
    };

    client.onConnect = () => {
      clearTimeout(timeout);
      resolve(client);
    };

    client.activate();
  });
}

/**
 * @param {import('@stomp/stompjs').Client} client
 */
export function publishChatSend(client, body) {
  client.publish({
    destination: '/app/chat.send',
    body: JSON.stringify({
      conversationId: body.conversationId,
      senderId: body.senderId,
      content: body.content,
      type: 'TEXT',
    }),
  });
}

/**
 * Send N messages; measure client wall/gap/publish time and server RTT via MESSAGE_CREATED on topic.
 *
 * @returns {{
 *   sendWallMs: number,
 *   sendGapTotalMs: number,
 *   sendPublishMs: number,
 *   serverRtts: number[],
 *   serverRttAvg: number,
 *   serverRttP95: number,
 *   serverRttReceived: number,
 *   serverRttMissed: number,
 * }}
 */
export async function sendMessagesMeasured(
  client,
  {
    conversationId,
    senderId,
    messageCount,
    sendGapMs = 0,
    serverWaitMs = 8000,
    perMessageTimeoutMs = 10000,
    subscribeSettleMs = 100,
  }
) {
  const topic = `/topic/conversation/${conversationId}`;
  /** @type {Map<string, number>} content -> sentAt */
  const pending = new Map();
  const serverRtts = [];

  const subscription = client.subscribe(topic, (frame) => {
    try {
      const body = JSON.parse(frame.body);
      // Server sends bare ChatMessage for MESSAGE_CREATED; envelope for TYPING etc.
      let msg = null;
      if (body?.eventType === 'MESSAGE_CREATED' && body.message) {
        msg = body.message;
      } else if (body?.messageId && body.senderId) {
        msg = body;
      }
      if (!msg || msg.senderId !== senderId) return;
      const content = msg.content;
      const sentAt = pending.get(content);
      if (sentAt == null) return;
      serverRtts.push(Math.round(performance.now() - sentAt));
      pending.delete(content);
    } catch {
      /* ignore malformed */
    }
  });

  if (subscribeSettleMs > 0) {
    await sleep(subscribeSettleMs);
  }

  let sendGapTotalMs = 0;
  const wallStart = performance.now();

  for (let i = 0; i < messageCount; i++) {
    const content = `loadtest-${senderId.slice(0, 8)}-${i}-${Date.now()}`;
    const sentAt = performance.now();
    pending.set(content, sentAt);

    publishChatSend(client, { conversationId, senderId, content });

    const deadline = sentAt + perMessageTimeoutMs;
    while (pending.has(content) && performance.now() < deadline) {
      await sleep(10);
    }

    if (i < messageCount - 1 && sendGapMs > 0) {
      await sleep(sendGapMs);
      sendGapTotalMs += sendGapMs;
    }
  }

  const waitEnd = performance.now() + serverWaitMs;
  while (pending.size > 0 && performance.now() < waitEnd) {
    await sleep(20);
  }

  const sendWallMs = Math.round(performance.now() - wallStart);
  const sendPublishMs = Math.max(0, sendWallMs - sendGapTotalMs);
  const serverRttMissed = pending.size;
  pending.clear();

  try {
    subscription.unsubscribe();
  } catch {
    /* ignore */
  }

  const sorted = [...serverRtts].sort((a, b) => a - b);
  const serverRttAvg =
    sorted.length > 0 ? Math.round(sorted.reduce((a, b) => a + b, 0) / sorted.length) : 0;
  const serverRttP95 = percentile(sorted, 95);

  return {
    sendWallMs,
    sendGapTotalMs,
    sendPublishMs,
    serverRtts: sorted,
    serverRttAvg,
    serverRttP95,
    serverRttReceived: serverRtts.length,
    serverRttMissed,
  };
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function percentile(sorted, p) {
  if (sorted.length === 0) return 0;
  const idx = Math.ceil((p / 100) * sorted.length) - 1;
  return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
}

export async function disconnectStomp(client) {
  try {
    await client.deactivate();
  } catch {
    /* ignore */
  }
}
