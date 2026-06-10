package compiler;

import compiler.CompilerException;
import compiler.Diagnostic;
import compiler.SourceSpan;
import compiler.Token;
import compiler.TokenType;

%%
%public
%class SchemeJFlexLexer
%unicode
%line
%column
%type Token
%function nextToken
%throws CompilerException

%{
    private Token token(TokenType type) {
        return new Token(type, yytext(), span());
    }

    private Token stringToken() {
        return new Token(TokenType.STRING, decodeString(yytext()), span());
    }

    private SourceSpan span() {
        return new SourceSpan(yyline + 1, yycolumn + 1, yyline + 1, yycolumn + yylength());
    }

    private CompilerException lexicalError(String message) {
        return new CompilerException(Diagnostic.lexical(span(), message));
    }

    private String decodeString(String raw) {
        String body = raw.substring(1, raw.length() - 1);
        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (escaping) {
                switch (c) {
                    case 'n':
                        out.append('\n');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case '"':
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    default:
                        out.append(c);
                        break;
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
%}

%eofval{
    return new Token(TokenType.EOF, "", SourceSpan.at(yyline + 1, yycolumn + 1));
%eofval}

DIGIT      [0-9]
HEX        [0-9a-fA-F]
IDSTART    [A-Za-z_+\-*/<>=!?$%&:]
IDCHAR     [A-Za-z0-9_+\-*/<>=!?$%&:.]

%%
[ \t\r\n]+              { /* ignora espacos */ }
";"[^\n]*               { /* ignora comentario */ }

"("                     { return token(TokenType.LPAREN); }
")"                     { return token(TokenType.RPAREN); }
"#("                    { return token(TokenType.VECTOR_START); }
"'"                     { return token(TokenType.QUOTE); }
"`"                     { return token(TokenType.QUASIQUOTE); }
","                     { return token(TokenType.UNQUOTE); }
",@"                    { return token(TokenType.UNQUOTE_SPLICING); }
"."                     { return token(TokenType.DOT); }

"#t"|"#f"               { return token(TokenType.BOOLEAN); }

"#\\"space              { return token(TokenType.CHARACTER); }
"#\\"newline            { return token(TokenType.CHARACTER); }
"#\\".                  { return token(TokenType.CHARACTER); }

-?{DIGIT}+              { return token(TokenType.INTEGER); }
-?{DIGIT}+"."{DIGIT}+   { return token(TokenType.FLOAT); }
-?{DIGIT}+"/"{DIGIT}+   { return token(TokenType.RATIONAL); }
"#x"{HEX}+              { return token(TokenType.HEX_INTEGER); }
"#b"[01]+               { return token(TokenType.BIN_INTEGER); }

\"([^\"\\]|\\.)*\"      { return stringToken(); }

"+"                     { return token(TokenType.IDENTIFIER); }
"-"                     { return token(TokenType.IDENTIFIER); }
"..."                   { return token(TokenType.IDENTIFIER); }
{IDSTART}{IDCHAR}*      { return token(TokenType.IDENTIFIER); }

.                       { throw lexicalError("Caractere inesperado no scanner JFlex: " + yytext()); }
