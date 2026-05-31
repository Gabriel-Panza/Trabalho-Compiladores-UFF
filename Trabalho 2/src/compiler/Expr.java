package compiler;

public abstract class Expr {
    public final SourceSpan span;

    protected Expr(SourceSpan span) {
        this.span = span;
    }

    public abstract String preview();
}
