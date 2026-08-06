package com.gainesant.coreframe.core;

/**
 * The handful of global constants that define how a bout feels.
 *
 * <p>These are deliberately gathered in one place rather than scattered across the
 * classes that use them, because balancing the game means changing these numbers and
 * re-reading the tests that pin them.
 */
public final class Rules {

    private Rules() {
    }

    public static final int TICKS_PER_SECOND = 20;

    /**
     * Every core has the same hit points regardless of body. Bodies differentiate
     * themselves through the knockdown gauge and damage reduction, never through HP.
     */
    public static final int MAX_HP = 1000;

    /**
     * Damage dealt to a downed core is scaled by this. It is the single number that
     * stops a knockdown from being a free kill: you may keep hitting a floored
     * opponent, but at a steep discount.
     */
    public static final double DOWNED_DAMAGE_MULTIPLIER = 0.30;

    /** Invulnerability granted on standing up, so getting floored is not a death spiral. */
    public static final int REBIRTH_TICKS = 3 * TICKS_PER_SECOND;

    /** How long a core lies on the floor before it begins standing up. */
    public static final int DOWNED_TICKS = 2 * TICKS_PER_SECOND;

    /** Mashing during the cube drop shaves this many ticks off the countdown per input. */
    public static final int CUBE_MASH_TICKS_PER_INPUT = 2;

    /** Fastest and slowest cube countdown, in seconds, drawn as a 1-6 die face. */
    public static final int CUBE_MIN_FACE = 1;
    public static final int CUBE_MAX_FACE = 6;

    /**
     * Projectile speed ceiling, in blocks per tick. Used to normalise speed when
     * enforcing the homing/speed tradeoff in {@link WeaponSpec}.
     */
    public static final double MAX_PROJECTILE_SPEED = 4.0;

    /**
     * Speed a projectile gets without spending any of its tracking budget.
     *
     * <p>Without this floor the budget makes perfect tracking unreachable: homing of 1.0
     * would demand a speed of exactly zero, and a projectile that never moves is not a
     * weapon. The floor says that below this speed a round is slow enough to dodge on
     * reaction regardless of how hard it tracks, so the relentless-but-crawling archetype
     * is allowed to exist.
     */
    public static final double FREE_TRACKING_SPEED = 0.5;
}
