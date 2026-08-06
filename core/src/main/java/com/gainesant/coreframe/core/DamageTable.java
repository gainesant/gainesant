package com.gainesant.coreframe.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * A weapon's damage authored across every {@link RangeBand} and {@link Stance}.
 *
 * <p>Four bands times two stances is eight authored numbers per weapon, plus the ability
 * to declare that a band is simply unreachable. That last part is not a damage of zero —
 * a shotgun does not "do no damage" at long range, it does not connect at all, and the
 * distinction matters to hit detection and to the AI's weapon selection.
 */
public final class DamageTable {

    /** Marks a band the weapon cannot reach at all. */
    public static final int NO_REACH = -1;

    private final Map<Stance, int[]> rows;

    private DamageTable(Map<Stance, int[]> rows) {
        this.rows = rows;
    }

    /**
     * Damage for a single projectile at this band and stance, or {@code 0} if the weapon
     * cannot reach. Callers that need to distinguish "no damage" from "no contact" should
     * ask {@link #reaches} first.
     */
    public int damageAt(RangeBand band, Stance stance) {
        int value = raw(band, stance);
        return value == NO_REACH ? 0 : value;
    }

    /** Whether the weapon connects at all at this band and stance. */
    public boolean reaches(RangeBand band, Stance stance) {
        return raw(band, stance) != NO_REACH;
    }

    private int raw(RangeBand band, Stance stance) {
        return rows.get(stance)[band.ordinal()];
    }

    /** The furthest band this weapon can reach from the given stance, or null if none. */
    public RangeBand maxReach(Stance stance) {
        RangeBand furthest = null;
        for (RangeBand band : RangeBand.values()) {
            if (reaches(band, stance)) {
                furthest = band;
            }
        }
        return furthest;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Stance, int[]> rows = new EnumMap<>(Stance.class);

        /** Damage at melee, close, medium and far range while standing on the ground. */
        public Builder ground(int melee, int close, int medium, int far) {
            return row(Stance.GROUND, melee, close, medium, far);
        }

        /** Damage at melee, close, medium and far range while airborne. */
        public Builder air(int melee, int close, int medium, int far) {
            return row(Stance.AIR, melee, close, medium, far);
        }

        /** Declares the weapon unusable while airborne. */
        public Builder groundOnly() {
            return row(Stance.AIR, NO_REACH, NO_REACH, NO_REACH, NO_REACH);
        }

        private Builder row(Stance stance, int melee, int close, int medium, int far) {
            rows.put(stance, new int[] {melee, close, medium, far});
            return this;
        }

        public DamageTable build() {
            for (Stance stance : Stance.values()) {
                if (!rows.containsKey(stance)) {
                    throw new IllegalStateException(
                            "damage table is missing its " + stance + " row; "
                                    + "call groundOnly() if the weapon cannot fire airborne");
                }
            }
            return new DamageTable(new EnumMap<>(rows));
        }
    }
}
