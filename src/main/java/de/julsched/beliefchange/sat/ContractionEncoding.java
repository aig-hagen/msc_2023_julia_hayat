package de.julsched.beliefchange.sat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.julsched.beliefchange.OptimumFinder;
import de.julsched.beliefchange.exceptions.EncodingFailureException;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.utils.Tseitin;
import de.julsched.beliefchange.values.Distance;

public class ContractionEncoding extends Encoding {

    private HashMap<Integer, List<Integer>> varMap = new HashMap<Integer, List<Integer>>();
    private List<String> baseClausesNew = new ArrayList<String>();
    private List<String> changeClausesNew = new ArrayList<String>();
    private List<String> negatedChangeClauses = new ArrayList<String>(); // Containing original variables + additional ones added by Tseitin method
    private int auxiliaryVarsNum;
    private int varNumMaxSat;
    private List<Integer> discrepancyVarsMaxSat = new ArrayList<Integer>();
    private List<String> discrepancyClausesMaxSat = new ArrayList<String>();


    public ContractionEncoding(BeliefChangeInstance instance, OptimumFinder optimumFinder, Distance distance) throws IOException {
        super(instance, optimumFinder, distance);
    }

    public HashMap<Integer, List<Integer>> getVarMap() {
        return this.varMap;
    }

    public List<String> getBaseClausesNew() {
        return this.baseClausesNew;
    }

    public int getVarNumMaxSat() {
        return this.varNumMaxSat;
    }

    public List<Integer> getDiscrepancyVarsMaxSat() {
        return this.discrepancyVarsMaxSat;
    }

    public List<String> getDiscrepancyClausesMaxSat() {
        return this.discrepancyClausesMaxSat;
    }

    public List<String> getNegatedChangeClauses() {
        return this.negatedChangeClauses;
    }

    protected void createVarMap() {
        String[] dimacsNegation = Tseitin.negateCnfFormula(this.varNum, this.instance.getChangeClauses());
        if (dimacsNegation[0].equals("tautology")) {
            throw new EncodingFailureException("Contraction formula is unsatisfiable");
        }
        if (dimacsNegation[0].equals("unsat")) {
            throw new EncodingFailureException("Contraction formula is a tautology");
        }
        this.varNum = Integer.parseInt(BeliefChangeInstance.extractVarNum(dimacsNegation[0]));
        this.auxiliaryVarsNum = this.varNum - this.instance.getVarNum();
        for (int i = 1; i < dimacsNegation.length; i++) {
            this.negatedChangeClauses.add(dimacsNegation[i]);
        }

        for (int v = 1; v <= this.instance.getVarNum(); v++) {
            List<Integer> vars = new ArrayList<Integer>();
            vars.add(this.varNum + v);
            vars.add(this.varNum + v + this.instance.getVarNum());
            this.varMap.put(v, vars);
        }
        this.varNumMaxSat = this.varNum + this.instance.getVarNum();
        this.varNum = this.varNum + (2 * this.instance.getVarNum());
    }

    protected void createBasicEncoding() {
        // Replace variables
        for (String clause : this.instance.getBaseClauses()) {
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
                             .append(this.varMap.get(Integer.parseInt(var)).get(0))
                             .append(" ");
            }
            alteredClause.append("0");
            this.baseClausesNew.add(alteredClause.toString());
            this.result.add(alteredClause.toString());
        }

