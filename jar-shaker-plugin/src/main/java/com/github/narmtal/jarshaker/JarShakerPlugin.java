package com.github.narmtal.jarshaker;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JarShakerPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        System.out.println("Hello from JarShaker plugin!");
    }
}
