plugins {
    `java-gradle-plugin`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":jar-shaker-core"))
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    plugins {
        create("jarShakerPlugin") {
            id = "com.github.narmtal.jarshaker"
            displayName = "Jar Shaker"
            description = "Removes unused classes and their dependencies from JARs to minimize CVE exposure."
            implementationClass = "com.github.narmtal.jarshaker.JarShakerPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
