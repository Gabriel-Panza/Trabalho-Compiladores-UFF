package compiler;

public final class Diagnostic {
    public enum Kind {
        LEXICAL("Erro lexico"),
        SYNTAX("Erro sintatico"),
        SEMANTIC("Erro semantico");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public final Kind kind;
    public final SourceSpan span;
    public final String message;

    private Diagnostic(Kind kind, SourceSpan span, String message) {
        this.kind = kind;
        this.span = span;
        this.message = message;
    }

    public static Diagnostic lexical(SourceSpan span, String message) {
        return new Diagnostic(Kind.LEXICAL, span, message);
    }

    public static Diagnostic syntax(SourceSpan span, String message) {
        return new Diagnostic(Kind.SYNTAX, span, message);
    }

    public static Diagnostic semantic(SourceSpan span, String message) {
        return new Diagnostic(Kind.SEMANTIC, span, message);
    }
}
