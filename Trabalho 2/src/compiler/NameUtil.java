package compiler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class NameUtil {
    private static final Set<String> PYTHON_KEYWORDS = new HashSet<>(Arrays.asList(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break",
            "class", "continue", "def", "del", "elif", "else", "except", "finally",
            "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
            "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"));

    private NameUtil() {
    }

    public static String toPythonName(String schemeName) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < schemeName.length(); i++) {
            char c = schemeName.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                out.append(c);
            } else {
                out.append('_');
            }
        }
        if (out.length() == 0 || Character.isDigit(out.charAt(0))) {
            out.insert(0, '_');
        }
        String name = out.toString();
        if (PYTHON_KEYWORDS.contains(name)) {
            return name + "_";
        }
        return name;
    }
}
