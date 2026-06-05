package compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class PythonGenerator {
    private final List<String> lines = new ArrayList<>();

    public String generate(Program program) {
        lines.clear();
        for (Expr expression : program.expressions) {
            emitStatement(expression, 0);
        }
        if (lines.isEmpty()) {
            lines.add("pass");
        }
        return String.join(System.lineSeparator(), lines) + System.lineSeparator();
    }

    private void emitStatement(Expr expression, int indent) {
        if (expression instanceof ListExpr) {
            ListExpr list = (ListExpr) expression;
            if (!list.elements.isEmpty()) {
                String head = headName(list);
                if ("define".equals(head)) {
                    emitDefine(list, indent);
                    return;
                }
                if ("display".equals(head)) {
                    line(indent, "print(" + expr(list.elements.get(1)) + ", end=\"\")");
                    return;
                }
                if ("newline".equals(head)) {
                    line(indent, "print()");
                    return;
                }
                if ("set!".equals(head)) {
                    AtomExpr name = (AtomExpr) list.elements.get(1);
                    line(indent, NameUtil.toPythonName(name.text) + " = " + expr(list.elements.get(2)));
                    return;
                }
                if ("begin".equals(head)) {
                    for (int i = 1; i < list.elements.size(); i++) {
                        emitStatement(list.elements.get(i), indent);
                    }
                    return;
                }
                if ("if".equals(head)) {
                    emitIfStatement(list, indent);
                    return;
                }
            }
        }
        line(indent, expr(expression));
    }

    private void emitDefine(ListExpr list, int indent) {
        Expr target = list.elements.get(1);
        if (target instanceof AtomExpr) {
            AtomExpr name = (AtomExpr) target;
            line(indent, NameUtil.toPythonName(name.text) + " = " + expr(list.elements.get(2)));
            return;
        }

        ListExpr header = (ListExpr) target;
        AtomExpr functionName = (AtomExpr) header.elements.get(0);
        String params = header.elements.stream()
                .skip(1)
                .map(param -> NameUtil.toPythonName(((AtomExpr) param).text))
                .collect(Collectors.joining(", "));

        line(indent, "def " + NameUtil.toPythonName(functionName.text) + "(" + params + "):");
        if (list.elements.size() == 2) {
            line(indent + 1, "pass");
            return;
        }

        for (int i = 2; i < list.elements.size() - 1; i++) {
            emitStatement(list.elements.get(i), indent + 1);
        }
        Expr last = list.elements.get(list.elements.size() - 1);
        emitReturn(last, indent + 1);
    }

    private void emitIfStatement(ListExpr list, int indent) {
        line(indent, "if " + expr(list.elements.get(1)) + ":");
        emitBranch(list.elements.get(2), indent + 1);
        line(indent, "else:");
        emitBranch(list.elements.get(3), indent + 1);
    }

    private void emitBranch(Expr expression, int indent) {
        if (expression instanceof ListExpr) {
            ListExpr list = (ListExpr) expression;
            if ("begin".equals(headName(list))) {
                if (list.elements.size() == 1) {
                    line(indent, "pass");
                    return;
                }
                for (int i = 1; i < list.elements.size(); i++) {
                    emitStatement(list.elements.get(i), indent);
                }
                return;
            }
        }
        emitStatement(expression, indent);
    }

    private void emitReturn(Expr expression, int indent) {
        if (expression instanceof ListExpr) {
            ListExpr list = (ListExpr) expression;
            String head = headName(list);
            if ("if".equals(head)) {
                line(indent, "if " + expr(list.elements.get(1)) + ":");
                emitReturn(list.elements.get(2), indent + 1);
                line(indent, "else:");
                emitReturn(list.elements.get(3), indent + 1);
                return;
            }
            if ("begin".equals(head)) {
                if (list.elements.size() == 1) {
                    line(indent, "return None");
                    return;
                }
                for (int i = 1; i < list.elements.size() - 1; i++) {
                    emitStatement(list.elements.get(i), indent);
                }
                emitReturn(list.elements.get(list.elements.size() - 1), indent);
                return;
            }
        }
        if (isVoidStatement(expression)) {
            emitStatement(expression, indent);
        } else {
            line(indent, "return " + expr(expression));
        }
    }

    private String expr(Expr expression) {
        if (expression instanceof AtomExpr) {
            return atomExpr((AtomExpr) expression);
        }
        if (expression instanceof VectorExpr) {
            return vectorExpr((VectorExpr) expression);
        }
        if (expression instanceof PrefixExpr) {
            return prefixExpr((PrefixExpr) expression);
        }
        if (expression instanceof DottedListExpr) {
            return dottedExpr((DottedListExpr) expression);
        }
        ListExpr list = (ListExpr) expression;
        if (list.elements.isEmpty()) {
            return "None";
        }
        String head = headName(list);
        if ("if".equals(head)) {
            return "(" + expr(list.elements.get(2)) + " if " + expr(list.elements.get(1))
                    + " else " + expr(list.elements.get(3)) + ")";
        }
        if ("begin".equals(head)) {
            return expr(list.elements.get(list.elements.size() - 1));
        }
        if ("lambda".equals(head)) {
            return lambdaExpr(list);
        }
        if ("let".equals(head)) {
            return letExpr(list);
        }
        if ("+".equals(head) || "*".equals(head) || "<".equals(head) || "<=".equals(head)
                || ">".equals(head) || ">=".equals(head) || "=".equals(head)) {
            return infix(list, pythonOperator(head));
        }
        if ("-".equals(head)) {
            return minusExpr(list);
        }
        if ("/".equals(head)) {
            return divisionExpr(list);
        }
        if ("and".equals(head) || "or".equals(head)) {
            return infix(list, head);
        }
        if ("not".equals(head)) {
            return "(not " + expr(list.elements.get(1)) + ")";
        }
        if ("display".equals(head) || "newline".equals(head) || "set!".equals(head) || "define".equals(head)) {
            return "None";
        }
        return callExpr(list);
    }

    private String atomExpr(AtomExpr atom) {
        switch (atom.type) {
            case INTEGER:
            case FLOAT:
                return atom.text;
            case RATIONAL:
                return rationalExpr(atom.text);
            case HEX_INTEGER:
                return "0x" + atom.text.substring(2);
            case BIN_INTEGER:
                return "0b" + atom.text.substring(2);
            case STRING:
                return quote(atom.text);
            case CHARACTER:
                return quote(characterValue(atom.text));
            case BOOLEAN:
                return atom.text.equals("#t") ? "True" : "False";
            case IDENTIFIER:
                return NameUtil.toPythonName(atom.text);
            default:
                return "None";
        }
    }

    private String vectorExpr(VectorExpr vector) {
        return vector.elements.stream()
                .map(this::expr)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String prefixExpr(PrefixExpr prefix) {
        if (prefix.prefix == TokenType.QUOTE || prefix.prefix == TokenType.QUASIQUOTE) {
            return literal(prefix.value);
        }
        return expr(prefix.value);
    }

    private String dottedExpr(DottedListExpr dotted) {
        String head = dotted.head.stream().map(this::literal).collect(Collectors.joining(", ", "[", "]"));
        return "(" + head + ", " + literal(dotted.tail) + ")";
    }

    private String literal(Expr expression) {
        if (expression instanceof AtomExpr) {
            AtomExpr atom = (AtomExpr) expression;
            if (atom.type == TokenType.IDENTIFIER) {
                return quote(atom.text);
            }
            return atomExpr(atom);
        }
        if (expression instanceof ListExpr) {
            ListExpr list = (ListExpr) expression;
            return list.elements.stream().map(this::literal).collect(Collectors.joining(", ", "[", "]"));
        }
        if (expression instanceof VectorExpr) {
            VectorExpr vector = (VectorExpr) expression;
            return vector.elements.stream().map(this::literal).collect(Collectors.joining(", ", "[", "]"));
        }
        if (expression instanceof DottedListExpr) {
            return dottedExpr((DottedListExpr) expression);
        }
        if (expression instanceof PrefixExpr) {
            PrefixExpr prefix = (PrefixExpr) expression;
            return literal(prefix.value);
        }
        return "None";
    }

    private String lambdaExpr(ListExpr list) {
        ListExpr params = (ListExpr) list.elements.get(1);
        String names = params.elements.stream()
                .map(param -> NameUtil.toPythonName(((AtomExpr) param).text))
                .collect(Collectors.joining(", "));
        return "(lambda " + names + ": " + expr(list.elements.get(list.elements.size() - 1)) + ")";
    }

    private String letExpr(ListExpr list) {
        ListExpr bindings = (ListExpr) list.elements.get(1);
        List<String> names = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (Expr bindingExpr : bindings.elements) {
            ListExpr binding = (ListExpr) bindingExpr;
            names.add(NameUtil.toPythonName(((AtomExpr) binding.elements.get(0)).text));
            values.add(expr(binding.elements.get(1)));
        }
        return "(lambda " + String.join(", ", names) + ": "
                + expr(list.elements.get(list.elements.size() - 1)) + ")("
                + String.join(", ", values) + ")";
    }

    private String infix(ListExpr list, String operator) {
        return list.elements.stream()
                .skip(1)
                .map(this::expr)
                .collect(Collectors.joining(" " + operator + " ", "(", ")"));
    }

    private String minusExpr(ListExpr list) {
        if (list.elements.size() == 2) {
            return "(-" + expr(list.elements.get(1)) + ")";
        }
        return infix(list, "-");
    }

    private String divisionExpr(ListExpr list) {
        if (list.elements.size() == 2) {
            return "(1 / " + expr(list.elements.get(1)) + ")";
        }
        return infix(list, "/");
    }

    private String rationalExpr(String value) {
        String[] parts = value.split("/", 2);
        return "(" + parts[0] + " / " + parts[1] + ")";
    }

    private String characterValue(String text) {
        String value = text.substring(2);
        if (value.equals("space")) {
            return " ";
        }
        if (value.equals("newline")) {
            return "\n";
        }
        return value;
    }

    private String callExpr(ListExpr list) {
        String name = NameUtil.toPythonName(((AtomExpr) list.elements.get(0)).text);
        String args = list.elements.stream().skip(1).map(this::expr).collect(Collectors.joining(", "));
        return name + "(" + args + ")";
    }

    private String pythonOperator(String operator) {
        return operator.equals("=") ? "==" : operator;
    }

    private String headName(ListExpr list) {
        if (list.elements.isEmpty()) {
            return "";
        }
        Expr head = list.elements.get(0);
        if (head instanceof AtomExpr) {
            AtomExpr atom = (AtomExpr) head;
            if (atom.type == TokenType.IDENTIFIER) {
                return atom.text;
            }
        }
        return "";
    }

    private boolean isVoidStatement(Expr expression) {
        if (!(expression instanceof ListExpr)) {
            return false;
        }
        ListExpr list = (ListExpr) expression;
        String head = headName(list);
        return head.equals("display")
                || head.equals("newline")
                || head.equals("set!")
                || head.equals("define");
    }

    private String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '"':
                    out.append("\\\"");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                default:
                    out.append(c);
                    break;
            }
        }
        out.append('"');
        return out.toString();
    }

    private void line(int indent, String text) {
        lines.add(repeat("    ", indent) + text);
    }

    private String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}
