package com.github.narmtal.jarshaker;

import org.vafer.jdependency.Clazz;
import org.vafer.jdependency.Clazzpath;
import org.vafer.jdependency.ClazzpathUnit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ReachabilityAnalyzer {

    public record Result(Set<String> reachable, Set<String> unreachable) {

        public void printSummary() {
            System.out.printf("  Reachable:   %d classes%n", reachable.size());
            System.out.printf("  Unreachable: %d classes%n", unreachable.size());
        }

        public void printFull() {
            System.out.println("--- REACHABLE ---");
            reachable.stream().sorted().forEach(c -> System.out.println("  " + c));
            System.out.println("--- UNREACHABLE ---");
            unreachable.stream().sorted().forEach(c -> System.out.println("  " + c));
        }
    }

    public static Result analyze(List<Path> entryJars, List<Path> dependencyJars) throws IOException {
        for (Path p : entryJars) validate(p);
        for (Path p : dependencyJars) validate(p);

        Clazzpath cp = new Clazzpath();

        List<ClazzpathUnit> entryUnits = new ArrayList<>();
        for (Path jar : entryJars) {
            entryUnits.add(cp.addClazzpathUnit(jar));
        }
        for (Path jar : dependencyJars) {
            cp.addClazzpathUnit(jar);
        }

        return computeResult(cp, entryUnits);
    }

    public static Result analyze(List<InputStream> entryStreams, List<InputStream> depStreams) throws IOException {
        Clazzpath cp = new Clazzpath();

        List<ClazzpathUnit> entryUnits = new ArrayList<>();
        for (int i = 0; i < entryStreams.size(); i++) {
            entryUnits.add(cp.addClazzpathUnit(entryStreams.get(i), "entry-" + i));
        }
        for (int i = 0; i < depStreams.size(); i++) {
            cp.addClazzpathUnit(depStreams.get(i), "dep-" + i);
        }

        return computeResult(cp, entryUnits);
    }

    private static Result computeResult(Clazzpath cp, List<ClazzpathUnit> entryUnits) {
        Set<Clazz> reachable = entryUnits.stream()
                .flatMap(unit -> {
                    Set<Clazz> fromUnit = new HashSet<>(unit.getClazzes());
                    fromUnit.addAll(unit.getTransitiveDependencies());
                    return fromUnit.stream();
                })
                .collect(Collectors.toSet());

        Set<String> reachableNames = reachable.stream()
                .map(Clazz::getName)
                .collect(Collectors.toUnmodifiableSet());

        Set<String> unreachableNames = cp.getClazzes().stream()
                .filter(c -> !reachable.contains(c))
                .map(Clazz::getName)
                .collect(Collectors.toUnmodifiableSet());

        return new Result(reachableNames, unreachableNames);
    }

    private static void validate(Path p) {
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("JAR not found: " + p);
        }
        if (!Files.isRegularFile(p)) {
            throw new IllegalArgumentException("Not a file: " + p);
        }
    }
}
