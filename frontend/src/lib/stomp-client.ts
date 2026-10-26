// lib/stomp-client.ts
// Minimal STOMP-over-SockJS client (no external dep beyond the native WebSocket).
// Supports subscribe, unsubscribe, send, and auto-reconnect.

export function backendWsUrl(): string {
  const base = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");
  // SockJS endpoint
  return base.replace(/^http/, "ws") + "/ws";
}

type SubscribeCallback = (body: unknown) => void;
type Unsubscribe = () => void;

interface Subscription {
  id: string;
  destination: string;
  callback: SubscribeCallback;
}

export class MiniStompClient {
  private ws: WebSocket | null = null;
  private subscriptions = new Map<string, Subscription>();
  private subCounter = 0;
  private connected = false;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;

  constructor(private readonly url: string) {}

  connect() {
    this._open();
  }

  disconnect() {
    this._clearTimers();
    this.connected = false;
    this.ws?.close();
    this.ws = null;
  }

  /** Subscribe to a STOMP destination. Returns an unsubscribe function. */
  subscribe(destination: string, callback: SubscribeCallback): Unsubscribe {
    const id = `sub-${++this.subCounter}`;
    const sub: Subscription = { id, destination, callback };
    this.subscriptions.set(id, sub);

    if (this.connected) {
      this._sendFrame("SUBSCRIBE", { id, destination }, null);
    }

    return () => {
      this.subscriptions.delete(id);
      if (this.connected) {
        this._sendFrame("UNSUBSCRIBE", { id }, null);
      }
    };
  }

  /** Send a message to a STOMP destination. */
  send(destination: string, body: string) {
    if (!this.connected) return;
    this._sendFrame("SEND", { destination, "content-type": "application/json" }, body);
  }

  // ────────────────────── internals ──────────────────────

  private _open() {
    if (this.ws) return;
    // Use native WebSocket; in browser this works with SockJS/raw WS
    // For SockJS compatibility you'd need the SockJS script loaded separately.
    // Here we connect directly to the Spring raw WebSocket endpoint.
    try {
      this.ws = new WebSocket(this.url);
    } catch {
      this._scheduleReconnect();
      return;
    }

    this.ws.onopen = () => {
      // Send STOMP CONNECT frame
      this._sendRaw(
        "CONNECT\naccept-version:1.1,1.2\nheart-beat:20000,20000\n\n\0"
      );
    };

    this.ws.onmessage = (event) => {
      const data = typeof event.data === "string" ? event.data : "";
      this._handleFrame(data);
    };

    this.ws.onclose = () => {
      this._clearTimers();
      this.connected = false;
      this.ws = null;
      this._scheduleReconnect();
    };

    this.ws.onerror = () => {
      this.ws?.close();
    };
  }

  private _handleFrame(raw: string) {
    if (raw === "\n" || raw === "\r\n") return; // heartbeat
    const nullIdx = raw.indexOf("\0");
    const frameStr = nullIdx >= 0 ? raw.slice(0, nullIdx) : raw;
    const lines = frameStr.split("\n");
    const command = lines[0].trim();

    if (command === "CONNECTED") {
      this.connected = true;
      // Re-subscribe all existing subs
      for (const sub of this.subscriptions.values()) {
        this._sendFrame("SUBSCRIBE", { id: sub.id, destination: sub.destination }, null);
      }
      // Start heartbeat
      this.heartbeatTimer = setInterval(() => this._sendRaw("\n"), 20000);
      return;
    }

    if (command === "MESSAGE") {
      // Parse headers
      let i = 1;
      const headers: Record<string, string> = {};
      while (i < lines.length && lines[i].trim() !== "") {
        const colonIdx = lines[i].indexOf(":");
        if (colonIdx >= 0) {
          headers[lines[i].slice(0, colonIdx).trim()] = lines[i].slice(colonIdx + 1).trim();
        }
        i++;
      }
      const body = lines.slice(i + 1).join("\n").replace(/\0$/, "");
      const subId = headers["subscription"];
      const sub = subId ? this.subscriptions.get(subId) : undefined;
      if (sub) {
        try {
          sub.callback(body ? JSON.parse(body) : null);
        } catch {
          sub.callback(body);
        }
      }
    }
  }

  private _sendFrame(command: string, headers: Record<string, string>, body: string | null) {
    let frame = `${command}\n`;
    for (const [k, v] of Object.entries(headers)) {
      frame += `${k}:${v}\n`;
    }
    frame += "\n";
    if (body) frame += body;
    frame += "\0";
    this._sendRaw(frame);
  }

  private _sendRaw(data: string) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(data);
    }
  }

  private _scheduleReconnect() {
    if (this.reconnectTimer) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this._open();
    }, 4000);
  }

  private _clearTimers() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }
}