# Kuromi Dash

A Kuromi-themed car dashboard for **Samsung DeX**. Your Galaxy phone is the computer. Plug
it into a screen in the car, DeX puts a desktop on that screen, and Kuromi Dash runs
full-screen on it. When you open Maps, Spotify and other apps from the dashboard, they open
as their own DeX windows on the car screen.

What's on the dashboard:
- A **live map** that follows you, with Kuromi as your marker
- A **"Where to?" box** that starts turn-by-turn directions in Google Maps or Waze
- A **GPS speedometer** that uses the phone's own GPS
- A **clock and date**, and the outside temperature
- **App shortcuts**: Maps, Waze, Spotify, YouTube Music, Phone, Messages, Email
- A **cabin panel** for temperature and fan (UI only for now, see below)
- **Kuromi** herself, saying things to you now and then

> Fan-made theme for personal use. Kuromi is a Sanrio character; this project isn't affiliated
> with or endorsed by Sanrio, and the mascot in `public/kuromi.svg` is an original drawing.

## What you need

- A **Samsung Galaxy phone that supports DeX**. That's most S-series and Note phones from the
  S8 on, plus some Z Fold and A phones. On Android 15 and later, DeX must be turned on under
  **Settings → Connected devices → Samsung DeX**.
- **A screen in the car**, connected in one of two ways:
  - **Wired:** a USB-C to HDMI adapter (or a USB-C hub with HDMI and charging) into the
    screen's HDMI input. For **touch**, the screen also needs a USB touch cable into the
    same hub. Touchscreen monitors with a single USB-C cable handle video, touch and power
    over one cable.
  - **Wireless DeX:** screens that support Miracast / screen mirroring.
- **Power.** DeX drains the battery, so use a hub that charges the phone while it's
  plugged in.
- A mouse or a Bluetooth keyboard is handy while you set things up.

## Set it up (once)

1. **Put the dashboard online over https.** The phone needs an `https://` address for GPS,
   keeping the screen awake and working offline. The simplest way is **GitHub Pages**:
   - In this repo, go to **Settings → Pages** and set *Source* to **GitHub Actions**.
   - Merge this branch into `main`. The workflow in `.github/workflows/kuromi-dash-pages.yml`
     then publishes `kuromi-dash/public/` to `https://gainesant.github.io/gainesant/`.
   - Any other static host works too (Netlify, Cloudflare Pages…). Just upload the `public/`
     folder.
2. **Connect the phone to the car screen.** DeX starts on its own; if it doesn't, pull
   down the quick settings and tap **DeX**.
3. **Open the address in Chrome or Samsung Internet** in DeX, then tap **Allow** when it asks
   for your location.
4. **Install it:** open the browser menu, choose **Add to Home screen / Install app**, and
   drag the new Kuromi Dash icon onto the DeX desktop or taskbar.
5. **Make it full screen:** open Kuromi Dash from that icon and tap **Full** in the dock
   (or maximize the window).

After that, getting in the car is: plug in the phone, then tap Kuromi Dash.

## Using it

| Where | What it does |
| --- | --- |
| Map | Drag to look around. **Me** (dock) or the pink button brings it back to you. |
| Where to, cutie? | Type a place, then tap **Maps** or **Waze** to start directions in that app. |
| Drive / Listen / Talk | Opens that app in its own DeX window. Close it or switch back to return. |
| Speed | Tap the unit to switch mph / km/h. Speed appears once you're moving. |
| Dock | Me, Focus (map fills the middle), Kuromi (new quip), Dim, Full screen |

Music controls (play/pause, skip) are in the Spotify or YouTube Music window, and in the
DeX notification panel at the bottom right. A web page isn't allowed to control other
apps' playback. Sound comes out of the phone, so pair it with the car over Bluetooth
or use the hub's audio out.

**With no signal:** the dashboard, clock, speedometer and app buttons still work. The map
and the weather come back when you have signal again.

## Troubleshooting

- **The map says "Location is off"**: long-press the Kuromi Dash icon, open **App info →
  Permissions**, and allow Location. Also check that location is on in quick settings.
- **An app button does nothing**: that app probably isn't installed. The button opens its
  website instead, or does nothing if the app has no website.
- **Touch doesn't work on the car screen**: HDMI only carries the picture. The screen's USB
  touch cable has to go into the same hub as the phone.
- **The screen goes dark**: go to **Settings → Display → Screen timeout** in DeX and turn it
  up. The dashboard also asks the phone to keep the screen on while it's in front.

## Hooking up real car controls

The Cabin panel only changes what's on screen. When you change it, the page sends a
`car:climate` event with `{ temp, fan }`, which you can forward to hardware that can talk
to the car:

```js
window.addEventListener("car:climate", (e) => console.log(e.detail));
```

## Working on it

Test it on a computer with:

```sh
cd kuromi-dash
npm start          # http://localhost:8080 (localhost counts as secure, so GPS prompts work)
```

On a computer, the app buttons open the apps' websites in a new tab. On the phone they
open the real apps.

| Path | Purpose |
| --- | --- |
| `public/index.html`, `style.css`, `app.js` | The dashboard |
| `public/kuromi.svg` | Mascot artwork (also used as the map marker) |
| `public/manifest.webmanifest`, `sw.js`, `icons/` | Install as an app and work offline |
| `public/vendor/leaflet/` | Map library (Leaflet 1.9.4, BSD-2-Clause) |
| `scripts/serve.mjs` | Local test server with no dependencies |

When you change a file, bump `VERSION` in `public/sw.js` so installed copies pick up the
update. Map data © OpenStreetMap contributors, tiles © CARTO.