        // Replace variables in negated contraction formula
        this.clauseNum -= this.instance.getChangeClauses().size();
        for (String newClause : this.negatedChangeClauses) {
            String[] vars = newClause.split(" ");
            StringBuilder alteredClause = new StringBuilder();
            for (int x = 0; x < vars.length - 1; x++) { // Skip last index (since that is '0')
                String var = vars[x];
                String sign = "";
                if (var.startsWith("-")) {
                    sign = "-";
                    var = var.replace("-", "");
                }
                if (this.varMap.containsKey(Integer.parseInt(var))) {
                    alteredClause.append(sign)
                                 .append(this.varMap.get(Integer.parseInt(var)).get(1))
                                 .append(" ");
                } else {
                    alteredClause.append(sign)
                                 .append(var)
                                 .append(" ");
                }
            }
            alteredClause.append("0");
            this.changeClausesNew.add(alteredClause.toString());
            this.result.add(alteredClause.toString());
            this.clauseNum++;
        }
        this.clauseNumBasicEncoding = this.clauseNum;
    }

    protected void createDiscrepancyClauses() {
        determineDiscrepancyVars(this.varMap.size());
        List<String> discrepancyClauseTemplates;
        if (this.distance == Distance.SATOH) {
            discrepancyClauseTemplates = discrepancyClauseTemplatesExact;
        } else {
            discrepancyClauseTemplates = discrepancyClauseTemplatesVague;
        }
        int counter = 0;
        for (Map.Entry<Integer, List<Integer>> entry : this.varMap.entrySet()) {
            for (String template : discrepancyClauseTemplates) {
                String clause = template.replaceAll("d", Integer.toString(this.discrepancyVars.get(counter)))
                                        .replaceAll("x", Integer.toString(entry.getValue().get(0)))
                                        .replaceAll("y", Integer.toString(entry.getValue().get(1)));
                this.result.add(clause);
                this.clauseNum++;
            }
            counter++;
        }

        if (this.optimumFinder instanceof MaxSat) {
            createDiscrepancyClausesMaxSat();
        }
    }

    private void createDiscrepancyClausesMaxSat() {
        for (int i = 1; i <= this.varMap.size(); i++) {
            this.varNumMaxSat++;
            this.discrepancyVarsMaxSat.add(this.varNumMaxSat);
        }

        List<String> discrepancyClauseTemplates;
        if (this.distance == Distance.SATOH) {
            discrepancyClauseTemplates = discrepancyClauseTemplatesExact;
        } else {
            discrepancyClauseTemplates = discrepancyClauseTemplatesVague;
        }
        int counter = 0;
        for (Map.Entry<Integer, List<Integer>> entry : this.varMap.entrySet()) {
            for (String template : discrepancyClauseTemplates) {
                this.discrepancyClausesMaxSat.add(template.replaceAll("d", Integer.toString(this.discrepancyVarsMaxSat.get(counter)))
                                                          .replaceAll("x", Integer.toString(entry.getKey()))
                                                          .replaceAll("y", Integer.toString(entry.getValue().get(0))));

            }
            counter++;
        }
    }

    protected void determineMinDistance() {
        if (this.optimumFinder instanceof MaxSat) {
            this.minDistance = ((MaxSat)this.optimumFinder).getOptimum(this);
        } else {
            this.minDistance = this.optimumFinder.getOptimum(this.instance);
        }
    }

    protected void finalizeEncoding() {
        this.discrepancyVarsMaxSat = null;
        this.discrepancyClausesMaxSat = null;
        this.baseClausesNew = null;
        this.negatedChangeClauses = null;
        this.optimumFinder = null;
        if (this.distance == Distance.SATOH && this.minimalDistanceSetConstraints.isEmpty()) {
            createSimpleFinalEncoding();
            return;
        }
        this.changeClausesNew = null;
        List<Integer> beliefBaseVars = new ArrayList<Integer>();
        for (int i = 1; i <= this.varMap.size(); i++) {
            this.varNum++;
            beliefBaseVars.add(this.varNum);
        }
        for (String clause : this.instance.getBaseClauses()) {
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
                             .append(beliefBaseVars.get(Integer.parseInt(var) - 1))
                             .append(" ");
            }
            alteredClause.append("0");
            this.result.add(alteredClause.toString());
            this.clauseNum++;
        }


        // Add clauses to ensure original variables represent the contraction models
        StringBuilder clause;
        for (Map.Entry<Integer, List<Integer>> entry1 : this.varMap.entrySet()) {
            for (Map.Entry<Integer, List<Integer>> entry2 : this.varMap.entrySet()) {
                if (entry1.getKey().equals(entry2.getKey())) {
                    continue;
                }
                clause = new StringBuilder();
                clause.append(entry2.getKey())
                      .append(" -")
                      .append(beliefBaseVars.get(entry2.getKey() - 1))
                      .append(" ")
                      .append(entry1.getKey())
                      .append(" -")
                      .append(entry1.getValue().get(1))
                      .append(" 0");
                this.result.add(clause.toString());
                clause = new StringBuilder();
                clause.append("-")
                      .append(entry2.getKey())
                      .append(" ")
                      .append(beliefBaseVars.get(entry2.getKey() - 1))
                      .append(" ")
                      .append(entry1.getKey())
                      .append(" -")
                      .append(entry1.getValue().get(1))
                      .append(" 0");
                this.result.add(clause.toString());

                clause = new StringBuilder();
                clause.append(entry2.getKey())
                      .append(" -")
                      .append(beliefBaseVars.get(entry2.getKey() - 1))
                      .append(" -")
                      .append(entry1.getKey())
                      .append(" ")
                      .append(entry1.getValue().get(1))
                      .append(" 0");
                this.result.add(clause.toString());
                clause = new StringBuilder();
                clause.append("-")
                      .append(entry2.getKey())
                      .append(" ")
                      .append(beliefBaseVars.get(entry2.getKey() - 1))
                      .append(" -")
                      .append(entry1.getKey())
                      .append(" ")
                      .append(entry1.getValue().get(1))
                      .append(" 0");
                this.result.add(clause.toString());
                this.clauseNum += 4;
            }

            clause = new StringBuilder();
            clause.append(entry1.getKey())
                  .append(" -")
                  .append(beliefBaseVars.get(entry1.getKey() - 1))
                  .append(" -")
                  .append(entry1.getValue().get(1))
                  .append(" 0");
            this.result.add(clause.toString());

            clause = new StringBuilder();
            clause.append("-")
                  .append(entry1.getKey())
                  .append(" ")
                  .append(beliefBaseVars.get(entry1.getKey() - 1))
                  .append(" ")
                  .append(entry1.getValue().get(1))
                  .append(" 0");
            this.result.add(clause.toString());
            this.clauseNum += 2;
        }
        addParamsLine();
    }

    private void createSimpleFinalEncoding() {
        this.result = new ArrayList<String>();
        this.result.addAll(this.changeClausesNew);
        List<Integer> beliefBaseVars = new ArrayList<Integer>();
        for (int i = (2 * this.instance.getVarNum() + 1); i <= (3 * this.instance.getVarNum()); i++) {
            beliefBaseVars.add(i);
        }
        for (String clause : this.instance.getBaseClauses()) {
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
                             .append(beliefBaseVars.get(Integer.parseInt(var) - 1))
                             .append(" ");
            }
            alteredClause.append("0");
            this.result.add(alteredClause.toString());
        }

        // Add clauses to ensure original variables represent the contraction models
        StringBuilder clause;
        for (Map.Entry<Integer, List<Integer>> entry1 : this.varMap.entrySet()) {
            for (Map.Entry<Integer, List<Integer>> entry2 : this.varMap.entrySet()) {
                if (entry1.getKey().equals(entry2.getKey())) {
                    continue;
                }
                clause = new StringBuilder();
                clause.append(entry2.getKey())
                      .append(" -")
                      .append(beliefBaseVars.get(entry2.getKey() - 1))
                      .append(" ")
                      .append(entry1.getKey())
                      .append(" -")
                      .append(entry1.getValue().get(1))
                      .append(" 0");
                this.result.add(clause.toString());
                clause = new StringBuilder();
                clause.append("-")
                      .append(entry2.getKey())
                      .append(" ")
                      .append(beliefBaseVars.get(entry2.getKey() - 1))
                      .append(" ")
                      .append(entry1.getKey())
                      .append(" -")
                      .append(entry1.getValue().get(1))
                      .append(" 0");
                this.result.add(clause.toString());

                clause = new StringBuilder();
                clause.append(entry2.getKey())
                      .append(" -")
                      .append(beliefBaseVars.get(entry2.getKey() - 1))
                      .append(" -")
                      .append(entry1.getKey())
                      .append(" ")
                      .append(entry1.getValue().get(1))
                      .append(" 0");
                this.result.add(clause.toString());
                clause = new StringBuilder();
                clause.append("-")
                      .append(entry2.getKey())
                      .append(" ")
                      .append(beliefBaseVars.get(entry2.getKey() - 1))
                      .append(" -")
                      .append(entry1.getKey())
                      .append(" ")
                      .append(entry1.getValue().get(1))
                      .append(" 0");
                this.result.add(clause.toString());
            }

            clause = new StringBuilder();
            clause.append(entry1.getKey())
                  .append(" -")
                  .append(beliefBaseVars.get(entry1.getKey() - 1))
                  .append(" -")
                  .append(entry1.getValue().get(1))
                  .append(" 0");
            this.result.add(clause.toString());

            clause = new StringBuilder();
            clause.append("-")
                  .append(entry1.getKey())
                  .append(" ")
                  .append(beliefBaseVars.get(entry1.getKey() - 1))
                  .append(" ")
                  .append(entry1.getValue().get(1))
                  .append(" 0");
            this.result.add(clause.toString());
        }
        this.result.add(0,"p cnf " + (3 * this.instance.getVarNum() + this.auxiliaryVarsNum) + " " + this.result.size());
        this.result.add(0,"c Belief base variables: " + this.instance.getVarNum());
        this.result.add("");
    }

    protected void createMinimalSetConstraints() {
        if (this.minimalDistanceSetConstraints.size() == 1 && this.minimalDistanceSetConstraints.get(0).equals("0")) {
            List<String> newResult = new ArrayList<String>();
            for (int i = 0; i < this.clauseNumBasicEncoding; i++) {
                newResult.add(this.result.get(i));
            }
            this.result = newResult;
            int counter = 0;
            for (Map.Entry<Integer, List<Integer>> entry : this.varMap.entrySet()) {
                for (String template : discrepancyClauseTemplatesVague) {
                    String clause = template.replaceAll("d", Integer.toString(this.discrepancyVars.get(counter)))
                                            .replaceAll("x", Integer.toString(entry.getValue().get(0)))
                                            .replaceAll("y", Integer.toString(entry.getValue().get(1)));
                    this.result.add(clause);
                }
                counter++;
            }
            this.clauseNum -= this.discrepancyVars.size() * 2;


            // Empty set is the only minimal set
            createZeroDistanceConstraintClauses();
            return;
        }
        createBinaryCounterClauses();
        StringBuilder conjunction = new StringBuilder();
        for (int i = 0; i < this.minimalDistanceSetConstraints.size(); i++) {
            String constraint = this.minimalDistanceSetConstraints.get(i);
            conjunction.append("~(");
            String[] vars = constraint.split(" ");
            for (int j = 0; j < vars.length; j++) {
                String var = vars[j];
                conjunction.append(var);
                conjunction.append(" & ");
            }

            conjunction.append("~(");
            String distanceBinary = Integer.toBinaryString(vars.length);
            int index = -1;
            for (int k = distanceBinary.length() - 1; k >= 0; k--) {
                char bit = distanceBinary.charAt(k);
                index++;
                if (bit == '0') {
                    conjunction.append("~")
                               .append(this.counterBitsReversed.get(index));
                } else if (bit == '1') {
                    conjunction.append(this.counterBitsReversed.get(index));
                }
                if (k != 0) {
                    conjunction.append(" & ");
                }
            }
            for (int k = index + 1; k < this.counterBitsReversed.size(); k++) {
                conjunction.append(" & ")
                           .append("~")
                           .append(this.counterBitsReversed.get(k));
            }
            conjunction.append(")");

            conjunction.append(")");
            if (i != this.minimalDistanceSetConstraints.size() - 1) {
                conjunction.append(" & ");
            }
        }

        String[] cnfClauses = Tseitin.transformToCnf(conjunction.toString(), this.varNum);
        this.varNum = Integer.parseInt(BeliefChangeInstance.extractVarNum(cnfClauses[0]));
        for (int i = 1; i < cnfClauses.length; i++) {
            this.result.add(cnfClauses[i]);
        }
        this.clauseNum += cnfClauses.length - 1;
    }
}
