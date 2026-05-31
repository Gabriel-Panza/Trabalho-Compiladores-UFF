package compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Program {
    public final List<Expr> expressions;

    public Program(List<Expr> expressions) {
        this.expressions = Collections.unmodifiableList(new ArrayList<>(expressions));
    }
}
