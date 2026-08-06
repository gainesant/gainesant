package com.gainesant.coreframe.core;

/**
 * The second health bar, and the one that actually decides fights.
 *
 * <p>Hit points only ever go down, so on their own they produce a war of attrition. This
 * gauge refills continuously — <em>including while its owner is being shot</em> — which
 * turns every exchange into a race between burst and regeneration. Landing four hits in a
 * second floors the opponent; landing the same four hits over ten seconds does not. That
 * asymmetry is the whole game, so the regeneration deliberately is not suppressed on hit.
 */
public final class KnockdownGauge {

    private final int max;
    private final double regenPerTick;
    private double current;

    public KnockdownGauge(int max, double regenPerTick) {
        if (max < 1) {
            throw new IllegalArgumentException("gauge maximum must be at least 1, got " + max);
        }
        if (regenPerTick < 0.0) {
            throw new IllegalArgumentException("regen must not be negative, got " + regenPerTick);
        }
        this.max = max;
        this.regenPerTick = regenPerTick;
        this.current = max;
    }

    public static KnockdownGauge forBody(BodySpec body) {
        return new KnockdownGauge(body.endurance(), body.gaugeRegenPerTick());
    }

    /** Advances regeneration by one tick. Safe to call in any state. */
    public void tick() {
        current = Math.min(max, current + regenPerTick);
    }

    /**
     * Applies knockdown damage.
     *
     * @return true if this drained the gauge and the owner should fall
     */
    public boolean applyKnockdown(int power) {
        if (power < 0) {
            throw new IllegalArgumentException("knockdown power must not be negative, got " + power);
        }
        if (isDepleted()) {
            return false;
        }
        current = Math.max(0.0, current - power);
        return isDepleted();
    }

    public boolean isDepleted() {
        return current <= 0.0;
    }

    /** Restores the gauge, as happens when a core finishes standing up. */
    public void refill() {
        current = max;
    }

    public double current() {
        return current;
    }

    public int max() {
        return max;
    }

    /** Gauge fill as a 0-1 fraction, for HUD rendering. */
    public double fraction() {
        return current / max;
    }
}
