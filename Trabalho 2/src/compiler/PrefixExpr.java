package compiler;

public final class PrefixExpr extends Expr {
    public final TokenType prefix;
    public final String text;
    public final Expr value;

    public PrefixExpr(Token prefixToken, Expr value) {
        super(prefixToken.span.until(value.span));
        this.prefix = prefixToken.type;
        this.text = prefixToken.text;
        this.value = value;
    }

    @Override
    public String preview() {
        return text + value.preview();
    }
}
