/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    java
    `maven-publish`
    kotlin("multiplatform") version "1.5.0"
    id("org.openjfx.javafxplugin") version "0.0.10"
    kotlin("kapt") version "1.5.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://builds.archive.org/maven2/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation("info.picocli:picocli:4.2.0")
    implementation("org.mozilla:juniversalchardet:1.0.3")
    implementation("org.lz4:lz4-pure-java:1.7.0")
    implementation("org.openjfx:javafx-controls:15")
    implementation("org.openjfx:javafx-swing:15")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.0")
    implementation("org.jetbrains:annotations:20.1.0")

}
javafx {
    version = "15"
    modules("javafx.controls", "javafx.swing")
}
group = "resaver"
version = "6.0"
description = "FallrimTools ReSaver"
java.sourceCompatibility = JavaVersion.VERSION_1_8

kotlin {
    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    sourceSets {
        //val commonMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf(
            "Main-Class" to "resaver.ReSaver"
        ))
    }
}