package compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Uso: java -cp build compiler.Main <entrada.scm> [saida.py]");
            System.exit(2);
        }

        Path inputPath = Paths.get(args[0]);
        Path outputPath = args.length == 2 ? Paths.get(args[1]) : defaultOutputPath(inputPath);
        PipelinePrinter printer = new PipelinePrinter(System.out);
        SourceFile source = null;

        try {
            printer.step(1, "Entrada");
            printer.item("arquivo", inputPath.toString());
            source = SourceFile.read(inputPath);
            printer.item("resultado", "arquivo carregado");
            printer.item("caracteres lidos", Integer.toString(source.text.length()));

            printer.step(2, "Scanner");
            List<Token> tokens = new Lexer(source).lex();
            printer.item("resultado", "analise lexica concluida");
            printer.tokens(tokens);

            printer.step(3, "Parser bottom-up");
            Program program = new Parser(tokens).parse();
            printer.item("resultado", "programa aceito");
            printer.ast(program);

            printer.step(4, "Verificacao semantica");
            new SemanticAnalyzer().analyze(program);
            printer.item("resultado", "sem erros");
            printer.message("identificadores, chamadas de funcao e tipos foram verificados");

            printer.step(5, "Geracao de Python");
            String python = new PythonGenerator().generate(program);

            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputPath, python.getBytes(StandardCharsets.UTF_8));
            printer.item("arquivo", outputPath.toString());
            printer.item("resultado", "codigo gerado com sucesso");
            printer.generatedCode(python);
        } catch (CompilerException error) {
            printer.item("resultado", "foram encontrados erros nesta etapa");
            printer.flush();
            if (source == null) {
                for (Diagnostic diagnostic : error.diagnostics()) {
                    System.err.println(diagnostic.kind.label() + ": " + diagnostic.message);
                }
            } else {
                for (Diagnostic diagnostic : error.diagnostics()) {
                    System.err.println(source.format(diagnostic));
                    System.err.println();
                }
            }
            System.exit(1);
        } catch (IOException error) {
            System.err.println("Nao consegui ler ou escrever o arquivo informado.");
            System.err.println("Detalhe: " + error.getMessage());
            System.exit(1);
        }
    }

    private static Path defaultOutputPath(Path inputPath) {
        String fileName = inputPath.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot >= 0 ? fileName.substring(0, dot) : fileName;
        return Paths.get("out", baseName + ".py");
    }
}
