package com.gainesant.coreframe.core;

/**
 * A core's chassis. Bodies never differ in hit points — every core has
 * {@link Rules#MAX_HP} — so all of a body's identity lives in the fields below.
 *
 * @param name            display name of the part
 * @param endurance       maximum knockdown gauge; how much burst damage it absorbs before falling
 * @param gaugeRegenPerTick how fast that gauge refills, including while under fire
 * @param defenseFactor   incoming damage multiplier; below 1 is tanky, above 1 is fragile
 * @param groundSpeed     walking speed in blocks per tick
 * @param airActions      air dashes or jumps available before touching down
 * @param cubeSize        hitbox scale during the drop-in; bigger bodies are bigger targets
 */
public record BodySpec(
        String name,
        int endurance,
        double gaugeRegenPerTick,
        double defenseFactor,
        double groundSpeed,
        int airActions,
        double cubeSize
) {

    public BodySpec {
        if (endurance < 1) {
            throw new IllegalArgumentException(name + ": endurance must be at least 1");
        }
        if (gaugeRegenPerTick < 0.0) {
            throw new IllegalArgumentException(name + ": gauge regen must not be negative");
        }
        if (defenseFactor <= 0.0) {
            throw new IllegalArgumentException(name + ": defense factor must be positive");
        }
        if (groundSpeed <= 0.0) {
            throw new IllegalArgumentException(name + ": ground speed must be positive");
        }
        if (airActions < 0) {
            throw new IllegalArgumentException(name + ": air actions must not be negative");
        }
        if (cubeSize <= 0.0) {
            throw new IllegalArgumentException(name + ": cube size must be positive");
        }
    }
}
