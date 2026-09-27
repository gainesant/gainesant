import { PhoneLink, KeyCode, isSupported } from "./vendor/phone.js";

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

// ---------- speed (GPS) ----------
let units = store.get("units", "mph");
let lastSpeed = null;
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

if ("geolocation" in navigator) {
    navigator.geolocation.watchPosition(
        (pos) => {
            // coords.speed is m/s, or null when the device can't tell (e.g. Wi-Fi location).
            lastSpeed = pos.coords.speed ?? (lastSpeed === null ? null : lastSpeed);
            $("#gps-note").textContent = pos.coords.speed === null ? "No speed from this GPS" : "GPS";
            renderSpeed();
            loadWeather(pos.coords);
        },
        () => ($("#gps-note").textContent = "No GPS on this computer"),
        { enableHighAccuracy: true, maximumAge: 1000 },
    );
} else {
    $("#gps-note").textContent = "No GPS on this computer";
}

// ---------- weather (optional, needs internet) ----------
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
        // offline in the car is normal; just leave the chip hidden
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

// Hook for real hardware: listen for this event and forward it to your car
// (e.g. an Arduino/CAN bridge over Web Serial). Nothing listens by default.
function emitClimate() {
    window.dispatchEvent(new CustomEvent("car:climate", { detail: { temp, fan: Number($("#fan").value) } }));
}

// ---------- phone ----------
const phone = new PhoneLink();

const apps = [
    { label: "Maps", pkg: "com.google.android.apps.maps", c: "#34a853", icon: '<svg viewBox="0 0 24 24"><path d="M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11z"/><circle cx="12" cy="10" r="2"/></svg>' },
    { label: "Waze", pkg: "com.waze", c: "#33ccff", icon: '<svg viewBox="0 0 24 24"><path d="M4 14a8 7 0 1 1 16 0c0 2-2 3-4 3H8c-2 0-4-1-4-3z"/><circle cx="9" cy="11" r="1"/><circle cx="15" cy="11" r="1"/></svg>' },
    { label: "Spotify", pkg: "com.spotify.music", c: "#1db954", icon: '<svg viewBox="0 0 24 24"><path d="M6 9c4-1 8-1 12 1M7 13c3-1 7-.5 10 1M8 16.5c2.5-.7 5-.4 7 .7"/></svg>' },
    { label: "YT Music", pkg: "com.google.android.apps.youtube.music", c: "#ff0033", icon: '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><path d="m10 9 5 3-5 3z"/></svg>' },
    { label: "Phone", name: "Phone", c: "#7a5af8", icon: '<svg viewBox="0 0 24 24"><path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a1 1 0 0 1-1 1A16 16 0 0 1 4 5a1 1 0 0 1 1-1z"/></svg>' },
    { label: "Messages", pkg: "com.google.android.apps.messaging", c: "#ff5fa2", icon: '<svg viewBox="0 0 24 24"><path d="M4 5h16v11H9l-5 4z"/></svg>' },
];
$("#apps").innerHTML = apps
    .map((a, i) => `<button class="app" data-app="${i}"><i style="--c:${a.c}">${a.icon}</i>${a.label}</button>`)
    .join("");
$("#apps").addEventListener("click", async (e) => {
    const btn = e.target.closest("[data-app]");
    if (!btn) return;
    const app = apps[btn.dataset.app];
    if (!phone.connected) return toast("Connect your phone first 💀");
    try {
        if (app.pkg) await phone.launch(app.pkg);
        else await phone.launch(app.name, { searchByName: true });
    } catch (err) {
        toast(`Couldn't open ${app.label}`);
    }
});

document.querySelectorAll("[data-key]").forEach((b) =>
    b.addEventListener("click", () => {
        if (!phone.connected) return toast("Connect your phone first 💀");
        phone.pressKey(KeyCode[b.dataset.key]);
    }),
);
$("#dock-home").addEventListener("click", () => phone.connected && phone.pressKey(KeyCode.Home));

function setChip(on, text) {
    const chip = $("#phone-chip");
    chip.classList.toggle("chip-on", on);
    chip.classList.toggle("chip-off", !on);
    $("#phone-chip-text").textContent = text;
}

phone.addEventListener("status", (e) => {
    $("#welcome-text").textContent = e.detail;
    if (!phone.connected) setChip(false, e.detail.length > 28 ? "Connecting…" : e.detail);
});
phone.addEventListener("connected", () => {
    $("#welcome").hidden = true;
    $("#phone-nav").hidden = false;
    $("#dock-unplug").hidden = false;
    setChip(true, phone.deviceName || "Phone");
    toast("Phone connected. Let's roll 🖤");
});
phone.addEventListener("disconnected", () => {
    $("#welcome").hidden = false;
    $("#phone-nav").hidden = true;
    $("#dock-unplug").hidden = true;
    $("#connect").disabled = false;
    setChip(false, "No phone");
});

// Mode: "car" gives apps their own landscape screen sized to the stage;
// "mirror" shows the phone's own screen.
const modeInputs = document.querySelectorAll('input[name="mode"]');
const savedMode = store.get("mode", "car");
modeInputs.forEach((i) => {
    i.checked = i.value === savedMode;
    i.addEventListener("change", () => store.set("mode", i.value));
});

function carDisplaySize() {
    const r = $("#stage").getBoundingClientRect();
    const scale = Math.min(1, 1280 / r.width); // cap resolution so the phone's encoder keeps up
    const even = (n) => Math.max(2, Math.round((n * scale) / 2) * 2);
    return { width: even(r.width), height: even(r.height), dpi: 160 };
}

async function connect(pick) {
    const mode = store.get("mode", "car");
    $("#connect").disabled = true;
    try {
        const ok = await phone.connect($("#phone-host"), {
            pick,
            carDisplay: mode === "car" ? carDisplaySize() : null,
        });
        if (!ok) $("#connect").disabled = false;
    } catch (err) {
        console.error(err);
        $("#connect").disabled = false;
        $("#welcome-text").textContent = err?.message ?? String(err);
        setChip(false, "No phone");
        if (pick) toast(err?.message ?? "Couldn't connect");
    }
}

if (!isSupported()) {
    $("#unsupported").hidden = false;
    $("#connect").disabled = true;
} else {
    $("#connect").addEventListener("click", () => connect(true));
    // Reconnect automatically to a phone this browser has already been allowed to use,
    // both on page load and whenever it gets plugged in.
    connect(false);
    navigator.usb.addEventListener("connect", () => {
        if (!phone.connected) setTimeout(() => connect(false), 800);
    });
}
$("#dock-unplug").addEventListener("click", () => phone.disconnect());

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

// Keep the screen on while the dashboard is up.
async function keepAwake() {
    try {
        await navigator.wakeLock?.request("screen");
    } catch {}
}
keepAwake();
document.addEventListener("visibilitychange", () => document.visibilityState === "visible" && keepAwake());
