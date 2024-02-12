package de.julsched.beliefchange.sat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.OptimumFinder;
import de.julsched.beliefchange.exceptions.MinDistanceException;
import de.julsched.beliefchange.exceptions.MinimalSetConstraintsDeterminationException;
import de.julsched.beliefchange.instance.ContractionInstance;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.instance.RevisionInstance;
import de.julsched.beliefchange.utils.Tseitin;
import de.julsched.beliefchange.utils.MaxHS;
import de.julsched.beliefchange.utils.Utils;
import de.julsched.beliefchange.values.Distance;

public class MaxSat extends OptimumFinder {

    private static final String optimizationFileName = Application.dirInterimResults + "/maxsat-%s-optimization.wcnf";

    public MaxSat(Distance distance) {
        super(distance);
    }

    public String getOptimum(ContractionEncoding encoding) {
        List<String> clauses = new ArrayList<String>();
        clauses.addAll(encoding.getBaseClausesNew());
        clauses.addAll(encoding.getNegatedChangeClauses());
        clauses.addAll(encoding.getDiscrepancyClausesMaxSat());

        String optimumEncoding = createEncoding(
            clauses,
            encoding.getVarNumMaxSat(),
            encoding.getDiscrepancyVarsMaxSat()
        );
        return getMaxhsResult(optimumEncoding);
    }

    public String getOptimum(RevisionEncoding encoding) {
        String optimumEncoding = createEncoding(
            encoding.getResult(),
            encoding.getVarNum(),
            encoding.getDiscrepancyVars()
        );
        return getMaxhsResult(optimumEncoding);
    }

    public String createEncoding(List<String> clauses, int varNum, List<Integer> discrepancyVars) {
        // Determine parameter line
        int maxSatClauseNum = clauses.size() + discrepancyVars.size();
        String top = Integer.toString(discrepancyVars.size() + 1);
        String paramsLine = "p wcnf " + varNum + " " + maxSatClauseNum + " " + top;

        List<String> maxSatClauses = new ArrayList<String>();
        maxSatClauses.add(paramsLine);

        // Mark hard clauses
        StringBuilder maxSatClause;
        for (String clause : clauses) {
            maxSatClause = new StringBuilder(top);
            maxSatClause.append(" ")
                        .append(clause);
            maxSatClauses.add(maxSatClause.toString());
        }

        // Add soft clauses
        for (Integer d : discrepancyVars) {
            maxSatClause = new StringBuilder("1 -");
            maxSatClause.append(d)
                        .append(" 0");
            maxSatClauses.add(maxSatClause.toString());
        }
        maxSatClauses.add("");

        return StringUtils.join(maxSatClauses, "\n");
    }

    private String getMaxhsResult(String optimumEncoding) {
        try {
            Utils.writeToFile(optimumEncoding, String.format(optimizationFileName, "dalal"));

            System.out.println("[INFO] Start solver call");
            Application.solverCallsStartTime = System.currentTimeMillis();
            Process process = MaxHS.execute(String.format(optimizationFileName, "dalal"), false);
            Application.solverCallsEndTime = System.currentTimeMillis();
            System.out.println("[INFO] Finished solver call");

            return MaxHS.readOptimumLine(process);
        } catch (MinDistanceException e) {
            throw e;
        } catch (Exception e) {
            throw new MinDistanceException(e);
        }
    }

    public String getMaxhsResultSatoh(String optimumEncoding) throws IOException, InterruptedException {
        Utils.writeToFile(optimumEncoding, String.format(optimizationFileName, "satoh"));
        Process process = MaxHS.execute(String.format(optimizationFileName, "satoh"), true);
        return MaxHS.readOptimumSolutionModel(process);
    }

    @Override
    public String getOptimum(BeliefChangeInstance instance) {
        List<String> clauses = new ArrayList<String>();
        int varNum = instance.getVarNum();
        clauses.addAll(instance.getBaseClauses());
        HashMap<Integer, Integer> varMap  = new HashMap<Integer, Integer>();
        ArrayList<Integer> discrepancyVars = new ArrayList<Integer>();

        if (instance instanceof RevisionInstance) {
            for (int v = 1; v <= varNum; v++) {
                varMap.put(v, varNum + v);
            }
            varNum = varNum * 2;
            for (String changeClause : instance.getChangeClauses()) {
                // Replace variables
                String[] vars = changeClause.split(" ");
                StringBuilder alteredClause = new StringBuilder();
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    String var = vars[i];
                    String sign = "";
                    if (var.startsWith("-")) {
                        sign = "-";
                        var = var.replace("-", "");
                    }
                    alteredClause.append(sign)
                                .append(varMap.get(Integer.parseInt(var)))
                                .append(" ");
                }
                alteredClause.append("0");
                clauses.add(alteredClause.toString());
            }
        } else if (instance instanceof ContractionInstance) {
            String[] dimacsNegation = Tseitin.negateCnfFormula(varNum, instance.getChangeClauses());
            if (dimacsNegation[0].equals("tautology")) {
                throw new MinDistanceException("Contraction formula is unsatisfiable");
            }
            if (dimacsNegation[0].equals("unsat")) {
                throw new MinDistanceException("Contraction formula is a tautology");
            }
            varNum = Integer.parseInt(BeliefChangeInstance.extractVarNum(dimacsNegation[0]));
            List<String> negatedChangeClauses = new ArrayList<String>();
            for (int i = 1; i < dimacsNegation.length; i++) {
                negatedChangeClauses.add(dimacsNegation[i]);
            }

            for (int v = 1; v <= instance.getVarNum(); v++) {
                varMap.put(v, varNum + v);
            }
            varNum += instance.getVarNum();

            for (String changeClause : negatedChangeClauses) {
                String[] vars = changeClause.split(" ");
                StringBuilder alteredClause = new StringBuilder();
                for (int x = 0; x < vars.length - 1; x++) { // Skip last index (since that is '0')
                    String var = vars[x];
                    String sign = "";
                    if (var.startsWith("-")) {
                        sign = "-";
                        var = var.replace("-", "");
                    }
                    if (varMap.containsKey(Integer.parseInt(var))) {
                        alteredClause.append(sign)
                                    .append(varMap.get(Integer.parseInt(var)))
                                    .append(" ");
                    } else {
                        alteredClause.append(sign)
                                    .append(var)
                                    .append(" ");
                    }
                }
                alteredClause.append("0");
                clauses.add(alteredClause.toString());
            }
        }

