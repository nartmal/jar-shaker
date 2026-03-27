package com.github.narmtal.jarshaker;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarShakerPluginTest {

    @TempDir
    File projectDir;

    @Test
    void pluginAppliesSuccessfully() throws IOException {
        Files.writeString(projectDir.toPath().resolve("settings.gradle.kts"), "");
        Files.writeString(projectDir.toPath().resolve("build.gradle.kts"), """
                plugins {
                    id("com.github.narmtal.jarshaker")
                }
                """);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir)
                .withPluginClasspath()
                .withArguments("help")
                .build();

        assertEquals(SUCCESS, result.task(":help").getOutcome());
        assertTrue(result.getOutput().contains("Hello from JarShaker plugin!"));
    }
}
