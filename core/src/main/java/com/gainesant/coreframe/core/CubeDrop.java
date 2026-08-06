package com.gainesant.coreframe.core;

import java.util.random.RandomGenerator;

/**
 * The drop-in: eight-odd seconds where you are a falling cube with a countdown, and the
 * only things you control are where you steer and how fast you mash.
 *
 * <p>The countdown is drawn as a die face, so who deploys first — and therefore who gets
 * to shoot first — is partly luck and partly how hard the player works for it. Being shot
 * while still folded up forces an immediate deployment, which is the counterplay against
 * an opponent who rolled well.
 */
public final class CubeDrop {

    private final int face;
    private int ticksRemaining;

    public CubeDrop(int face) {
        if (face < Rules.CUBE_MIN_FACE || face > Rules.CUBE_MAX_FACE) {
            throw new IllegalArgumentException(
                    "cube face must be " + Rules.CUBE_MIN_FACE + ".." + Rules.CUBE_MAX_FACE
                            + ", got " + face);
        }
        this.face = face;
        this.ticksRemaining = face * Rules.TICKS_PER_SECOND;
    }

    public static CubeDrop roll(RandomGenerator random) {
        return new CubeDrop(random.nextInt(Rules.CUBE_MIN_FACE, Rules.CUBE_MAX_FACE + 1));
    }

    public void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    /** One button press during the descent. Mashing is mechanically load-bearing here. */
    public void mash() {
        ticksRemaining = Math.max(0, ticksRemaining - Rules.CUBE_MASH_TICKS_PER_INPUT);
    }

    /** Taking a hit while still a cube snaps the core straight into deployment. */
    public void forceDeploy() {
        ticksRemaining = 0;
    }

    public boolean isComplete() {
        return ticksRemaining <= 0;
    }

    public int ticksRemaining() {
        return ticksRemaining;
    }

    /** The rolled face, 1-6. Lower deploys sooner. */
    public int face() {
        return face;
    }
}
