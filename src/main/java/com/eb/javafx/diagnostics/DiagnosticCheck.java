package com.eb.javafx.diagnostics;

import java.util.List;

/** Produces reusable diagnostic problems for one subsystem. */
@FunctionalInterface
public interface DiagnosticCheck {
    List<DiagnosticProblem> run();
}
