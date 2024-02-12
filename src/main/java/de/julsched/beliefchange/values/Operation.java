package de.julsched.beliefchange.values;

public enum Operation {
    CONTRACTION("contraction"),
    REVISION("revision");

    private static Operation defaulOperation = REVISION;

    private String name;

    Operation(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Operation getOperation(String name) {
        for (Operation operation : Operation.values()) {
            if (operation.toString().equals(name)) {
                return operation;
            }
        }
        return null;
    }

    public static Operation getDefault() {
        return defaulOperation;
    }

    public static String getValues() {
        String values = "";
        for (Operation operation : Operation.values()) {
            if (!values.isEmpty()) {
                values += "|";
            }
            values += operation.toString();
        }
        return values;
    }
}
