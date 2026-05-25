DIGIT      [0-9]
HEX        [0-9a-fA-F]
IDSTART    [A-Za-z_+\-*/<>=!?$%&:]
IDCHAR     [A-Za-z0-9_+\-*/<>=!?$%&:.]

%%
[ \t\r\n]+              ;
";"[^\n]* ;

"("                     return LPAREN;
")"                     return RPAREN;
"#("                    return VECTOR_START;
"'"                     return QUOTE;
"`"                     return QUASIQUOTE;
","                     return UNQUOTE;
",@"                    return UNQUOTE_SPLICING;
"."                     return DOT;

"#t"|"#f"               return BOOLEAN;

"#\\"space              return CHARACTER;
"#\\"newline            return CHARACTER;
"#\\".                  return CHARACTER;

-?{DIGIT}+              return INTEGER;
-?{DIGIT}+"."{DIGIT}+   return FLOAT;
-?{DIGIT}+"/"{DIGIT}+   return RATIONAL;
"#x"{HEX}+              return HEX_INTEGER;
"#b"[01]+               return BIN_INTEGER;

\"([^\"\\]|\\.)*\"      return STRING;

"+"                     return IDENTIFIER;
"-"                     return IDENTIFIER;
"..."                   return IDENTIFIER;
{IDSTART}{IDCHAR}* return IDENTIFIER;

.                       return ERROR;
%%
