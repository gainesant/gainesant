plugins {
    `java-library`
}

java {
    // Deliberately lower than the mod's Java 25. The rules engine is the part
    // worth being able to build and test everywhere, including on toolchains
    // that predate the Minecraft 26.x requirement.
    toolchain.languageVersion = JavaLanguageVersion.of((property("coreJavaVersion") as String).toInt())
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}
