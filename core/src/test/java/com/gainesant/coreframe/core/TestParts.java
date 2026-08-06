package com.gainesant.coreframe.core;

/**
 * Part fixtures for the rules tests.
 *
 * <p>These live in test sources on purpose. The shipped part catalogue will be datapack
 * JSON so it can be balanced and extended without recompiling, and putting a Java
 * catalogue in main sources would quietly establish the wrong pipeline.
 *
 * <p>The numbers are chosen to make behaviour legible rather than to be balanced.
 */
final class TestParts {

    private TestParts() {
    }

    /** Middleweight all-rounder: two air actions, no damage modifier. */
    static final BodySpec STANDARD_BODY = new BodySpec(
            "Standard", 100, 1.0, 1.0, 0.22, 2, 1.0);

    /** Tanky: bigger gauge, takes less damage, slower, bigger cube. */
    static final BodySpec HEAVY_BODY = new BodySpec(
            "Heavy", 160, 0.8, 0.75, 0.16, 2, 1.4);

    /** Air specialist: three air actions, fragile. */
    static final BodySpec FLYER_BODY = new BodySpec(
            "Flyer", 70, 1.2, 1.30, 0.24, 3, 0.85);

    static final LegsSpec STANDARD_LEGS = LegsSpec.builder("Standard").build();

    /** The archetypal "+1" part: its entire identity is one more air action. */
    static final LegsSpec PLUS_ONE_LEGS = LegsSpec.builder("Plus One")
            .bonusAirActions(1)
            .build();

    /** Trades jump height for a much cheaper air-dash landing. */
    static final LegsSpec GROUND_LEGS = LegsSpec.builder("Ground")
            .jumpPower(0.40)
            .landing(LandingKind.AFTER_AIRDASH, 4)
            .build();

    /**
     * Flat damage at every band, reaches everywhere. Useful when a test wants to isolate
     * something other than the range table.
     */
    static final WeaponSpec FLAT_GUN = new WeaponSpec(
            "Flat", 1,
            DamageTable.builder().ground(50, 50, 50, 50).air(50, 50, 50, 50).build(),
            0.2, 2.0, 4, 10, 10);

    /** Hits hardest in your face and cannot reach far at all. */
    static final WeaponSpec SHOTGUN = new WeaponSpec(
            "Shotgun", 1,
            DamageTable.builder()
                    .ground(142, 110, 73, DamageTable.NO_REACH)
                    .groundOnly()
                    .build(),
            0.0, 3.0, 12, 24, 34);

    /** Weakest point-blank, strongest at range as the spread converges. */
    static final WeaponSpec GATLING = new WeaponSpec(
            "Gatling", 8,
            DamageTable.builder().ground(62, 69, 88, 109).air(69, 92, 109, 109).build(),
            0.1, 2.5, 8, 16, 6);

    /** Tracks relentlessly, travels slowly. The other end of the tradeoff from Shotgun. */
    static final WeaponSpec HOMING = new WeaponSpec(
            "Homing", 1,
            DamageTable.builder().ground(60, 60, 60, 60).air(60, 60, 60, 60).build(),
            0.9, 0.4, 6, 30, 8);

    /** Floors a standard body in exactly two volleys with no regeneration in between. */
    static final WeaponSpec HEAVY_KNOCKDOWN = new WeaponSpec(
            "Slammer", 1,
            DamageTable.builder().ground(40, 40, 40, 40).air(40, 40, 40, 40).build(),
            0.0, 2.0, 10, 20, 50);

    static Loadout standardLoadout() {
        return new Loadout(STANDARD_BODY, STANDARD_LEGS, FLAT_GUN);
    }

    /** A core that has finished its drop-in and is standing, ready to be tested on. */
    static CombatState deployed(Loadout loadout) {
        CombatState core = new CombatState(loadout);
        while (core.state() == CoreState.CUBE) {
            core.tick();
        }
        return core;
    }

    static CombatState deployed() {
        return deployed(standardLoadout());
    }

    static void tick(CombatState core, int times) {
        for (int i = 0; i < times; i++) {
            core.tick();
        }
    }
}
