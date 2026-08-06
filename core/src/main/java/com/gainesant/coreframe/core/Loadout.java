package com.gainesant.coreframe.core;

/**
 * The assembled core a player brings to a bout.
 *
 * <p>Only the three parts the rules engine currently reasons about are here. Bombs and
 * pods carry {@link BlastType}s and behave as their own entities, so they join once the
 * projectile layer exists rather than being stubbed in now.
 */
public record Loadout(BodySpec body, LegsSpec legs, WeaponSpec gun) {

    public Loadout {
        if (body == null || legs == null || gun == null) {
            throw new IllegalArgumentException("a loadout needs a body, legs and a gun");
        }
    }

    /**
     * Total air actions before touching down. The body sets the baseline and the legs
     * adjust it, which is why a "+1" legs part is meaningful on any chassis.
     */
    public int totalAirActions() {
        return body.airActions() + legs.bonusAirActions();
    }
}
