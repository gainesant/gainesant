package com.gainesant.coreframe.core;

/** Where a core is in its lifecycle during a bout. */
public enum CoreState {

    /** Dropping into the Grid, still folded up and counting down to deployment. */
    CUBE,

    /** Deployed and standing on the floor. */
    NORMAL,

    /** Off the ground, spending its air-action budget. */
    AIRBORNE,

    /** Touched down and rooted for the recovery window its landing earned. */
    LANDING,

    /** Floored. Still takes damage, but at a steep discount. */
    DOWNED,

    /** Back on its feet and briefly untouchable. */
    REBIRTH,

    /** Out of hit points. The bout is over. */
    DESTROYED;

    /** Whether the core can act on player input in this state. */
    public boolean acceptsInput() {
        return this == NORMAL || this == AIRBORNE || this == REBIRTH;
    }
}
