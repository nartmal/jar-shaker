package com.github.narmtal.jarshaker;

import java.io.IOException;

public interface ReportWriter {
    void write(Result result) throws IOException;
}
