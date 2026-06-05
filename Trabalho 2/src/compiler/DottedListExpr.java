package compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class DottedListExpr extends Expr {
    public final List<Expr> head;
    public final Expr tail;

    public DottedListExpr(SourceSpan span, List<Expr> head, Expr tail) {
        super(span);
        this.head = Collections.unmodifiableList(new ArrayList<>(head));
        this.tail = tail;
    }

    @Override
    public String preview() {
        return "(" + head.stream().map(Expr::preview).collect(Collectors.joining(" "))
                + " . " + tail.preview() + ")";
    }
}
