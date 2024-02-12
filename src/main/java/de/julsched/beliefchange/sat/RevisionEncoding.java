package de.julsched.beliefchange.sat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.julsched.beliefchange.OptimumFinder;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.utils.Tseitin;
import de.julsched.beliefchange.values.Distance;

public class RevisionEncoding extends Encoding {

    private HashMap<Integer, Integer> varMap = new HashMap<Integer, Integer>();

    public RevisionEncoding(BeliefChangeInstance instance, OptimumFinder optimumFinder, Distance distance) throws IOException {
        super(instance, optimumFinder, distance);
    }

    protected void createVarMap() {
        for (int v = 1; v <= this.varNum; v++) {
            this.varMap.put(v, this.varNum + v);
        }
        this.varNum = this.varNum * 2;
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
                             .append(this.varMap.get(Integer.parseInt(var)))
                             .append(" ");
            }
            alteredClause.append("0");
            this.result.add(alteredClause.toString());
        }
        this.result.addAll(this.instance.getChangeClauses());
        this.clauseNumBasicEncoding = this.instance.getBaseClauses().size() + this.instance.getChangeClauses().size();
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
        for (Map.Entry<Integer, Integer> entry : this.varMap.entrySet()) {
            for (String template : discrepancyClauseTemplates) {
                String clause = template.replaceAll("d", Integer.toString(this.discrepancyVars.get(counter)))
                                        .replaceAll("x", Integer.toString(entry.getKey()))
                                        .replaceAll("y", Integer.toString(entry.getValue()));
                this.result.add(clause);
            }
            counter++;
        }
        this.clauseNum += this.discrepancyVars.size() * discrepancyClauseTemplates.size();
    }

    protected void determineMinDistance() {
        if (this.optimumFinder instanceof MaxSat) {
            this.minDistance = ((MaxSat)this.optimumFinder).getOptimum(this);
        } else {
            this.minDistance = this.optimumFinder.getOptimum(this.instance);
        }
    }

    protected void finalizeEncoding() {
        this.optimumFinder = null;
        if (this.distance == Distance.SATOH && this.minimalDistanceSetConstraints.isEmpty()) {
            createSimpleFinalEncoding();
            return;
        }
        addParamsLine();
    }

    private void createSimpleFinalEncoding() {
        this.result = new ArrayList<String>();
        this.result.add("c Belief base variables: " + this.instance.getVarNum());
        this.result.add("p cnf " + this.instance.getVarNum() + " " + this.instance.getChangeClauses().size());
        this.result.addAll(this.instance.getChangeClauses());
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
            for (Map.Entry<Integer, Integer> entry : this.varMap.entrySet()) {
                for (String template : discrepancyClauseTemplatesVague) {
                    String clause = template.replaceAll("d", Integer.toString(this.discrepancyVars.get(counter)))
                                            .replaceAll("x", Integer.toString(entry.getKey()))
                                            .replaceAll("y", Integer.toString(entry.getValue()));
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
                    conjunction.append("~" + this.counterBitsReversed.get(index));
                } else if (bit == '1') {
                    conjunction.append(this.counterBitsReversed.get(index));
                }
                if (k != 0) {
                    conjunction.append(" & ");
                }
            }
            for (int k = index + 1; k < this.counterBitsReversed.size(); k++) {
                conjunction.append(" & ");
                conjunction.append("~"+ this.counterBitsReversed.get(k));
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

    public HashMap<Integer, Integer> getVarMap() {
        return this.varMap;
    }

    public List<String> getResult() {
        return this.result;
    }
}
