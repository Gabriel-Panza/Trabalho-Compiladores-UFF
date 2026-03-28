/* 
1. Tokenização (Lexer)
Antes de converter, o algoritmo agrupa os caracteres da string original em 'Tokens' (blocos com significado). Isso é necessário porque:
- Entidades como '[0-9]' ou caracteres de escape como '\.' são lidos como um único operando, e não caracteres soltos.
- O sinal '+' precisa ser desambiguado pelo contexto: pode ser um sinal matemático (operando) ou o operador de Fecho Positivo (uma ou mais repetições).

2. getPrecedence
Define a hierarquia de resolução das operações. O método define a seguinte precedência (do maior para o menor):
- Nível 3: * (Fecho de Kleene), + (Fecho Positivo) e ? (Opcional). A mais alta.
- Nível 2: · (Concatenação). A do meio.
- Nível 1: | ou U (União / Ou). A mais baixa.

3. addExplicitConcatenation
Nas expressões regulares, a concatenação é muitas vezes implícita (ex: lendo AB, entende-se A concatenado com B).
O método avalia a lista de Tokens e insere um operador explícito de concatenação ('·') entre tokens adjacentes que precisam ser concatenados. Usamos o ponto mediano ('·') internamente para não conflitar com o ponto literal ('.') usado em floats.

4. convertPostfix
Esta é a implementação do algoritmo Shunting Yard adaptada para Tokens. Ele lê a lista de tokens da esquerda para a direita, usando uma Pilha (Stack<Token>) para guardar temporariamente os operadores.
As regras de transição são:
4.1. Se for um operando (literais, blocos [...], escapes):
 Ele vai direto para a string de saída (output), separado por um espaço.
4.2. Se for um parêntese de abertura '(':
 Ele é empilhado na operatorStack.
4.3. Se for um parêntese de fechamento ')':
 O algoritmo desempilha todos os operadores da pilha e os joga na saída, até encontrar o parêntese de abertura '('. Os parênteses em si são descartados.
4.4. Se for um operador (*, +, ?, ·, |, U):
 Ele compara a precedência do operador atual com o que está no topo da pilha. Se o topo da pilha tiver uma precedência maior ou igual, ele desempilha o topo para a saída. Só depois disso ele empilha o operador atual.
4.5. Fim da leitura:
 Qualquer operador que tenha sobrado na pilha é desempilhado para a saída. 
*/

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class RegexToPostfix {

    private static final String CONCAT_OPERATOR = "·"; 
    
    // Categorias de tokens para facilitar o parser
    enum TokenType {
        OPERAND, OPERATOR, PAREN_LEFT, PAREN_RIGHT
    }

    static class Token {
        String value;
        TokenType type;

        Token(String value, TokenType type) {
            this.value = value;
            this.type = type;
        }
    }

    // 1. Hierarquia de precedência
    private static int getPrecedence(String operator) {
        return switch (operator) {
            case "*", "+", "?" -> 3;  
            case "·"           -> 2;  
            case "|", "U"      -> 1;  
            default            -> 0;
        };
    }

    // Tokenizador (Lexer): Analisa a string e agrupa símbolos corretamente
    private static List<Token> tokenize(String regex) {
        List<Token> tokens = new ArrayList<>();
        TokenType prevType = null;
        String prevValue = "";

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            
            // Tratamento de Escapes (ex: \. \* \+)
            if (c == '\\' && i + 1 < regex.length()) {
                String escapedChar = "\\" + regex.charAt(i + 1);
                tokens.add(new Token(escapedChar, TokenType.OPERAND));
                prevType = TokenType.OPERAND;
                prevValue = escapedChar;
                i++; // Pula o próximo caractere pois já foi processado
                continue;
            } else if (c == '*' || c == 'U' || c == '|' || c == '?') {
                tokens.add(new Token(String.valueOf(c), TokenType.OPERATOR));
                prevType = TokenType.OPERATOR;
                prevValue = String.valueOf(c);
            } else if (c == '[') {
                // Lê o bloco [ ... ] inteiro como um único Token de Operando
                StringBuilder sb = new StringBuilder();
                while (i < regex.length() && regex.charAt(i) != ']') {
                    sb.append(regex.charAt(i));
                    i++;
                }
                if (i < regex.length()) sb.append(regex.charAt(i));
                String val = sb.toString();
                tokens.add(new Token(val, TokenType.OPERAND));
                prevType = TokenType.OPERAND;
                prevValue = val;
            } else if (c == '(') {
                tokens.add(new Token("(", TokenType.PAREN_LEFT));
                prevType = TokenType.PAREN_LEFT;
                prevValue = "(";
            } else if (c == ')') {
                tokens.add(new Token(")", TokenType.PAREN_RIGHT));
                prevType = TokenType.PAREN_RIGHT;
                prevValue = ")";
            } else if (c == '*' || c == 'U' || c == '|') {
                tokens.add(new Token(String.valueOf(c), TokenType.OPERATOR));
                prevType = TokenType.OPERATOR;
                prevValue = String.valueOf(c);
            } else if (c == '+') {
                // Lógica de desambiguação: É operador de Kleene ou é sinal matemático de positivo?
                if (prevType == TokenType.OPERAND || prevType == TokenType.PAREN_RIGHT || 
                    prevValue.equals("*") || prevValue.equals("+")) {
                    tokens.add(new Token("+", TokenType.OPERATOR));
                    prevType = TokenType.OPERATOR;
                } else {
                    // Caso contrário, atua como operando (sinal literal)
                    tokens.add(new Token("+", TokenType.OPERAND));
                    prevType = TokenType.OPERAND;
                }
                prevValue = "+";
            } else {
                // Qualquer outro caractere (ex: '.', '-', letras, números soltos) é operando literal
                tokens.add(new Token(String.valueOf(c), TokenType.OPERAND));
                prevType = TokenType.OPERAND;
                prevValue = String.valueOf(c);
            }
        }
        return tokens;
    }

    // Adição de concatenação explícita agora baseada na lista de tokens
    private static List<Token> addExplicitConcatenation(List<Token> tokens) {
        List<Token> result = new ArrayList<>();
        
        for (int i = 0; i < tokens.size(); i++) {
            Token current = tokens.get(i);
            result.add(current);

            if (i + 1 < tokens.size()) {
                Token next = tokens.get(i + 1);
                
                boolean currentCanEndLiteral = (current.type == TokenType.OPERAND) || 
                                               (current.type == TokenType.PAREN_RIGHT) || 
                                               (current.type == TokenType.OPERATOR && (current.value.equals("*") || current.value.equals("+")));
                
                boolean nextCanStartLiteral = (next.type == TokenType.OPERAND) || 
                                              (next.type == TokenType.PAREN_LEFT);
                
                if (currentCanEndLiteral && nextCanStartLiteral) {
                    result.add(new Token(CONCAT_OPERATOR, TokenType.OPERATOR));
                }
            }
        }
        return result;
    }

    // Algoritmo Shunting Yard adaptado para ler Tokens
    public static String convertPostfix(String regex) {
        List<Token> tokens = tokenize(regex);
        List<Token> preparedTokens = addExplicitConcatenation(tokens);
        
        StringBuilder output = new StringBuilder();
        Stack<Token> operatorStack = new Stack<>();
        for (Token token : preparedTokens) {
            if (token.type == TokenType.OPERAND) {
                // Adicionamos espaços na saída para separar operandos com múltiplos caracteres visualmente
                output.append(token.value).append(" ");
            } else if (token.type == TokenType.PAREN_LEFT) {
                operatorStack.push(token);
            } else if (token.type == TokenType.PAREN_RIGHT) {
                while (!operatorStack.isEmpty() && operatorStack.peek().type != TokenType.PAREN_LEFT) {
                    output.append(operatorStack.pop().value).append(" ");
                }
                if (!operatorStack.isEmpty()) operatorStack.pop(); 
            } else if (token.type == TokenType.OPERATOR) {
                while (!operatorStack.isEmpty() && operatorStack.peek().type != TokenType.PAREN_LEFT && 
                       getPrecedence(operatorStack.peek().value) >= getPrecedence(token.value)) {
                    output.append(operatorStack.pop().value).append(" ");
                }
                operatorStack.push(token);
            }
        }

        while (!operatorStack.isEmpty()) {
            output.append(operatorStack.pop().value).append(" ");
        }
        return output.toString().trim();
    }
}