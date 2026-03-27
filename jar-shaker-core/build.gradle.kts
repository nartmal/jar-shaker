plugins {
    java
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.github.narmtal.jarshaker.JarShaker"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.vafer:jdependency:2.15")
    testImplementation("com.squareup:javapoet:1.13.0")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
