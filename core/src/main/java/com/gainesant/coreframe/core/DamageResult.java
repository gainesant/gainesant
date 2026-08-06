package com.gainesant.coreframe.core;

/**
 * What came of a single attempt to hit a core.
 *
 * @param outcome         why the hit did or did not land
 * @param damageDealt     hit points actually removed, after every modifier
 * @param causedKnockdown whether this hit emptied the gauge and floored the target
 * @param remainingHp     the target's hit points after the hit
 */
public record DamageResult(
        Outcome outcome,
        int damageDealt,
        boolean causedKnockdown,
        int remainingHp
) {

    public enum Outcome {
        /** Connected and dealt damage. */
        HIT,

        /** The weapon does not reach this band from this stance. */
        OUT_OF_RANGE,

        /** The target was mid-Rebirth and ignored it. */
        IMMUNE,

        /** The target was already out. */
        ALREADY_DESTROYED
    }

    public boolean landed() {
        return outcome == Outcome.HIT;
    }
}
