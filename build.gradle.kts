import org.gradle.internal.extensions.core.serviceOf
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.nio.file.Paths
import kotlin.io.path.exists

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
        mainClass = "cn.touchair.quicklabel.MainKt"

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
            packageVersion = "1.0.4"

            windows {
                iconFile = File("src/main/resources/ic_launcher.ico")
                upgradeUuid = "57ccadbb-a7c2-4d31-9e18-de17e2575144"
            }

            linux {
                iconFile = File("src/main/resources/ic_launcher.png")
            }
        }
    }
}

tasks.register("buildReleaseJarForWindows") {
    dependsOn("packageReleaseUberJarForCurrentOS")
    val output = Paths.get("build/app/release")
    if (!output.exists()) {
        output.toFile().mkdirs()
    }
    val runtimeDir = output.resolve("runtime")
    if (!runtimeDir.toFile().exists()) {
        val execOps = serviceOf<ExecOperations>()
        execOps.exec {
            commandLine("cmd.exe", "/c",
                "jlink --add-modules java.base,java.desktop,jdk.compiler --strip-debug --no-header-files --no-man-pages --output $runtimeDir")
        }
    }
    val buildJarsPath = Paths.get("build/compose/jars")
    val jarsFiles = buildJarsPath.toFile().listFiles { file ->
        file.name.endsWith(".jar")
    }

    if (jarsFiles.isNotEmpty()) {
        val jarFile = output.resolve("lib.jar").toFile()
        if (jarFile.exists()) {
            jarFile.delete()
        }
        jarsFiles.first().copyTo(jarFile)
    }
    val launchScript = output.resolve("run.bat").toFile()
    if (!launchScript.exists()) {
        launchScript.createNewFile()
        launchScript.writeText("""
        @echo off
        pushd "%~dp0"
        start "" ".\runtime\bin\javaw.exe" -jar lib.jar
        popd
        exit
    """.trimIndent())
    }
}
