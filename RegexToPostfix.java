/*  
1. getPrecedence
Para converter a expressão corretamente, o algoritmo precisa saber qual operação resolver primeiro. O método define a seguinte hierarquia de precedência:
* (Fecho de Kleene / Kleene Star): A mais alta.
.  (Concatenação): A do meio.
|  (União / Ou): A mais baixa.

2. addExplicitConcatenation
Diferente da matemática tradicional, onde os operadores são sempre visíveis (ex: 1 + 2), nas expressões regulares a concatenação é muitas vezes implícita. Por exemplo, escrevemos AB, mas o computador precisa entender isso como A concatenado com B.
O método needsConcatenation verifica quando dois caracteres adjacentes precisam de um operador explícito (.).

3. convertPostfix
Esta é a implementação do algoritmo Shunting Yard. Ele lê a expressão modificada (com os pontos de concatenação) caractere por caractere da esquerda para a direita com notação posfixa, usando uma Pilha (Stack<Character>) para guardar temporariamente os operadores.
As regras de transição são:
3.1. Se for um operando (letra ou número):
 O método isOperand identifica e ele vai direto para a string de saída (output).
3.2. Se for um parêntese de abertura (:
 Ele é empilhado na operatorStack.
3.3. Se for um parêntese de fechamento ):
 O método handleClosingParenthesis desempilha todos os operadores da pilha e os joga na saída, até encontrar o parêntese de abertura '('. Os parênteses em si são descartados, pois a notação posfixa não precisa deles.
3.4. Se for um operador (simbolos):
 O método handleOperator compara a precedência do operador atual com o que está no topo da pilha. Se o topo da pilha tiver uma precedência maior ou igual, ele desempilha o topo para a saída. Só depois disso ele empilha o operador atual.
3.5. Fim da leitura:
 Qualquer operador que tenha sobrado na pilha é desempilhado para a saída. 
*/

import java.util.Stack;

public class RegexToPostfix {
    private static final char CONCAT_OPERATOR = '.';
    private static final char UNION_OPERATOR = '|';
    private static final char KLEENE_STAR = '*';

    private static int getPrecedence(char operator) {
        return switch (operator) {
            case KLEENE_STAR     -> 3;  // asterisco (*)
            case CONCAT_OPERATOR -> 2;  // ponto (.)
            case UNION_OPERATOR  -> 1;  // pipe (|)
            default              -> 0;  // operadores não reconhecidos ou operandos
        };
    }

    private static String addExplicitConcatenation(String regex) {
        StringBuilder result = new StringBuilder();
        char[] chars = regex.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char current = chars[i];
            result.append(current);

            if (i + 1 < chars.length) {
                char next = chars[i + 1];
                if (needsConcatenation(current, next)) {
                    result.append(CONCAT_OPERATOR);
                }
            }
        }
        return result.toString();
    }

    private static boolean needsConcatenation(char current, char next) {
        boolean currentCanEndLiteral = isOperand(current) || current == ')' || current == KLEENE_STAR;
        boolean nextCanStartLiteral = isOperand(next) || next == '(';
        return currentCanEndLiteral && nextCanStartLiteral;
    }

    private static boolean isOperand(char c) {
        return Character.isLetterOrDigit(c);
    }
    
    public static String convertPostfix(String regex) {
        String preparedRegex = addExplicitConcatenation(regex);
        StringBuilder output = new StringBuilder();
        Stack<Character> operatorStack = new Stack<>();

        for (char symbol : preparedRegex.toCharArray()) {
            if (isOperand(symbol)) {
                output.append(symbol);
            } else if (symbol == '(') {
                operatorStack.push(symbol);
            } else if (symbol == ')') {
                handleClosingParenthesis(operatorStack, output);
            } else {
                handleOperator(symbol, operatorStack, output);
            }
        }

        while (!operatorStack.isEmpty()) {
            output.append(operatorStack.pop());
        }
        return output.toString();
    }

    private static void handleClosingParenthesis(Stack<Character> stack, StringBuilder output) {
        while (!stack.isEmpty() && stack.peek() != '(') {
            output.append(stack.pop());
        }
        if (!stack.isEmpty()) stack.pop();
    }

    private static void handleOperator(char operator, Stack<Character> stack, StringBuilder output) {
        while (!stack.isEmpty() && getPrecedence(stack.peek()) >= getPrecedence(operator)) {
            output.append(stack.pop());
        }
        stack.push(operator);
    }
}