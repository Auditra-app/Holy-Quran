// Simple in-memory sliding window rate limiter per Worker edge instance
const rateLimitMap = new Map();
const RATE_LIMIT_WINDOW_MS = 5 * 60 * 1000; // 5 minutes
const MAX_REQUESTS_PER_WINDOW = 15; // Max 15 download requests per 5 minutes per IP

// Known abusive scanners and malicious bot signatures
const BLOCKED_USER_AGENTS = [
  /sqlmap/i,
  /nikto/i,
  /masscan/i,
  /zgrab/i,
  /nmap/i,
  /dirbuster/i,
  /gobuster/i,
  /wpscan/i,
  /acunetix/i,
  /havij/i,
  /censys/i,
  /shodan/i,
];

function isRateLimited(ip) {
  const now = Date.now();
  const record = rateLimitMap.get(ip);

  if (!record) {
    rateLimitMap.set(ip, { count: 1, resetAt: now + RATE_LIMIT_WINDOW_MS });
    return false;
  }

  if (now > record.resetAt) {
    rateLimitMap.set(ip, { count: 1, resetAt: now + RATE_LIMIT_WINDOW_MS });
    return false;
  }

  record.count += 1;
  return record.count > MAX_REQUESTS_PER_WINDOW;
}

// Periodic cleanup of stale rate limit entries to prevent memory growth
function cleanupStaleEntries() {
  const now = Date.now();
  for (const [ip, record] of rateLimitMap.entries()) {
    if (now > record.resetAt) {
      rateLimitMap.delete(ip);
    }
  }
}

export default {
  async fetch(request, env) {
    // 1. HTTP Method Restriction: only GET and HEAD allowed
    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response("Method Not Allowed", {
        status: 405,
        headers: {
          "Allow": "GET, HEAD",
          "Content-Type": "text/plain; charset=utf-8",
        },
      });
    }

    const url = new URL(request.url);
    const filename = url.pathname.slice(1);

    // Root status probe
    if (!filename) {
      return new Response("Holy Quran Official Download Server is Online and Protected.", {
        status: 200,
        headers: { "Content-Type": "text/plain; charset=utf-8" },
      });
    }

    // 2. User-Agent Validation (block known vulnerability scanners and malicious bots)
    const userAgent = request.headers.get("user-agent") || "";
    for (const pattern of BLOCKED_USER_AGENTS) {
      if (pattern.test(userAgent)) {
        return new Response("Access Denied: Malicious or automated client detected.", {
          status: 403,
          headers: { "Content-Type": "text/plain; charset=utf-8" },
        });
      }
    }

    // 3. Client IP Extraction & Anti-Flood Rate Limiting
    const clientIp = request.headers.get("cf-connecting-ip") || "unknown";
    if (clientIp !== "unknown" && isRateLimited(clientIp)) {
      return new Response("Too Many Requests: Rate limit exceeded. Please wait a few minutes before trying again.", {
        status: 429,
        headers: {
          "Retry-After": "300",
          "Content-Type": "text/plain; charset=utf-8",
        },
      });
    }

    if (Math.random() < 0.05) {
      cleanupStaleEntries();
    }

    // 4. Retrieve requested asset from R2 Bucket
    const object = await env.BUCKET.get(filename, {
      range: request.headers,
      onlyIf: request.headers,
    });

    if (!object) {
      return new Response("File not found: " + filename, { status: 404 });
    }

    // 5. Response Headers: Caching, Android APK delivery, and Security Headers
    const headers = new Headers();
    object.writeHttpMetadata(headers);
    headers.set("etag", object.httpEtag);
    headers.set("Content-Type", "application/vnd.android.package-archive");
    headers.set("Content-Disposition", `attachment; filename="${filename}"`);
    headers.set("Cache-Control", "public, max-age=31536000, immutable");
    headers.set("Access-Control-Allow-Origin", "*");

    // Abuse & Crawler Protection Headers
    headers.set("X-Robots-Tag", "noindex, nofollow, noarchive");
    headers.set("X-Content-Type-Options", "nosniff");
    headers.set("X-Frame-Options", "DENY");

    // Support HEAD requests without body
    if (request.method === "HEAD") {
      return new Response(null, {
        status: 200,
        headers,
      });
    }

    return new Response(object.body, {
      status: object.body ? (request.headers.get("range") ? 206 : 200) : 304,
      headers,
    });
  },
};
