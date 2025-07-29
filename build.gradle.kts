plugins {
    kotlin("jvm") version "1.8.22"
}

group = "com.github.postyizhan"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    // Vault仓库
    maven("https://nexus.hc.to/content/repositories/pub_releases")
    // PlayerPoints仓库
    maven("https://repo.rosewooddev.io/repository/public/")
    // PlaceholderAPI仓库
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.13-R0.1-SNAPSHOT")
    // Vault API
    compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    // PlayerPoints API (使用反射调用，不需要直接依赖)
    // compileOnly("dev.rosewood:playerpoints:3.2.7")
    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("net.wesjd:anvilgui:1.10.5-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("com.google.code.gson:gson:2.8.9")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    from(configurations.runtimeClasspath.get().map { 
        if (it.isDirectory) it else zipTree(it) 
    })
    
    exclude("META-INF/DEPENDENCIES")
    exclude("META-INF/LICENSE")
    exclude("META-INF/LICENSE.txt")
    exclude("META-INF/license.txt")
    exclude("META-INF/NOTICE")
    exclude("META-INF/NOTICE.txt")
    exclude("META-INF/notice.txt")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
