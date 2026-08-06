package com.gainesant.coreframe.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests pin the mechanics that decide whether the game feels right. They are less
 * about defending the code from regressions than about stating, executably, what the
 * design actually is.
 */
class CombatStateTest {

    @Nested
    @DisplayName("hit points")
    class HitPoints {

        @Test
        @DisplayName("every body starts at the same hit points, whatever its stats")
        void hpIsFlatAcrossBodies() {
            assertEquals(Rules.MAX_HP,
                    TestParts.deployed(new Loadout(TestParts.STANDARD_BODY,
                            TestParts.STANDARD_LEGS, TestParts.FLAT_GUN)).hp());
            assertEquals(Rules.MAX_HP,
                    TestParts.deployed(new Loadout(TestParts.HEAVY_BODY,
                            TestParts.STANDARD_LEGS, TestParts.FLAT_GUN)).hp());
            assertEquals(Rules.MAX_HP,
                    TestParts.deployed(new Loadout(TestParts.FLYER_BODY,
                            TestParts.STANDARD_LEGS, TestParts.FLAT_GUN)).hp());
        }

        @Test
        @DisplayName("bodies differentiate through damage reduction, not hit points")
        void defenseFactorScalesIncomingDamage() {
            CombatState heavy = TestParts.deployed(new Loadout(TestParts.HEAVY_BODY,
                    TestParts.STANDARD_LEGS, TestParts.FLAT_GUN));
            CombatState flyer = TestParts.deployed(new Loadout(TestParts.FLYER_BODY,
                    TestParts.STANDARD_LEGS, TestParts.FLAT_GUN));

            int heavyDealt = heavy.applyHit(TestParts.FLAT_GUN, RangeBand.MEDIUM, Stance.GROUND)
                    .damageDealt();
            int flyerDealt = flyer.applyHit(TestParts.FLAT_GUN, RangeBand.MEDIUM, Stance.GROUND)
                    .damageDealt();

            assertTrue(heavyDealt < flyerDealt,
                    "the 0.75x body should take less than the 1.30x body");
            assertEquals(38, heavyDealt);  // 50 * 0.75
            assertEquals(65, flyerDealt);  // 50 * 1.30
        }

        @Test
        @DisplayName("reaching zero hit points destroys the core")
        void zeroHpDestroys() {
            CombatState core = TestParts.deployed();
            while (core.isAlive()) {
                core.applyHit(TestParts.FLAT_GUN, RangeBand.MEDIUM, Stance.GROUND);
                core.tick();
            }
            assertEquals(0, core.hp());
            assertSame(CoreState.DESTROYED, core.state());

            DamageResult afterDeath =
                    core.applyHit(TestParts.FLAT_GUN, RangeBand.MEDIUM, Stance.GROUND);
            assertSame(DamageResult.Outcome.ALREADY_DESTROYED, afterDeath.outcome());
        }
    }

    @Nested
    @DisplayName("the knockdown gauge")
    class Knockdown {

        @Test
        @DisplayName("burst damage floors a core that the same damage spread out does not")
        void regenerationRacesBurst() {
            // Two volleys back to back total 100 knockdown against a 100 gauge.
            CombatState burst = TestParts.deployed();
            burst.applyHit(TestParts.HEAVY_KNOCKDOWN, RangeBand.MEDIUM, Stance.GROUND);
            DamageResult second =
                    burst.applyHit(TestParts.HEAVY_KNOCKDOWN, RangeBand.MEDIUM, Stance.GROUND);

            assertTrue(second.causedKnockdown(), "two immediate volleys should floor a 100 gauge");
            assertSame(CoreState.DOWNED, burst.state());

            // The identical two volleys, with time to regenerate in between, do not.
            CombatState spread = TestParts.deployed();
            spread.applyHit(TestParts.HEAVY_KNOCKDOWN, RangeBand.MEDIUM, Stance.GROUND);
            TestParts.tick(spread, 60);
            DamageResult later =
                    spread.applyHit(TestParts.HEAVY_KNOCKDOWN, RangeBand.MEDIUM, Stance.GROUND);

            assertFalse(later.causedKnockdown(), "60 ticks of regen should have refilled the gauge");
            assertNotEquals(CoreState.DOWNED, spread.state());
        }

