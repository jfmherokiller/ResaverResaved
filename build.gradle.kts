import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Date
import kotlin.collections.listOf
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
/*
 * This file was generated by the Gradle 'init' task.
 */


fun buildTime(): String {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'") // you can change it
    df.timeZone = TimeZone.getTimeZone("UTC")
    return df.format(Date())
}
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
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.0")
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("com.github.ajalt.clikt:clikt:3.1.0")
    implementation("io.github.microutils:kotlin-logging:1.12.5")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.0")
    implementation("com.squareup.okio:okio:3.0.0-alpha.5")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("org.slf4j:slf4j-simple:1.7.5")
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
    options.compilerArgs.addAll(listOf("-Xopt-in=kotlin.RequiresOptIn"))
}
tasks.withType<KotlinCompile> {
}
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf(
            "Main-Class" to "resaver.ReSaver",
            "Implementation-Title" to  project.name,
            "Implementation-Version" to project.version,
            "url" to "https://www.nexusmods.com/skyrimspecialedition/mods/5031",
            "Built-Date" to buildTime(),
            "Implementation-Build" to 35
        ))
    }
}
tasks.withType<ShadowJar> {

}
tasks.withType<Test> {
    minHeapSize="2048m"
    maxHeapSize="4096m"
}
//task copyRuntimeLibs(type: Copy) {
//    into "lib"
//    from configurations.testRuntime - configurations.runtime
//}


tasks.create<Copy>("copyRuntimeLibsAndJar") {
    dependsOn(tasks.withType<ShadowJar>())
    val Libraries by configurations.runtimeClasspath
    into("lib")
    from(Libraries)
    from(tasks.withType<ShadowJar>())
}

