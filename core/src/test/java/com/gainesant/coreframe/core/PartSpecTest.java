package com.gainesant.coreframe.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the part data model — the numbers that make archetypes distinct. */
class PartSpecTest {

    @Nested
    @DisplayName("range bands")
    class Bands {

        @Test
        @DisplayName("distance classifies into the expected band")
        void classification() {
            assertSame(RangeBand.MELEE, RangeBand.fromDistance(0.0));
            assertSame(RangeBand.MELEE, RangeBand.fromDistance(1.49));
            assertSame(RangeBand.CLOSE, RangeBand.fromDistance(1.5));
            assertSame(RangeBand.CLOSE, RangeBand.fromDistance(4.9));
            assertSame(RangeBand.MEDIUM, RangeBand.fromDistance(5.0));
            assertSame(RangeBand.MEDIUM, RangeBand.fromDistance(11.9));
            assertSame(RangeBand.FAR, RangeBand.fromDistance(12.0));
            assertSame(RangeBand.FAR, RangeBand.fromDistance(1000.0));
        }

        @Test
        @DisplayName("negative distance is rejected rather than silently clamped")
        void negativeDistanceRejected() {
            assertThrows(IllegalArgumentException.class, () -> RangeBand.fromDistance(-1.0));
        }
    }

    @Nested
    @DisplayName("damage tables")
    class Tables {

        @Test
        @DisplayName("a band a weapon cannot reach is distinct from a band it does no damage at")
        void noReachIsNotZeroDamage() {
            DamageTable table = TestParts.SHOTGUN.damage();

            assertTrue(table.reaches(RangeBand.MELEE, Stance.GROUND));
            assertFalse(table.reaches(RangeBand.FAR, Stance.GROUND));
            assertEquals(0, table.damageAt(RangeBand.FAR, Stance.GROUND));
            assertSame(RangeBand.MEDIUM, table.maxReach(Stance.GROUND));
        }

        @Test
        @DisplayName("damage can ramp up with distance, not only decay")
        void tablesExpressNonMonotonicShapes() {
            DamageTable shotgun = TestParts.SHOTGUN.damage();
            assertTrue(shotgun.damageAt(RangeBand.MELEE, Stance.GROUND)
                    > shotgun.damageAt(RangeBand.MEDIUM, Stance.GROUND),
                    "a shotgun should fall off hard");

            DamageTable gatling = TestParts.GATLING.damage();
            assertTrue(gatling.damageAt(RangeBand.FAR, Stance.GROUND)
                    > gatling.damageAt(RangeBand.MELEE, Stance.GROUND),
                    "a gatling should be strongest at range — this is the shape a "
                            + "falloff curve could not express");
        }

        @Test
        @DisplayName("a ground-only weapon reaches nothing while airborne")
        void groundOnlyWeapons() {
            DamageTable table = TestParts.SHOTGUN.damage();
            for (RangeBand band : RangeBand.values()) {
                assertFalse(table.reaches(band, Stance.AIR));
            }
            assertSame(null, table.maxReach(Stance.AIR));
        }

        @Test
        @DisplayName("a table missing a stance row fails loudly at build time")
        void incompleteTableRejected() {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> DamageTable.builder().ground(10, 10, 10, 10).build());
            assertTrue(error.getMessage().contains("AIR"));
        }