        @Test
        @DisplayName("the gauge keeps refilling while the core is under fire")
        void gaugeRegeneratesDuringSustainedDamage() {
            CombatState core = TestParts.deployed();

            // Chip away with a low-knockdown weapon, ticking between hits. Regeneration
            // of 1.0/tick outpaces 10 knockdown every 20 ticks, so the core never falls
            // however long this goes on.
            for (int volley = 0; volley < 20; volley++) {
                core.applyHit(TestParts.FLAT_GUN, RangeBand.MEDIUM, Stance.GROUND);
                TestParts.tick(core, 20);
                if (!core.isAlive()) {
                    break;
                }
            }

            assertNotEquals(CoreState.DOWNED, core.state(),
                    "sustained chip damage should never produce a knockdown");
            assertTrue(core.hp() < Rules.MAX_HP, "it should still have been losing hit points");
        }

        @Test
        @DisplayName("the gauge never overfills")
        void gaugeClampsToMaximum() {
            CombatState core = TestParts.deployed();
            TestParts.tick(core, 500);
            assertEquals(TestParts.STANDARD_BODY.endurance(), core.gauge().current(), 1e-9);
        }
    }

    @Nested
    @DisplayName("downed and Rebirth")
    class DownedAndRebirth {

        private CombatState floored() {
            CombatState core = TestParts.deployed();
            core.applyHit(TestParts.HEAVY_KNOCKDOWN, RangeBand.MEDIUM, Stance.GROUND);
            core.applyHit(TestParts.HEAVY_KNOCKDOWN, RangeBand.MEDIUM, Stance.GROUND);
            assertSame(CoreState.DOWNED, core.state());
            return core;
        }

        @Test
        @DisplayName("a downed core still takes damage, at a steep discount")
        void downedDamageIsReduced() {
            CombatState core = floored();
            int before = core.hp();

            int dealt = core.applyHit(TestParts.FLAT_GUN, RangeBand.MEDIUM, Stance.GROUND)
                    .damageDealt();

            // 50 damage * 1.0 defense * 0.30 downed multiplier.
            assertEquals(15, dealt);
            assertEquals(before - 15, core.hp());
        }

        @Test
        @DisplayName("hitting a downed core cannot knock it down again")
        void downedCoreTakesNoFurtherKnockdown() {
            CombatState core = floored();
            DamageResult result =
                    core.applyHit(TestParts.HEAVY_KNOCKDOWN, RangeBand.MEDIUM, Stance.GROUND);
            assertFalse(result.causedKnockdown());
        }

        @Test
        @DisplayName("standing up restores the gauge and grants three seconds of safety")
        void rebirthGrantsInvulnerability() {
            CombatState core = floored();
            TestParts.tick(core, Rules.DOWNED_TICKS);

            assertSame(CoreState.REBIRTH, core.state());
            assertTrue(core.isInvulnerable());
            assertEquals(TestParts.STANDARD_BODY.endurance(), core.gauge().current(), 1e-9);

            DamageResult blocked =
                    core.applyHit(TestParts.FLAT_GUN, RangeBand.MEDIUM, Stance.GROUND);
            assertSame(DamageResult.Outcome.IMMUNE, blocked.outcome());
            assertEquals(0, blocked.damageDealt());
        }

        @Test
        @DisplayName("the safety window expires after exactly three seconds")
        void rebirthExpires() {
            CombatState core = floored();
            TestParts.tick(core, Rules.DOWNED_TICKS);
            assertTrue(core.isInvulnerable());

            TestParts.tick(core, Rules.REBIRTH_TICKS);

            assertFalse(core.isInvulnerable());
            assertSame(CoreState.NORMAL, core.state());
            assertTrue(core.applyHit(TestParts.FLAT_GUN, RangeBand.MEDIUM, Stance.GROUND).landed());
        }

        @Test
        @DisplayName("dashing out of Rebirth keeps the safety window")
        void invulnerabilitySurvivesLeavingTheRebirthState() {
            CombatState core = floored();
            TestParts.tick(core, Rules.DOWNED_TICKS);

            // Leaving the ground changes the state but must not strip the protection —
            // dashing away the moment you stand up is exactly what the window is for.
            assertTrue(core.consumeAirAction());
            assertSame(CoreState.AIRBORNE, core.state());
            assertTrue(core.isInvulnerable());

            assertSame(DamageResult.Outcome.IMMUNE,
                    core.applyHit(TestParts.FLAT_GUN, RangeBand.MEDIUM, Stance.AIR).outcome());
        }
    }

    @Nested
    @DisplayName("air actions")
    class AirActions {

        @Test
        @DisplayName("the budget comes from the body plus the legs")
        void budgetCombinesBodyAndLegs() {
            assertEquals(2, new Loadout(TestParts.STANDARD_BODY, TestParts.STANDARD_LEGS,
                    TestParts.FLAT_GUN).totalAirActions());
            assertEquals(3, new Loadout(TestParts.STANDARD_BODY, TestParts.PLUS_ONE_LEGS,
                    TestParts.FLAT_GUN).totalAirActions());
            assertEquals(4, new Loadout(TestParts.FLYER_BODY, TestParts.PLUS_ONE_LEGS,
                    TestParts.FLAT_GUN).totalAirActions());
        }

