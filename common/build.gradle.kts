plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.1.0"
}

kotlin {
    jvm()
    jvmToolchain(17)
    
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
