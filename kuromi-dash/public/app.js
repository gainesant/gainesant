const $ = (sel) => document.querySelector(sel);

const store = {
    get(key, fallback) {
        try {
            const v = localStorage.getItem(`kuromi-dash:${key}`);
            return v === null ? fallback : JSON.parse(v);
        } catch {
            return fallback;
        }
    },
    set(key, value) {
        try {
            localStorage.setItem(`kuromi-dash:${key}`, JSON.stringify(value));
        } catch {}
    },
};

let toastTimer;
function toast(text, ms = 3000) {
    const el = $("#toast");
    el.textContent = text;
    el.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove("show"), ms);
}

// ---------- clock ----------
function tickClock() {
    const now = new Date();
    const h = now.getHours();
    const m = String(now.getMinutes()).padStart(2, "0");
    $("#clock").textContent = `${h % 12 || 12}:${m}`;
    $("#ampm").textContent = h < 12 ? "AM" : "PM";
    $("#top-clock").textContent = `${h % 12 || 12}:${m}`;
    $("#date").textContent = now.toLocaleDateString(undefined, { weekday: "long", month: "short", day: "numeric" });
}
tickClock();
setInterval(tickClock, 1000);

// ---------- opening Android apps ----------
// In DeX, following an intent: link opens the app in its own window on top of the
// dashboard. Chrome and Samsung Internet only launch activities that accept web links
// (BROWSABLE), so each app is reached through a link it already handles.
const isAndroid = /Android/i.test(navigator.userAgent);

function intentFor(url, pkg) {
    const u = new URL(url);
    const fallback = encodeURIComponent(url);
    return `intent://${u.host}${u.pathname}${u.search}#Intent;scheme=${u.protocol.slice(0, -1)};package=${pkg};S.browser_fallback_url=${fallback};end`;
}

function openApp(app, url = app.url) {
    if (!isAndroid) {
        // Testing on a computer: show the web version instead.
        if (app.pkg) window.open(url, "_blank", "noopener");
        else toast(`${app.label} opens on the phone`);
        return;
    }
    location.href = app.pkg ? intentFor(url, app.pkg) : url;
}

const icons = {
    pin: '<svg viewBox="0 0 24 24"><path d="M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11z"/><circle cx="12" cy="10" r="2"/></svg>',
    waze: '<svg viewBox="0 0 24 24"><path d="M4 14a8 7 0 1 1 16 0c0 2-2 3-4 3H8c-2 0-4-1-4-3z"/><circle cx="9" cy="11" r="1"/><circle cx="15" cy="11" r="1"/></svg>',
    spotify: '<svg viewBox="0 0 24 24"><path d="M6 9c4-1 8-1 12 1M7 13c3-1 7-.5 10 1M8 16.5c2.5-.7 5-.4 7 .7"/></svg>',
    play: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><path d="m10 9 5 3-5 3z"/></svg>',
    phone: '<svg viewBox="0 0 24 24"><path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a1 1 0 0 1-1 1A16 16 0 0 1 4 5a1 1 0 0 1 1-1z"/></svg>',
    chat: '<svg viewBox="0 0 24 24"><path d="M4 5h16v11H9l-5 4z"/></svg>',
    mail: '<svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg>',
};

const apps = {
    drive: [
        { label: "Maps", pkg: "com.google.android.apps.maps", url: "https://maps.google.com/", c: "#34a853", icon: icons.pin },
        { label: "Waze", pkg: "com.waze", url: "https://waze.com/ul", c: "#33ccff", icon: icons.waze },
    ],
    listen: [
        { label: "Spotify", pkg: "com.spotify.music", url: "https://open.spotify.com/", c: "#1db954", icon: icons.spotify },
        { label: "YT Music", pkg: "com.google.android.apps.youtube.music", url: "https://music.youtube.com/", c: "#ff0033", icon: icons.play },
    ],
    talk: [
        { label: "Phone", url: "tel:", c: "#7a5af8", icon: icons.phone },
        { label: "Messages", url: "sms:", c: "#ff5fa2", icon: icons.chat },
        { label: "Email", url: "mailto:", c: "#6b3fa0", icon: icons.mail },
    ],
};

for (const [group, list] of Object.entries(apps)) {
    const host = document.querySelector(`[data-group="${group}"]`);
    host.innerHTML = list
        .map((a, i) => `<button class="app" data-app="${group}:${i}"><i style="--c:${a.c}">${a.icon}</i>${a.label}</button>`)
        .join("");
}
document.querySelectorAll(".apps").forEach((host) =>
    host.addEventListener("click", (e) => {
        const btn = e.target.closest("[data-app]");
        if (!btn) return;
        const [group, i] = btn.dataset.app.split(":");
        openApp(apps[group][i]);
    }),
);

