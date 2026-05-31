package compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class ListExpr extends Expr {
    public final List<Expr> elements;

    public ListExpr(SourceSpan span, List<Expr> elements) {
        super(span);
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements));
    }

    @Override
    public String preview() {
        return "(" + elements.stream().map(Expr::preview).collect(Collectors.joining(" ")) + ")";
    }
}
