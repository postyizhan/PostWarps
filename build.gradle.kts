plugins {
    kotlin("jvm") version "1.8.22"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.github.postyizhan"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.13-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.slf4j:slf4j-simple:1.7.36")
}

val targetJavaVersion = 8
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("kotlin", "com.github.postyizhan.shadow.kotlin")
    relocate("com.zaxxer", "com.github.postyizhan.shadow.zaxxer")
    relocate("org.slf4j", "com.github.postyizhan.shadow.slf4j")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
