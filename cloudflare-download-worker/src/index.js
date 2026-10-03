export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const filename = url.pathname.slice(1);

    if (!filename) {
      return new Response("Holy Quran Official Download Server is Online.", {
        status: 200,
        headers: { "Content-Type": "text/plain; charset=utf-8" },
      });
    }

    const object = await env.BUCKET.get(filename, {
      range: request.headers,
      onlyIf: request.headers,
    });

    if (!object) {
      return new Response("File not found: " + filename, { status: 404 });
    }

    const headers = new Headers();
    object.writeHttpMetadata(headers);
    headers.set("etag", object.httpEtag);
    headers.set("Content-Type", "application/vnd.android.package-archive");
    headers.set("Content-Disposition", `attachment; filename="${filename}"`);
    headers.set("Cache-Control", "public, max-age=31536000, immutable");
    headers.set("Access-Control-Allow-Origin", "*");

    return new Response(object.body, {
      status: object.body ? (request.headers.get("range") ? 206 : 200) : 304,
      headers,
    });
  },
};
