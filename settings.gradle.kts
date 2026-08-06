pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        // NeoForge's ModDevGradle plugin. Not reachable from every network;
        // see README "Build environment" if this host is blocked for you.
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
    }
}

rootProject.name = "coreframe"

// The combat rules engine. Plain Java, zero Minecraft types, so it builds and
// tests anywhere a JDK exists. This is where the game's actual rules live.
include("core")

// The Minecraft mod. Needs maven.neoforged.net, piston-meta.mojang.com and
// libraries.minecraft.net, plus a Java 25 toolchain.
//
// Some networks block those hosts. Rather than leaving the whole build unusable there,
// this module can be dropped with -PincludeModModule=false so the rules engine still
// compiles and tests. Normal development leaves it on.
val includeModModule = providers.gradleProperty("includeModModule")
    .getOrElse("true")
    .toBoolean()

if (includeModModule) {
    include("neoforge")
} else {
    logger.lifecycle("Skipping :neoforge — building the rules engine only.")
}
