import com.adarshr.gradle.testlogger.theme.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    id("com.adarshr.test-logger") version "3.2.0"

    `maven-publish`
}

group = "me.fan87"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-util:9.3")
}

testlogger {
    theme = ThemeType.MOCHA
}

tasks.test {
    dependsOn(":test-source:classes")
    println(project(":test-source").sourceSets.getByName("main").output.classesDirs.singleFile)
    workingDir = project(":test-source").sourceSets.getByName("main").output.classesDirs.singleFile
    useJUnitPlatform()

    testLogging {
//        outputs.upToDateWhen { false }
//        showStandardStreams = true
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}