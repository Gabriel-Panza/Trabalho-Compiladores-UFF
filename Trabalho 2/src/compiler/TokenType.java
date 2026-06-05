package compiler;

public enum TokenType {
    LPAREN,
    RPAREN,
    INTEGER,
    FLOAT,
    STRING,
    BOOLEAN,
    IDENTIFIER,
    DOT,
    QUOTE,
    QUASIQUOTE,
    UNQUOTE,
    UNQUOTE_SPLICING,
    VECTOR_START,
    CHARACTER,
    RATIONAL,
    HEX_INTEGER,
    BIN_INTEGER,
    EOF
}
