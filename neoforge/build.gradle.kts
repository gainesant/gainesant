// The Minecraft mod itself.
//
// UNVERIFIED: this module has never been compiled. The environment it was written in
// blocks maven.neoforged.net, piston-meta.mojang.com and libraries.minecraft.net, so
// nothing here could be resolved, let alone built. Treat the plugin and dependency
// versions below as a starting point to confirm on a machine with network access —
// see the README's "Build environment" section.

plugins {
    java
    id("net.neoforged.moddev") version "2.0.107"
}

val modId = property("modId") as String
val modJavaVersion = (property("modJavaVersion") as String).toInt()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(modJavaVersion)
}

neoForge {
    version = property("neoVersion") as String

    // Minecraft ships official parameter names from 26.1 onward, so there is no
    // Parchment mappings block here. Adding one would be a regression, not an upgrade.

    accessTransformers.from("src/main/resources/META-INF/accesstransformer.cfg")

    runs {
        register("client") {
            client()
        }
        register("server") {
            server()
            programArgument("--nogui")
        }
        register("data") {
            clientData()
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    // The rules engine. Everything about how a bout actually plays lives there, in
    // plain Java, so it can be tested without launching a client.
    implementation(project(":core"))

    implementation("software.bernie.geckolib:geckolib-neoforge-${property("minecraftVersion")}:${property("geckolibVersion")}")
}

tasks.withType<ProcessResources>().configureEach {
    val replacements = mapOf(
        "modId" to modId,
        "modName" to property("modName"),
        "modVersion" to project.version,
        "modAuthors" to property("modAuthors"),
        "modLicense" to property("modLicense"),
        "modDescription" to property("modDescription"),
        "minecraftVersion" to property("minecraftVersion"),
        "neoVersion" to property("neoVersion"),
    )
    inputs.properties(replacements)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(replacements)
    }
}

tasks.jar {
    // The rules engine is a plain Java library with no Minecraft types, so folding it
    // into the mod jar is simpler than asking players to install a second file.
    from(project(":core").sourceSets["main"].output)
}
