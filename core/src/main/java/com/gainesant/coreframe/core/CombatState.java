package com.gainesant.coreframe.core;

/**
 * One core's runtime state through a bout: its two health bars, its lifecycle state, and
 * its air-action budget.
 *
 * <p>This class is deliberately free of any Minecraft type. The rules that decide how the
 * game feels are worth being able to unit-test without launching a client, and keeping
 * them here means the entity layer above only has to move a model around and report
 * collisions.
 *
 * <p>Callers drive it with {@link #tick()} once per game tick, plus the event methods
 * ({@link #applyHit}, {@link #consumeAirAction}, {@link #land}) as those things happen.
 */
public final class CombatState {

    private final Loadout loadout;
    private final KnockdownGauge gauge;
    private final CubeDrop cubeDrop;

    private int hp = Rules.MAX_HP;
    private CoreState state = CoreState.CUBE;
    private int stateTicks;
    private int airActionsUsed;

    /**
     * Invulnerability is tracked as its own timer rather than being inferred from
     * {@link CoreState#REBIRTH}. A core that dashes the instant it stands up leaves the
     * REBIRTH state for AIRBORNE, and tying protection to the state would silently strip
     * the safety window exactly when a player is most likely to use it.
     */
    private int invulnerableTicks;

    public CombatState(Loadout loadout, CubeDrop cubeDrop) {
        if (loadout == null || cubeDrop == null) {
            throw new IllegalArgumentException("loadout and cube drop are both required");
        }
        this.loadout = loadout;
        this.gauge = KnockdownGauge.forBody(loadout.body());
        this.cubeDrop = cubeDrop;
    }

    /** Convenience for tests and for bouts that do not care which face was rolled. */
    public CombatState(Loadout loadout) {
        this(loadout, new CubeDrop(Rules.CUBE_MIN_FACE));
    }

    // ---------------------------------------------------------------- lifecycle

    /** Advances one game tick: regeneration, countdowns and state transitions. */
    public void tick() {
        if (state == CoreState.DESTROYED) {
            return;
        }

        // The gauge refills in every state where the core is intact and on its feet.
        // Notably this includes while taking fire — that race between incoming burst and
        // regeneration is the central tension of a fight.
        if (state != CoreState.CUBE && state != CoreState.DOWNED) {
            gauge.tick();
        }
        if (invulnerableTicks > 0) {
            invulnerableTicks--;
        }

        switch (state) {
            case CUBE -> {
                cubeDrop.tick();
                if (cubeDrop.isComplete()) {
                    deploy();
                }
            }
            case LANDING -> {
                if (--stateTicks <= 0) {
                    state = CoreState.NORMAL;
                }
            }
            case DOWNED -> {
                if (--stateTicks <= 0) {
                    // Standing up restores the gauge and buys a moment of safety, so a
                    // knockdown resets the exchange instead of snowballing into a loss.
                    gauge.refill();
                    state = CoreState.REBIRTH;
                    stateTicks = Rules.REBIRTH_TICKS;
                    invulnerableTicks = Rules.REBIRTH_TICKS;
                }
            }
            case REBIRTH -> {
                if (--stateTicks <= 0) {
                    state = CoreState.NORMAL;
                }
            }
            default -> {
                // NORMAL and AIRBORNE have no timer of their own.
            }
        }
    }

    private void deploy() {
        state = CoreState.NORMAL;
        airActionsUsed = 0;
    }

    // ---------------------------------------------------------------- damage

    /**
     * Resolves a hit from an attacker's weapon.
     *
     * @param weapon         the gun being fired
     * @param band           separation between attacker and this core
     * @param attackerStance whether the attacker was grounded or airborne when firing
     */
    public DamageResult applyHit(WeaponSpec weapon, RangeBand band, Stance attackerStance) {
        if (state == CoreState.DESTROYED) {
            return new DamageResult(DamageResult.Outcome.ALREADY_DESTROYED, 0, false, hp);
        }
        if (!weapon.reaches(band, attackerStance)) {
            return new DamageResult(DamageResult.Outcome.OUT_OF_RANGE, 0, false, hp);
        }
        if (isInvulnerable()) {
            return new DamageResult(DamageResult.Outcome.IMMUNE, 0, false, hp);
        }

        // A cube that gets shot unfolds immediately rather than riding out its countdown.
        if (state == CoreState.CUBE) {
            cubeDrop.forceDeploy();
            deploy();
        }

        double scaled = weapon.volleyDamage(band, attackerStance) * loadout.body().defenseFactor();
        if (state == CoreState.DOWNED) {
            scaled *= Rules.DOWNED_DAMAGE_MULTIPLIER;
        }
        int dealt = (int) Math.max(0, Math.round(scaled));
        hp = Math.max(0, hp - dealt);

        boolean causedKnockdown = false;
        if (state != CoreState.DOWNED) {
            causedKnockdown = gauge.applyKnockdown(weapon.knockdownPower());
            if (causedKnockdown) {
                enterDowned();
            }
        }

        if (hp == 0) {
            state = CoreState.DESTROYED;
            stateTicks = 0;
        }

        return new DamageResult(DamageResult.Outcome.HIT, dealt, causedKnockdown, hp);
    }

    private void enterDowned() {
        state = CoreState.DOWNED;
        stateTicks = Rules.DOWNED_TICKS;
        airActionsUsed = 0;
    }

    // ---------------------------------------------------------------- movement

    /**
     * Spends one air action — a dash or an air jump, depending on the legs.
     *
     * @return false if the core is out of air actions or cannot act right now
     */
    public boolean consumeAirAction() {
        if (!state.acceptsInput() || airActionsRemaining() <= 0) {
            return false;
        }
        airActionsUsed++;
        state = CoreState.AIRBORNE;
        return true;
    }

    /**
     * Touches down, refreshing the air budget and paying the recovery this landing earned.
     *
     * <p>Refreshing only here is what makes the budget meaningful: air actions are not a
     * meter that trickles back, they are a fixed allowance you get again by committing to
     * the floor and eating the recovery.
     */
    public void land(LandingKind kind) {
        if (state == CoreState.DESTROYED || state == CoreState.DOWNED
                || state == CoreState.CUBE) {
            return;
        }
        airActionsUsed = 0;
        int recovery = loadout.legs().landingTicks(kind);
        if (recovery > 0) {
            state = CoreState.LANDING;
            stateTicks = recovery;
        } else {
            state = CoreState.NORMAL;
        }
    }

    public int airActionsRemaining() {
        return Math.max(0, loadout.totalAirActions() - airActionsUsed);
    }

    // ---------------------------------------------------------------- queries

    public Loadout loadout() {
        return loadout;
    }

    public KnockdownGauge gauge() {
        return gauge;
    }

    public CubeDrop cubeDrop() {
        return cubeDrop;
    }

    public int hp() {
        return hp;
    }

    public CoreState state() {
        return state;
    }

    /** Ticks left in whichever timed state the core is in, or 0 if it is untimed. */
    public int stateTicksRemaining() {
        return stateTicks;
    }

    /** Whether the core is currently ignoring damage. */
    public boolean isInvulnerable() {
        return invulnerableTicks > 0;
    }

    public int invulnerableTicksRemaining() {
        return invulnerableTicks;
    }

    /** Which row of a weapon's damage table this core fires from right now. */
    public Stance stance() {
        return state == CoreState.AIRBORNE ? Stance.AIR : Stance.GROUND;
    }

    public boolean isAlive() {
        return state != CoreState.DESTROYED;
    }
}
