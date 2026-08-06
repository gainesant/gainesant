plugins {
    java
}

allprojects {
    group = property("modGroup") as String
    version = property("modVersion") as String

    repositories {
        mavenCentral()
    }
}
