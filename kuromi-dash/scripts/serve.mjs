// Zero-dependency static server. WebUSB only works in a secure context, and
// http://localhost counts as one, so serve the dashboard from here.
import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../public/", import.meta.url));
const port = Number(process.env.PORT ?? 8080);
const types = {
    ".html": "text/html; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".svg": "image/svg+xml",
    ".json": "application/json",
    ".map": "application/json",
    ".png": "image/png",
    ".webmanifest": "application/manifest+json",
};

createServer(async (req, res) => {
    const path = normalize(decodeURIComponent(new URL(req.url, "http://x").pathname)).replace(/^([/\\])+/, "");
    const file = join(root, path || "index.html");
    if (!file.startsWith(root)) return res.writeHead(403).end();
    try {
        const body = await readFile(file);
        res.writeHead(200, { "content-type": types[extname(file)] ?? "application/octet-stream" }).end(body);
    } catch {
        res.writeHead(404).end("not found");
    }
}).listen(port, "127.0.0.1", () => console.log(`Kuromi Dash → http://localhost:${port}`));
