// Downloads the scrcpy server that gets pushed to the phone. Its version must
// match SERVER_VERSION in src/phone.js and the AdbScrcpyOptions class used there.
import { writeFile } from "node:fs/promises";

const VERSION = "3.3.3";
const url = `https://github.com/Genymobile/scrcpy/releases/download/v${VERSION}/scrcpy-server-v${VERSION}`;
const out = new URL("../public/vendor/scrcpy-server", import.meta.url);

const res = await fetch(url);
if (!res.ok) throw new Error(`${res.status} fetching ${url}`);
await writeFile(out, new Uint8Array(await res.arrayBuffer()));
console.log(`scrcpy-server v${VERSION} -> public/vendor/scrcpy-server`);
