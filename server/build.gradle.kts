plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.lfigueira.hundir_la_flota.server.MainKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("HundirLaFlotaServer")
    archiveClassifier.set("")
    archiveVersion.set("1.0.0")
}
