plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "io.github.mee1080.umasim"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":race"))
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.serializationJson)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

application {
    mainClass.set("io.github.mee1080.umasim.race.cli.RaceCliKt")
}

val fatJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Builds a standalone race CLI jar."
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    configurations.runtimeClasspath.get().forEach { file ->
        from(zipTree(file.absoluteFile))
    }
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
}