        for (int i = 1; i <= varMap.size(); i++) {
            varNum++;
            discrepancyVars.add(varNum);
        }
        List<String> discrepancyClauseTemplates;
        if (this.distance == Distance.SATOH) {
            discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesExact;
        } else {
            discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesVague;
        }
        int counter = 0;
        for (Map.Entry<Integer, Integer> entry : varMap.entrySet()) {
            for (String template : discrepancyClauseTemplates) {
                clauses.add(template.replaceAll("d", Integer.toString(discrepancyVars.get(counter)))
                                    .replaceAll("x", Integer.toString(entry.getKey()))
                                    .replaceAll("y", Integer.toString(entry.getValue())));

            }
            counter++;
        }

        String optimumEncoding = createEncoding(
            clauses,
            varNum,
            discrepancyVars
        );
        return getMaxhsResult(optimumEncoding);
    }

    @Override
    public String getMinSetConstraints(BeliefChangeInstance instance) {
        try {
            StringBuilder minimalDistanceSetConstraints = new StringBuilder();
            int varNum = instance.getVarNum();
            List<Integer> discrepancyVars = new ArrayList<Integer>();
            List<Integer> discrepancyVars2 = new ArrayList<Integer>();
            List<String> baseClausesNew = new ArrayList<>();
            List<String> changeClausesNew = new ArrayList<>(instance.getChangeClauses());
            List<String> discrepancyClauses = new ArrayList<String>();
            if (instance instanceof RevisionInstance) {
                HashMap<Integer, Integer> varMap = new HashMap<>();
                for (int v = 1; v <= varNum; v++) {
                    varMap.put(v, varNum + v);
                }
                varNum = varNum * 2;

                // Replace variables
                for (String clause : instance.getBaseClauses()) {
                    String[] vars = clause.split(" ");
                    StringBuilder alteredClause = new StringBuilder();
                    for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                        String var = vars[i];
                        String sign = "";
                        if (var.startsWith("-")) {
                            sign = "-";
                            var = var.replace("-", "");
                        }
                        alteredClause.append(sign)
                                     .append(varMap.get(Integer.parseInt(var)))
                                     .append(" ");
                    }
                    alteredClause.append("0\n");
                    baseClausesNew.add(alteredClause.toString().trim());
                }

                for (int i = 1; i <= varMap.size(); i++) {
                    varNum++;
                    discrepancyVars.add(varNum);
                }

                List<String> discrepancyClauseTemplates;
                if (this.distance == Distance.SATOH) {
                    discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesExact;
                } else {
                    discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesVague;
                }

                int counter = 0;
                for (Map.Entry<Integer, Integer> entry : varMap.entrySet()) {
                    for (String template : discrepancyClauseTemplates) {
                        String clause = template.replaceAll("d", Integer.toString(discrepancyVars.get(counter)))
                                                .replaceAll("x", Integer.toString(entry.getKey()))
                                                .replaceAll("y", Integer.toString(entry.getValue()));
                        discrepancyClauses.add(clause);
                    }
                    counter++;
                }

                List<String> minimalSetEncoding = createEncodingSatohRevision(instance,
                                                                            varNum,
                                                                            baseClausesNew,
                                                                            changeClausesNew,
                                                                            discrepancyClauses,
                                                                            discrepancyVars,
                                                                            discrepancyVars2);

                String paramLine = minimalSetEncoding.get(0);
                List<String> clauses = new ArrayList<>();
                for (int i = 1; i < minimalSetEncoding.size(); i++) { // Skip index 0 since that is the param line
                    clauses.add(minimalSetEncoding.get(i));
                }
                String maxSatEncoding = createEncoding(clauses,
                                                       Integer.parseInt(BeliefChangeInstance.extractVarNum(paramLine)),
                                                       discrepancyVars2);
                System.out.println("[INFO] Start solver calls");
                Application.solverCallsStartTime = System.currentTimeMillis();
                String result = getMaxhsResultSatoh(maxSatEncoding);
                while (!result.isEmpty()) {
                    StringBuilder newConstraintMaxSat = new StringBuilder();
                    StringBuilder minimalDistanceSetConstraint = new StringBuilder();
                    String [] vars = result.split(" ");
                    boolean isEmptySet = true;
                    for (int i = 0; i < vars.length; i++) {
                        String var = vars[i];
                        if (!var.startsWith("-")) {
                            int varInt = Integer.parseInt(var);
                            if (discrepancyVars2.contains(varInt)) {
                                isEmptySet = false;
                                int index = discrepancyVars2.indexOf(varInt);
                                minimalDistanceSetConstraint.append(index + 1)
                                                            .append(" ");

                                newConstraintMaxSat.append("-")
                                                   .append(varInt)
                                                   .append(" ");
                            }
                        }
                    }
                    if (isEmptySet) {
                        minimalDistanceSetConstraints = new StringBuilder("0");
                        break;
                    }
                    newConstraintMaxSat.append("0");
                    clauses.add(newConstraintMaxSat.toString());
                    minimalDistanceSetConstraints.append(minimalDistanceSetConstraint.toString().trim())
                                                 .append("\n");

                    maxSatEncoding = createEncoding(clauses, Integer.parseInt(BeliefChangeInstance.extractVarNum(paramLine)), discrepancyVars2);
                    result = getMaxhsResultSatoh(maxSatEncoding);
                }
                Application.solverCallsEndTime = System.currentTimeMillis();
                System.out.println("[INFO] Finished solver calls");
            } else if (instance instanceof ContractionInstance) {
                String[] dimacsNegation = Tseitin.negateCnfFormula(varNum, instance.getChangeClauses());
                if (dimacsNegation[0].equals("tautology")) {
                    throw new MinimalSetConstraintsDeterminationException("Contraction formula is unsatisfiable");
                }
                if (dimacsNegation[0].equals("unsat")) {
                    throw new MinimalSetConstraintsDeterminationException("Contraction formula is a tautology");
                }
                varNum = Integer.parseInt(BeliefChangeInstance.extractVarNum(dimacsNegation[0]));
                List<String> negatedChangeClauses = new ArrayList<String>();
                for (int i = 1; i < dimacsNegation.length; i++) {
                    negatedChangeClauses.add(dimacsNegation[i]);
                }

                HashMap<Integer, List<Integer>> varMap = new HashMap<Integer, List<Integer>>();
                for (int v = 1; v <= instance.getVarNum(); v++) {
                    List<Integer> vars = new ArrayList<Integer>();
                    vars.add(varNum + v);
                    vars.add(varNum + v + instance.getVarNum());
                    varMap.put(v, vars);
                }
                varNum += instance.getVarNum();

                // Replace variables
                for (String clause : instance.getBaseClauses()) {
                    String[] vars = clause.split(" ");
                    StringBuilder alteredClause = new StringBuilder();
                    for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                        String var = vars[i];
                        String sign = "";
                        if (var.startsWith("-")) {
                            sign = "-";
                            var = var.replace("-", "");
                        }
                        alteredClause.append(sign)
                                     .append(varMap.get(Integer.parseInt(var)).get(0))
                                     .append(" ");
                    }
                    alteredClause.append("0\n");
                    baseClausesNew.add(alteredClause.toString().trim());
                }

                // Replace variables in negated contraction formula
                for (String newClause : negatedChangeClauses) {
                    String[] vars = newClause.split(" ");
                    StringBuilder alteredClause = new StringBuilder();
                    for (int x = 0; x < vars.length - 1; x++) { // Skip last index (since that is '0')
                        String var = vars[x];
                        String sign = "";
                        if (var.startsWith("-")) {
                            sign = "-";
                            var = var.replace("-", "");
                        }
                        if (varMap.containsKey(Integer.parseInt(var))) {
                            alteredClause.append(sign)
                                         .append(varMap.get(Integer.parseInt(var)).get(1))
                                         .append(" ");
                        } else {
                            alteredClause.append(sign)
                                         .append(var)
                                         .append(" ");
                        }
                    }
                    alteredClause.append("0\n");
                    changeClausesNew.add(alteredClause.toString());
                }

                List<String> discrepancyClauseTemplates;
                if (this.distance == Distance.SATOH) {
                    discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesExact;
                } else {
                    discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesVague;
                }

                for (int i = 1; i <= varMap.size(); i++) {
                    varNum++;
                    discrepancyVars.add(varNum);
                }

                if (this.distance == Distance.SATOH) {
                    discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesExact;
                } else {
                    discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesVague;
                }
                List<String> discrepancyClausesMaxSat = new ArrayList<String>();
                int counter = 0;
                for (Map.Entry<Integer, List<Integer>> entry : varMap.entrySet()) {
                    for (String template : discrepancyClauseTemplates) {
                        discrepancyClausesMaxSat.add(template.replaceAll("d", Integer.toString(discrepancyVars.get(counter)))
                                                             .replaceAll("x", Integer.toString(entry.getKey()))
                                                             .replaceAll("y", Integer.toString(entry.getValue().get(0))));

                    }
                    counter++;
                }


                List<String> minimalSetEncoding = createEncodingSatohContraction(varNum,
                                                                                baseClausesNew,
                                                                                negatedChangeClauses,
                                                                                discrepancyClausesMaxSat,
                                                                                instance,
                                                                                discrepancyVars,
                                                                                discrepancyVars2);
                String paramLine = minimalSetEncoding.get(0);
                List<String> clauses = new ArrayList<>();
                for (int i = 1; i < minimalSetEncoding.size(); i++) { // Skip index 0 since that is the param line
                    clauses.add(minimalSetEncoding.get(i));
                }
                String maxSatEncoding = createEncoding(clauses, Integer.parseInt(BeliefChangeInstance.extractVarNum(paramLine)), discrepancyVars2);
                System.out.println("[INFO] Start solver calls");
                Application.solverCallsStartTime = System.currentTimeMillis();
                String result = getMaxhsResultSatoh(maxSatEncoding);
                while (!result.isEmpty()) {
                    StringBuilder newConstraintMaxSat = new StringBuilder();
                    StringBuilder minimalDistanceSetConstraint = new StringBuilder();
                    String [] vars = result.split(" ");
                    boolean isEmptySet = true;
                    for (int i = 0; i < vars.length; i++) {
                        String var = vars[i];
                        if (!var.startsWith("-")) {
                            int varInt = Integer.parseInt(var);
                            if (discrepancyVars2.contains(varInt)) {
                                isEmptySet = false;
                                int index = discrepancyVars2.indexOf(varInt);
                                minimalDistanceSetConstraint.append(index + 1)
                                                            .append(" ");

                                newConstraintMaxSat.append("-")
                                                   .append(varInt)
                                                   .append(" ");
                            }
                        }
                    }
                    if (isEmptySet) {
                        minimalDistanceSetConstraints = new StringBuilder("0");
                        break;
                    }
                    newConstraintMaxSat.append("0");
                    clauses.add(newConstraintMaxSat.toString());
                    minimalDistanceSetConstraints.append(minimalDistanceSetConstraint.toString().trim())
                                                 .append("\n");

                    maxSatEncoding = createEncoding(clauses, Integer.parseInt(BeliefChangeInstance.extractVarNum(paramLine)), discrepancyVars2);
                    result = getMaxhsResultSatoh(maxSatEncoding);
                }
                Application.solverCallsEndTime = System.currentTimeMillis();
                System.out.println("[INFO] Finished solver calls");
            }
            return minimalDistanceSetConstraints.toString();
        } catch (Exception e) {
            throw new MinimalSetConstraintsDeterminationException(e);
        }
    }

    public String getMinSetConstraints(Encoding encoding) {
        try {
            StringBuilder minimalDistanceSetConstraints = new StringBuilder();
            List<Integer> discrepancyVars2 = new ArrayList<>();
            if (encoding.getInstance() instanceof RevisionInstance) {
                List<String> minimalSetEncoding = createEncodingSatohRevision(encoding, discrepancyVars2);

                String paramLine = minimalSetEncoding.get(0);
                List<String> clauses = new ArrayList<>();
                for (int i = 1; i < minimalSetEncoding.size(); i++) { // Skip index 0 since that is the param line
                    clauses.add(minimalSetEncoding.get(i));
                }
                String maxSatEncoding = createEncoding(clauses, Integer.parseInt(BeliefChangeInstance.extractVarNum(paramLine)), discrepancyVars2);
                System.out.println("[INFO] Start solver calls");
                Application.solverCallsStartTime = System.currentTimeMillis();
                String result = getMaxhsResultSatoh(maxSatEncoding);
                while (!result.isEmpty()) {
                    StringBuilder newConstraintMaxSat = new StringBuilder();
                    StringBuilder minimalDistanceSetConstraint = new StringBuilder();
                    String [] vars = result.split(" ");
                    boolean isEmptySet = true;
                    for (int i = 0; i < vars.length; i++) {
                        String var = vars[i];
                        if (!var.startsWith("-")) {
                            int varInt = Integer.parseInt(var);
                            if (discrepancyVars2.contains(varInt)) {
                                isEmptySet = false;
                                int index = discrepancyVars2.indexOf(varInt);
                                minimalDistanceSetConstraint.append(index + 1)
                                                            .append(" ");

                                newConstraintMaxSat.append("-")
                                                   .append(varInt)
                                                   .append(" ");
                            }
                        }
                    }
                    if (isEmptySet) {
                        minimalDistanceSetConstraints = new StringBuilder("0");
                        break;
                    }
                    newConstraintMaxSat.append("0");
                    clauses.add(newConstraintMaxSat.toString());
                    minimalDistanceSetConstraints.append(minimalDistanceSetConstraint.toString().trim())
                                                 .append("\n");

                    maxSatEncoding = createEncoding(clauses, Integer.parseInt(BeliefChangeInstance.extractVarNum(paramLine)), discrepancyVars2);
                    result = getMaxhsResultSatoh(maxSatEncoding);
                }
                Application.solverCallsEndTime = System.currentTimeMillis();
                System.out.println("[INFO] Finished solver calls");
            } else if (encoding.getInstance() instanceof ContractionInstance) {
                List<String> minimalSetEncoding = createEncodingSatohContraction(encoding, discrepancyVars2);
                String paramLine = minimalSetEncoding.get(0);
                List<String> clauses = new ArrayList<>();
                for (int i = 1; i < minimalSetEncoding.size(); i++) { // Skip index 0 since that is the param line
                    clauses.add(minimalSetEncoding.get(i));
                }
                String maxSatEncoding = createEncoding(clauses, Integer.parseInt(BeliefChangeInstance.extractVarNum(paramLine)), discrepancyVars2);
                System.out.println("[INFO] Start solver calls");
                Application.solverCallsStartTime = System.currentTimeMillis();
                String result = getMaxhsResultSatoh(maxSatEncoding);
                while (!result.isEmpty()) {
                    StringBuilder newConstraintMaxSat = new StringBuilder();
                    StringBuilder minimalDistanceSetConstraint = new StringBuilder();
                    String [] vars = result.split(" ");
                    boolean isEmptySet = true;
                    for (int i = 0; i < vars.length; i++) {
                        String var = vars[i];
                        if (!var.startsWith("-")) {
                            int varInt = Integer.parseInt(var);
                            if (discrepancyVars2.contains(varInt)) {
                                isEmptySet = false;
                                int index = discrepancyVars2.indexOf(varInt);
                                minimalDistanceSetConstraint.append(index + 1)
                                                            .append(" ");

                                newConstraintMaxSat.append("-")
                                                   .append(varInt)
                                                   .append(" ");
                            }
                        }
                    }
                    if (isEmptySet) {
                        minimalDistanceSetConstraints = new StringBuilder("0");
                        break;
                    }
                    newConstraintMaxSat.append("0");
                    clauses.add(newConstraintMaxSat.toString());
                    minimalDistanceSetConstraints.append(minimalDistanceSetConstraint.toString().trim())
                                                 .append("\n");

                    maxSatEncoding = createEncoding(clauses, Integer.parseInt(BeliefChangeInstance.extractVarNum(paramLine)), discrepancyVars2);
                    result = getMaxhsResultSatoh(maxSatEncoding);
                }
                Application.solverCallsEndTime = System.currentTimeMillis();
                System.out.println("[INFO] Finished solver calls");
            }
            return minimalDistanceSetConstraints.toString();
        } catch (Exception e) {
            throw new MinimalSetConstraintsDeterminationException(e);
        }
    }

    private List<String> createEncodingSatohRevision(Encoding encoding, List<Integer> discrepancyVars2) {
        List<String> discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesExact;

        int varNumMinimalSetEncoding = encoding.getVarNum();
        List<String> clauses = new ArrayList<String>();
        clauses.addAll(((RevisionEncoding) encoding).getResult());

        HashMap<Integer, List<Integer>> newVarMap = new HashMap<>();
        for (int i = 1; i <= encoding.getInstance().getVarNum(); i++) {
            List<Integer> vars = new ArrayList<>();
            vars.add(varNumMinimalSetEncoding + i);
            vars.add(varNumMinimalSetEncoding + encoding.getInstance().getVarNum() + i);
            newVarMap.put(i, vars);
        }
        varNumMinimalSetEncoding += 2 * encoding.getInstance().getVarNum();

        for (String clause : encoding.getInstance().getBaseClauses()) {
            String[] vars = clause.split(" ");
            StringBuilder alteredClause = new StringBuilder();
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                String var = vars[i];
                String sign = "";
                if (var.startsWith("-")) {
                    sign = "-";
                    var = var.replace("-", "");
                }
                alteredClause.append(sign)
                             .append(newVarMap.get(Integer.parseInt(var)).get(0))
                             .append(" ");
            }
            alteredClause.append("0");
            clauses.add(alteredClause.toString());
        }

        for (String clause : encoding.getInstance().getChangeClauses()) {
            String[] vars = clause.split(" ");
            StringBuilder alteredClause = new StringBuilder();
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                String var = vars[i];
                String sign = "";
                if (var.startsWith("-")) {
                    sign = "-";
                    var = var.replace("-", "");
                }
                alteredClause.append(sign)
                             .append(newVarMap.get(Integer.parseInt(var)).get(1))
                             .append(" ");
            }
            alteredClause.append("0");
            clauses.add(alteredClause.toString());
        }


        for (int i = 1; i <= encoding.getInstance().getVarNum(); i++) {
            varNumMinimalSetEncoding++;
            discrepancyVars2.add(varNumMinimalSetEncoding);
        }
        int counter = 0;
        for (Map.Entry<Integer, List<Integer>> entry : newVarMap.entrySet()) {
            for (String template : discrepancyClauseTemplates) {
                String clause = template.replaceAll("d", Integer.toString(discrepancyVars2.get(counter)))
                                        .replaceAll("x", Integer.toString(entry.getValue().get(0)))
                                        .replaceAll("y", Integer.toString(entry.getValue().get(1)));
                clauses.add(clause);
            }
            counter++;
        }

        List<Integer> discrepancyVarsNew = new ArrayList<>();
        for (int i = 1; i <= encoding.getInstance().getVarNum(); i++) {
            varNumMinimalSetEncoding++;
            discrepancyVarsNew.add(varNumMinimalSetEncoding);
            for (String template : discrepancyClauseTemplates) {
                String clause = template.replaceAll("d", Integer.toString(varNumMinimalSetEncoding))
                                        .replaceAll("x", Integer.toString(encoding.getDiscrepancyVars().get(i - 1)))
                                        .replaceAll("y", Integer.toString(discrepancyVars2.get(i - 1)));
                clauses.add(clause);
            }
        }
        StringBuilder discrepancyVarsConstraint = new StringBuilder();
        for (int discrepancyVar : discrepancyVarsNew) {
            discrepancyVarsConstraint.append(discrepancyVar)
                                     .append(" ");
        }
        discrepancyVarsConstraint.append("0");
        clauses.add(discrepancyVarsConstraint.toString());

        List<Integer> discrepancyVars = encoding.getDiscrepancyVars();
        for (int x = 0; x < discrepancyVars.size(); x++) {
            int discrepancyVar = discrepancyVars.get(x);
            StringBuilder constraint = new StringBuilder();
            constraint.append(discrepancyVar)
                      .append(" -")
                      .append(discrepancyVars2.get(x))
                      .append(" 0");
            clauses.add(constraint.toString());
        }

        List<String> minimalSetEncoding = new ArrayList<String>();
        minimalSetEncoding.add("p cnf " + varNumMinimalSetEncoding + " " + clauses.size());
        minimalSetEncoding.addAll(clauses);
        return minimalSetEncoding;
    }

    private List<String> createEncodingSatohRevision(BeliefChangeInstance instance,
                                                     int varNum,
                                                     List<String> baseClausesNew,
                                                     List<String> changeClausesNew,
                                                     List<String> discrepancyClauses,
                                                     List<Integer> discrepancyVars,
                                                     List<Integer> discrepancyVars2) {
        List<String> discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesExact;

        int varNumMinimalSetEncoding = varNum;
        List<String> clauses = new ArrayList<String>();
        clauses.addAll(baseClausesNew);
        clauses.addAll(changeClausesNew);
        clauses.addAll(discrepancyClauses);

        HashMap<Integer, List<Integer>> newVarMap = new HashMap<>();
        for (int i = 1; i <= instance.getVarNum(); i++) {
            List<Integer> vars = new ArrayList<>();
            vars.add(varNumMinimalSetEncoding + i);
            vars.add(varNumMinimalSetEncoding + instance.getVarNum() + i);
            newVarMap.put(i, vars);
        }
        varNumMinimalSetEncoding += 2 * instance.getVarNum();


        for (String clause : instance.getBaseClauses()) {
            String[] vars = clause.split(" ");
            StringBuilder alteredClause = new StringBuilder();
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                String var = vars[i];
                String sign = "";
                if (var.startsWith("-")) {
                    sign = "-";
                    var = var.replace("-", "");
                }
                alteredClause.append(sign)
                             .append(newVarMap.get(Integer.parseInt(var)).get(0))
                             .append(" ");
            }
            alteredClause.append("0");
            clauses.add(alteredClause.toString());
        }

        for (String clause : instance.getChangeClauses()) {
            String[] vars = clause.split(" ");
            StringBuilder alteredClause = new StringBuilder();
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                String var = vars[i];
                String sign = "";
                if (var.startsWith("-")) {
                    sign = "-";
                    var = var.replace("-", "");
                }
                alteredClause.append(sign)
                             .append(newVarMap.get(Integer.parseInt(var)).get(1))
                             .append(" ");
            }
            alteredClause.append("0");
            clauses.add(alteredClause.toString());
        }


        for (int i = 1; i <= instance.getVarNum(); i++) {
            varNumMinimalSetEncoding++;
            discrepancyVars2.add(varNumMinimalSetEncoding);
        }
        int counter = 0;
        for (Map.Entry<Integer, List<Integer>> entry : newVarMap.entrySet()) {
            for (String template : discrepancyClauseTemplates) {
                String clause = template.replaceAll("d", Integer.toString(discrepancyVars2.get(counter)))
                                        .replaceAll("x", Integer.toString(entry.getValue().get(0)))
                                        .replaceAll("y", Integer.toString(entry.getValue().get(1)));
                clauses.add(clause);
            }
            counter++;
        }

        List<Integer> discrepancyVarsNew = new ArrayList<>();
        for (int i = 1; i <= instance.getVarNum(); i++) {
            varNumMinimalSetEncoding++;
            discrepancyVarsNew.add(varNumMinimalSetEncoding);
            for (String template : discrepancyClauseTemplates) {
                String clause = template.replaceAll("d", Integer.toString(varNumMinimalSetEncoding))
                                        .replaceAll("x", Integer.toString(discrepancyVars.get(i - 1)))
                                        .replaceAll("y", Integer.toString(discrepancyVars2.get(i - 1)));
                clauses.add(clause);
            }
        }
        StringBuilder discrepancyVarsConstraint = new StringBuilder();
        for (int discrepancyVar : discrepancyVarsNew) {
            discrepancyVarsConstraint.append(discrepancyVar)
                                  .append(" ");
        }
        discrepancyVarsConstraint.append("0");
        clauses.add(discrepancyVarsConstraint.toString());

        for (int x = 0; x < discrepancyVars.size(); x++) {
            int discrepancyVar = discrepancyVars.get(x);
            StringBuilder constraint = new StringBuilder();
            constraint.append(discrepancyVar)
                      .append(" -")
                      .append(discrepancyVars2.get(x))
                      .append(" 0");
            clauses.add(constraint.toString());
        }

        List<String> minimalSetEncoding = new ArrayList<String>();
        minimalSetEncoding.add("p cnf " + varNumMinimalSetEncoding + " " + clauses.size());
        minimalSetEncoding.addAll(clauses);
        return minimalSetEncoding;
    }

    private List<String> createEncodingSatohContraction(Encoding encoding, List<Integer> discrepancyVars2) {
        List<String> discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesExact;

        int varNumMinimalSetEncoding = ((ContractionEncoding) encoding).getVarNumMaxSat();
        List<String> clauses = new ArrayList<String>();
        clauses.addAll(((ContractionEncoding) encoding).getBaseClausesNew());
        clauses.addAll(((ContractionEncoding) encoding).getNegatedChangeClauses());
        clauses.addAll(((ContractionEncoding) encoding).getDiscrepancyClausesMaxSat());

        HashMap<Integer, List<Integer>> newVarMap = new HashMap<>();
        for (int i = 1; i <= encoding.getInstance().getVarNum(); i++) {
            List<Integer> vars = new ArrayList<>();
            vars.add(varNumMinimalSetEncoding + i);
            vars.add(varNumMinimalSetEncoding + encoding.getInstance().getVarNum() + i);
            newVarMap.put(i, vars);
        }
        varNumMinimalSetEncoding += 2 * encoding.getInstance().getVarNum();


        for (String clause : encoding.getInstance().getBaseClauses()) {
            String[] vars = clause.split(" ");
            StringBuilder alteredClause = new StringBuilder();
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                String var = vars[i];
                String sign = "";
                if (var.startsWith("-")) {
                    sign = "-";
                    var = var.replace("-", "");
                }
                alteredClause.append(sign)
                             .append(newVarMap.get(Integer.parseInt(var)).get(0))
                             .append(" ");
            }
            alteredClause.append("0");
            clauses.add(alteredClause.toString());
        }

        HashMap<Integer, Integer> auxVarsMap = new HashMap<>();
        for (String clause : ((ContractionEncoding) encoding).getNegatedChangeClauses()) {
            String[] vars = clause.split(" ");
            StringBuilder alteredClause = new StringBuilder();
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                String var = vars[i];
                String sign = "";
                if (var.startsWith("-")) {
                    sign = "-";
                    var = var.replace("-", "");
                }
                alteredClause.append(sign);
                if (newVarMap.containsKey(Integer.parseInt(var))) {
                    alteredClause.append(newVarMap.get(Integer.parseInt(var)).get(1));
                } else if (auxVarsMap.containsKey(Integer.parseInt(var))) {
                    alteredClause.append(auxVarsMap.get(Integer.parseInt(var)));
                } else {
                    varNumMinimalSetEncoding++;
                    alteredClause.append(varNumMinimalSetEncoding);
                    auxVarsMap.put(Integer.parseInt(var), varNumMinimalSetEncoding);
                }
                alteredClause.append(" ");
            }
            alteredClause.append("0");
            clauses.add(alteredClause.toString());
        }


        for (int i = 1; i <= encoding.getInstance().getVarNum(); i++) {
            varNumMinimalSetEncoding++;
            discrepancyVars2.add(varNumMinimalSetEncoding);
        }
        int counter = 0;
        for (Map.Entry<Integer, List<Integer>> entry : newVarMap.entrySet()) {
            for (String template : discrepancyClauseTemplates) {
                String clause = template.replaceAll("d", Integer.toString(discrepancyVars2.get(counter)))
                                        .replaceAll("x", Integer.toString(entry.getValue().get(0)))
                                        .replaceAll("y", Integer.toString(entry.getValue().get(1)));
                clauses.add(clause);
            }
            counter++;
        }

        List<Integer> discrepancyVarsMaxSat = ((ContractionEncoding) encoding).getDiscrepancyVarsMaxSat();
        List<Integer> discrepancyVarsNew = new ArrayList<>();
        for (int i = 1; i <= encoding.getInstance().getVarNum(); i++) {
            varNumMinimalSetEncoding++;
            discrepancyVarsNew.add(varNumMinimalSetEncoding);
            for (String template : discrepancyClauseTemplates) {
                String clause = template.replaceAll("d", Integer.toString(varNumMinimalSetEncoding))
                                        .replaceAll("x", Integer.toString(discrepancyVarsMaxSat.get(i - 1)))
                                        .replaceAll("y", Integer.toString(discrepancyVars2.get(i - 1)));
                clauses.add(clause);
            }
        }
        StringBuilder discrepancyVarsConstraint = new StringBuilder();
        for (int discrepancyVar : discrepancyVarsNew) {
            discrepancyVarsConstraint.append(discrepancyVar)
                                     .append(" ");
        }
        discrepancyVarsConstraint.append("0");
        clauses.add(discrepancyVarsConstraint.toString());

        for (int x = 0; x < discrepancyVarsMaxSat.size(); x++) {
            int discrepancyVar = discrepancyVarsMaxSat.get(x);
            StringBuilder constraint = new StringBuilder();
            constraint.append(discrepancyVar)
                      .append(" -")
                      .append(discrepancyVars2.get(x))
                      .append(" 0");
            clauses.add(constraint.toString());
        }

        List<String> minimalSetEncoding = new ArrayList<String>();
        minimalSetEncoding.add("p cnf " + varNumMinimalSetEncoding + " " + clauses.size());
        minimalSetEncoding.addAll(clauses);
        return minimalSetEncoding;
    }

    private List<String> createEncodingSatohContraction(int varNumMaxSat,
                                                        List<String> baseClausesNew,
                                                        List<String> negatedChangeClauses,
                                                        List<String> discrepancyClausesMaxSat,
                                                        BeliefChangeInstance instance,
                                                        List<Integer> discrepancyVarsMaxSat,
                                                        List<Integer> discrepancyVars2) {
        List<String> discrepancyClauseTemplates = Encoding.discrepancyClauseTemplatesExact;

        int varNumMinimalSetEncoding = varNumMaxSat;
        List<String> clauses = new ArrayList<String>();
        clauses.addAll(baseClausesNew);
        clauses.addAll(negatedChangeClauses);
        clauses.addAll(discrepancyClausesMaxSat);

        HashMap<Integer, List<Integer>> newVarMap = new HashMap<>();
        for (int i = 1; i <= instance.getVarNum(); i++) {
            List<Integer> vars = new ArrayList<>();
            vars.add(varNumMinimalSetEncoding + i);
            vars.add(varNumMinimalSetEncoding + instance.getVarNum() + i);
            newVarMap.put(i, vars);
        }
        varNumMinimalSetEncoding += 2 * instance.getVarNum();


        for (String clause : instance.getBaseClauses()) {
            String[] vars = clause.split(" ");
            StringBuilder alteredClause = new StringBuilder();
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                String var = vars[i];
                String sign = "";
                if (var.startsWith("-")) {
                    sign = "-";
                    var = var.replace("-", "");
                }
                alteredClause.append(sign)
                             .append(newVarMap.get(Integer.parseInt(var)).get(0))
                             .append(" ");
            }
            alteredClause.append("0");
            clauses.add(alteredClause.toString());
        }

        HashMap<Integer, Integer> auxVarsMap = new HashMap<>();
        for (String clause : negatedChangeClauses) {
            String[] vars = clause.split(" ");
            StringBuilder alteredClause = new StringBuilder();
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                String var = vars[i];
                String sign = "";
                if (var.startsWith("-")) {
                    sign = "-";
                    var = var.replace("-", "");
                }
                alteredClause.append(sign);
                if (newVarMap.containsKey(Integer.parseInt(var))) {
                    alteredClause.append(newVarMap.get(Integer.parseInt(var)).get(1));
                } else if (auxVarsMap.containsKey(Integer.parseInt(var))) {
                    alteredClause.append(auxVarsMap.get(Integer.parseInt(var)));
                } else {
                    varNumMinimalSetEncoding++;
                    alteredClause.append(varNumMinimalSetEncoding);
                    auxVarsMap.put(Integer.parseInt(var), varNumMinimalSetEncoding);
                }
                alteredClause.append(" ");
            }
            alteredClause.append("0");
            clauses.add(alteredClause.toString());
        }


        for (int i = 1; i <= instance.getVarNum(); i++) {
            varNumMinimalSetEncoding++;
            discrepancyVars2.add(varNumMinimalSetEncoding);
        }
        int counter = 0;
        for (Map.Entry<Integer, List<Integer>> entry : newVarMap.entrySet()) {
            for (String template : discrepancyClauseTemplates) {
                String clause = template.replaceAll("d", Integer.toString(discrepancyVars2.get(counter)))
                                        .replaceAll("x", Integer.toString(entry.getValue().get(0)))
                                        .replaceAll("y", Integer.toString(entry.getValue().get(1)));
                clauses.add(clause);
            }
            counter++;
        }

        List<Integer> discrepancyVarsNew = new ArrayList<>();
        for (int i = 1; i <= instance.getVarNum(); i++) {
            varNumMinimalSetEncoding++;
            discrepancyVarsNew.add(varNumMinimalSetEncoding);
            for (String template : discrepancyClauseTemplates) {
                String clause = template.replaceAll("d", Integer.toString(varNumMinimalSetEncoding))
                                        .replaceAll("x", Integer.toString(discrepancyVarsMaxSat.get(i - 1)))
                                        .replaceAll("y", Integer.toString(discrepancyVars2.get(i - 1)));
                clauses.add(clause);
            }
        }
        StringBuilder discrepancyVarsConstraint = new StringBuilder();
        for (int discrepancyVar : discrepancyVarsNew) {
            discrepancyVarsConstraint.append(discrepancyVar)
                                     .append(" ");
        }
        discrepancyVarsConstraint.append("0");
        clauses.add(discrepancyVarsConstraint.toString());

        for (int x = 0; x < discrepancyVarsMaxSat.size(); x++) {
            int discrepancyVar = discrepancyVarsMaxSat.get(x);
            StringBuilder constraint = new StringBuilder();
            constraint.append(discrepancyVar)
                      .append(" -")
                      .append(discrepancyVars2.get(x))
                      .append(" 0");
            clauses.add(constraint.toString());
        }

        List<String> minimalSetEncoding = new ArrayList<String>();
        minimalSetEncoding.add("p cnf " + varNumMinimalSetEncoding + " " + clauses.size());
        minimalSetEncoding.addAll(clauses);
        return minimalSetEncoding;
    }
}
