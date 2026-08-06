package com.gainesant.coreframe.core;

/**
 * Weapons author their damage across discrete range bands rather than along a
 * continuous falloff curve.
 *
 * <p>This matters more than it looks. A curve can only ever decay, but a band table can
 * do whatever it likes: a shotgun peaks in your face and cannot reach {@link #FAR} at
 * all, while a gatling is weakest point-blank and strongest at distance because more of
 * its spread converges on target. Bands are what make those shapes expressible.
 */
public enum RangeBand {

    /** Physically touching. */
    MELEE(1.5),

    /** Roughly one core-width away. */
    CLOSE(5.0),

    MEDIUM(12.0),

    /** Everything beyond {@link #MEDIUM}. */
    FAR(Double.POSITIVE_INFINITY);

    private final double maxDistance;

    RangeBand(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    /** Exclusive upper bound of this band, in blocks. */
    public double maxDistance() {
        return maxDistance;
    }

    /** Classifies a separation between two cores into the band it falls in. */
    public static RangeBand fromDistance(double distance) {
        if (distance < 0.0) {
            throw new IllegalArgumentException("distance must not be negative: " + distance);
        }
        for (RangeBand band : values()) {
            if (distance < band.maxDistance) {
                return band;
            }
        }
        return FAR;
    }
}
