import java.security.MessageDigest
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
    jacoco
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.nightbeam"
version = "1.5.2"

val paperApi = "io.papermc.paper:paper-api:26.3.build.49-alpha"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly(paperApi)
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
        exclude(group = "org.bukkit", module = "craftbukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("com.zaxxer:HikariCP:6.3.3")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.4")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    testCompileOnly(paperApi)
    testRuntimeOnly(paperApi)
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

configurations.matching { it.isCanBeResolved }.configureEach {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}
tasks.processResources {
    filesMatching("plugin.yml") { expand("version" to project.version) }
}
tasks.test { useJUnitPlatform() }
tasks.jar { isPreserveFileTimestamps = false; isReproducibleFileOrder = true }
tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    relocate("com.zaxxer.hikari", "com.nightbeam.donutshards.lib.hikari")
    relocate("org.spongepowered.configurate", "com.nightbeam.donutshards.lib.configurate")
    relocate("org.yaml.snakeyaml", "com.nightbeam.donutshards.lib.snakeyaml")
    relocate("org.bstats", "com.nightbeam.donutshards.lib.bstats")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val releaseJarName = "DonutShards-${project.version}-paper-folia-mc1.20.1-26.3.jar"

val release by tasks.registering(Copy::class) {
    dependsOn(tasks.shadowJar, tasks.test)
    from(tasks.shadowJar) { rename { releaseJarName } }
    into(layout.buildDirectory.dir("release"))
    from(listOf("README.md", "INSTALLATION.md", "COMMANDS.md", "PERMISSIONS.md", "CONFIGURATION.md", "API.md", "FOLIA_COMPATIBILITY.md", "TESTING.md", "CHANGELOG.md", "PATCH_NOTES.md", "LICENSE", "NOTICE", "CURSEFORGE.md", "MODRINTH.md"))
    from("src/main/resources") { include("*.yml"); into("config-defaults") }
    from("release") { include("supported-minecraft.json") }
    doLast {
        val jar = destinationDir.resolve(releaseJarName)
        val hash = MessageDigest.getInstance("SHA-256").digest(jar.readBytes()).joinToString("") { "%02x".format(it) }
        destinationDir.resolve("SHA256SUMS.txt").writeText("$hash  ${jar.name}\n", Charsets.US_ASCII)
        val releasesDir = layout.projectDirectory.dir("releases").asFile
        releasesDir.mkdirs()
        jar.copyTo(releasesDir.resolve(releaseJarName), overwrite = true)
    }
}

val verifySourceSafety by tasks.registering {
    doLast {
        val forbidden = listOf("Bukkit" + "Scheduler", "Bukkit" + "Runnable", "com.nightbeam." + "astralshards")
        val violations = fileTree("src") { include("**/*.java", "**/*.yml") }.files.flatMap { file ->
            forbidden.filter { file.readText().contains(it) }.map { "${file.relativeTo(projectDir)} contains $it" }
        }
        check(violations.isEmpty()) { violations.joinToString("\n") }
    }
}
release { dependsOn(verifySourceSafety) }

tasks.build { dependsOn(tasks.shadowJar) }
