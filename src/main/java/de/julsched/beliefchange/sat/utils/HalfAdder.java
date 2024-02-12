package de.julsched.beliefchange.sat.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


class HalfAdder {

    private static final List<String> clausesTemplate = Arrays.asList(new String[] {
        "-n1 -n2 c 0",
        "n1 -n2 s 0",
        "-n1 n2 s 0"
    });

    public static List<String> getClauses(int num1Var, int num2Var, int counterVar, int sumVar) {
        List<String> clauses = new ArrayList<String>();
        for (String clause : clausesTemplate) {
            clauses.add(clause.replaceAll("n1", Integer.toString(num1Var))
                              .replaceAll("n2", Integer.toString(num2Var))
                              .replaceAll("c", Integer.toString(counterVar))
                              .replaceAll("s", Integer.toString(sumVar))
            );
        }
        return clauses;
    }
}
