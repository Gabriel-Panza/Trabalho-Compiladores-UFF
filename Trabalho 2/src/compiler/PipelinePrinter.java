package compiler;

import java.io.PrintStream;
import java.util.List;

public final class PipelinePrinter {
    private final PrintStream out;

    public PipelinePrinter(PrintStream out) {
        this.out = out;
    }

    public void step(int number, String title) {
        out.println();
        out.println("[" + number + "] " + title);
    }

    public void item(String name, String value) {
        out.println("    " + name + ": " + value);
    }

    public void message(String text) {
        out.println("    " + text);
    }

    public void tokens(List<Token> tokens) {
        int count = 0;
        for (Token token : tokens) {
            if (token.type != TokenType.EOF) {
                count++;
            }
        }
        item("tokens reconhecidos", Integer.toString(count));
        for (Token token : tokens) {
            if (token.type == TokenType.EOF) {
                continue;
            }
            out.printf(
                    "        %-11s %-18s linha %d, coluna %d%n",
                    token.type.name(),
                    quote(token.text),
                    token.span.startLine,
                    token.span.startColumn);
        }
    }

    public void ast(Program program) {
        item("expressoes no topo", Integer.toString(program.expressions.size()));
        out.println("    AST:");
        if (program.expressions.isEmpty()) {
            out.println("        <programa vazio>");
            return;
        }
        for (Expr expression : program.expressions) {
            printExpr(expression, 2);
        }
    }

    public void symbolTable(List<ParseContext.SymbolInfo> symbols, List<ParseContext.ScopeInfo> functionScopes) {
        out.println("    tabela de simbolos global:");
        printSymbols(symbols);
        for (ParseContext.ScopeInfo scope : functionScopes) {
            out.println("    escopo da funcao " + scope.name + ":");
            printSymbols(scope.symbols);
        }
    }

    private void printSymbols(List<ParseContext.SymbolInfo> symbols) {
        if (symbols.isEmpty()) {
            out.println("        <vazia>");
            return;
        }
        out.println("        nome        categoria  tipo");
        for (ParseContext.SymbolInfo symbol : symbols) {
            out.printf(
                    "        %-11s %-10s %s%n",
                    symbol.name,
                    symbol.kind,
                    symbol.type.name());
        }
    }

    public void generatedCode(String code) {
        out.println("    codigo gerado:");
        String[] lines = code.split("\\R", -1);
        for (String line : lines) {
            if (!line.isEmpty()) {
                out.println("        " + line);
            }
        }
    }

    public void flush() {
        out.flush();
    }

    private void printExpr(Expr expression, int level) {
        String prefix = indent(level);
        if (expression instanceof AtomExpr) {
            AtomExpr atom = (AtomExpr) expression;
            out.println(prefix + atom.type.name() + " " + quote(atom.text));
            return;
        }
        if (expression instanceof PrefixExpr) {
            PrefixExpr prefixExpr = (PrefixExpr) expression;
            out.println(prefix + prefixExpr.prefix.name() + " " + quote(prefixExpr.text));
            printExpr(prefixExpr.value, level + 1);
            return;
        }
        if (expression instanceof VectorExpr) {
            VectorExpr vector = (VectorExpr) expression;
            out.println(prefix + "VETOR");
            for (Expr child : vector.elements) {
                printExpr(child, level + 1);
            }
            return;
        }
        if (expression instanceof DottedListExpr) {
            DottedListExpr dotted = (DottedListExpr) expression;
            out.println(prefix + "LISTA_PONTUADA");
            for (Expr child : dotted.head) {
                printExpr(child, level + 1);
            }
            out.println(indent(level + 1) + "PONTO");
            printExpr(dotted.tail, level + 1);
            return;
        }

        ListExpr list = (ListExpr) expression;
        out.println(prefix + "LISTA");
        for (Expr child : list.elements) {
            printExpr(child, level + 1);
        }
    }

    private String quote(String text) {
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\n':
                    builder.append("\\n");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                default:
                    builder.append(c);
                    break;
            }
        }
        builder.append('"');
        return builder.toString();
    }

    private String indent(int level) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < level; i++) {
            builder.append("    ");
        }
        return builder.toString();
    }
}
