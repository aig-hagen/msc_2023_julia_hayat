package de.julsched.beliefchange.asp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.OptimumFinder;
import de.julsched.beliefchange.exceptions.EncodingFailureException;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.instance.RevisionInstance;
import de.julsched.beliefchange.utils.Utils;
import de.julsched.beliefchange.values.Distance;

public class Encoding {

    private static final List<String> discrepancyConstraints = Arrays.asList(new String[] {
        "d%1$s(M) :- r%1$s(M,N), t(M), not t(N).",
        "d%1$s(M) :- r%1$s(M,N), not t(M), t(N)."
    });

    private static final String optimumConstraint = ":- #count {P : d(P)} != %s.";

    private Distance distance;
    private BeliefChangeInstance instance;
    private OptimumFinder optimumFinder;
    private int varNum;

    private List<String> varDefinitionEncoding = new ArrayList<String>();
    private List<String> representationEncoding = new ArrayList<String>();
    private List<String> baseClauseEncoding = new ArrayList<String>();
    private List<String> changeClauseEncoding = new ArrayList<String>();
    private List<String> discrepancyConstraintsEncoding = new ArrayList<String>();
    private String minDistance = "";
    private String minimalSetConstraints = "";
    private List<String> finalEncoding = new ArrayList<String>();
    private HashMap<Integer, Integer> varMap = new HashMap<Integer, Integer>();

    public Encoding(BeliefChangeInstance instance, OptimumFinder optimumFinder, Distance distance) {
        this.distance = distance;
        this.instance = instance;
        this.optimumFinder = optimumFinder;
        this.varNum = this.instance.getVarNum();
    }

    public static List<String> createVarDefinitionEncoding(int varNum) {
        return List.of("{t(1.." + varNum + ")}.");
    }

    public static List<String> createRepresentationConstraints(HashMap<Integer, Integer> varMap) {
        List<String> representationEncoding = new ArrayList<String>();
        for (HashMap.Entry<Integer, Integer> entry : varMap.entrySet()) {
            StringBuilder encodingLine = new StringBuilder();
            encodingLine.append("r(")
                        .append(entry.getKey())
                        .append(",")
                        .append(entry.getValue())
                        .append(").");
            representationEncoding.add(encodingLine.toString());
        }
        return representationEncoding;
    }

