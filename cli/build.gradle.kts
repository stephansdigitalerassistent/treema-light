plugins {
    application
    kotlin("jvm")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":common"))
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("io.insert-koin:koin-core:4.1.1")
}

application {
    mainClass.set("ch.threema.cli.BridgeMainKt")
}

tasks.withType<JavaExec> {
    systemProperty("jna.library.path", "${project.rootDir}/domain/libthreema/target/release")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}
