package de.julsched.beliefchange.values;

public enum Distance {
    DALAL("dalal"),
    SATOH("satoh");

    private static Distance defaultDistance = DALAL;

    private String name;

    Distance(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Distance getDistance(String name) {
        for (Distance distance : Distance.values()) {
            if (distance.toString().equals(name)) {
                return distance;
            }
        }
        return null;
    }

    public static Distance getDefault() {
        return defaultDistance;
    }

    public static String getValues() {
        String values = "";
        for (Distance distance : Distance.values()) {
            if (!values.isEmpty()) {
                values += "|";
            }
            values += distance.toString();
        }
        return values;
    }
}
