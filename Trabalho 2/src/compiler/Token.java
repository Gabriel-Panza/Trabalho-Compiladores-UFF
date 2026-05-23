package compiler;

public final class Token {
    public final TokenType type;
    public final String text;
    public final SourceSpan span;

    public Token(TokenType type, String text, SourceSpan span) {
        this.type = type;
        this.text = text;
        this.span = span;
    }

    public boolean isAtom() {
        return type == TokenType.INTEGER
                || type == TokenType.FLOAT
                || type == TokenType.STRING
                || type == TokenType.BOOLEAN
                || type == TokenType.IDENTIFIER;
    }
}