    public static List<String> createBaseClauseConstraints(List<String> baseClauses, HashMap<Integer, Integer> varMap) {
        List<String> baseClauseEncoding = new ArrayList<String>();
        for (String baseClause : baseClauses) {
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
                             .append(varMap.get(Integer.parseInt(var)))
                             .append(")");
                } else {
                    aspClause.append("not t(")
                             .append(varMap.get(Integer.parseInt(var)))
                             .append(")");
                }
            }
            aspClause.append(".");
            baseClauseEncoding.add(aspClause.toString());
        }
        return baseClauseEncoding;
    }

    public static List<String> createChangeClauseConstraints(List<String> changeClauses, boolean negateChangeClauses) {
        List<String> changeClauseEncoding = new ArrayList<String>();
        if (!negateChangeClauses) {
            for (String changeClause : changeClauses) {
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
                                 .append(var)
                                 .append(")");
                    } else {
                        aspClause.append("not t(")
                                 .append(var)
                                 .append(")");
                    }
                }
                aspClause.append(".");
                changeClauseEncoding.add(aspClause.toString());
            }
        } else {
            int counter = 0;
            StringBuilder aspClause = new StringBuilder(":- ");
            for (String changeClause : changeClauses) {
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
                                 .append(var)
                                 .append(")");
                    } else {
                        aspClause.append("t(")
                                 .append(var)
                                 .append(")");
                    }
                }
                aspClause.append("}");
            }
            aspClause.append(".");
            changeClauseEncoding.add(aspClause.toString());
        }
        return changeClauseEncoding;
    }

    public static List<String> createDiscrepancyConstraints(String prefix) {
        List<String> discrepancyConstraintsEncoding = new ArrayList<String>();
        for (String constraint : discrepancyConstraints) {
            discrepancyConstraintsEncoding.add(String.format(constraint, prefix));
        }
        return discrepancyConstraintsEncoding;
    }

    public void create() {
        System.out.println("[INFO] Start compilation");
        Application.compilationStartTime = System.currentTimeMillis();
        try {
            createVarMap();
            createBasicEncoding();
            switch(this.distance) {
                case DALAL:
                    findMinDistance();
                    break;
                case SATOH:
                    findMinSetConstraints();
                    break;
                default:
                    throw new WrongInputException("Unknown distance '" + this.distance + "'");
            }
            createFinalEncoding();
            System.out.println("[INFO] Writing encoding to file '" + Application.resultFilePath + "'");
            Utils.writeToFile(StringUtils.join(this.finalEncoding, "\n"), Application.resultFilePath);
        } catch (Exception e) {
            throw new EncodingFailureException(e);
        }
        Application.compilationEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished compilation");
    }

    private void createVarMap() {
        for (int v = 1; v <= this.instance.getVarNum(); v++) {
            this.varNum++;
            this.varMap.put(v, this.varNum);
        }
    }

    private void createBasicEncoding() {
        this.varDefinitionEncoding = createVarDefinitionEncoding(this.varNum);
        this.representationEncoding = createRepresentationConstraints(this.varMap);
        this.baseClauseEncoding = createBaseClauseConstraints(this.instance.getBaseClauses(), this.varMap);
        if (this.instance instanceof RevisionInstance) {
            this.changeClauseEncoding = createChangeClauseConstraints(this.instance.getChangeClauses(), false);
        } else {
            this.changeClauseEncoding = createChangeClauseConstraints(this.instance.getChangeClauses(), true);
        }
        this.discrepancyConstraintsEncoding = createDiscrepancyConstraints("");
    }

    private void findMinDistance() {
        System.out.println("[INFO] Start minimum distance determination");
        Application.optimumFinderStartTime = System.currentTimeMillis();
        if (this.optimumFinder instanceof Asp) {
            this.minDistance = ((Asp) this.optimumFinder).getOptimum(this);
        } else {
            this.minDistance = this.optimumFinder.getOptimum(this.instance);
        }
        Application.optimumFinderEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Minimum distance: " + this.minDistance);
        System.out.println("[INFO] Finished minimum distance determination");
    }

    private void findMinSetConstraints() {
        System.out.println("[INFO] Start minimal distance sets determination");
        Application.optimumFinderStartTime = System.currentTimeMillis();
        String result;
        if (this.optimumFinder instanceof Asp) {
            result = ((Asp) this.optimumFinder).getMinSetConstraints(this);
        } else {
            result = this.optimumFinder.getMinSetConstraints(this.instance);
        }
        if (result.isEmpty()) {
            System.out.println("[INFO] Determined minimal distance sets: " + 0);
        } else if (result.equals("0")) {
            System.out.println("[INFO] Determined minimal distance sets: " + 1);
        } else {
            System.out.println("[INFO] Determined minimal distance sets: " + result.split("\n").length);
        }
        if (!result.isEmpty() && !result.equals("0")) {
            StringBuilder minimalSetConstraintsBuilder = new StringBuilder();
            for (String constraint : result.split("\n")) {
                String[] modelVars = constraint.split(" ");
                for (int i = 0; i < modelVars.length; i++) {
                    String modelVar = modelVars[i];
                    minimalSetConstraintsBuilder.append("d(")
                                                .append(modelVar)
                                                .append("), ");
                }
                minimalSetConstraintsBuilder.append("#count {P : d(P)} != " + (modelVars.length));
                minimalSetConstraintsBuilder.append("\n");
            }
            this.minimalSetConstraints = minimalSetConstraintsBuilder.toString();
        }
        if (result.equals("0")) {
            this.minimalSetConstraints = "0";
        }
        Application.optimumFinderEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished minimal distance sets determination");
    }

    private void createFinalEncoding() {
        this.optimumFinder = null;
        this.finalEncoding.add("% Belief base variables: " + this.instance.getVarNum());
        // If empty, then all difference sets are minimal sets
        if (this.distance == Distance.SATOH && this.minimalSetConstraints.isEmpty() && this.instance instanceof RevisionInstance) {
            createSimpleFinalEncoding();
            return;
        }
        this.finalEncoding.addAll(this.varDefinitionEncoding);
        this.finalEncoding.add("");
        this.finalEncoding.addAll(this.representationEncoding);
        this.finalEncoding.add("");
        this.finalEncoding.addAll(this.baseClauseEncoding);
        this.finalEncoding.add("");
        if (this.instance instanceof RevisionInstance) {
            this.finalEncoding.addAll(this.changeClauseEncoding);
            this.finalEncoding.add("");
            this.finalEncoding.addAll(this.discrepancyConstraintsEncoding);
            this.finalEncoding.add("");
            if (this.distance == Distance.DALAL) {
                this.finalEncoding.add(String.format(optimumConstraint, this.minDistance));
            } else if (this.distance == Distance.SATOH) {
                if (this.minimalSetConstraints.equals("0")) {
                    this.finalEncoding.add(String.format(optimumConstraint, "0"));
                } else {
                    for (String constraint : this.minimalSetConstraints.split("\n")) {
                        this.finalEncoding.add(":- " + constraint + ".");
                    }
                }
            }
        } else {
            this.finalEncoding.addAll(this.discrepancyConstraintsEncoding);
            this.finalEncoding.add("");
            this.finalEncoding.add("isRevisionModel :- #count {P : d(P)} != 0.");

            // In contraction, changeClauseEncoding is a one-liner
            this.finalEncoding.add("not isRevisionModel " + this.changeClauseEncoding.get(0));

            if (this.distance == Distance.DALAL) {
                this.finalEncoding.add("not isRevisionModel :- #count {S : d(S)} != " + this.minDistance + ".");
            } else if (this.distance == Distance.SATOH) {
                if (!this.minimalSetConstraints.isEmpty()) {
                    for (String constraint : this.minimalSetConstraints.split("\n")) {
                        if (constraint.equals("0")) {
                            this.finalEncoding.add("not isRevisionModel :- #count {S : d(S)} != 0.");
                            break;
                        }
                        this.finalEncoding.add("not isRevisionModel :- " + constraint + ".");
                    }
                }
            }
        }
        this.finalEncoding.add("");
    }

    private void createSimpleFinalEncoding() {
        this.finalEncoding.addAll(createVarDefinitionEncoding(this.instance.getVarNum()));
        this.finalEncoding.add("");
        this.finalEncoding.addAll(this.changeClauseEncoding);
        this.finalEncoding.add("");
    }

    public List<String> getVarDefinitionEncoding() {
        return this.varDefinitionEncoding;
    }

    public List<String> getRepresentationEncoding() {
        return this.representationEncoding;
    }

    public List<String> getBaseClauseEncoding() {
        return this.baseClauseEncoding;
    }

    public List<String> getChangeClauseEncoding() {
        return this.changeClauseEncoding;
    }

    public List<String> getDiscrepancyConstraintsEncoding() {
        return this.discrepancyConstraintsEncoding;
    }

    public int getVarNum() {
        return this.varNum;
    }

    public HashMap<Integer, Integer> getVarMap() {
        return this.varMap;
    }

    public BeliefChangeInstance getInstance() {
        return this.instance;
    }
}
