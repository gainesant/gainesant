# Kuromi Dash

A Kuromi-themed car dashboard that runs in a web browser. Plug an Android phone into the
computer with a USB cable and its screen shows up in the middle of the dashboard, where you
can tap, swipe and launch apps. Clock, speedometer, music buttons, app shortcuts and a cabin
panel sit around it.

> Fan-made theme for personal use. Kuromi is a Sanrio character; this project isn't affiliated
> with or endorsed by Sanrio, and the mascot in `public/kuromi.svg` is an original drawing.

## About Android Auto

**A web page can't run real Android Auto.** Android Auto only sends its car interface to
head units that Google has certified, over a protocol that browsers don't support. On
current Android versions there's also no Android Auto mode on the phone screen itself.

What Kuromi Dash does instead is **project the phone** over USB, using
[scrcpy](https://github.com/Genymobile/scrcpy) through [Tango ADB](https://tangoadb.dev)
(WebUSB), all inside Chrome:

- **Car screen** mode (default, Android 10+): scrcpy creates a separate landscape screen
  on the phone, sized to fit the dashboard. The Launch buttons open Maps, Waze, Spotify and
  other apps on that screen, so it works like a car display while your phone's own screen
  stays free.
- **Mirror phone** mode shows your phone's normal screen.

To get **real Android Auto** on a computer, use an open-source head unit such as
[OpenAuto](https://github.com/f1xpl/openauto) or
[Crankshaft](https://getcrankshaft.com) (Raspberry Pi). Those run as their own apps, so
this page can't embed them.

## What you need

- A computer in the car running Chrome or Edge (a laptop, mini PC or Raspberry Pi with
  Chromium). Firefox and Safari don't support WebUSB.
- An Android phone and a USB data cable.
- On the phone: go to **Settings → About phone** and tap **Build number** seven times, then
  turn on **Developer options → USB debugging**.

## Run it

```sh
cd kuromi-dash
npm install        # only needed to rebuild; the built files are already committed
npm start          # serves http://localhost:8080
```

Then open **http://localhost:8080** in Chrome and press **Connect phone**:

1. Pick your phone from the USB list.
2. On the phone, tap **Allow** on the "Allow USB debugging?" prompt. Tick
   *Always allow from this computer* so you don't get asked again.
3. The phone screen appears. After that, the dashboard reconnects on its own when the
   page loads or when you plug the phone back in.

The page has to be served from `localhost` (or HTTPS). Opening `index.html` as a file
won't work, because WebUSB is blocked there.

### Kiosk mode (boots straight into the dashboard)

```sh
chromium --kiosk --app=http://localhost:8080
```

On a Raspberry Pi, add `npm start` and that command to autostart.

## Controls

| Where | What it does |
| --- | --- |
| Phone screen | Tap and swipe like the real phone. Scroll wheel scrolls. Right-click = Back. |
| Side buttons on the phone screen | Back, Home, Recent apps, Assistant |
| Music | Play/pause, previous/next and volume, sent to the phone as media keys |
| Launch | Opens the app on the phone (on the car screen in Car screen mode) |
| Speed | GPS speed if the computer has GPS. Tap the unit to switch mph / km/h. |
| Cabin | Temperature and fan (UI only, see below) |
| Dock | Home, Focus (phone fills the middle), Kuromi (new quip), Dim, Full screen, Unplug |

## Troubleshooting

- **"The phone is busy"**: a desktop ADB is already using the phone. Run `adb kill-server`
  (or close Android Studio or scrcpy) and try again.
- **Nothing in the USB list**: check the cable carries data, USB debugging is on, and the
  phone is unlocked. On Linux you may need a udev rule for your phone (the same one `adb`
  needs).
- **Black screen in Car screen mode**: the phone may be older than Android 10. Switch to
  **Mirror phone**.
- **No sound**: sound stays on the phone. Pair it with the car over Bluetooth or use an AUX
  cable.

## Hooking up real car controls

The Cabin panel only changes what's on screen. When you change it, the page sends a
`car:climate` event with `{ temp, fan }`. To control real hardware, listen for that event
and forward it to something that can talk to the car, such as an Arduino over
[Web Serial](https://developer.chrome.com/docs/capabilities/serial):

```js
window.addEventListener("car:climate", (e) => console.log(e.detail));
```

## Files

| Path | Purpose |
| --- | --- |
| `public/index.html`, `style.css`, `app.js` | The dashboard |
| `public/kuromi.svg` | Mascot artwork |
| `src/phone.js` | USB/ADB + scrcpy phone link; bundled into `public/vendor/phone.js` |
| `public/vendor/scrcpy-server` | scrcpy server v3.3.3 (Apache-2.0), pushed to the phone on connect |
| `scripts/serve.mjs` | Static server with no dependencies |
| `scripts/fetch-server.mjs` | Downloads the scrcpy server again |

After you edit `src/phone.js`, run `npm run build`. If you upgrade scrcpy, update the
version in `scripts/fetch-server.mjs`, `SERVER_VERSION` in `src/phone.js` and the matching
`AdbScrcpyOptions…` class together.
