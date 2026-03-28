package com.github.narmtal.jarshaker;

import java.io.PrintStream;

public final class ResultPrinter implements ReportWriter {

    private final PrintStream out;
    private final boolean verbose;

    public ResultPrinter(PrintStream out, boolean verbose) {
        this.out = out;
        this.verbose = verbose;
    }

    public static ResultPrinter summary() {
        return new ResultPrinter(System.out, false);
    }

    public static ResultPrinter verbose() {
        return new ResultPrinter(System.out, true);
    }

    @Override
    public void write(Result result) {
        if (verbose) {
            writeFull(result);
        } else {
            writeSummary(result);
        }
    }

    private void writeSummary(Result result) {
        out.printf("  Total:       %d classes%n", result.total());
        out.printf("  Reachable:   %d classes%n", result.reachable().size());
        out.printf("  Unreachable: %d classes%n", result.unreachable().size());
        out.printf("  Retention:   %.1f%%%n", result.retentionRatio() * 100);
    }

    private void writeFull(Result result) {
        writeSummary(result);
        out.println();
        out.println("--- REACHABLE ---");
        result.reachable().stream().sorted().forEach(c -> out.println("  " + c));
        out.println();
        out.println("--- UNREACHABLE ---");
        result.unreachable().stream().sorted().forEach(c -> out.println("  " + c));
    }
}
