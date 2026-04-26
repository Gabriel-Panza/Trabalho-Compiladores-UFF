package scanner.pipeline;

/* 
 * LOGICA E ESPECIFICAÇÕES IMPLEMENTADAS:
 * * 1. TOKENIZAÇÃO (LEXER DE REGEX):
     O método 'tokenize' realiza a análise léxica da própria expressão regular. 
     Diferente de um parser simples, ele agrupa caracteres em blocos lógicos:
     - OPERANDOS: Literais simples, caracteres escapados (ex: '\.', '\+') e 
     classes de caracteres complexas (ex: '[0-9]', '[^\n]').
     - OPERADORES: Símbolos reservados (*, +, ?, |, U).
     - ESPECIALIZAÇÃO RACKET: O leitor de colchetes '[' entra em modo de captura 
     direta até o fechamento ']', ignorando metacaracteres internos e 
     suportando escapes (ex: '\]'), essencial para capturar strings e comentários.
     - DESAMBIGUAÇÃO: O sinal '+' é classificado dinamicamente. Se precedido por 
     um operando ou fechamento, vira operador (Kleene Plus). Caso contrário, 
     é tratado como operando literal (sinal de número).
 * * 2. HIERARQUIA DE PRECEDÊNCIA (SHUNTING-YARD):
     O método 'getPrecedence' garante que a matemática das linguagens formais 
     seja respeitada, resolvendo operações na ordem:
     - Nível 3 (Alta): Unários (*, +, ?) - Fechos e Opcionalidade.
     - Nível 2 (Média): Binário (·) - Concatenação (frequentemente implícita).
     - Nível 1 (Baixa): Binário (| ou U) - União (Alternância).
 * * 3. CONCATENAÇÃO EXPLÍCITA (INSERÇÃO DE '·'):
     Em REs, a concatenação não possui um símbolo visível (ex: 'ab'). O método 
     'addExplicitConcatenation' varre os tokens e insere um ponto mediano ('·') 
     sempre que detecta adjacência que implique junção (ex: entre operando-operando, 
     fecho-operando ou parêntese-operando). Isso é vital para que o algoritmo 
     Shunting Yard processe a concatenação como um operador binário comum.
 * * 4. ALGORITMO SHUNTING YARD ('convertPostfix'):
     Implementa a conversão Infixa -> Posfixa utilizando uma pilha de operadores.
     - Operandos vão direto para a saída.
     - Parênteses controlam o escopo de precedência e são descartados ao final.
     - Operadores são empilhados/desempilhados com base no nível de prioridade, 
     garantindo que o AFN de Thompson seja construído na ordem correta.
 * * 5. ESTRUTURA DE REGRA LÉXICA:
     Utiliza um 'record' Java para encapsular o Tipo do Token, sua lista posfixa e 
     metadados de anotação (ex: "skip"). A anotação "skip" permite que o Scanner ignore 
     automaticamente WHITESPACE e COMMENTS durante a leitura do código fonte Racket.
*/

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class RegexToPostfix {

    // Template para guardar o Tipo, a Lista Postfix e uma Anotação (ex: "skip") 
    public record RegraLexica(String tipo, List<String> postfix, String anotacao) {
        public RegraLexica(String tipo, List<String> postfix) {
            this(tipo, postfix, "");
        }
    }

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

    // Hierarquia de precedência
    private static int getPrecedence(String operator) {
        return switch (operator) {
            case "*", "+", "?" -> 3;  
            case "·"           -> 2;  
            case "|", "U"      -> 1;  
            default            -> 0;
        };
    }

    // Tokenizador (Lexer): Analisa a string e agrupa símbolos corretamente. 
    private static List<Token> tokenize(String regex) {
        List<Token> tokens = new ArrayList<>();
        TokenType prevType = null;
        String prevValue = "";

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            
            // Tratamento de Escapes (ex: \. \* \+ \()
            if (c == '\\' && i + 1 < regex.length()) {
                String escapedChar = "\\" + regex.charAt(i + 1);
                tokens.add(new Token(escapedChar, TokenType.OPERAND));
                prevType = TokenType.OPERAND;
                prevValue = escapedChar;
                i++; 
                continue;

            // Leitura robusta do bloco [...]
            } else if (c == '[') {
                StringBuilder sb = new StringBuilder();
                sb.append(c);
                i++;
                while (i < regex.length()) {
                    // Se encontrar um escape dentro do colchete (ex: \]), ignora a função do ]
                    if (regex.charAt(i) == '\\' && i + 1 < regex.length()) {
                        sb.append(regex.charAt(i));
                        sb.append(regex.charAt(i + 1));
                        i += 2;
                    } else if (regex.charAt(i) == ']') {
                        sb.append(regex.charAt(i));
                        break;
                    } else {
                        sb.append(regex.charAt(i));
                        i++;
                    }
                }
                String val = sb.toString();
                tokens.add(new Token(val, TokenType.OPERAND));
                prevType = TokenType.OPERAND;
                prevValue = val;

            // Parênteses
            } else if (c == '(') {
                tokens.add(new Token("(", TokenType.PAREN_LEFT));
                prevType = TokenType.PAREN_LEFT;
                prevValue = "(";
            } else if (c == ')') {
                tokens.add(new Token(")", TokenType.PAREN_RIGHT));
                prevType = TokenType.PAREN_RIGHT;
                prevValue = ")";

            // Operadores Unários e Binários
            } else if (c == '*' || c == 'U' || c == '|' || c == '?') {
                tokens.add(new Token(String.valueOf(c), TokenType.OPERATOR));
                prevType = TokenType.OPERATOR;
                prevValue = String.valueOf(c);

            // Desambiguação do sinal '+'
            } else if (c == '+') {
                if (prevType == TokenType.OPERAND || prevType == TokenType.PAREN_RIGHT || 
                    prevValue.equals("*") || prevValue.equals("+") || prevValue.equals("?")) {
                    tokens.add(new Token("+", TokenType.OPERATOR));
                    prevType = TokenType.OPERATOR;
                } else {
                    tokens.add(new Token("+", TokenType.OPERAND));
                    prevType = TokenType.OPERAND;
                }
                prevValue = "+";

            // Literais comuns
            } else {
                tokens.add(new Token(String.valueOf(c), TokenType.OPERAND));
                prevType = TokenType.OPERAND;
                prevValue = String.valueOf(c);
            }
        }
        return tokens;
    }

    // Adição de concatenação explícita (inserindo '·') 
    private static List<Token> addExplicitConcatenation(List<Token> tokens) {
        List<Token> result = new ArrayList<>();
        
        for (int i = 0; i < tokens.size(); i++) {
            Token current = tokens.get(i);
            result.add(current);

            if (i + 1 < tokens.size()) {
                Token next = tokens.get(i + 1);
                
                boolean currentCanEndLiteral = (current.type == TokenType.OPERAND) || 
                                               (current.type == TokenType.PAREN_RIGHT) || 
                                               (current.type == TokenType.OPERATOR && (current.value.equals("*") || current.value.equals("+") || current.value.equals("?")));
                
                boolean nextCanStartLiteral = (next.type == TokenType.OPERAND) || (next.type == TokenType.PAREN_LEFT);
                
                if (currentCanEndLiteral && nextCanStartLiteral) {
                    result.add(new Token(CONCAT_OPERATOR, TokenType.OPERATOR));
                }
            }
        }
        return result;
    }

    // Algoritmo Shunting Yard: Converte a lista de tokens para notação Posfixa. 
    public static List<String> convertPostfix(String regex) {
        List<Token> tokens = tokenize(regex);
        List<Token> preparedTokens = addExplicitConcatenation(tokens);
        
        List<String> output = new ArrayList<>();
        Stack<Token> operatorStack = new Stack<>();
        
        for (Token token : preparedTokens) {
            if (token.type == TokenType.OPERAND) {
                output.add(token.value);
            } else if (token.type == TokenType.PAREN_LEFT) {
                operatorStack.push(token);
            } else if (token.type == TokenType.PAREN_RIGHT) {
                while (!operatorStack.isEmpty() && operatorStack.peek().type != TokenType.PAREN_LEFT) {
                    output.add(operatorStack.pop().value);
                }
                if (!operatorStack.isEmpty()) operatorStack.pop(); 
            } else if (token.type == TokenType.OPERATOR) {
                while (!operatorStack.isEmpty() && operatorStack.peek().type != TokenType.PAREN_LEFT && 
                       getPrecedence(operatorStack.peek().value) >= getPrecedence(token.value)) {
                    output.add(operatorStack.pop().value);
                }
                operatorStack.push(token);
            }
        }

        while (!operatorStack.isEmpty()) {
            output.add(operatorStack.pop().value);
        }
        
        return output;
    }
}