import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
}

group = "me.fan87"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-util:9.3")
}

tasks.test {
    dependsOn(":test-source:classes")
    println(project(":test-source").sourceSets.getByName("main").output.classesDirs.singleFile)
    workingDir = project(":test-source").sourceSets.getByName("main").output.classesDirs.singleFile
    useJUnitPlatform()

    testLogging {
        outputs.upToDateWhen { false }
        showStandardStreams = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}