import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String[][] testCases = {
                { "[0-9]+\\.[0-9]+", "FLOAT" },
                { "ab|c", "STRING" },
                { "[a-z][a-z0-9]*", "IDENTIFICADOR" },
                { "while", "PALAVRA_RESERVADA" },
                { "(+ U -) [0-9]+ (.) [0-9]+", "NUMERO_COMPLEXO" },
                { "a(b|c)*", "TESTE_A" }
        };

        List<RegexToPostfix.RegraLexica> regrasProntas = new ArrayList<>();
        for (String[] test : testCases) {
            String regex = test[0];
            String tipo = test[1];

            String regexLimpa = regex.replace(" ", "");
            List<String> postfix = RegexToPostfix.convertPostfix(regexLimpa);

            RegexToPostfix.RegraLexica regraLexica = new RegexToPostfix.RegraLexica(tipo, postfix);
            regrasProntas.add(regraLexica);

            System.out.println("Entrada: (" + regex + ", " + tipo + ")");
            System.out.println("Saída:   RegraLexica(\"" + regraLexica.tipo() + "\", " + regraLexica.postfix() + ")\n");
        }
    }
}