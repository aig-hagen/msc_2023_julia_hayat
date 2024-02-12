package de.julsched.beliefchange.values;

public enum EncodingType {
    ASP("asp", ".lp"),
    ILP("ilp", ".mod"),
    NAIVE("naive", ""),
    SAT("sat", ".cnf");

    private static EncodingType defaultType = SAT;

    private String name;
    private String fileExtension;

    EncodingType(String name, String fileExtension) {
        this.name = name;
        this.fileExtension = fileExtension;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getFileExtension() {
        return this.fileExtension;
    }

    public static EncodingType getType(String name) {
        for (EncodingType type : EncodingType.values()) {
            if (type.toString().equals(name)) {
                return type;
            }
        }
        return null;
    }

    public static EncodingType getDefault() {
        return defaultType;
    }

    public static String getValues() {
        String values = "";
        for (EncodingType type : EncodingType.values()) {
            if (!values.isEmpty()) {
                values += "|";
            }
            values += type.toString();
        }
        return values;
    }
}
