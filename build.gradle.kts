plugins {
    kotlin("jvm") version "2.1.20"
    java
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.lincheck:lincheck:3.0-alpha")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/concurrency-tools/maven")
    }}

sourceSets.main {
    java.srcDir("src")
}

sourceSets.test {
    java.srcDir("test")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    test {
        maxHeapSize = "10g"
        jvmArgs("-XX:+EnableDynamicAgentLoading")
    }
}
