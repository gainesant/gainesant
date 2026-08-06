# Coreframe

An arena mecha mod for Minecraft. You assemble a **core** from swappable parts — body,
gun, bomb, pod and legs — drop into an arena called **the Grid**, and duel. Movement is
built around air dashing rather than ground dodging, aiming is lock-on, and fights are
decided less by chipping away at hit points than by bursting through a regenerating
knockdown gauge.

> **Not affiliated with, endorsed by, or derived from any existing game or its
> publisher.** Coreframe is an original work. It reproduces a *genre* of arena combat —
> and game mechanics are not copyrightable — but every name, model, texture, sound and
> line of text in this repository is its own. See [Contributing](#contributing) for the
> rules this project holds itself to.

**Status: pre-alpha.** The rules engine is implemented and tested. The mod itself is a
scaffold and has not been compiled yet.

---

## What makes it play the way it does

Five mechanics carry the design. They are implemented in `core/` and pinned by tests, so
the executable statement of the design is `core/src/test/`.

**Two health bars.** Every core has a flat 1000 hit points regardless of body, plus a
**knockdown gauge** that refills continuously — *including while under fire*. Four hits in
a second floors an opponent; the same four hits spread over ten seconds do not. Bodies
differentiate through the size of that gauge and through damage reduction, never through
hit points.

**Knockdown is not a kill.** A floored core still takes damage, at 30%, then stands up
with three seconds of invulnerability. Those two numbers are what stop a knockdown from
snowballing into a loss and hand the round rhythm back.

**All evasion is airborne, and it is countable.** There is no ground dodge and no boost
meter. You get a fixed number of air actions, refreshed *only* by landing, and landing
costs recovery time that scales with what you did up there — a short drop is nearly free,
an air-dash landing is a punish window. "One air action left, over open ground" is the
tension the whole game runs on.

**Damage is a table, not a curve.** Every weapon authors damage across four range bands
and two stances. That lets a shotgun peak in your face and fail to reach long range at
all, while a gatling is *weakest* point-blank and strongest at distance. A falloff curve
can only decay; a table can have a shape.

**Fast rounds don't track.** Homing and projectile speed are spent from a shared budget,
enforced in `WeaponSpec`'s constructor rather than left to reviewer discipline. Without
it, the gun that both homes and arrives instantly dominates and every other gun in the
set becomes decoration.

---

## Layout

| Module | What it is | Builds anywhere? |
|---|---|---|
| `core/` | The rules engine. Plain Java, zero Minecraft types. | Yes — JDK 21+ |
| `neoforge/` | The mod. NeoForge, Minecraft 26.2. | Needs network access, JDK 25 |

The split is deliberate. The rules that decide how the game feels are worth being able to
test without launching a client, so they live in a library the mod consumes. The mod layer
moves models around and reports collisions; it does not decide who wins.

---

## Build environment

```bash
./gradlew build                              # everything
./gradlew -PincludeModModule=false :core:test  # rules engine only
```

Building the mod requires reaching these hosts:

- `maven.neoforged.net` — NeoForge and ModDevGradle
- `piston-meta.mojang.com`, `libraries.minecraft.net` — Minecraft itself
- a **Java 25** toolchain (required by Minecraft 26.x)

Restricted networks frequently block all of them. Rather than making the whole build
unusable there, `-PincludeModModule=false` drops the mod module so the rules engine still
compiles and tests — which is most of the interesting logic. CI runs both paths
separately for the same reason.

**Minecraft versioning changed in 2026.** Java Edition moved to calendar versioning
(`year.drop.hotfix`), so this targets **26.2**, not "1.21.x". Vanilla ships official
parameter names from 26.1 onward, so Parchment mappings are no longer used — adding them
back would be a regression.

---

## Roadmap

- [x] **M1** — Project skeleton, rules engine, CI
- [ ] **M0** — Technical spike: prove the decoupled orbit camera, and prove a dash that a
      *real dedicated server* accepts without rubber-banding. Both are risky enough to
      validate before building on them. Test against an actual server process, never LAN —
      the speed check is skipped for the singleplayer owner and will give a false pass.
- [ ] **M2** — Core combat loop: arena, cube drop-in, one body and one gun, lock-on
      camera, HUD. This is the milestone that decides whether the project is worth
      continuing; it should be fun before any part content exists.
- [ ] **M3** — Parts as datapack JSON across all five categories
- [ ] **M4** — Loadout GUI and progression
- [ ] **M5** — AI opponent
- [ ] **M6** — Art pass and first CurseForge release

---

## Contributing

Three rules, and the first one is not negotiable.

**1. No third-party assets, ever.** No ripped models, textures, audio or text from any
other game — not as placeholders, not "temporarily", not converted into another format.
Reformatting a mesh does not launder it. Reference material stays off this repository
entirely; `/reference/` is gitignored so it cannot be committed by accident.

**2. Original names.** Parts, cores, arenas and characters get names invented here.
Generic category words (`gun`, `bomb`, `pod`, `legs`, `body`) are fine because they belong
to nobody.

**3. Mechanics are fair game.** Systems, rules and numbers are not copyrightable, and
reproducing how a genre *plays* is exactly what this project is for. The line is
expression: how things look, what they are called, and what they say.

This matters practically, not just ethically. CurseForge human-reviews every new project
before first approval and rejects third-party IP at intake, and fan projects built on
someone else's characters get shut down even when the art is original and the mod is free.
Renaming after sixty assets exist is miserable; starting clean costs nothing.
