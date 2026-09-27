// Phone projection: talks to an Android phone over WebUSB (ADB), starts a
// scrcpy server on it, decodes the H.264 stream with WebCodecs, and sends
// touches and key presses back. Bundled to public/vendor/phone.js by esbuild.

import { Adb, AdbDaemonTransport } from "@yume-chan/adb";
import AdbWebCredentialStore from "@yume-chan/adb-credential-web";
import { AdbDaemonWebUsbDevice, AdbDaemonWebUsbDeviceManager } from "@yume-chan/adb-daemon-webusb";
import { AdbScrcpyClient, AdbScrcpyOptions3_3_3 } from "@yume-chan/adb-scrcpy";
import {
    AndroidKeyEventAction,
    AndroidMotionEventAction,
    DefaultServerPath,
    ScrcpyNewDisplay,
    ScrcpyPointerId,
} from "@yume-chan/scrcpy";
import {
    WebCodecsVideoDecoder,
    WebGLVideoFrameRenderer,
    BitmapVideoFrameRenderer,
} from "@yume-chan/scrcpy-decoder-webcodecs";
import { ReadableStream } from "@yume-chan/stream-extra";

// Must match the scrcpy-server binary in public/vendor (see scripts/fetch-server.mjs).
const SERVER_VERSION = "3.3.3";
const SERVER_URL = "vendor/scrcpy-server";

export const KeyCode = {
    Home: 3,
    Back: 4,
    VolumeUp: 24,
    VolumeDown: 25,
    MediaPlayPause: 85,
    MediaNext: 87,
    MediaPrevious: 88,
    AppSwitch: 187,
    Assist: 219,
};

export const isSupported = () =>
    !!AdbDaemonWebUsbDeviceManager.BROWSER && WebCodecsVideoDecoder.isSupported;

export class PhoneLink extends EventTarget {
    #adb;
    #client;
    #decoder;
    #canvas;
    #credentialStore = new AdbWebCredentialStore("Kuromi Dash");

    get connected() {
        return !!this.#client;
    }

    get deviceName() {
        return this.#adb?.banner.model ?? this.#adb?.serial ?? "";
    }