        @Test
        @DisplayName("the budget runs out and does not trickle back in the air")
        void budgetIsExhaustible() {
            CombatState core = TestParts.deployed();

            assertTrue(core.consumeAirAction());
            assertTrue(core.consumeAirAction());
            assertFalse(core.consumeAirAction(), "a two-action body should be spent after two");

            // Hanging in the air does not restore anything, however long you wait.
            TestParts.tick(core, 100);
            assertFalse(core.consumeAirAction());
            assertEquals(0, core.airActionsRemaining());
        }

        @Test
        @DisplayName("only landing refreshes the budget")
        void landingRefreshesBudget() {
            CombatState core = TestParts.deployed();
            core.consumeAirAction();
            core.consumeAirAction();
            assertEquals(0, core.airActionsRemaining());

            core.land(LandingKind.NORMAL_JUMP);

            assertEquals(2, core.airActionsRemaining());
        }
    }

    @Nested
    @DisplayName("landing recovery")
    class LandingRecovery {

        @Test
        @DisplayName("recovery length depends on what the core did in the air")
        void recoveryScalesWithLandingKind() {
            CombatState core = TestParts.deployed();

            core.land(LandingKind.SHORT_DROP);
            assertEquals(2, core.stateTicksRemaining());

            core.land(LandingKind.NORMAL_JUMP);
            assertEquals(5, core.stateTicksRemaining());

            core.land(LandingKind.AFTER_AIRDASH);
            assertEquals(12, core.stateTicksRemaining(),
                    "the air-dash landing is the punish window and must cost the most");
        }

        @Test
        @DisplayName("a core is rooted for the whole recovery, then acts again")
        void recoveryRootsTheCore() {
            CombatState core = TestParts.deployed();
            core.consumeAirAction();
            core.land(LandingKind.AFTER_AIRDASH);

            assertSame(CoreState.LANDING, core.state());
            assertFalse(core.consumeAirAction(), "a landing core cannot act");

            TestParts.tick(core, 12);

            assertSame(CoreState.NORMAL, core.state());
            assertTrue(core.consumeAirAction());
        }

        @Test
        @DisplayName("legs choose how expensive a dash landing is")
        void legsChangeTheDashPenalty() {
            CombatState standard = TestParts.deployed();
            CombatState ground = TestParts.deployed(new Loadout(
                    TestParts.STANDARD_BODY, TestParts.GROUND_LEGS, TestParts.FLAT_GUN));

            standard.land(LandingKind.AFTER_AIRDASH);
            ground.land(LandingKind.AFTER_AIRDASH);

            assertEquals(12, standard.stateTicksRemaining());
            assertEquals(4, ground.stateTicksRemaining());
        }
    }

    @Nested
    @DisplayName("stance")
    class StanceSelection {

        @Test
        @DisplayName("being airborne selects the air row of the damage table")
        void stanceFollowsState() {
            CombatState core = TestParts.deployed();
            assertSame(Stance.GROUND, core.stance());

            core.consumeAirAction();
            assertSame(Stance.AIR, core.stance());

            core.land(LandingKind.NORMAL_JUMP);
            assertSame(Stance.GROUND, core.stance());
        }
    }

    @Nested
    @DisplayName("the drop-in")
    class DropIn {

        @Test
        @DisplayName("a core is a cube until its countdown expires")
        void cubeDeploysOnCountdown() {
            CombatState core = new CombatState(TestParts.standardLoadout(), new CubeDrop(3));
            assertSame(CoreState.CUBE, core.state());

            TestParts.tick(core, 3 * Rules.TICKS_PER_SECOND - 1);
            assertSame(CoreState.CUBE, core.state());

            core.tick();
            assertSame(CoreState.NORMAL, core.state());
        }

        @Test
        @DisplayName("being shot as a cube forces an immediate deployment")
        void beingHitForcesDeployment() {
            CombatState core = new CombatState(TestParts.standardLoadout(), new CubeDrop(6));
            assertSame(CoreState.CUBE, core.state());

            DamageResult result =
                    core.applyHit(TestParts.FLAT_GUN, RangeBand.MEDIUM, Stance.GROUND);

            assertTrue(result.landed(), "a cube is a legitimate target");
            assertSame(CoreState.NORMAL, core.state());
        }
    }
}
