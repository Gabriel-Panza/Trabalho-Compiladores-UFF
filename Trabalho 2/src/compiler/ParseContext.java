package compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ParseContext {
    private enum SymbolKind {
        VARIABLE,
        FUNCTION
    }

    private static final class Symbol {
        final String name;
        final SymbolKind kind;
        Type type;
        int arity;

        Symbol(String name, SymbolKind kind, Type type, int arity) {
            this.name = name;
            this.kind = kind;
            this.type = type;
            this.arity = arity;
        }
    }

    private static final class Scope {
        final Scope parent;
        final Map<String, Symbol> symbols = new HashMap<>();

        Scope(Scope parent) {
            this.parent = parent;
        }

        Symbol lookup(String name) {
            Symbol local = symbols.get(name);
            if (local != null) {
                return local;
            }
            return parent == null ? null : parent.lookup(name);
        }

        boolean hasLocal(String name) {
            return symbols.containsKey(name);
        }

        void define(Symbol symbol) {
            symbols.put(symbol.name, symbol);
        }
    }

    private static final class FunctionHeader {
        final String name;
        final List<AtomExpr> params;

        FunctionHeader(String name, List<AtomExpr> params) {
            this.name = name;
            this.params = params;
        }
    }

    private static final Set<String> SPECIAL_NAMES = new HashSet<>(Arrays.asList(
            "define", "if", "begin", "lambda", "let", "set!",
            "+", "-", "*", "/", "<", "<=", ">", ">=", "=",
            "and", "or", "not", "display", "newline"));

    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final Scope global = new Scope(null);

    public void acceptTopLevel(Expr expression) {
        analyzeTopLevel(expression, global);
    }

    public List<Diagnostic> diagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    private void analyzeTopLevel(Expr expression, Scope scope) {
        if (expression instanceof ListExpr) {
            ListExpr list = (ListExpr) expression;
            if (isIdentifier(list, 0, "define")) {
                analyzeDefine(list, scope);
                return;
            }
        }
        infer(expression, scope);
    }

    private Type analyzeDefine(ListExpr list, Scope scope) {
        if (list.elements.size() < 3) {
            report(list.span, "A forma 'define' precisa receber um nome e um valor.");
            return Type.UNKNOWN;
        }

        Expr target = list.elements.get(1);
        if (target instanceof AtomExpr) {
            AtomExpr atom = (AtomExpr) target;
            if (atom.type == TokenType.IDENTIFIER) {
                if (SPECIAL_NAMES.contains(atom.text)) {
                    report(atom.span, "O nome '" + atom.text + "' ja tem um significado na linguagem. Escolha outro nome.");
                    return Type.UNKNOWN;
                }
                if (list.elements.size() != 3) {
                    report(list.span, "Para criar uma variavel, use o formato (define nome valor).");
                    return Type.UNKNOWN;
                }
                Type valueType = infer(list.elements.get(2), scope);
                if (scope.hasLocal(atom.text)) {
                    report(atom.span, "O nome '" + atom.text + "' ja foi definido neste mesmo bloco.");
                } else {
                    scope.define(new Symbol(atom.text, SymbolKind.VARIABLE, valueType, 0));
                }
                return Type.VOID;
            }
        }

        if (target instanceof ListExpr) {
            return analyzeFunctionDefine(list, scope);
        }

        report(target.span, "Depois de 'define', use um nome de variavel ou uma lista como (funcao parametro).");
        return Type.UNKNOWN;
    }

    private Type analyzeFunctionDefine(ListExpr list, Scope scope) {
        FunctionHeader header = readFunctionHeader(list);
        if (header == null) {
            return Type.UNKNOWN;
        }
        if (list.elements.size() < 3) {
            report(list.span, "A funcao '" + header.name + "' precisa ter pelo menos uma expressao no corpo.");
            return Type.UNKNOWN;
        }

        Symbol function = scope.lookup(header.name);
        if (function == null) {
            function = new Symbol(header.name, SymbolKind.FUNCTION, Type.UNKNOWN, header.params.size());
            scope.define(function);
        } else if (function.kind != SymbolKind.FUNCTION) {
            report(list.elements.get(1).span, "O nome '" + header.name + "' ja foi usado como variavel.");
            return Type.UNKNOWN;
        }

        Scope functionScope = new Scope(scope);
        Set<String> seenParams = new HashSet<>();
        for (AtomExpr param : header.params) {
            if (SPECIAL_NAMES.contains(param.text)) {
                report(param.span, "O parametro '" + param.text + "' usa um nome reservado. Escolha outro nome.");
            } else if (!seenParams.add(param.text)) {
                report(param.span, "O parametro '" + param.text + "' apareceu mais de uma vez.");
            } else {
                functionScope.define(new Symbol(param.text, SymbolKind.VARIABLE, Type.UNKNOWN, 0));
            }
        }

        Type bodyType = Type.VOID;
        for (int i = 2; i < list.elements.size(); i++) {
            bodyType = infer(list.elements.get(i), functionScope);
        }
        function.type = bodyType;
        function.arity = header.params.size();
        return Type.VOID;
    }

    private Type infer(Expr expression, Scope scope) {
        if (expression instanceof AtomExpr) {
            return inferAtom((AtomExpr) expression, scope);
        }
        if (expression instanceof ListExpr) {
            return inferList((ListExpr) expression, scope);
        }
        if (expression instanceof VectorExpr) {
            return inferVector((VectorExpr) expression, scope);
        }
        if (expression instanceof PrefixExpr) {
            return inferPrefix((PrefixExpr) expression, scope);
        }
        if (expression instanceof DottedListExpr) {
            report(expression.span, "Pares pontuados sao reconhecidos, mas ainda nao podem ser usados como expressao executavel.");
            return Type.UNKNOWN;
        }
        return Type.UNKNOWN;
    }

    private Type inferAtom(AtomExpr atom, Scope scope) {
        switch (atom.type) {
            case INTEGER:
            case HEX_INTEGER:
            case BIN_INTEGER:
                return Type.INT;
            case FLOAT:
            case RATIONAL:
                return Type.FLOAT;
            case STRING:
            case CHARACTER:
                return Type.STRING;
            case BOOLEAN:
                return Type.BOOLEAN;
            case IDENTIFIER:
                Symbol symbol = scope.lookup(atom.text);
                if (symbol == null) {
                    report(atom.span, "O nome '" + atom.text + "' foi usado antes de ser definido.");
                    return Type.UNKNOWN;
                }
                return symbol.kind == SymbolKind.FUNCTION ? Type.FUNCTION : symbol.type;
            default:
                return Type.UNKNOWN;
        }
    }

    private Type inferVector(VectorExpr vector, Scope scope) {
        for (Expr element : vector.elements) {
            infer(element, scope);
        }
        return Type.UNKNOWN;
    }

    private Type inferPrefix(PrefixExpr prefix, Scope scope) {
        if (prefix.prefix == TokenType.QUOTE || prefix.prefix == TokenType.QUASIQUOTE) {
            return Type.UNKNOWN;
        }
        if (prefix.prefix == TokenType.UNQUOTE || prefix.prefix == TokenType.UNQUOTE_SPLICING) {
            report(prefix.span, "Esta forma com virgula so faz sentido dentro de uma quasi-citacao.");
            return infer(prefix.value, scope);
        }
        return infer(prefix.value, scope);
    }

    private Type inferList(ListExpr list, Scope scope) {
        if (list.elements.isEmpty()) {
            report(list.span, "Uma lista vazia nao forma um comando valido neste subconjunto.");
            return Type.UNKNOWN;
        }
        Expr head = list.elements.get(0);
        if (!(head instanceof AtomExpr) || ((AtomExpr) head).type != TokenType.IDENTIFIER) {
            report(head.span, "Uma lista precisa comecar com um nome de comando, operador ou funcao.");
            for (int i = 1; i < list.elements.size(); i++) {
                infer(list.elements.get(i), scope);
            }
            return Type.UNKNOWN;
        }

        String name = ((AtomExpr) head).text;
        if ("define".equals(name)) {
            return analyzeDefine(list, scope);
        }
        if ("if".equals(name)) {
            return inferIf(list, scope);
        }
        if ("begin".equals(name)) {
            return inferBegin(list, scope);
        }
        if ("lambda".equals(name)) {
            return inferLambda(list, scope);
        }
        if ("let".equals(name)) {
            return inferLet(list, scope);
        }
        if ("set!".equals(name)) {
            return inferSet(list, scope);
        }
        if ("display".equals(name)) {
            return inferDisplay(list, scope);
        }
        if ("newline".equals(name)) {
            return inferNewline(list);
        }
        if ("+".equals(name) || "-".equals(name) || "*".equals(name) || "/".equals(name)) {
            return inferArithmetic(list, scope, name);
        }
        if ("<".equals(name) || "<=".equals(name) || ">".equals(name) || ">=".equals(name) || "=".equals(name)) {
            return inferComparison(list, scope);
        }
        if ("and".equals(name) || "or".equals(name)) {
            return inferBooleanMany(list, scope, name);
        }
        if ("not".equals(name)) {
            return inferNot(list, scope);
        }
        return inferCall(list, scope, (AtomExpr) head);
    }

    private Type inferIf(ListExpr list, Scope scope) {
        if (!expectSize(list, 4, "A forma 'if' precisa ter condicao, valor se verdadeiro e valor se falso.")) {
            inferRemaining(list, scope, 1);
            return Type.UNKNOWN;
        }
        Type condition = infer(list.elements.get(1), scope);
        if (condition.isKnown() && condition != Type.BOOLEAN) {
            report(list.elements.get(1).span, "A condicao do 'if' precisa resultar em verdadeiro ou falso.");
        }
        Type thenType = infer(list.elements.get(2), scope);
        Type elseType = infer(list.elements.get(3), scope);
        return commonType(thenType, elseType, list.span);
    }

    private Type inferBegin(ListExpr list, Scope scope) {
        Type result = Type.VOID;
        for (int i = 1; i < list.elements.size(); i++) {
            result = infer(list.elements.get(i), scope);
        }
        return result;
    }

    private Type inferLambda(ListExpr list, Scope scope) {
        if (list.elements.size() < 3) {
            report(list.span, "A forma 'lambda' precisa de uma lista de parametros e de um corpo.");
            return Type.UNKNOWN;
        }
        if (!(list.elements.get(1) instanceof ListExpr)) {
            report(list.elements.get(1).span, "Depois de 'lambda', coloque os parametros entre parenteses.");
            return Type.UNKNOWN;
        }
        ListExpr params = (ListExpr) list.elements.get(1);
        Scope lambdaScope = new Scope(scope);
        for (Expr paramExpr : params.elements) {
            if (paramExpr instanceof AtomExpr && ((AtomExpr) paramExpr).type == TokenType.IDENTIFIER) {
                AtomExpr param = (AtomExpr) paramExpr;
                lambdaScope.define(new Symbol(param.text, SymbolKind.VARIABLE, Type.UNKNOWN, 0));
            } else {
                report(paramExpr.span, "Cada parametro do 'lambda' precisa ser um nome.");
            }
        }
        for (int i = 2; i < list.elements.size(); i++) {
            infer(list.elements.get(i), lambdaScope);
        }
        return Type.FUNCTION;
    }

    private Type inferLet(ListExpr list, Scope scope) {
        if (list.elements.size() < 3) {
            report(list.span, "A forma 'let' precisa de definicoes locais e de um corpo.");
            return Type.UNKNOWN;
        }
        if (!(list.elements.get(1) instanceof ListExpr)) {
            report(list.elements.get(1).span, "As definicoes do 'let' precisam ficar em uma lista.");
            return Type.UNKNOWN;
        }

        ListExpr bindings = (ListExpr) list.elements.get(1);
        Scope local = new Scope(scope);
        for (Expr bindingExpr : bindings.elements) {
            if (!(bindingExpr instanceof ListExpr) || ((ListExpr) bindingExpr).elements.size() != 2) {
                report(bindingExpr.span, "Cada definicao do 'let' precisa ter o formato (nome valor).");
                continue;
            }
            ListExpr binding = (ListExpr) bindingExpr;
            Expr nameExpr = binding.elements.get(0);
            if (!(nameExpr instanceof AtomExpr) || ((AtomExpr) nameExpr).type != TokenType.IDENTIFIER) {
                report(nameExpr.span, "A primeira parte da definicao do 'let' precisa ser um nome.");
                continue;
            }
            AtomExpr name = (AtomExpr) nameExpr;
            Type valueType = infer(binding.elements.get(1), scope);
            local.define(new Symbol(name.text, SymbolKind.VARIABLE, valueType, 0));
        }

        Type result = Type.VOID;
        for (int i = 2; i < list.elements.size(); i++) {
            result = infer(list.elements.get(i), local);
        }
        return result;
    }

    private Type inferSet(ListExpr list, Scope scope) {
        if (!expectSize(list, 3, "A forma 'set!' precisa receber um nome e um novo valor.")) {
            inferRemaining(list, scope, 1);
            return Type.UNKNOWN;
        }
        Expr target = list.elements.get(1);
        if (!(target instanceof AtomExpr) || ((AtomExpr) target).type != TokenType.IDENTIFIER) {
            report(target.span, "Depois de 'set!', informe o nome que sera atualizado.");
            infer(list.elements.get(2), scope);
            return Type.UNKNOWN;
        }
        AtomExpr name = (AtomExpr) target;
        Symbol symbol = scope.lookup(name.text);
        if (symbol == null) {
            report(name.span, "O nome '" + name.text + "' precisa existir antes de receber um novo valor.");
        } else if (symbol.kind != SymbolKind.VARIABLE) {
            report(name.span, "O nome '" + name.text + "' nao e uma variavel que possa receber valor.");
        }
        Type newType = infer(list.elements.get(2), scope);
        if (symbol != null && symbol.type.isKnown() && newType.isKnown() && !compatible(symbol.type, newType)) {
            report(list.elements.get(2).span, "O novo valor de '" + name.text + "' nao combina com o tipo usado antes.");
        }
        return Type.VOID;
    }

    private Type inferDisplay(ListExpr list, Scope scope) {
        if (!expectSize(list, 2, "A forma 'display' precisa receber exatamente um valor.")) {
            inferRemaining(list, scope, 1);
            return Type.UNKNOWN;
        }
        Type value = infer(list.elements.get(1), scope);
        if (value == Type.VOID) {
            report(list.elements.get(1).span, "'display' precisa receber um valor que possa ser mostrado.");
        }
        return Type.VOID;
    }

    private Type inferNewline(ListExpr list) {
        expectSize(list, 1, "A forma 'newline' nao recebe valores.");
        return Type.VOID;
    }

    private Type inferArithmetic(ListExpr list, Scope scope, String operator) {
        if (list.elements.size() < 2) {
            report(list.span, "O operador '" + operator + "' precisa receber pelo menos um numero.");
            return Type.UNKNOWN;
        }
        Type result = operator.equals("/") ? Type.FLOAT : Type.INT;
        for (int i = 1; i < list.elements.size(); i++) {
            Type value = infer(list.elements.get(i), scope);
            if (value.isKnown() && !value.isNumeric()) {
                report(list.elements.get(i).span, "O operador '" + operator + "' so aceita numeros aqui.");
            }
            result = Type.numericResult(result, value, operator.equals("/"));
        }
        return result;
    }

    private Type inferComparison(ListExpr list, Scope scope) {
        if (list.elements.size() < 3) {
            report(list.span, "Esta comparacao precisa receber pelo menos dois valores numericos.");
            return Type.UNKNOWN;
        }
        for (int i = 1; i < list.elements.size(); i++) {
            Type value = infer(list.elements.get(i), scope);
            if (value.isKnown() && !value.isNumeric()) {
                report(list.elements.get(i).span, "Comparacoes como esta so aceitam numeros.");
            }
        }
        return Type.BOOLEAN;
    }

    private Type inferBooleanMany(ListExpr list, Scope scope, String operator) {
        if (list.elements.size() < 2) {
            report(list.span, "A forma '" + operator + "' precisa receber pelo menos um valor verdadeiro/falso.");
            return Type.UNKNOWN;
        }
        for (int i = 1; i < list.elements.size(); i++) {
            Type value = infer(list.elements.get(i), scope);
            if (value.isKnown() && value != Type.BOOLEAN) {
                report(list.elements.get(i).span, "A forma '" + operator + "' so aceita valores verdadeiro/falso.");
            }
        }
        return Type.BOOLEAN;
    }

    private Type inferNot(ListExpr list, Scope scope) {
        if (!expectSize(list, 2, "A forma 'not' precisa receber exatamente um valor verdadeiro/falso.")) {
            inferRemaining(list, scope, 1);
            return Type.UNKNOWN;
        }
        Type value = infer(list.elements.get(1), scope);
        if (value.isKnown() && value != Type.BOOLEAN) {
            report(list.elements.get(1).span, "A forma 'not' so aceita valor verdadeiro/falso.");
        }
        return Type.BOOLEAN;
    }

    private Type inferCall(ListExpr list, Scope scope, AtomExpr name) {
        Symbol symbol = scope.lookup(name.text);
        for (int i = 1; i < list.elements.size(); i++) {
            infer(list.elements.get(i), scope);
        }
        if (symbol == null) {
            report(name.span, "A funcao '" + name.text + "' foi chamada, mas nao foi definida.");
            return Type.UNKNOWN;
        }
        if (symbol.kind != SymbolKind.FUNCTION) {
            report(name.span, "O nome '" + name.text + "' existe, mas nao foi definido como funcao.");
            return Type.UNKNOWN;
        }
        int given = list.elements.size() - 1;
        if (symbol.arity != given) {
            report(name.span, "A funcao '" + name.text + "' espera " + symbol.arity
                    + " argumento(s), mas recebeu " + given + ".");
        }
        return symbol.type;
    }

    private void inferRemaining(ListExpr list, Scope scope, int fromIndex) {
        for (int i = fromIndex; i < list.elements.size(); i++) {
            infer(list.elements.get(i), scope);
        }
    }

    private boolean expectSize(ListExpr list, int expected, String message) {
        if (list.elements.size() != expected) {
            report(list.span, message);
            return false;
        }
        return true;
    }

    private Type commonType(Type left, Type right, SourceSpan span) {
        if (left == right) {
            return left;
        }
        if ((left == Type.INT && right == Type.FLOAT) || (left == Type.FLOAT && right == Type.INT)) {
            return Type.FLOAT;
        }
        if (left == Type.UNKNOWN || right == Type.UNKNOWN) {
            return Type.UNKNOWN;
        }
        report(span, "Os dois caminhos desta expressao produzem tipos diferentes.");
        return Type.UNKNOWN;
    }

    private boolean compatible(Type expected, Type actual) {
        return expected == actual || (expected.isNumeric() && actual.isNumeric());
    }

    private boolean isIdentifier(ListExpr list, int index, String expected) {
        if (index >= list.elements.size()) {
            return false;
        }
        Expr expression = list.elements.get(index);
        return expression instanceof AtomExpr
                && ((AtomExpr) expression).type == TokenType.IDENTIFIER
                && ((AtomExpr) expression).text.equals(expected);
    }

    private boolean isFunctionDefine(ListExpr list) {
        return list.elements.size() >= 2 && list.elements.get(1) instanceof ListExpr;
    }

    private FunctionHeader readFunctionHeader(ListExpr defineList) {
        if (!(defineList.elements.get(1) instanceof ListExpr)) {
            report(defineList.elements.get(1).span, "A definicao de funcao precisa ter o formato (nome parametros...).");
            return null;
        }
        ListExpr headerList = (ListExpr) defineList.elements.get(1);
        if (headerList.elements.isEmpty()) {
            report(headerList.span, "A definicao de funcao precisa informar o nome da funcao.");
            return null;
        }
        Expr nameExpr = headerList.elements.get(0);
        if (!(nameExpr instanceof AtomExpr) || ((AtomExpr) nameExpr).type != TokenType.IDENTIFIER) {
            report(nameExpr.span, "O primeiro item da definicao de funcao precisa ser o nome da funcao.");
            return null;
        }
        AtomExpr name = (AtomExpr) nameExpr;
        if (SPECIAL_NAMES.contains(name.text)) {
            report(name.span, "O nome '" + name.text + "' ja tem um significado na linguagem. Escolha outro nome.");
            return null;
        }
        List<AtomExpr> params = new ArrayList<>();
        for (int i = 1; i < headerList.elements.size(); i++) {
            Expr paramExpr = headerList.elements.get(i);
            if (paramExpr instanceof AtomExpr && ((AtomExpr) paramExpr).type == TokenType.IDENTIFIER) {
                params.add((AtomExpr) paramExpr);
            } else {
                report(paramExpr.span, "Cada parametro da funcao precisa ser um nome.");
            }
        }
        return new FunctionHeader(name.text, params);
    }

    private void report(SourceSpan span, String message) {
        diagnostics.add(Diagnostic.semantic(span, message));
    }
}