    #status(text) {
        this.dispatchEvent(new CustomEvent("status", { detail: text }));
    }

    /**
     * Attach to a phone. When `pick` is false it only reconnects to a phone the
     * browser already has permission for (no USB chooser), so it can run on page load.
     *
     * `carDisplay` asks scrcpy to create a separate landscape virtual display on the
     * phone (Android 10+), so apps launch in a car-shaped screen while the phone's own
     * screen stays free. Otherwise the phone screen is mirrored as-is.
     */
    async connect(host, { pick = true, carDisplay = null } = {}) {
        const manager = AdbDaemonWebUsbDeviceManager.BROWSER;
        if (!manager) throw new Error("This browser has no WebUSB. Use Chrome or Edge.");

        let device;
        if (pick) {
            device = await manager.requestDevice();
        } else {
            [device] = await manager.getDevices();
        }
        if (!device) return false;

        this.#status("Opening USB…");
        let connection;
        try {
            connection = await device.connect();
        } catch (e) {
            if (e instanceof AdbDaemonWebUsbDevice.DeviceBusyError) {
                throw new Error(
                    "The phone is busy. Another ADB is holding it — run `adb kill-server` on this computer and try again.",
                );
            }
            throw e;
        }

        this.#status("Waiting for you to tap “Allow” on the phone…");
        const transport = await AdbDaemonTransport.authenticate({
            serial: device.serial,
            connection,
            credentialStore: this.#credentialStore,
        });
        this.#adb = new Adb(transport);
        transport.disconnected.then(() => this.#teardown("Phone unplugged"));

        this.#status("Starting projection…");
        const serverBytes = await fetch(SERVER_URL).then((r) => {
            if (!r.ok) throw new Error(`Missing ${SERVER_URL} — run npm run fetch-server`);
            return r.arrayBuffer();
        });
        await AdbScrcpyClient.pushServer(
            this.#adb,
            new ReadableStream({
                start(controller) {
                    controller.enqueue(new Uint8Array(serverBytes));
                    controller.close();
                },
            }),
        );

        const init = {
            audio: false, // phone audio goes to the car over Bluetooth/AUX
            control: true,
            clipboardAutosync: false,
            maxFps: 60,
            videoBitRate: 8_000_000,
            stayAwake: true,
        };
        if (carDisplay) {
            init.newDisplay = new ScrcpyNewDisplay(carDisplay.width, carDisplay.height, carDisplay.dpi);
        } else {
            init.maxSize = 1280;
        }
        const options = new AdbScrcpyOptions3_3_3(init, { version: SERVER_VERSION });

        this.#client = await AdbScrcpyClient.start(this.#adb, DefaultServerPath, options);
        this.#pipeOutput(this.#client.output);
        this.#client.exited.then(() => this.#teardown("Projection stopped"));

        const video = await this.#client.videoStream;
        const renderer = WebGLVideoFrameRenderer.isSupported
            ? new WebGLVideoFrameRenderer()
            : new BitmapVideoFrameRenderer();
        this.#decoder = new WebCodecsVideoDecoder({ codec: video.metadata.codec, renderer });
        this.#canvas = renderer.canvas;
        this.#canvas.className = "phone-canvas";
        host.replaceChildren(this.#canvas);
        this.#bindInput(this.#canvas);

        this.#decoder.sizeChanged(({ width, height }) =>
            this.dispatchEvent(new CustomEvent("resize", { detail: { width, height } })),
        );
        video.stream.pipeTo(this.#decoder.writable).catch(() => {});

        this.#status(`Connected · ${this.deviceName}`);
        this.dispatchEvent(new Event("connected"));
        return true;
    }

    async #pipeOutput(output) {
        // The server log must be drained or the ADB connection stalls.
        try {
            for await (const line of output) console.debug("[scrcpy]", line);
        } catch {}
    }

    async disconnect() {
        await this.#client?.close().catch(() => {});
        await this.#adb?.close().catch(() => {});
        this.#teardown("Disconnected");
    }

    #teardown(reason) {
        if (!this.#client && !this.#adb) return;
        this.#decoder?.dispose();
        this.#canvas?.remove();
        this.#client = this.#adb = this.#decoder = this.#canvas = undefined;
        this.#status(reason);
        this.dispatchEvent(new Event("disconnected"));
    }

    async pressKey(keyCode) {
        const c = this.#client?.controller;
        if (!c) return;
        for (const action of [AndroidKeyEventAction.Down, AndroidKeyEventAction.Up]) {
            await c.injectKeyCode({ action, keyCode, repeat: 0, metaState: 0 });
        }
    }

    /**
     * Launch an app by package name, or by its label with `searchByName` (scrcpy 3.0+).
     * Opens on the car display when one is in use.
     */
    async launch(name, options) {
        await this.#client?.controller?.startApp(name, options);
    }

    #bindInput(canvas) {
        canvas.style.touchAction = "none";

        // The canvas is letterboxed with object-fit: contain, so map through the drawn area.
        const toVideo = (e) => {
            const rect = canvas.getBoundingClientRect();
            const w = this.#decoder.width;
            const h = this.#decoder.height;
            const scale = Math.min(rect.width / w, rect.height / h);
            const offX = (rect.width - w * scale) / 2;
            const offY = (rect.height - h * scale) / 2;
            return {
                pointerX: Math.max(0, Math.min(w, (e.clientX - rect.left - offX) / scale)),
                pointerY: Math.max(0, Math.min(h, (e.clientY - rect.top - offY) / scale)),
                videoWidth: w,
                videoHeight: h,
            };
        };

        const send = (e, action) => {
            const c = this.#client?.controller;
            if (!c || !this.#decoder) return;
            e.preventDefault();
            const isMouse = e.pointerType === "mouse";
            c.injectTouch({
                action,
                pointerId: isMouse ? ScrcpyPointerId.Mouse : BigInt(e.pointerId),
                ...toVideo(e),
                pressure: action === AndroidMotionEventAction.Up ? 0 : 1,
                actionButton: isMouse && action !== AndroidMotionEventAction.Move ? 1 : 0,
                buttons: isMouse ? e.buttons : 0,
            });
        };

        canvas.addEventListener("pointerdown", (e) => {
            canvas.setPointerCapture(e.pointerId);
            send(e, AndroidMotionEventAction.Down);
        });
        canvas.addEventListener("pointermove", (e) => {
            if (e.buttons || e.pointerType !== "mouse") send(e, AndroidMotionEventAction.Move);
        });
        canvas.addEventListener("pointerup", (e) => send(e, AndroidMotionEventAction.Up));
        canvas.addEventListener("pointercancel", (e) => send(e, AndroidMotionEventAction.Up));
        canvas.addEventListener("contextmenu", (e) => {
            e.preventDefault();
            this.pressKey(KeyCode.Back);
        });
        canvas.addEventListener(
            "wheel",
            (e) => {
                const c = this.#client?.controller;
                if (!c || !this.#decoder) return;
                e.preventDefault();
                c.injectScroll({
                    ...toVideo(e),
                    scrollX: -Math.sign(e.deltaX),
                    scrollY: -Math.sign(e.deltaY),
                    buttons: 0,
                });
            },
            { passive: false },
        );
    }
}