// Destination box: hands the trip to Google Maps or Waze with turn-by-turn started.
$("#go").addEventListener("submit", (e) => {
    e.preventDefault();
    const dest = $("#dest").value.trim();
    if (!dest) return $("#dest").focus();
    const q = encodeURIComponent(dest);
    if (e.submitter?.dataset.nav === "waze") {
        openApp(apps.drive[1], `https://waze.com/ul?q=${q}&navigate=yes`);
    } else {
        openApp(apps.drive[0], `https://www.google.com/maps/dir/?api=1&destination=${q}&travelmode=driving&dir_action=navigate`);
    }
    $("#dest").blur();
});

// ---------- map ----------
let map;
let marker;
let following = true;
let tileErrors = 0;

function initMap() {
    if (!window.L) return;
    map = L.map("map", { zoomControl: false, attributionControl: true }).setView([39.5, -98.35], 4);
    // Dark basemap; the pink/purple tint comes from a CSS filter in style.css.
    L.tileLayer("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png", {
        maxZoom: 19,
        subdomains: "abcd",
        attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> © <a href="https://carto.com/attributions">CARTO</a>',
    })
        .on("tileerror", () => {
            if (++tileErrors === 4 && !navigator.onLine) showWelcome("Map needs internet", "Speed, clock and apps still work offline.");
        })
        .on("tileload", () => {
            tileErrors = 0;
            if (lastFix) $("#welcome").hidden = true;
        })
        .addTo(map);
    marker = L.marker([0, 0], {
        icon: L.divIcon({ className: "me-marker", html: '<img src="kuromi.svg" alt="">', iconSize: [56, 56], iconAnchor: [28, 44] }),
        interactive: false,
    });
    map.on("dragstart", () => {
        following = false;
        $("#recenter").hidden = false;
    });
    new ResizeObserver(() => map.invalidateSize()).observe($("#stage"));
}
initMap();

function recenter() {
    following = true;
    $("#recenter").hidden = true;
    if (lastFix) map?.setView(lastFix, Math.max(map.getZoom(), 16));
}
$("#recenter").addEventListener("click", recenter);
$("#dock-me").addEventListener("click", recenter);

function showWelcome(title, text) {
    $("#welcome-title").textContent = title;
    $("#welcome-text").textContent = text;
    $("#welcome").hidden = false;
}

// ---------- GPS: speed + map ----------
let units = store.get("units", "mph");
let lastSpeed = null;
let lastFix = null;
const GAUGE_LEN = 252; // length of the gauge arc path
const GAUGE_MAX = { mph: 100, "km/h": 160 };

function renderSpeed() {
    $("#units").textContent = units;
    if (lastSpeed === null) {
        $("#speed").textContent = "--";
        $("#gauge-fill").style.strokeDashoffset = GAUGE_LEN;
        return;
    }
    const value = Math.round(lastSpeed * (units === "mph" ? 2.23694 : 3.6));
    $("#speed").textContent = value;
    const frac = Math.min(1, value / GAUGE_MAX[units]);
    $("#gauge-fill").style.strokeDashoffset = GAUGE_LEN * (1 - frac);
}
$("#units").addEventListener("click", () => {
    units = units === "mph" ? "km/h" : "mph";
    store.set("units", units);
    renderSpeed();
});
renderSpeed();

function setGpsChip(on, text) {
    const chip = $("#gps-chip");
    chip.classList.toggle("chip-on", on);
    chip.classList.toggle("chip-off", !on);
    $("#gps-chip-text").textContent = text;
}

function onFix(pos) {
    const { latitude, longitude, speed, accuracy } = pos.coords;
    // coords.speed is m/s, or null when the phone can't tell yet.
    lastSpeed = speed === null ? lastSpeed : speed;
    $("#gps-note").textContent = speed === null ? "Speed shows once you're moving" : "GPS";
    renderSpeed();
    setGpsChip(true, accuracy < 30 ? "GPS" : `GPS ±${Math.round(accuracy)} m`);

    const first = !lastFix;
    lastFix = [latitude, longitude];
    if (map) {
        marker.setLatLng(lastFix);
        if (first) {
            marker.addTo(map);
            map.setView(lastFix, 16);
            $("#welcome").hidden = true;
        } else if (following) {
            map.panTo(lastFix, { animate: true, duration: 0.8 });
        }
    } else if (first) {
        showWelcome("Map unavailable", "Couldn't load the map library.");
    }
    loadWeather(pos.coords);
}

