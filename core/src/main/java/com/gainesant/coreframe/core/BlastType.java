package com.gainesant.coreframe.core;

/**
 * The launch vector a bomb or pod applies to whatever it hits.
 *
 * <p>This is the mechanism that turns a few dozen projectile behaviours into a few
 * hundred distinct parts: the same bomb carrying a different blast type is a genuinely
 * different tool, because combos here are assembled out of launch vectors rather than
 * out of link timing. A part that floats its target slowly upward opens a juggle; one
 * that drags the target toward you sets up a point-blank weapon that could not otherwise
 * reach.
 *
 * <p>{@code horizontalPush} is expressed relative to the blast's facing, so a negative
 * value pulls the target back toward the attacker — that is the whole of Traction.
 */
public enum BlastType {

    /** Blows slowly upward. The combo starter. */
    CYCLONE('C', 0.00, 0.45, true, false),

    /** Blows upward. */
    GAZER('G', 0.00, 0.85, false, false),

    /** Upward, lingering, tight radius and high force — good for pinning against walls. */
    PILLAR('P', 0.00, 1.10, true, false),

    /** Diagonally upward, lingering. */
    DESTROY('D', 0.45, 0.65, true, false),

    /** Sideways slowly, lingering. */
    BURST('B', 0.35, 0.05, true, false),

    /** Sideways. */
    FLIPPER('F', 0.80, 0.10, false, false),

    /** Sideways slowly. */
    HORIZON('H', 0.35, 0.05, false, false),

    /** Immobilises rather than displacing. */
    STUN('S', 0.00, 0.00, false, true),

    /** Pulls the target toward the attacker. */
    TRACTION('T', -0.70, 0.10, false, false);

    private final char suffix;
    private final double horizontalPush;
    private final double verticalPush;
    private final boolean lingering;
    private final boolean immobilising;

    BlastType(char suffix, double horizontalPush, double verticalPush,
              boolean lingering, boolean immobilising) {
        this.suffix = suffix;
        this.horizontalPush = horizontalPush;
        this.verticalPush = verticalPush;
        this.lingering = lingering;
        this.immobilising = immobilising;
    }

    /** The letter this blast type is identified by in part names. */
    public char suffix() {
        return suffix;
    }

    /** Push along the blast's facing. Negative pulls the target toward the attacker. */
    public double horizontalPush() {
        return horizontalPush;
    }

    public double verticalPush() {
        return verticalPush;
    }

    /** Whether the blast persists as a hazard after detonating. */
    public boolean isLingering() {
        return lingering;
    }

    public boolean isImmobilising() {
        return immobilising;
    }

    /** Whether this blast drags its target inward rather than away. */
    public boolean isTraction() {
        return horizontalPush < 0.0;
    }

    /**
     * Whether this blast can start a juggle. A launch has to lift the target without
     * flinging it out of reach, which is exactly what the slow upward vectors do.
     */
    public boolean canStartJuggle() {
        return verticalPush > 0.0 && horizontalPush <= 0.0;
    }

    public static BlastType fromSuffix(char suffix) {
        char upper = Character.toUpperCase(suffix);
        for (BlastType type : values()) {
            if (type.suffix == upper) {
                return type;
            }
        }
        throw new IllegalArgumentException("no blast type with suffix '" + suffix + "'");
    }
}
