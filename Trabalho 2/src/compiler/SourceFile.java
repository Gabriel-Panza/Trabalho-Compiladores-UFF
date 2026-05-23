package compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceFile {
    public final Path path;
    public final String text;
    private final String[] lines;

    private SourceFile(Path path, String text) {
        this.path = path;
        this.text = text;
        this.lines = text.split("\\R", -1);
    }

    public static SourceFile read(Path path) throws IOException {
        return new SourceFile(path, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    public String line(int lineNumber) {
        if (lineNumber < 1 || lineNumber > lines.length) {
            return "";
        }
        return lines[lineNumber - 1];
    }

    public String format(Diagnostic diagnostic) {
        StringBuilder builder = new StringBuilder();
        int line = Math.max(1, diagnostic.span.startLine);
        int column = Math.max(1, diagnostic.span.startColumn);
        String sourceLine = line(line);

        builder.append(diagnostic.kind.label())
                .append(" em ")
                .append(path)
                .append(":")
                .append(line)
                .append(":")
                .append(column)
                .append(System.lineSeparator());

        if (!sourceLine.isEmpty()) {
            builder.append("  ").append(sourceLine).append(System.lineSeparator());
            builder.append("  ");
            for (int i = 1; i < column; i++) {
                builder.append(sourceLine.charAt(Math.min(i - 1, sourceLine.length() - 1)) == '\t' ? '\t' : ' ');
            }
            builder.append("^").append(System.lineSeparator());
        }

        builder.append("  ").append(diagnostic.message);
        return builder.toString();
    }
}
