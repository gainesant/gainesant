package com.gainesant.coreframe.core;

/**
 * A gun: the parameters that make one archetype feel different from another.
 *
 * <p>Damage is only half the identity. The rest lives in {@code hits} (a twelve-hit drill
 * and a one-hit magnum can total the same damage and play nothing alike), in the recoil
 * that roots you after emptying a clip, and above all in the homing/speed tradeoff
 * enforced by the constructor.
 *
 * @param name           display name of the part
 * @param hits           projectiles per trigger pull
 * @param damage         damage authored per band and stance
 * @param homing         tracking strength, 0 (none) to 1 (relentless)
 * @param speed          projectile speed in blocks per tick
 * @param recoilTicks    ticks the shooter is rooted after emptying the clip
 * @param reloadTicks    ticks before the weapon can fire again
 * @param knockdownPower damage dealt to the target's knockdown gauge, independent of HP
 */
public record WeaponSpec(
        String name,
        int hits,
        DamageTable damage,
        double homing,
        double speed,
        int recoilTicks,
        int reloadTicks,
        int knockdownPower
) {

    public WeaponSpec {
        if (hits < 1) {
            throw new IllegalArgumentException(name + ": hits must be at least 1, got " + hits);
        }
        if (homing < 0.0 || homing > 1.0) {
            throw new IllegalArgumentException(name + ": homing must be in [0,1], got " + homing);
        }
        if (speed <= 0.0 || speed > Rules.MAX_PROJECTILE_SPEED) {
            throw new IllegalArgumentException(
                    name + ": speed must be in (0," + Rules.MAX_PROJECTILE_SPEED + "], got " + speed);
        }
        if (recoilTicks < 0 || reloadTicks < 0) {
            throw new IllegalArgumentException(name + ": recoil and reload must not be negative");
        }
        if (knockdownPower < 0) {
            throw new IllegalArgumentException(name + ": knockdown power must not be negative");
        }
        if (!withinTrackingBudget(homing, speed)) {
            throw new IllegalArgumentException(
                    name + ": violates the homing/speed tradeoff — homing " + homing
                            + " with speed " + speed + " exceeds the budget of 1.0. "
                            + "Fast rounds must not also track.");
        }
    }

    /**
     * Enforces the balance invariant that fast projectiles do not track and tracking
     * projectiles are dodgeable.
     *
     * <p>Without this, the dominant strategy is always "the gun that homes and also
     * arrives instantly", and every other gun in the set becomes decoration. Spending
     * homing and speed out of a shared budget is what keeps the archetypes distinct, so
     * it is checked at construction rather than left to reviewer discipline.
     */
    public static boolean withinTrackingBudget(double homing, double speed) {
        double chargeableSpeed = Math.max(0.0, speed - Rules.FREE_TRACKING_SPEED);
        return homing + (chargeableSpeed / Rules.MAX_PROJECTILE_SPEED) <= 1.0 + 1e-9;
    }

    /** Total damage of a full volley at this band and stance, before any target modifiers. */
    public int volleyDamage(RangeBand band, Stance stance) {
        return damage.damageAt(band, stance) * hits;
    }

    public boolean reaches(RangeBand band, Stance stance) {
        return damage.reaches(band, stance);
    }
}
