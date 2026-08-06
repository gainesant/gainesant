package com.gainesant.coreframe.core;

/**
 * Whether the shooter had its feet on the floor when it pulled the trigger.
 *
 * <p>Stance is not a modifier applied to a shot — it selects a different row of the
 * weapon's damage table outright, and in the original it often selects a different
 * firing pattern too (one gun's ground shot travels vertically while its air shot
 * travels horizontally). Treating it as a first-class axis rather than an airborne
 * penalty is what makes jumping a tactical choice instead of a strictly worse option.
 */
public enum Stance {
    GROUND,
    AIR
}
