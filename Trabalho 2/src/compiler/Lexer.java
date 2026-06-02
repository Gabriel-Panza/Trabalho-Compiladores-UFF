package compiler;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {
    private final SourceFile source;
    private final String input;
    private final List<Token> tokens = new ArrayList<>();
    private int index = 0;
    private int line = 1;
    private int column = 1;

    public Lexer(SourceFile source) {
        this.source = source;
        this.input = source.text;
    }

    public List<Token> lex() throws CompilerException {
        while (!isAtEnd()) {
            char c = peek();
            if (Character.isWhitespace(c)) {
                advance();
            } else if (c == ';') {
                skipComment();
            } else if (c == '(') {
                addSingle(TokenType.LPAREN);
            } else if (c == ')') {
                addSingle(TokenType.RPAREN);
            } else if (c == '"') {
                readString();
            } else if (c == '#') {
                readBoolean();
            } else if (isNumberStart()) {
                readNumber();
            } else if (isIdentifierStart(c)) {
                readIdentifier();
            } else {
                SourceSpan span = SourceSpan.at(line, column);
                throw new CompilerException(Diagnostic.lexical(
                        span,
                        "Encontrei o caractere '" + c + "', mas ele nao faz parte da linguagem aceita."));
            }
        }
        tokens.add(new Token(TokenType.EOF, "", SourceSpan.at(line, column)));
        return new ArrayList<>(tokens);
    }

    private void addSingle(TokenType type) {
        int startLine = line;
        int startColumn = column;
        String text = Character.toString(advance());
        tokens.add(new Token(type, text, new SourceSpan(startLine, startColumn, line, column - 1)));
    }

    private void skipComment() {
        while (!isAtEnd() && peek() != '\n') {
            advance();
        }
    }

    private void readBoolean() throws CompilerException {
        int startLine = line;
        int startColumn = column;
        advance();
        if (!isAtEnd() && (peek() == 't' || peek() == 'f')) {
            char value = advance();
            tokens.add(new Token(
                    TokenType.BOOLEAN,
                    "#" + value,
                    new SourceSpan(startLine, startColumn, line, column - 1)));
            return;
        }
        throw new CompilerException(Diagnostic.lexical(
                new SourceSpan(startLine, startColumn, line, column),
                "Depois de '#', use '#t' para verdadeiro ou '#f' para falso."));
    }

    private void readString() throws CompilerException {
        int startLine = line;
        int startColumn = column;
        advance();
        StringBuilder value = new StringBuilder();

        while (!isAtEnd()) {
            char c = advance();
            if (c == '"') {
                tokens.add(new Token(
                        TokenType.STRING,
                        value.toString(),
                        new SourceSpan(startLine, startColumn, line, column - 1)));
                return;
            }
            if (c == '\n') {
                throw new CompilerException(Diagnostic.lexical(
                        new SourceSpan(startLine, startColumn, line, column),
                        "Esta string foi aberta, mas a linha acabou antes das aspas finais."));
            }
            if (c == '\\') {
                if (isAtEnd()) {
                    break;
                }
                char escaped = advance();
                switch (escaped) {
                    case 'n':
                        value.append('\n');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case '"':
                        value.append('"');
                        break;
                    case '\\':
                        value.append('\\');
                        break;
                    default:
                        value.append(escaped);
                        break;
                }
            } else {
                value.append(c);
            }
        }

        throw new CompilerException(Diagnostic.lexical(
                new SourceSpan(startLine, startColumn, line, column),
                "Esta string foi aberta, mas faltou fechar com aspas."));
    }

    private void readNumber() {
        int startLine = line;
        int startColumn = column;
        StringBuilder value = new StringBuilder();

        if (peek() == '-') {
            value.append(advance());
        }
        while (!isAtEnd() && Character.isDigit(peek())) {
            value.append(advance());
        }

        boolean isFloat = false;
        if (!isAtEnd() && peek() == '.' && hasNextDigit()) {
            isFloat = true;
            value.append(advance());
            while (!isAtEnd() && Character.isDigit(peek())) {
                value.append(advance());
            }
        }

        tokens.add(new Token(
                isFloat ? TokenType.FLOAT : TokenType.INTEGER,
                value.toString(),
                new SourceSpan(startLine, startColumn, line, column - 1)));
    }

    private void readIdentifier() {
        int startLine = line;
        int startColumn = column;
        StringBuilder value = new StringBuilder();

        while (!isAtEnd() && isIdentifierPart(peek())) {
            value.append(advance());
        }

        tokens.add(new Token(
                TokenType.IDENTIFIER,
                value.toString(),
                new SourceSpan(startLine, startColumn, line, column - 1)));
    }

    private boolean isNumberStart() {
        return Character.isDigit(peek()) || (peek() == '-' && hasNextDigit());
    }

    private boolean hasNextDigit() {
        return index + 1 < input.length() && Character.isDigit(input.charAt(index + 1));
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c)
                || c == '_'
                || "+-*/<>=!?$%&:.".indexOf(c) >= 0;
    }

    private boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || Character.isDigit(c);
    }

    private boolean isAtEnd() {
        return index >= input.length();
    }

    private char peek() {
        return input.charAt(index);
    }

    private char advance() {
        char c = input.charAt(index++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }
}
