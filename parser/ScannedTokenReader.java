package parser;

import parser.model.TokenLido;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScannedTokenReader {

    // Lê tokens de um arquivo .scanned.txt gerado pelo Scanner.
    // Formato de cada linha: "001  TIPO                  lexema: "LEXEMA""
    // Filtra WHITESPACE e COMMENT; rastreia número de linha pelo conteúdo.
    public static List<TokenLido> lerTokens(Path caminho) throws IOException {
        List<TokenLido> tokens = new ArrayList<>();
        int linhaFonte = 1;
        int colunaFonte = 1;

        for (String linha : Files.readAllLines(caminho, StandardCharsets.UTF_8)) {
            if (linha.isBlank() || linha.startsWith("=") || linha.equals("TOKENS RECONHECIDOS")) {
                continue;
            }

            int sep = linha.indexOf("  lexema: \"");
            if (sep == -1) continue;

            String[] partes = linha.substring(0, sep).trim().split("\\s+", 2);
            if (partes.length < 2) continue;
            String tipo = partes[1].trim();

            String lexemaBruto = linha.substring(sep + "  lexema: \"".length());
            if (lexemaBruto.endsWith("\"")) {
                lexemaBruto = lexemaBruto.substring(0, lexemaBruto.length() - 1);
            }
            String lexema = desescapar(lexemaBruto);

            if (tipo.equals("WHITESPACE") || tipo.equals("COMMENT")) {
                for (char c : lexema.toCharArray()) {
                    if (c == '\n') { linhaFonte++; colunaFonte = 1; }
                    else { colunaFonte++; }
                }
                continue;
            }

            tokens.add(new TokenLido(tipo, lexema, linhaFonte, colunaFonte));
            colunaFonte += lexema.length();
        }

        return tokens;
    }

    private static String desescapar(String s) {
        return s.replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
