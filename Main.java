import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        // Ler o arquivo com as regex
        String[] testCases = {
                "a(b|c)*",
                "a.b*",
                "(a|b)*abb",
                "ab|c"
        };

        List<String> postfixRegexs = new ArrayList<>();

        for (String test : testCases) {
            String postfix = RegexToPostfix.convertPostfix(test);

            System.out.println("Regex:   " + test);
            System.out.println("Postfix: " + postfix + "\n");

            postfixRegexs.add(postfix);
        }
    }
}