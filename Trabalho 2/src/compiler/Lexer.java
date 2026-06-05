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
            } else if (c == '\'') {
                addSingle(TokenType.QUOTE);
            } else if (c == '`') {
                addSingle(TokenType.QUASIQUOTE);
            } else if (c == ',') {
                readUnquote();
            } else if (c == '.') {
                readDotOrEllipsis();
            } else if (c == '"') {
                readString();
            } else if (c == '#') {
                readHashToken();
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

    private void readUnquote() {
        int startLine = line;
        int startColumn = column;
        advance();
        if (!isAtEnd() && peek() == '@') {
            advance();
            tokens.add(new Token(
                    TokenType.UNQUOTE_SPLICING,
                    ",@",
                    new SourceSpan(startLine, startColumn, line, column - 1)));
            return;
        }
        tokens.add(new Token(
                TokenType.UNQUOTE,
                ",",
                new SourceSpan(startLine, startColumn, line, column - 1)));
    }

    private void readDotOrEllipsis() {
        int startLine = line;
        int startColumn = column;
        if (matches("...")) {
            advance();
            advance();
            advance();
            tokens.add(new Token(
                    TokenType.IDENTIFIER,
                    "...",
                    new SourceSpan(startLine, startColumn, line, column - 1)));
            return;
        }
        addSingle(TokenType.DOT);
    }

    private void readHashToken() throws CompilerException {
        int startLine = line;
        int startColumn = column;
        advance();
        if (isAtEnd()) {
            throw new CompilerException(Diagnostic.lexical(
                    new SourceSpan(startLine, startColumn, line, column),
                    "Depois de '#', use uma forma valida como '#t', '#f', '#(...)', '#\\a', '#xFF' ou '#b1010'."));
        }

        if (peek() == '(') {
            advance();
            tokens.add(new Token(
                    TokenType.VECTOR_START,
                    "#(",
                    new SourceSpan(startLine, startColumn, line, column - 1)));
            return;
        }
        if (peek() == 't' || peek() == 'f') {
            char value = advance();
            tokens.add(new Token(
                    TokenType.BOOLEAN,
                    "#" + value,
                    new SourceSpan(startLine, startColumn, line, column - 1)));
            return;
        }
        if (peek() == '\\') {
            readCharacter(startLine, startColumn);
            return;
        }
        if (peek() == 'x') {
            readBaseInteger(startLine, startColumn, TokenType.HEX_INTEGER, "0123456789abcdefABCDEF", "#x");
            return;
        }
        if (peek() == 'b') {
            readBaseInteger(startLine, startColumn, TokenType.BIN_INTEGER, "01", "#b");
            return;
        }
        throw new CompilerException(Diagnostic.lexical(
                new SourceSpan(startLine, startColumn, line, column),
                "Depois de '#', use uma forma valida como '#t', '#f', '#(...)', '#\\a', '#xFF' ou '#b1010'."));
    }

    private void readCharacter(int startLine, int startColumn) throws CompilerException {
        advance();
        StringBuilder value = new StringBuilder("#\\");
        while (!isAtEnd() && !Character.isWhitespace(peek()) && peek() != '(' && peek() != ')') {
            value.append(advance());
        }

        if (value.length() == 2) {
            throw new CompilerException(Diagnostic.lexical(
                    new SourceSpan(startLine, startColumn, line, column),
                    "Depois de '#\\', informe o caractere desejado."));
        }

        tokens.add(new Token(
                TokenType.CHARACTER,
                value.toString(),
                new SourceSpan(startLine, startColumn, line, column - 1)));
    }

    private void readBaseInteger(
            int startLine,
            int startColumn,
            TokenType type,
            String allowedDigits,
            String prefix) throws CompilerException {
        advance();
        StringBuilder value = new StringBuilder(prefix);

        while (!isAtEnd() && allowedDigits.indexOf(peek()) >= 0) {
            value.append(advance());
        }

        if (value.length() == prefix.length()) {
            throw new CompilerException(Diagnostic.lexical(
                    new SourceSpan(startLine, startColumn, line, column),
                    "Depois de '" + prefix + "', informe pelo menos um digito valido."));
        }

        tokens.add(new Token(
                type,
                value.toString(),
                new SourceSpan(startLine, startColumn, line, column - 1)));
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

        TokenType type = TokenType.INTEGER;
        if (!isAtEnd() && peek() == '/' && hasNextDigit()) {
            type = TokenType.RATIONAL;
            value.append(advance());
            while (!isAtEnd() && Character.isDigit(peek())) {
                value.append(advance());
            }
            tokens.add(new Token(
                    type,
                    value.toString(),
                    new SourceSpan(startLine, startColumn, line, column - 1)));
            return;
        }

        if (!isAtEnd() && peek() == '.' && hasNextDigit()) {
            type = TokenType.FLOAT;
            value.append(advance());
            while (!isAtEnd() && Character.isDigit(peek())) {
                value.append(advance());
            }
        }

        tokens.add(new Token(
                type,
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
                || "+-*/<>=!?$%&:".indexOf(c) >= 0;
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

    private boolean matches(String text) {
        if (index + text.length() > input.length()) {
            return false;
        }
        return input.substring(index, index + text.length()).equals(text);
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
