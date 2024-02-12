package de.julsched.beliefchange.values;

public enum Algorithm {
    ASP("asp"),
    ILP("ilp"),
    MAXSAT("maxsat");

    private static Algorithm defaultAlgorithm = MAXSAT;

    private String name;

    Algorithm(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Algorithm getAlgorithm(String name) {
        for (Algorithm algorithm : Algorithm.values()) {
            if (algorithm.toString().equals(name)) {
                return algorithm;
            }
        }
        return null;
    }

    public static Algorithm getDefault() {
        return defaultAlgorithm;
    }

    public static String getValues() {
        String values = "";
        for (Algorithm algorithm : Algorithm.values()) {
            if (!values.isEmpty()) {
                values += "|";
            }
            values += algorithm.toString();
        }
        return values;
    }
}
