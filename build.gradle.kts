plugins {
    java
    id("com.gradleup.shadow") version "8.3.0"
}

group = "com.beautiful.plugin"
version = "1.1.51"

val pluginBaseModules = listOf("library", "paper")
val pluginBaseVersion = "1.7.27"
val shadowGroup = "com.beautiful.plugin.libs"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    for (artifact in pluginBaseModules) {
        implementation("top.mrxiaom.pluginbase:$artifact:$pluginBaseVersion")
    }
    // PacketEvents（网络包拦截模块）
    compileOnly(files("libs/PacketEvents-2.12.1.jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    shadowJar {
        relocate("top.mrxiaom.pluginbase", "$shadowGroup.base")
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().resources.srcDirs) {
            expand(mapOf("version" to version))
            include("plugin.yml", "config.yml")
        }
    }
    build {
        dependsOn(shadowJar)
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}
