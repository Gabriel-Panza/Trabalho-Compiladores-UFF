package compiler;

public enum Type {
    INT,
    FLOAT,
    STRING,
    BOOLEAN,
    VOID,
    FUNCTION,
    UNKNOWN;

    public boolean isNumeric() {
        return this == INT || this == FLOAT || this == UNKNOWN;
    }

    public boolean isKnown() {
        return this != UNKNOWN;
    }

    public static Type numericResult(Type left, Type right, boolean forceFloat) {
        if (forceFloat || left == FLOAT || right == FLOAT) {
            return FLOAT;
        }
        if (left == UNKNOWN || right == UNKNOWN) {
            return UNKNOWN;
        }
        return INT;
    }
}
