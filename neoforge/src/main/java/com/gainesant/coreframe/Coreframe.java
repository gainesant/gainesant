package com.gainesant.coreframe;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint.
 *
 * <p>Deliberately thin. The rules that decide how a bout plays live in the {@code core}
 * module as plain Java with no Minecraft types, so they can be unit-tested without
 * launching a client; this class exists to wire that engine into the game, not to hold
 * game logic of its own.
 *
 * <p><strong>UNVERIFIED:</strong> this module has never been compiled — see the README's
 * "Build environment" section.
 */
@Mod(Coreframe.MOD_ID)
public class Coreframe {

    public static final String MOD_ID = "coreframe";

    private static final Logger LOGGER = LoggerFactory.getLogger(Coreframe.class);

    public Coreframe(IEventBus modBus, ModContainer container) {
        LOGGER.info("Coreframe loading");

        // Registries, network payloads, entity types and the arena dimension attach here
        // as the milestones land. Keeping registration explicit rather than annotation-
        // scanned makes the dedicated-server load order easy to reason about, which is
        // where client-only classes usually leak in and crash a server.
    }
}
