package de.julsched.beliefchange.asp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.OptimumFinder;
import de.julsched.beliefchange.exceptions.MinDistanceException;
import de.julsched.beliefchange.exceptions.MinimalSetConstraintsDeterminationException;
import de.julsched.beliefchange.instance.ContractionInstance;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.instance.RevisionInstance;
import de.julsched.beliefchange.utils.Clingo;
import de.julsched.beliefchange.utils.Utils;
import de.julsched.beliefchange.values.Distance;

public class Asp extends OptimumFinder {

    private static final String optimizationConstraint = "#minimize {1,P : d(P)}.";
    private static final String optimizationConstraint2 = "#minimize {1,P : d2(P)}.";

    private static final String optimizationFileName = Application.dirInterimResults + "/asp-%s-optimization.lp";

    private List<String> optimumEncoding = new ArrayList<String>();

    public Asp(Distance distance) {
        super(distance);
    }

    private void createOptimumEncodingDalal(BeliefChangeInstance instance) {
        HashMap<Integer, Integer> varMap = new HashMap<Integer, Integer>();
        int varNum = instance.getVarNum();
        for (int v = 1; v <= instance.getVarNum(); v++) {
            varNum++;
            varMap.put(v, varNum);
        }

        this.optimumEncoding.addAll(Encoding.createVarDefinitionEncoding(varNum));
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(Encoding.createRepresentationConstraints(varMap));
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(Encoding.createBaseClauseConstraints(instance.getBaseClauses(), varMap));
        this.optimumEncoding.add("");
        if (instance instanceof RevisionInstance) {
            optimumEncoding.addAll(Encoding.createChangeClauseConstraints(instance.getChangeClauses(), false));
        } else {
            optimumEncoding.addAll(Encoding.createChangeClauseConstraints(instance.getChangeClauses(), true));
        }
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(Encoding.createDiscrepancyConstraints(""));
        this.optimumEncoding.add("");
        this.optimumEncoding.add(optimizationConstraint);
        this.optimumEncoding.add("");
    }

    private void createOptimumEncodingDalal(Encoding encoding) {
        this.optimumEncoding.addAll(encoding.getVarDefinitionEncoding());
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(encoding.getRepresentationEncoding());
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(encoding.getBaseClauseEncoding());
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(encoding.getChangeClauseEncoding());
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(encoding.getDiscrepancyConstraintsEncoding());
        this.optimumEncoding.add("");
        this.optimumEncoding.add(optimizationConstraint);
        this.optimumEncoding.add("");
    }

    private void createOptimumEncodingSatoh(Encoding encoding) {
        this.optimumEncoding.addAll(Encoding.createVarDefinitionEncoding(encoding.getVarNum() * 2));
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(encoding.getRepresentationEncoding());
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(encoding.getBaseClauseEncoding());
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(encoding.getChangeClauseEncoding());
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(encoding.getDiscrepancyConstraintsEncoding());
        this.optimumEncoding.add("");

        int varNumNew = encoding.getVarNum();
        List<Integer> newChangeVars = new ArrayList<>();
        for (int x = 1; x <= encoding.getVarMap().size(); x++) {
            varNumNew++;
            newChangeVars.add(varNumNew);
        }
        List<Integer> newBaseVars = new ArrayList<>();
        for (int x = 1; x <= encoding.getVarMap().size(); x++) {
            varNumNew++;
            newBaseVars.add(varNumNew);
        }

        StringBuilder constraint;
        for (int i = 0; i < newBaseVars.size(); i++) {
            constraint = new StringBuilder("r2(")
                            .append(newChangeVars.get(i))
                            .append(",")
                            .append(newBaseVars.get(i))
                            .append(").");
            optimumEncoding.add(constraint.toString());
        }
        this.optimumEncoding.add("");

        for (String baseClause : encoding.getInstance().getBaseClauses()) {
            StringBuilder aspClause = new StringBuilder();

            String[] vars = baseClause.split(" ");
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                if (i == 0) {
                    aspClause.append(":- ");
                } else {
                    aspClause.append(", ");
                }
                String var = vars[i];
                if (var.startsWith("-")) {
                    var = var.replace("-", "");
                    aspClause.append("t(")
                             .append(newBaseVars.get(Integer.parseInt(var) - 1))
                             .append(")");
                } else {
                    aspClause.append("not t(")
                             .append(newBaseVars.get(Integer.parseInt(var) - 1))
                             .append(")");
                }
            }
            aspClause.append(".");
            this.optimumEncoding.add(aspClause.toString());
        }
        this.optimumEncoding.add("");

