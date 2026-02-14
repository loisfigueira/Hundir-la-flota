plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
    application
}

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
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
