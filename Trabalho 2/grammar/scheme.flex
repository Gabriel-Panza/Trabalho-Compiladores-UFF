%{
/* Especificacao de referencia no estilo Flex.
   A implementacao executavel deste trabalho esta em Java em src/compiler. */
%}

DIGIT      [0-9]
IDCHAR     [A-Za-z0-9_+\-*/<>=!?$%&:.]
IDENT      [A-Za-z_+\-*/<>=!?$%&:.][A-Za-z0-9_+\-*/<>=!?$%&:.]*

%%
[ \t\r\n]+              ;
";"[^\n]*               ;
"("                     return LPAREN;
")"                     return RPAREN;
"#t"|"#f"               return BOOLEAN;
-?{DIGIT}+              return INTEGER;
-?{DIGIT}+"."{DIGIT}+   return FLOAT;
\"([^\"\\]|\\.)*\"      return STRING;
{IDENT}                 return IDENTIFIER;
.                       return ERROR;
%%
