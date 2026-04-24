plugins {
    java
}

group = "com.mrfloris"
description = "1MB helper plugin that prevents end crystals from damaging blocks"

val pluginVersion = providers.gradleProperty("pluginVersion").get()
val buildNumber = providers.gradleProperty("buildNumber").get()
val javaTarget = providers.gradleProperty("javaTarget").get()
val paperApiVersion = providers.gradleProperty("paperApiVersion").get()
val targetPaperVersion = providers.gradleProperty("targetPaperVersion").get()
val declaredApiVersion = providers.gradleProperty("declaredApiVersion").get()
val targetMinecraftVersion = providers.gradleProperty("targetMinecraftVersion").get()

version = pluginVersion

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaTarget.toInt()))
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(javaTarget.toInt())
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    val properties = mapOf(
        "pluginName" to "1MB-EndCrystals",
        "pluginVersion" to pluginVersion,
        "pluginDescription" to project.description,
        "buildNumber" to buildNumber,
        "javaTarget" to javaTarget,
        "paperApiVersion" to paperApiVersion,
        "targetPaperVersion" to targetPaperVersion,
        "declaredApiVersion" to declaredApiVersion,
        "targetMinecraftVersion" to targetMinecraftVersion,
        "website" to "https://github.com/mrfdev/EndCrystals"
    )

    inputs.properties(properties)
    filteringCharset = "UTF-8"

    filesMatching(listOf("plugin.yml", "build-info.properties")) {
        expand(properties)
    }
}

tasks.jar {
    archiveFileName.set("1MB-EndCrystals-v${pluginVersion}-${buildNumber}-v${javaTarget}-${targetPaperVersion}.jar")

    manifest {
        attributes(
            "Implementation-Title" to "1MB-EndCrystals",
            "Implementation-Version" to pluginVersion,
            "Build-Number" to buildNumber,
            "Target-Paper-Version" to targetPaperVersion,
            "Target-Minecraft-Version" to targetMinecraftVersion,
            "Build-Java-Version" to javaTarget
        )
    }
}

val copyJarToLibs by tasks.registering(Copy::class) {
    dependsOn(tasks.jar)
    from(tasks.jar.flatMap { it.archiveFile })
    into(layout.projectDirectory.dir("libs"))
}

tasks.build {
    dependsOn(copyJarToLibs)
}
