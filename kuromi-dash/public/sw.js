// Caches the dashboard so it opens with no signal. Map tiles and weather are
// left to the network. Bump VERSION whenever a file below changes.
const VERSION = "kuromi-dash-v1";
const SHELL = [
    "./",
    "index.html",
    "style.css",
    "app.js",
    "kuromi.svg",
    "manifest.webmanifest",
    "icons/icon-192.png",
    "icons/icon-512.png",
    "vendor/leaflet/leaflet.js",
    "vendor/leaflet/leaflet.css",
];

self.addEventListener("install", (e) => {
    e.waitUntil(caches.open(VERSION).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
    e.waitUntil(
        caches
            .keys()
            .then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
            .then(() => self.clients.claim()),
    );
});

// Network first for our own files so updates show up, falling back to the cache offline.
self.addEventListener("fetch", (e) => {
    const url = new URL(e.request.url);
    if (e.request.method !== "GET" || url.origin !== location.origin) return;
    e.respondWith(
        fetch(e.request)
            .then((res) => {
                if (res.ok) {
                    const copy = res.clone();
                    caches.open(VERSION).then((c) => c.put(e.request, copy));
                }
                return res;
            })
            .catch(() => caches.match(e.request, { ignoreSearch: true })),
    );
});