function onGpsError(err) {
    setGpsChip(false, "No GPS");
    $("#gps-note").textContent = "No location";
    if (!lastFix) {
        showWelcome(
            err.code === err.PERMISSION_DENIED ? "Location is off" : "Can't find you",
            err.code === err.PERMISSION_DENIED
                ? "Allow location for this site in your browser settings so the map and speed work."
                : "Make sure location is turned on in the phone's quick settings.",
        );
    }
}

if (!window.isSecureContext) {
    showWelcome("Open over https", "Location only works on an https:// address or localhost. See the README.");
    setGpsChip(false, "No GPS");
} else if ("geolocation" in navigator) {
    navigator.geolocation.watchPosition(onFix, onGpsError, { enableHighAccuracy: true, maximumAge: 1000 });
} else {
    onGpsError({ code: 0, PERMISSION_DENIED: 1 });
}

// ---------- weather (needs internet) ----------
let weatherLoadedAt = 0;
async function loadWeather({ latitude, longitude }) {
    if (Date.now() - weatherLoadedAt < 15 * 60_000) return;
    weatherLoadedAt = Date.now();
    try {
        const unit = units === "mph" ? "fahrenheit" : "celsius";
        const url = `https://api.open-meteo.com/v1/forecast?latitude=${latitude.toFixed(2)}&longitude=${longitude.toFixed(2)}&current=temperature_2m&temperature_unit=${unit}`;
        const data = await fetch(url).then((r) => r.json());
        const el = $("#weather");
        el.textContent = `${Math.round(data.current.temperature_2m)}° outside`;
        el.hidden = false;
    } catch {
        weatherLoadedAt = 0; // offline in the car is normal; try again on a later fix
    }
}

// ---------- Kuromi ----------
const quips = [
    "Buckle up, cutie. We ride at midnight.",
    "Eyes on the road, not on me!",
    "Speed limits are… a suggestion? No. Obey them.",
    "Mischief mode: activated 💀",
    "Snacks before road trips. That's the rule.",
    "My Melody would've taken the scenic route.",
    "Turn up the music, bestie.",
    "Hydrate. Then villain.",
];
let quipIndex = 0;
function nextQuip() {
    quipIndex = (quipIndex + 1) % quips.length;
    $("#quip").textContent = quips[quipIndex];
    const m = $("#mascot");
    m.classList.remove("wiggle");
    void m.offsetWidth; // restart the animation
    m.classList.add("wiggle");
}
$("#mascot").addEventListener("click", nextQuip);
$("#dock-kuromi").addEventListener("click", nextQuip);
setInterval(nextQuip, 90_000);

// ---------- cabin (UI only until wired to the car) ----------
let temp = store.get("temp", 70);
function renderTemp() {
    $("#temp").textContent = temp;
}
document.querySelectorAll("[data-temp]").forEach((b) =>
    b.addEventListener("click", () => {
        temp = Math.max(60, Math.min(85, temp + Number(b.dataset.temp)));
        store.set("temp", temp);
        renderTemp();
        emitClimate();
    }),
);
$("#fan").value = store.get("fan", 2);
$("#fan").addEventListener("input", () => {
    store.set("fan", Number($("#fan").value));
    emitClimate();
});
renderTemp();

// Hook for real hardware: listen for this event and forward it to your car.
// Nothing listens by default.
function emitClimate() {
    window.dispatchEvent(new CustomEvent("car:climate", { detail: { temp, fan: Number($("#fan").value) } }));
}

// ---------- dock toggles ----------
$("#dock-focus").addEventListener("click", (e) => {
    const on = $(".dash").classList.toggle("focus");
    e.currentTarget.classList.toggle("on", on);
});
let dim = store.get("dim", false);
function renderDim() {
    document.documentElement.style.setProperty("--dim", dim ? 0.45 : 0);
    $("#dock-night").classList.toggle("on", dim);
}
$("#dock-night").addEventListener("click", () => {
    dim = !dim;
    store.set("dim", dim);
    renderDim();
});
renderDim();
$("#dock-full").addEventListener("click", () => {
    if (document.fullscreenElement) document.exitFullscreen();
    else document.documentElement.requestFullscreen?.().catch(() => {});
});

// Keep the screen on while the dashboard is showing.
async function keepAwake() {
    try {
        await navigator.wakeLock?.request("screen");
    } catch {}
}
keepAwake();
document.addEventListener("visibilitychange", () => document.visibilityState === "visible" && keepAwake());

// Offline support: cache the dashboard so it opens without signal.
if ("serviceWorker" in navigator && window.isSecureContext) {
    navigator.serviceWorker.register("sw.js").catch(() => {});
}
