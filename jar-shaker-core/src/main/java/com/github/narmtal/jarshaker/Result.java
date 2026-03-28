package com.github.narmtal.jarshaker;

import java.util.Set;

public record Result(Set<String> reachable, Set<String> unreachable) {

    public int total() {
        return reachable.size() + unreachable.size();
    }

    /** Fraction of total classes that are reachable, in [0.0, 1.0]. */
    public double retentionRatio() {
        if (total() == 0) return 0.0;
        return (double) reachable.size() / total();
    }
}