        if (encoding.getInstance() instanceof RevisionInstance) {
            for (String changeClause : encoding.getInstance().getChangeClauses()) {
                StringBuilder aspClause = new StringBuilder();

                String[] vars = changeClause.split(" ");
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (i == 0) {
                        aspClause.append(":- ");
                    } else {
                        aspClause.append(", ");
                    }
                    String var = vars[i];
                    if (var.startsWith("-")) {
                        var = var.replace("-", "");
                        aspClause.append("t(")
                                 .append(newChangeVars.get(Integer.parseInt(var) - 1))
                                 .append(")");
                    } else {
                        aspClause.append("not t(")
                                 .append(newChangeVars.get(Integer.parseInt(var) - 1))
                                 .append(")");
                    }
                }
                aspClause.append(".");
                optimumEncoding.add(aspClause.toString());
            }
        } else if (encoding.getInstance() instanceof ContractionInstance) {
            int counter = 0;
            StringBuilder aspClause = new StringBuilder(":- ");
            for (String changeClause : encoding.getInstance().getChangeClauses()) {
                counter++;
                if (counter > 1) {
                    aspClause.append(", ");
                }
                aspClause.append("1 {");
                String[] vars = changeClause.split(" ");
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (i > 0) {
                        aspClause.append("; ");
                    }
                    String var = vars[i];
                    if (var.startsWith("-")) {
                        var = var.replace("-", "");
                        aspClause.append("not t(")
                                 .append(newChangeVars.get(Integer.parseInt(var) - 1))
                                 .append(")");
                    } else {
                        aspClause.append("t(")
                                 .append(newChangeVars.get(Integer.parseInt(var) - 1))
                                 .append(")");
                    }
                }
                aspClause.append("}");
            }
            aspClause.append(".");
            this.optimumEncoding.add(aspClause.toString());
        }
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(Encoding.createDiscrepancyConstraints("2"));
        this.optimumEncoding.add("");

        for (int i = 0; i < newBaseVars.size(); i++) {
            StringBuilder encodingLine = new StringBuilder();
            encodingLine.append("r3(")
                        .append(i + 1)
                        .append(",")
                        .append(newChangeVars.get(i))
                        .append(").");
            optimumEncoding.add(encodingLine.toString());
        }
        this.optimumEncoding.add("");
        this.optimumEncoding.add("d3(A) :- r3(A,B), d(A), not d2(B).");
        this.optimumEncoding.add("d3(A) :- r3(A,B), not d(A), d2(B).");
        this.optimumEncoding.add("");
        this.optimumEncoding.add(":- #count {A : d3(A)} = 0.");
        this.optimumEncoding.add("");
        this.optimumEncoding.add("not d2(B) :- r3(A,B), not d(A).");
        this.optimumEncoding.add("");

        this.optimumEncoding.add(optimizationConstraint2);
        this.optimumEncoding.add("");
        this.optimumEncoding.add("#show d2/1.");
        this.optimumEncoding.add("");
    }

    private void createOptimumEncodingSatoh(BeliefChangeInstance instance) {
        HashMap<Integer, Integer> varMap = new HashMap<Integer, Integer>();
        int varNum = instance.getVarNum();
        for (int v = 1; v <= instance.getVarNum(); v++) {
            varNum++;
            varMap.put(v, varNum);
        }

        this.optimumEncoding.addAll(Encoding.createVarDefinitionEncoding(varNum * 2));
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(Encoding.createRepresentationConstraints(varMap));
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(Encoding.createBaseClauseConstraints(instance.getBaseClauses(), varMap));
        this.optimumEncoding.add("");
        if (instance instanceof RevisionInstance) {
            this.optimumEncoding.addAll(Encoding.createChangeClauseConstraints(instance.getChangeClauses(), false));
        } else {
            this.optimumEncoding.addAll(Encoding.createChangeClauseConstraints(instance.getChangeClauses(), true));
        }
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(Encoding.createDiscrepancyConstraints(""));
        this.optimumEncoding.add("");

        int varNumNew = varNum;
        List<Integer> newChangeVars = new ArrayList<>();
        for (int x = 1; x <= varMap.size(); x++) {
            varNumNew++;
            newChangeVars.add(varNumNew);
        }
        List<Integer> newBaseVars = new ArrayList<>();
        for (int x = 1; x <= varMap.size(); x++) {
            varNumNew++;
            newBaseVars.add(varNumNew);
        }

        for (int i = 0; i < newBaseVars.size(); i++) {
            StringBuilder encodingLine = new StringBuilder();
            encodingLine.append("r2(")
                        .append(newChangeVars.get(i))
                        .append(",")
                        .append(newBaseVars.get(i))
                        .append(").");
            this.optimumEncoding.add(encodingLine.toString());
        }
        this.optimumEncoding.add("");

        for (String baseClause : instance.getBaseClauses()) {
            StringBuilder aspClause = new StringBuilder();

            String[] vars = baseClause.split(" ");
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                if (i == 0) {
                    aspClause.append(":- ");
                } else {
                    aspClause.append(", ");
                }
                String var = vars[i];
                if (var.startsWith("-")) {
                    var = var.replace("-", "");
                    aspClause.append("t(")
                             .append(newBaseVars.get(Integer.parseInt(var) - 1))
                             .append(")");
                } else {
                    aspClause.append("not t(")
                             .append(newBaseVars.get(Integer.parseInt(var) - 1))
                             .append(")");
                }
            }
            aspClause.append(".");
            this.optimumEncoding.add(aspClause.toString());
        }
        this.optimumEncoding.add("");

        if (instance instanceof RevisionInstance) {
            for (String changeClause : instance.getChangeClauses()) {
                StringBuilder aspClause = new StringBuilder();

                String[] vars = changeClause.split(" ");
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (i == 0) {
                        aspClause.append(":- ");
                    } else {
                        aspClause.append(", ");
                    }
                    String var = vars[i];
                    if (var.startsWith("-")) {
                        var = var.replace("-", "");
                        aspClause.append("t(")
                                 .append(newChangeVars.get(Integer.parseInt(var) - 1))
                                 .append(")");
                    } else {
                        aspClause.append("not t(")
                                 .append(newChangeVars.get(Integer.parseInt(var) - 1))
                                 .append(")");
                    }
                }
                aspClause.append(".");
                this.optimumEncoding.add(aspClause.toString());
            }
        } else if (instance instanceof ContractionInstance) {
            int counter = 0;
            StringBuilder aspClause = new StringBuilder(":- ");
            for (String changeClause : instance.getChangeClauses()) {
                counter++;
                if (counter > 1) {
                    aspClause.append(", ");
                }
                aspClause.append("1 {");
                String[] vars = changeClause.split(" ");
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (i > 0) {
                        aspClause.append("; ");
                    }
                    String var = vars[i];
                    if (var.startsWith("-")) {
                        var = var.replace("-", "");
                        aspClause.append("not t(")
                                 .append(newChangeVars.get(Integer.parseInt(var) - 1))
                                 .append(")");
                    } else {
                        aspClause.append("t(")
                                 .append(newChangeVars.get(Integer.parseInt(var) - 1))
                                 .append(")");
                    }
                }
                aspClause.append("}");
            }
            aspClause.append(".");
            this.optimumEncoding.add(aspClause.toString());
        }
        this.optimumEncoding.add("");
        this.optimumEncoding.addAll(Encoding.createDiscrepancyConstraints("2"));
        this.optimumEncoding.add("");

        for (int i = 0; i < newBaseVars.size(); i++) {
            StringBuilder encodingLine = new StringBuilder();
            encodingLine.append("r3(")
                        .append(i + 1)
                        .append(",")
                        .append(newChangeVars.get(i))
                        .append(").");
            this.optimumEncoding.add(encodingLine.toString());
        }
        this.optimumEncoding.add("");
        this.optimumEncoding.add("d3(A) :- r3(A,B), d(A), not d2(B).");
        this.optimumEncoding.add("d3(A) :- r3(A,B), not d(A), d2(B).");
        this.optimumEncoding.add("");
        this.optimumEncoding.add(":- #count {A : d3(A)} = 0.");
        this.optimumEncoding.add("");
        this.optimumEncoding.add("not d2(B) :- r3(A,B), not d(A).");
        this.optimumEncoding.add("");

        this.optimumEncoding.add(optimizationConstraint2);
        this.optimumEncoding.add("");
        this.optimumEncoding.add("#show d2/1.");
        this.optimumEncoding.add("");
    }

    @Override
    public String getOptimum(BeliefChangeInstance instance) {
        try {
            createOptimumEncodingDalal(instance);
            Utils.writeToFile(StringUtils.join(this.optimumEncoding, "\n"), String.format(optimizationFileName, "dalal"));
            System.out.println("[INFO] Start solver call");
            Application.solverCallsStartTime = System.currentTimeMillis();
            Process process = Clingo.execute(String.format(optimizationFileName, "dalal"), false);
            Application.solverCallsEndTime = System.currentTimeMillis();
            System.out.println("[INFO] Finished solver call");
            return Clingo.readOptimumLine(process);
        } catch (MinDistanceException e) {
            throw e;
        } catch (Exception e) {
            throw new MinDistanceException(e);
        }
    }

    public String getOptimum(Encoding encoding) {
        try {
            createOptimumEncodingDalal(encoding);
            Utils.writeToFile(StringUtils.join(this.optimumEncoding, "\n"), String.format(optimizationFileName, "dalal"));
            System.out.println("[INFO] Start solver call");
            Application.solverCallsStartTime = System.currentTimeMillis();
            Process process = Clingo.execute(String.format(optimizationFileName, "dalal"), false);
            Application.solverCallsEndTime = System.currentTimeMillis();
            System.out.println("[INFO] Finished solver call");
            return Clingo.readOptimumLine(process);
        } catch (MinDistanceException e) {
            throw e;
        } catch (Exception e) {
            throw new MinDistanceException(e);
        }
    }

    public String getMinSetConstraints(BeliefChangeInstance instance) {
        createOptimumEncodingSatoh(instance);
        return determineMinSetConstraints(instance.getVarNum());
    }

    public String getMinSetConstraints(Encoding encoding) {
        createOptimumEncodingSatoh(encoding);
        return determineMinSetConstraints(encoding.getInstance().getVarNum());
    }

    private String determineMinSetConstraints(int varNum) {
        try {
            StringBuilder minimalSetConstraints = new StringBuilder();
            Utils.writeToFile(StringUtils.join(this.optimumEncoding, "\n"), String.format(optimizationFileName, "satoh"));
            System.out.println("[INFO] Start solver calls");
            Application.solverCallsStartTime = System.currentTimeMillis();
            Process process = Clingo.execute(String.format(optimizationFileName, "satoh"), false);
            String result = Clingo.readOptimumResult(process);
            List<String> updatedOptimumEncoding = new ArrayList<String>();
            updatedOptimumEncoding.addAll(this.optimumEncoding);
            while (result != null) {
                if (!result.contains("d2(")) {
                    // We have found the empty set as minimal set
                    minimalSetConstraints = new StringBuilder("0");
                    break;
                }
                int index = -1;
                for (int i = 0; i < updatedOptimumEncoding.size(); i++) {
                    if (updatedOptimumEncoding.get(i).equals(optimizationConstraint2)) {
                        index = i;
                        break;
                    }
                }
                updatedOptimumEncoding.add(index, "");
                updatedOptimumEncoding.add(index, ":- " + result.replace(" ", ", ") + ".");
                String[] parts = result.split("d2\\(");
                StringBuilder newConstraint = new StringBuilder();
                for (int i = 1; i < parts.length; i++) { // Index 0 will be empty since String result starts with 'd2('
                    String distinctPart = parts[i];
                    int modelVar = Integer.parseInt(distinctPart.split("\\)")[0]);
                    modelVar = modelVar - (2 * varNum);
                    newConstraint.append(modelVar);
                    if (i != parts.length - 1) {
                        newConstraint.append(" ");
                    }
                }
                minimalSetConstraints.append(newConstraint.toString());
                minimalSetConstraints.append("\n");

                Utils.writeToFile(StringUtils.join(updatedOptimumEncoding, "\n"), String.format(optimizationFileName, "satoh"));
                process = Clingo.execute(String.format(optimizationFileName, "satoh"), false);
                result = Clingo.readOptimumResult(process);
            }
            Application.solverCallsEndTime = System.currentTimeMillis();
            System.out.println("[INFO] Finished solver calls");
            return minimalSetConstraints.toString();
        } catch (Exception e) {
            throw new MinimalSetConstraintsDeterminationException(e);
        }
    }
}
