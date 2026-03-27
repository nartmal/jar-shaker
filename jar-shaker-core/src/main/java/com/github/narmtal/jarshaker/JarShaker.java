package com.github.narmtal.jarshaker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * CLI entry point.
 *
 * <p>Usage: jar-shaker &lt;entry-jar&gt; [dep-jar...]
 *
 * <p>The first JAR is treated as the entry point (your project's compiled classes).
 * Remaining JARs are dependency JARs to scan for unused classes.
 * Pass --verbose to print the full class list instead of just the summary.
 */
public class JarShaker {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: jar-shaker <entry-jar> [dep-jar...] [--verbose]");
            System.exit(1);
        }

        boolean verbose = false;
        List<String> jarArgs = new java.util.ArrayList<>();
        for (String arg : args) {
            if (arg.equals("--verbose")) {
                verbose = true;
            } else {
                jarArgs.add(arg);
            }
        }

        List<Path> entryJars = List.of(Path.of(jarArgs.get(0)));
        List<Path> depJars = jarArgs.subList(1, jarArgs.size()).stream()
                .map(Path::of)
                .toList();

        System.out.println("Analyzing...");
        System.out.println("  Entry:  " + entryJars);
        System.out.println("  Deps:   " + depJars);
        System.out.println();

        ReachabilityAnalyzer.Result result = ReachabilityAnalyzer.analyze(entryJars, depJars);

        if (verbose) {
            result.printFull();
        } else {
            result.printSummary();
        }
    }
}
