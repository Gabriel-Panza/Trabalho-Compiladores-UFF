package compiler;

public final class SourceSpan {
    public final int startLine;
    public final int startColumn;
    public final int endLine;
    public final int endColumn;

    public SourceSpan(int startLine, int startColumn, int endLine, int endColumn) {
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }

    public static SourceSpan at(int line, int column) {
        return new SourceSpan(line, column, line, column);
    }

    public SourceSpan until(SourceSpan other) {
        return new SourceSpan(startLine, startColumn, other.endLine, other.endColumn);
    }
}
