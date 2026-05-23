package compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CompilerException extends Exception {
    private final List<Diagnostic> diagnostics;

    public CompilerException(Diagnostic diagnostic) {
        this(Collections.singletonList(diagnostic));
    }

    public CompilerException(List<Diagnostic> diagnostics) {
        super("Compilation failed");
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
