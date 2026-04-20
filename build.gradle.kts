import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "cn.touchair"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("com.formdev:flatlaf:3.4")
}



compose.desktop {
    application {
        mainClass = "MainKt"

        buildTypes {
            release {
                proguard {
                    configurationFiles.from("proguard-rules.pro")
                }
            }
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "quick-label"
            packageVersion = "1.0.2"

            windows {
                includeAllModules = true
                iconFile = File("src/main/resources/ic_launcher.ico")
                upgradeUuid = "57ccadbb-a7c2-4d31-9e18-de17e2575144"
            }

            linux {
                iconFile = File("src/main/resources/ic_launcher.png")
            }
        }
    }
}
