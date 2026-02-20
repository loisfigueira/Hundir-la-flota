import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    kotlin("plugin.serialization") version "2.1.0"
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.lfigueira.hundir_la_flota.generated.resources"
}

kotlin {
    jvm()
    jvmToolchain(17)
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":common"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.lfigueira.hundir_la_flota.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe, TargetFormat.AppImage)
            packageName = "HundirLaFlota"
            packageVersion = "1.0.0"
            description = "Clásico juego de Hundir la Flota - Cliente"
            copyright = "© 2026 Lois Figueira"
            vendor = "Lois Figueira"
            
            linux {
                shortcut = true
                packageName = "hundir-la-flota"
                menuGroup = "Games"
            }
            windows {
                shortcut = true
                console = false
                dirChooser = true
                packageName = "HundirLaFlota"
            }
        }
    }
}
