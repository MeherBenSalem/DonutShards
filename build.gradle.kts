import java.security.MessageDigest
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    java
    jacoco
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.nightbeam"
version = "2.0.0"

// The release jar is compiled against the oldest supported API (1.20.1, Java 17 bytecode) so it loads on every
// supported server; compileMatrix compiles the same sources against each newer API to catch removed methods.
val paperApi = "io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT"
val paperApiMatrix = mapOf(
    "1.21.1" to ("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT" to 21),
    "26.1.2" to ("io.papermc.paper:paper-api:26.1.2.build.74-stable" to 25),
    "26.2" to ("io.papermc.paper:paper-api:26.2.build.134-stable" to 25),
    "26.3" to ("io.papermc.paper:paper-api:26.3.build.222-stable" to 25),
)

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
    options.release.set(17)
    options.encoding = "UTF-8"
}

configurations.matching { it.isCanBeResolved }.configureEach {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

val compileMatrix by tasks.registering {
    group = "verification"
    description = "Compiles the plugin against every supported Paper API."
}
paperApiMatrix.forEach { (mc, spec) ->
    val (coordinates, release) = spec
    val id = mc.replace(".", "_")
    val api = configurations.create("paperApi_$id") {
        isCanBeConsumed = false
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
    dependencies.add(api.name, coordinates)
    val task = tasks.register<JavaCompile>("compileAgainst_$id") {
        source = sourceSets.main.get().java
        classpath = api + configurations.compileClasspath.get().filter { !it.name.startsWith("paper-api") }
        destinationDirectory.set(layout.buildDirectory.dir("matrix/$mc"))
        options.release.set(release)
        options.encoding = "UTF-8"
    }
    compileMatrix { dependsOn(task) }
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

val releaseJarName = "donutshards-paper-1.20.1-26.3-${project.version}.jar"

val release by tasks.registering(Copy::class) {
    group = "build"
    description = "Builds the release jar into build/release and releases/ with SHA256SUMS.txt."
    dependsOn(tasks.shadowJar, tasks.test)
    from(tasks.shadowJar) { rename { releaseJarName } }
    into(layout.buildDirectory.dir("release"))
    doLast {
        val jar = destinationDir.resolve(releaseJarName)
        val hash = MessageDigest.getInstance("SHA-256").digest(jar.readBytes()).joinToString("") { "%02x".format(it) }
        destinationDir.resolve("SHA256SUMS.txt").writeText("$hash  ${jar.name}\n", Charsets.US_ASCII)
        val releasesDir = layout.projectDirectory.dir("releases").asFile
        releasesDir.mkdirs()
        jar.copyTo(releasesDir.resolve(releaseJarName), overwrite = true)
        // releases/SHA256SUMS.txt keeps one line per jar in releases/ (the publish workflow checks it).
        val sums = releasesDir.resolve("SHA256SUMS.txt")
        val lines = (if (sums.exists()) sums.readLines() else emptyList())
            .filter { it.isNotBlank() && !it.endsWith("  ${jar.name}") } + "$hash  ${jar.name}"
        sums.writeText(lines.sorted().joinToString("\n", postfix = "\n"), Charsets.US_ASCII)
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
