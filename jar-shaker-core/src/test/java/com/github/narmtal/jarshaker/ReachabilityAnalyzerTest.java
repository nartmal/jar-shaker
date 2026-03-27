package com.github.narmtal.jarshaker;

import com.github.narmtal.jarshaker.testing.InMemoryJar;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReachabilityAnalyzerTest {

    // -------------------------------------------------------------------------
    // Basic reachability
    // -------------------------------------------------------------------------

    @Test
    void classWithNoReferencesIsUnreachable() throws IOException {
        InMemoryJar entry = InMemoryJar.builder()
                .addClass("com.example.App").done()
                .build();

        InMemoryJar dep = InMemoryJar.builder()
                .addClass("com.example.Orphan").done()
                .build();

        ReachabilityAnalyzer.Result result = analyze(entry, dep);

        assertThat(result.unreachable()).contains("com.example.Orphan");
        assertThat(result.reachable()).doesNotContain("com.example.Orphan");
    }

    @Test
    void directFieldReferenceIsReachable() throws IOException {
        InMemoryJar entry = InMemoryJar.builder()
                .addClass("com.example.App")
                    .field("com.example.Service", "service")
                    .done()
                .build();

        InMemoryJar dep = InMemoryJar.builder()
                .addClass("com.example.Service").done()
                .build();

        ReachabilityAnalyzer.Result result = analyze(entry, dep);

        assertThat(result.reachable()).contains("com.example.Service");
        assertThat(result.unreachable()).doesNotContain("com.example.Service");
    }

    @Test
    void transitiveDepIsReachable() throws IOException {
        // App -> Service -> Repository
        // App only directly references Service, but Repository should still be reachable.
        InMemoryJar entry = InMemoryJar.builder()
                .addClass("com.example.App")
                    .field("com.example.Service", "service")
                    .done()
                .build();

        InMemoryJar dep = InMemoryJar.builder()
                .addClass("com.example.Service")
                    .field("com.example.Repository", "repo")
                    .done()
                .addClass("com.example.Repository").done()
                .build();

        ReachabilityAnalyzer.Result result = analyze(entry, dep);

        assertThat(result.reachable()).contains("com.example.Service", "com.example.Repository");
    }

    // -------------------------------------------------------------------------
    // Partial dep JAR — the core jar-shaker scenario
    // -------------------------------------------------------------------------

    @Test
    void unreferencedClassInDepJarIsUnreachable() throws IOException {
        // The dep JAR contains both a used and an unused class.
        // Only the used one should end up reachable.
        InMemoryJar entry = InMemoryJar.builder()
                .addClass("com.example.App")
                    .field("com.example.Used", "used")
                    .done()
                .build();

        InMemoryJar dep = InMemoryJar.builder()
                .addClass("com.example.Used").done()
                .addClass("com.example.Unused").done()
                .build();

        ReachabilityAnalyzer.Result result = analyze(entry, dep);

        assertThat(result.reachable()).contains("com.example.Used");
        assertThat(result.unreachable()).contains("com.example.Unused");
    }

    @Test
    void entryClassesAreAlwaysReachable() throws IOException {
        // Entry classes have no outbound references but must never appear as unreachable.
        InMemoryJar entry = InMemoryJar.builder()
                .addClass("com.example.App").done()
                .addClass("com.example.AnotherEntryClass").done()
                .build();

        ReachabilityAnalyzer.Result result = ReachabilityAnalyzer.analyze(
                List.of(entry.toInputStream()), List.of());

        assertThat(result.reachable()).contains("com.example.App", "com.example.AnotherEntryClass");
        assertThat(result.unreachable()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static ReachabilityAnalyzer.Result analyze(InMemoryJar entry, InMemoryJar dep)
            throws IOException {
        return ReachabilityAnalyzer.analyze(
                List.of(entry.toInputStream()),
                List.of(dep.toInputStream()));
    }
}
