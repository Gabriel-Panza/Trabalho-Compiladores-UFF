package compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Parser {
    private static final class OpenList {
        final SourceSpan span;

        OpenList(SourceSpan span) {
            this.span = span;
        }
    }

    private final List<Token> tokens;
    private final List<Diagnostic> diagnostics = new ArrayList<>();

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
                    stack.add(new OpenList(token.span));
                    break;
                case RPAREN:
                    reduceList(stack, token);
                    break;
                default:
                    if (token.isAtom()) {
                        stack.add(new AtomExpr(token));
                    }
                    break;
            }
        }

        List<Expr> topLevel = new ArrayList<>();
        for (int i = stack.size() - 1; i >= 0; i--) {
            Object item = stack.get(i);
            if (item instanceof OpenList) {
                OpenList openList = (OpenList) item;
                diagnostics.add(Diagnostic.syntax(
                        openList.span,
                        "Este '(' foi aberto, mas nao encontrei o ')' correspondente."));
            } else if (item instanceof Expr) {
                topLevel.add((Expr) item);
            }
        }
        Collections.reverse(topLevel);

        if (!diagnostics.isEmpty()) {
            throw new CompilerException(diagnostics);
        }
        return new Program(topLevel);
    }

    private void reduceList(List<Object> stack, Token closingToken) {
        List<Expr> elements = new ArrayList<>();
        while (!stack.isEmpty()) {
            Object item = stack.remove(stack.size() - 1);
            if (item instanceof OpenList) {
                OpenList openList = (OpenList) item;
                Collections.reverse(elements);
                SourceSpan span = openList.span.until(closingToken.span);
                stack.add(new ListExpr(span, elements));
                return;
            }
            if (item instanceof Expr) {
                elements.add((Expr) item);
            }
        }

        diagnostics.add(Diagnostic.syntax(
                closingToken.span,
                "Encontrei um ')' sem um '(' aberto antes dele. Confira os parenteses desta parte."));
    }
}
