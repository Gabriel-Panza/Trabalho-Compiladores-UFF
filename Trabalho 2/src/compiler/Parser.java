package compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Parser {
    private static final class OpenList {
        final SourceSpan span;
        final boolean vector;

        OpenList(SourceSpan span, boolean vector) {
            this.span = span;
            this.vector = vector;
        }
    }

    private static final class DotMark {
        final SourceSpan span;

        DotMark(SourceSpan span) {
            this.span = span;
        }
    }

    private static final class PrefixMark {
        final Token token;

        PrefixMark(Token token) {
            this.token = token;
        }
    }

    private final List<Token> tokens;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final ParseContext context = new ParseContext();
    private int analyzedTopLevel = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public Program parse() throws CompilerException {
        List<Object> stack = new ArrayList<>();

        for (Token token : tokens) {
            if (token.type == TokenType.EOF) {
                break;
            }
            switch (token.type) {
                case LPAREN:
                    stack.add(new OpenList(token.span, false));
                    break;
                case VECTOR_START:
                    stack.add(new OpenList(token.span, true));
                    break;
                case RPAREN:
                    reduceList(stack, token);
                    reducePrefixes(stack);
                    consumeCompleteTopLevel(stack);
                    break;
                case DOT:
                    stack.add(new DotMark(token.span));
                    break;
                case QUOTE:
                case QUASIQUOTE:
                case UNQUOTE:
                case UNQUOTE_SPLICING:
                    stack.add(new PrefixMark(token));
                    break;
                default:
                    if (token.isAtom()) {
                        stack.add(new AtomExpr(token));
                        reducePrefixes(stack);
                        consumeCompleteTopLevel(stack);
                    }
                    break;
            }
        }

        List<Expr> topLevel = collectTopLevel(stack);
        if (!diagnostics.isEmpty()) {
            throw new CompilerException(diagnostics);
        }
        return new Program(topLevel);
    }

    public List<Diagnostic> semanticDiagnostics() {
        return context.diagnostics();
    }

    private void reduceList(List<Object> stack, Token closingToken) {
        List<Object> items = new ArrayList<>();
        while (!stack.isEmpty()) {
            Object item = stack.remove(stack.size() - 1);
            if (item instanceof OpenList) {
                OpenList openList = (OpenList) item;
                Collections.reverse(items);
                SourceSpan span = openList.span.until(closingToken.span);
                Expr expression = openList.vector
                        ? reduceVector(items, span)
                        : reduceNormalList(items, span);
                if (expression != null) {
                    stack.add(expression);
                }
                return;
            }
            items.add(item);
        }

        diagnostics.add(Diagnostic.syntax(
                closingToken.span,
                "Encontrei um ')' sem um '(' aberto antes dele. Confira os parenteses desta parte."));
    }

    private Expr reduceVector(List<Object> items, SourceSpan span) {
        List<Expr> elements = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof DotMark) {
                diagnostics.add(Diagnostic.syntax(
                        ((DotMark) item).span,
                        "O ponto nao pode aparecer dentro de um vetor."));
                return null;
            }
            if (item instanceof PrefixMark) {
                diagnostics.add(Diagnostic.syntax(
                        ((PrefixMark) item).token.span,
                        "Esta marcacao precisa vir antes de uma expressao."));
                return null;
            }
            elements.add((Expr) item);
        }
        return new VectorExpr(span, elements);
    }

    private Expr reduceNormalList(List<Object> items, SourceSpan span) {
        int dotIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item instanceof PrefixMark) {
                diagnostics.add(Diagnostic.syntax(
                        ((PrefixMark) item).token.span,
                        "Esta marcacao precisa vir antes de uma expressao."));
                return null;
            }
            if (item instanceof DotMark) {
                if (dotIndex >= 0) {
                    diagnostics.add(Diagnostic.syntax(
                            ((DotMark) item).span,
                            "Esta lista tem mais de um ponto. Use apenas um ponto para formar um par."));
                    return null;
                }
                dotIndex = i;
            }
        }

        if (dotIndex < 0) {
            List<Expr> elements = new ArrayList<>();
            for (Object item : items) {
                elements.add((Expr) item);
            }
            return new ListExpr(span, elements);
        }

        if (dotIndex == 0 || dotIndex != items.size() - 2) {
            SourceSpan errorSpan = ((DotMark) items.get(dotIndex)).span;
            diagnostics.add(Diagnostic.syntax(
                    errorSpan,
                    "Para usar ponto em lista, escreva no formato (valor . resto)."));
            return null;
        }

        List<Expr> head = new ArrayList<>();
        for (int i = 0; i < dotIndex; i++) {
            head.add((Expr) items.get(i));
        }
        Expr tail = (Expr) items.get(dotIndex + 1);
        return new DottedListExpr(span, head, tail);
    }

    private void reducePrefixes(List<Object> stack) {
        boolean changed = true;
        while (changed && stack.size() >= 2) {
            changed = false;
            Object value = stack.get(stack.size() - 1);
            Object prefix = stack.get(stack.size() - 2);
            if (value instanceof Expr && prefix instanceof PrefixMark) {
                stack.remove(stack.size() - 1);
                PrefixMark mark = (PrefixMark) stack.remove(stack.size() - 1);
                stack.add(new PrefixExpr(mark.token, (Expr) value));
                changed = true;
            }
        }
    }

    private void consumeCompleteTopLevel(List<Object> stack) {
        for (Object item : stack) {
            if (!(item instanceof Expr)) {
                return;
            }
        }

        while (analyzedTopLevel < stack.size()) {
            context.acceptTopLevel((Expr) stack.get(analyzedTopLevel));
            analyzedTopLevel++;
        }
    }

    private List<Expr> collectTopLevel(List<Object> stack) {
        List<Expr> topLevel = new ArrayList<>();
        for (int i = stack.size() - 1; i >= 0; i--) {
            Object item = stack.get(i);
            if (item instanceof OpenList) {
                OpenList openList = (OpenList) item;
                diagnostics.add(Diagnostic.syntax(
                        openList.span,
                        "Este '(' foi aberto, mas nao encontrei o ')' correspondente."));
            } else if (item instanceof PrefixMark) {
                PrefixMark prefix = (PrefixMark) item;
                diagnostics.add(Diagnostic.syntax(
                        prefix.token.span,
                        "Esta marcacao precisa vir antes de uma expressao."));
            } else if (item instanceof DotMark) {
                DotMark dot = (DotMark) item;
                diagnostics.add(Diagnostic.syntax(
                        dot.span,
                        "O ponto so pode aparecer dentro de uma lista."));
            } else if (item instanceof Expr) {
                topLevel.add((Expr) item);
            }
        }
        Collections.reverse(topLevel);
        return topLevel;
    }
}
