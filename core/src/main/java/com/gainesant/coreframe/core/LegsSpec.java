package com.gainesant.coreframe.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Legs are pure movement. They carry no weapon and deal no damage, but because all
 * evasion in this game happens in the air, they decide more about how a core plays than
 * anything except the body.
 *
 * <p>The three landing-recovery values are the interesting part of the data model: they
 * are what the player is really spending when they choose to dash.
 */
public final class LegsSpec {

    /** Whether this core's air action lifts it or flings it forward. */
    public enum AirActionKind {
        /** A near-horizontal boost. Covers ground, hugs the floor when done low. */
        DASH,
        /** A second (or fourth) jump. Gains height instead of distance. */
        JUMP
    }

    private final String name;
    private final AirActionKind airActionKind;
    private final int bonusAirActions;
    private final Map<LandingKind, Integer> landingTicks;
    private final double jumpPower;
    private final double gravityScale;
    private final double airDashSpeed;

    private LegsSpec(Builder builder) {
        this.name = builder.name;
        this.airActionKind = builder.airActionKind;
        this.bonusAirActions = builder.bonusAirActions;
        this.landingTicks = new EnumMap<>(builder.landingTicks);
        this.jumpPower = builder.jumpPower;
        this.gravityScale = builder.gravityScale;
        this.airDashSpeed = builder.airDashSpeed;
    }

    public String name() {
        return name;
    }

    public AirActionKind airActionKind() {
        return airActionKind;
    }

    /**
     * Air actions added on top of the body's own allowance. The archetypal use is a legs
     * part whose entire identity is "+1", which only makes sense because air actions are
     * a discrete countable budget rather than a meter.
     */
    public int bonusAirActions() {
        return bonusAirActions;
    }

    /** How long the core is rooted after touching down for this reason. */
    public int landingTicks(LandingKind kind) {
        return landingTicks.get(kind);
    }

    public double jumpPower() {
        return jumpPower;
    }

    /** Multiplier on gravity. Below 1 floats, which buys air time at the cost of speed. */
    public double gravityScale() {
        return gravityScale;
    }

    /** Air dash speed is per-legs, not global — swapping legs changes the dash itself. */
    public double airDashSpeed() {
        return airDashSpeed;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private AirActionKind airActionKind = AirActionKind.DASH;
        private int bonusAirActions = 0;
        private final Map<LandingKind, Integer> landingTicks = new EnumMap<>(LandingKind.class);
        private double jumpPower = 0.55;
        private double gravityScale = 1.0;
        private double airDashSpeed = 1.2;

        private Builder(String name) {
            this.name = name;
            landingTicks.put(LandingKind.SHORT_DROP, 2);
            landingTicks.put(LandingKind.NORMAL_JUMP, 5);
            landingTicks.put(LandingKind.AFTER_AIRDASH, 12);
        }

        public Builder airActionKind(AirActionKind kind) {
            this.airActionKind = kind;
            return this;
        }

        public Builder bonusAirActions(int bonus) {
            this.bonusAirActions = bonus;
            return this;
        }

        public Builder landing(LandingKind kind, int ticks) {
            if (ticks < 0) {
                throw new IllegalArgumentException(name + ": landing ticks must not be negative");
            }
            landingTicks.put(kind, ticks);
            return this;
        }

        public Builder jumpPower(double jumpPower) {
            this.jumpPower = jumpPower;
            return this;
        }

        public Builder gravityScale(double gravityScale) {
            this.gravityScale = gravityScale;
            return this;
        }

        public Builder airDashSpeed(double airDashSpeed) {
            this.airDashSpeed = airDashSpeed;
            return this;
        }

        public LegsSpec build() {
            if (bonusAirActions < 0) {
                throw new IllegalArgumentException(name + ": bonus air actions must not be negative");
            }
            if (jumpPower <= 0.0 || gravityScale <= 0.0 || airDashSpeed <= 0.0) {
                throw new IllegalArgumentException(
                        name + ": jump power, gravity scale and air dash speed must all be positive");
            }
            return new LegsSpec(this);
        }
    }
}