        @Test
        @DisplayName("multi-hit weapons multiply their per-projectile damage")
        void volleyDamageAccountsForHitCount() {
            // 8 pellets at 109 apiece at long range.
            assertEquals(872, TestParts.GATLING.volleyDamage(RangeBand.FAR, Stance.GROUND));
            assertEquals(50, TestParts.FLAT_GUN.volleyDamage(RangeBand.FAR, Stance.GROUND));
        }
    }

    @Nested
    @DisplayName("the homing/speed tradeoff")
    class TrackingBudget {

        @Test
        @DisplayName("a fast projectile that also tracks is rejected at construction")
        void fastHomingIsRejected() {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> new WeaponSpec("Impossible", 1,
                            DamageTable.builder().ground(50, 50, 50, 50).air(50, 50, 50, 50).build(),
                            0.9, 3.8, 4, 10, 10));
            assertTrue(error.getMessage().contains("homing/speed tradeoff"));
        }

        @Test
        @DisplayName("the two ends of the tradeoff are both legal")
        void bothExtremesAreAllowed() {
            assertTrue(WeaponSpec.withinTrackingBudget(0.0, Rules.MAX_PROJECTILE_SPEED),
                    "a maximally fast projectile with no tracking is fine");
            assertTrue(WeaponSpec.withinTrackingBudget(1.0, 0.001),
                    "a relentless tracker that crawls is fine");
            assertFalse(WeaponSpec.withinTrackingBudget(0.75, 3.0),
                    "spending more than the whole budget is not");
        }

        @Test
        @DisplayName("the shipped fixtures all respect the budget")
        void fixturesAreLegal() {
            // Constructing them at all would have thrown, so this documents intent as
            // much as it verifies: every gun in the set sits somewhere on the curve.
            assertEquals(0.0, TestParts.SHOTGUN.homing());
            assertEquals(0.9, TestParts.HOMING.homing());
            assertTrue(TestParts.SHOTGUN.speed() > TestParts.HOMING.speed(),
                    "the non-tracking gun must be the faster one");
        }
    }

    @Nested
    @DisplayName("blast types")
    class Blasts {

        @Test
        @DisplayName("traction pulls its target inward instead of away")
        void tractionPulls() {
            assertTrue(BlastType.TRACTION.isTraction());
            assertTrue(BlastType.TRACTION.horizontalPush() < 0.0);

            for (BlastType type : BlastType.values()) {
                if (type != BlastType.TRACTION) {
                    assertFalse(type.isTraction(), type + " should not pull");
                }
            }
        }

        @Test
        @DisplayName("the slow upward vectors are the ones that open a juggle")
        void juggleStarters() {
            assertTrue(BlastType.CYCLONE.canStartJuggle());
            assertTrue(BlastType.GAZER.canStartJuggle());
            assertTrue(BlastType.PILLAR.canStartJuggle());

            assertFalse(BlastType.FLIPPER.canStartJuggle(), "flinging sideways ends a combo");
            assertFalse(BlastType.STUN.canStartJuggle(), "a stun displaces nothing");
        }

        @Test
        @DisplayName("every blast type is reachable by its suffix letter")
        void suffixLookup() {
            for (BlastType type : BlastType.values()) {
                assertSame(type, BlastType.fromSuffix(type.suffix()));
                assertSame(type, BlastType.fromSuffix(Character.toLowerCase(type.suffix())));
            }
            assertThrows(IllegalArgumentException.class, () -> BlastType.fromSuffix('Z'));
        }

        @Test
        @DisplayName("suffix letters are unique")
        void suffixesDoNotCollide() {
            long distinct = java.util.Arrays.stream(BlastType.values())
                    .map(BlastType::suffix)
                    .distinct()
                    .count();
            assertEquals(BlastType.values().length, distinct);
        }
    }

    @Nested
    @DisplayName("the cube drop")
    class Drop {

        @Test
        @DisplayName("a lower roll deploys sooner")
        void lowerFaceDeploysFirst() {
            assertTrue(new CubeDrop(1).ticksRemaining() < new CubeDrop(6).ticksRemaining());
        }

        @Test
        @DisplayName("mashing shortens the countdown")
        void mashingHelps() {
            CubeDrop patient = new CubeDrop(4);
            CubeDrop frantic = new CubeDrop(4);

            for (int i = 0; i < 10; i++) {
                patient.tick();
                frantic.tick();
                frantic.mash();
            }

            assertTrue(frantic.ticksRemaining() < patient.ticksRemaining());
            assertEquals(patient.ticksRemaining() - 10 * Rules.CUBE_MASH_TICKS_PER_INPUT,
                    frantic.ticksRemaining());
        }

        @Test
        @DisplayName("the countdown cannot be mashed below zero")
        void mashingClamps() {
            CubeDrop drop = new CubeDrop(1);
            for (int i = 0; i < 200; i++) {
                drop.mash();
            }
            assertEquals(0, drop.ticksRemaining());
            assertTrue(drop.isComplete());
        }

        @Test
        @DisplayName("only faces one through six exist")
        void faceIsBounded() {
            assertThrows(IllegalArgumentException.class, () -> new CubeDrop(0));
            assertThrows(IllegalArgumentException.class, () -> new CubeDrop(7));
        }
    }

    @Nested
    @DisplayName("part validation")
    class Validation {

        @Test
        @DisplayName("nonsensical part stats are rejected at construction")
        void invalidStatsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> new BodySpec("Bad", 0, 1.0, 1.0, 0.2, 2, 1.0));
            assertThrows(IllegalArgumentException.class,
                    () -> new BodySpec("Bad", 100, 1.0, 0.0, 0.2, 2, 1.0));
            assertThrows(IllegalArgumentException.class,
                    () -> new BodySpec("Bad", 100, 1.0, 1.0, 0.2, -1, 1.0));
            assertThrows(IllegalArgumentException.class,
                    () -> LegsSpec.builder("Bad").landing(LandingKind.SHORT_DROP, -1).build());
            assertThrows(IllegalArgumentException.class,
                    () -> new Loadout(null, TestParts.STANDARD_LEGS, TestParts.FLAT_GUN));
        }

        @Test
        @DisplayName("a gauge cannot be built with a negative regeneration rate")
        void gaugeValidation() {
            assertThrows(IllegalArgumentException.class, () -> new KnockdownGauge(100, -1.0));
            assertThrows(IllegalArgumentException.class, () -> new KnockdownGauge(0, 1.0));
        }
    }
}
