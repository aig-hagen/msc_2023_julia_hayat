package de.julsched.beliefchange.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.transformations.cnf.TseitinTransformation;

import de.julsched.beliefchange.exceptions.CnfConversionException;

public class Tseitin {

    // Caution: returned array also contains the parameter line
    public static String[] negateCnfFormula(int varNum, List<String> clauses) {
        try {
            StringBuilder conjunction = new StringBuilder();
            for (int x = 0; x < clauses.size(); x++) {
                String clause = clauses.get(x);
                if (x > 0) {
                    conjunction.append(" & ");
                }
                StringBuilder disjunction = new StringBuilder();
                String[] vars = clause.split(" ");
                for (int i = 0; i < vars.length - 1; i++) {
                    if (i > 0) {
                        disjunction.append(" | ");
                    }
                    disjunction.append(vars[i].replace('-', '~'));
                }
                conjunction.append("(")
                           .append(disjunction)
                           .append(")");
            }

            FormulaFactory f = new FormulaFactory();
            PropositionalParser p = new PropositionalParser(f);
            Formula formula = p.parse(conjunction.toString()).negate();
            return Tseitin.runCnfTransformation(formula, varNum);
        } catch (Exception e) {
            throw new CnfConversionException(e);
        }
    }

    private static String[] runCnfTransformation(Formula formula, int varNum) {
        TseitinTransformation transformation = new TseitinTransformation(0);
        String formulaCnf = transformation.apply(formula, true).toString();
        if (formulaCnf.contains("$true")) {
            // Formula is a tautology
            String[] array = new String[1];
            array[0] = "tautology";
            return array;
        }
        if (formulaCnf.contains("$false")) {
            String[] array = new String[1];
            array[0] = "unsat";
            return array;
        }

        List<String> disjunctionsDimacs = new ArrayList<>();
        int nextNum = varNum + 1;
        HashMap<String, String> newVarMap = new HashMap<>();
        String[] disjunctions = formulaCnf.split(" & ");
        for (String disjunction : disjunctions) {
            StringBuilder disjunctionDimacs = new StringBuilder();
            String[] vars = disjunction.replace("(", "").replace(")", "").trim().split(" \\| ");
            for (String var : vars) {
                StringBuilder literal = new StringBuilder(var.replace('~', '-').trim());
                if (literal.toString().startsWith("-@RESERVED_CNF_")) {
                    String newVariable = literal.toString().replaceAll("-", "");
                    if (newVarMap.containsKey(newVariable)) {
                        literal = new StringBuilder("-")
                                    .append(newVarMap.get(newVariable));
                    } else {
                        literal = new StringBuilder("-")
                                    .append(nextNum);
                        newVarMap.put(newVariable, Integer.toString(nextNum));
                        nextNum++;
                    }
                } else if (literal.toString().startsWith("@RESERVED_CNF_")) {
                    if (newVarMap.containsKey(literal.toString())) {
                        literal = new StringBuilder(newVarMap.get(literal.toString()));
                    } else {
                        literal = new StringBuilder(Integer.toString(nextNum));
                        newVarMap.put(literal.toString(), Integer.toString(nextNum));
                        nextNum++;
                    }
                }
                disjunctionDimacs.append(literal)
                                    .append(" ");
            }
            disjunctionDimacs.append("0");
            disjunctionsDimacs.add(disjunctionDimacs.toString());
        }

        StringBuilder finalDimacs = new StringBuilder("p cnf ")
                                        .append(nextNum - 1)
                                        .append(" ")
                                        .append(disjunctionsDimacs.size())
                                        .append("\n");
        for (String disjunction : disjunctionsDimacs) {
            finalDimacs.append(disjunction)
                        .append("\n");
        }

        return finalDimacs.toString().split("\n");
    }

    public static String[] transformToCnf(String formula, int varNum) {
        try {
            FormulaFactory f = new FormulaFactory();
            PropositionalParser p = new PropositionalParser(f);
            return Tseitin.runCnfTransformation(p.parse(formula.toString()), varNum);
        } catch (Exception e) {
            throw new CnfConversionException(e);
        }
    }
}
