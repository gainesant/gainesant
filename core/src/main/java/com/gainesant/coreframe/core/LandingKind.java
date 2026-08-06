package com.gainesant.coreframe.core;

/**
 * Why a core is touching down, which decides how long it is rooted on arrival.
 *
 * <p>Splitting landing recovery three ways is what gives the movement system its risk.
 * Stepping off a ledge costs almost nothing, an ordinary jump costs a little, and an air
 * dash costs enough to be punished — so committing to a dash is a real decision rather
 * than free repositioning.
 */
public enum LandingKind {

    /** Dropped off a small ledge without jumping. */
    SHORT_DROP,

    /** Came down from an ordinary jump. */
    NORMAL_JUMP,

    /** Came down out of an air dash. This is the punish window. */
    AFTER_AIRDASH
}
