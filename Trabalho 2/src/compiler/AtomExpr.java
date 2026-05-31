package compiler;

public final class AtomExpr extends Expr {
    public final TokenType type;
    public final String text;

    public AtomExpr(Token token) {
        super(token.span);
        this.type = token.type;
        this.text = token.text;
    }

    @Override
    public String preview() {
        return text;
    }
}
