import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm").version("1.5.20")
    id("org.jlleitschuh.gradle.ktlint").version("10.0.0")
}

group = "io.github.nealgandhi"
version = "0.1"

repositories {
    mavenCentral()
    maven(url = "https://maven.kotlindiscord.com/repository/maven-public/") {
        name = "Kotlin Discord"
    }
}

dependencies {
    val kordexVersion: String by project
    val slf4jVersion: String by project

    implementation(kotlin("stdlib"))
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:$kordexVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions {
        jvmTarget = "1.8"

        freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
}
