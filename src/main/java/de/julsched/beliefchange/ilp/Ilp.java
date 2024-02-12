package de.julsched.beliefchange.ilp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.OptimumFinder;
import de.julsched.beliefchange.exceptions.MinDistanceException;
import de.julsched.beliefchange.exceptions.MinimalSetConstraintsDeterminationException;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.instance.ContractionInstance;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.instance.RevisionInstance;
import de.julsched.beliefchange.utils.Glpsol;
import de.julsched.beliefchange.utils.Utils;
import de.julsched.beliefchange.values.Distance;


public class Ilp extends OptimumFinder {

    private static final String optimizationFileName = Application.dirInterimResults + "/ilp-%s-optimization.mod";
    public static final String optimizationResultFileName = Application.dirInterimResults + "/ilp-%s-optimization-result";

    private List<String> ilpModelOptimum = new ArrayList<String>();

    private List<Integer> discrepancyVarsPositions = new ArrayList<Integer>();
    private List<Integer> discrepancyVars2Positions = new ArrayList<Integer>();

    public Ilp(Distance distance) {
        super(distance);
    }

    public void createOptimumEncoding(Encoding encoding) {
        switch (this.distance) {
            case DALAL:
                this.ilpModelOptimum.addAll(encoding.getVarDeclarationEncoding());
                this.ilpModelOptimum.add("");
                this.ilpModelOptimum.addAll(encoding.getBaseClausesEncoding());
                this.ilpModelOptimum.addAll(encoding.getChangeClausesEncoding());
                this.ilpModelOptimum.addAll(encoding.getDiscrepancyConstraintsEncoding());
                addObjectiveConstraint(encoding.getVarNum());
                break;
            case SATOH:
                int varNum = encoding.getVarNum();
                if (encoding.getInstance() instanceof RevisionInstance) {
                    int startPosition = varNum * 2 + 1;
                    int endPosition = varNum * 3;
                    for (int x = startPosition; x <= endPosition; x++) {
                        this.discrepancyVarsPositions.add(x);
                    }
                    startPosition = varNum * 5 + 1;
                    endPosition = varNum * 6;
                    for (int x = startPosition; x <= endPosition; x++) {
                        this.discrepancyVars2Positions.add(x);
                    }
                } else {
                    int startPosition = varNum * 2 + encoding.getInstance().getChangeClauses().size() + 1;
                    int endPosition = varNum * 3 + encoding.getInstance().getChangeClauses().size();
                    for (int x = startPosition; x <= endPosition; x++) {
                        this.discrepancyVarsPositions.add(x);
                    }
                    startPosition = varNum * 5 + 2 * encoding.getInstance().getChangeClauses().size() + 1;
                    endPosition = varNum * 6 + 2 * encoding.getInstance().getChangeClauses().size();
                    for (int x = startPosition; x <= endPosition; x++) {
                        this.discrepancyVars2Positions.add(x);
                    }
                }
                addMinimalSetConstraints(encoding);
                break;
            default:
                throw new WrongInputException("Unknown distance '" + this.distance + "'");
        }
    }

    public void createOptimumEncoding(BeliefChangeInstance instance) {
        int varNum = instance.getVarNum();
        if (instance instanceof RevisionInstance) {
            List<String> varDeclarationEncoding = Encoding.createVarDeclarationEncodingRevision(varNum);
            List<String> baseClausesEncoding = Encoding.createBaseClauseEncodingRevision(instance);
            List<String> changeClausesEncoding = Encoding.createChangeClauseEncodingRevision(instance);
            List<String> discrepancyConstraintsEncoding = Encoding.createDiscrepancyConstraintsEncodingRevision(this.distance, varNum);
            switch (this.distance) {
                case DALAL:
                    this.ilpModelOptimum.addAll(varDeclarationEncoding);
                    this.ilpModelOptimum.add("");
                    this.ilpModelOptimum.addAll(baseClausesEncoding);
                    this.ilpModelOptimum.addAll(changeClausesEncoding);
                    this.ilpModelOptimum.addAll(discrepancyConstraintsEncoding);
                    addObjectiveConstraint(varNum);
                    break;
                case SATOH:
                    int startPosition = varNum * 2 + 1;
                    int endPosition = varNum * 3;
                    for (int x = startPosition; x <= endPosition; x++) {
                        this.discrepancyVarsPositions.add(x);
                    }
                    startPosition = varNum * 5 + 1;
                    endPosition = varNum * 6;
                    for (int x = startPosition; x <= endPosition; x++) {
                        this.discrepancyVars2Positions.add(x);
                    }
                    addMinimalSetConstraints(instance,
                                             varDeclarationEncoding,
                                             baseClausesEncoding,
                                             changeClausesEncoding,
                                             discrepancyConstraintsEncoding
                    );
                    break;
                default:
                    throw new WrongInputException("Unknown distance '" + this.distance + "'");
            }
        } else if (instance instanceof ContractionInstance) {
            List<String> varDeclarationEncoding = Encoding.createVarDeclarationEncodingContraction(varNum, instance);
            List<String> baseClausesEncoding = Encoding.createBaseClauseEncodingContraction(instance);
            List<String> changeClausesEncoding = Encoding.createChangeClauseEncodingContraction(instance);
            List<String> discrepancyConstraintsEncoding = Encoding.createDiscrepancyConstraintsEncodingContraction(this.distance, varNum);

            switch(this.distance) {
                case DALAL:
                    this.ilpModelOptimum.addAll(varDeclarationEncoding);
                    this.ilpModelOptimum.add("");
                    this.ilpModelOptimum.addAll(baseClausesEncoding);
                    this.ilpModelOptimum.addAll(changeClausesEncoding);
                    this.ilpModelOptimum.addAll(discrepancyConstraintsEncoding);
                    addObjectiveConstraint(varNum);
                    break;
                case SATOH:
                    int startPosition = varNum * 2 + instance.getChangeClauses().size() + 1;
                    int endPosition = varNum * 3 + instance.getChangeClauses().size();
                    for (int x = startPosition; x <= endPosition; x++) {
                        this.discrepancyVarsPositions.add(x);
                    }
                    startPosition = varNum * 5 + 2 * instance.getChangeClauses().size() + 1;
                    endPosition = varNum * 6 + 2 * instance.getChangeClauses().size();
                    for (int x = startPosition; x <= endPosition; x++) {
                        this.discrepancyVars2Positions.add(x);
                    }
                    addMinimalSetConstraints(instance,
                                             varDeclarationEncoding,
                                             baseClausesEncoding,
                                             changeClausesEncoding,
                                             discrepancyConstraintsEncoding
                    );
                    break;
                default:
                    throw new WrongInputException("Unknown distance '" + this.distance + "'");
            }
        }
    }

    private void addObjectiveConstraint(int varNum) {
        StringBuilder optimizeLine = new StringBuilder("minimize distance: ");
        for (int d = 1; d <= varNum; d++) {
            if (d != 1) {
                optimizeLine.append(" + ");
            }
            optimizeLine.append("d")
                        .append(d);
        }
        optimizeLine.append(";");
        this.ilpModelOptimum.add(optimizeLine.toString());
        this.ilpModelOptimum.add("");
        this.ilpModelOptimum.add("end;");
        this.ilpModelOptimum.add("");
    }

    private void addMinimalSetConstraints(Encoding encoding) {
        BeliefChangeInstance instance = encoding.getInstance();

        List<String> varDeclarationNew = new ArrayList<String>();
        varDeclarationNew.addAll(encoding.getVarDeclarationEncoding());

        if (instance instanceof ContractionInstance) {
            for (int i = 1; i <= instance.getChangeClauses().size(); i++) {
                // Create another set of auxiliary vars
                varDeclarationNew.add(String.format(Encoding.varDeclarationTemplate, "as" + i));
            }
        }

        for (int i = 1; i <= instance.getVarNum(); i++) {
            // Create another set of belief base vars
            varDeclarationNew.add(String.format(Encoding.varDeclarationTemplate, "ys" + i));
        }
        for (int i = 1; i <= instance.getVarNum(); i++) {
            // Create another set of change formula vars
            varDeclarationNew.add(String.format(Encoding.varDeclarationTemplate, "zs" + i));
        }
        for (int i = 1; i <= instance.getVarNum(); i++) {
            // Create another set of discrepancy vars
            varDeclarationNew.add(String.format(Encoding.varDeclarationTemplate, "ds" + i));
        }
        for (int i = 1; i <= instance.getVarNum(); i++) {
            // Create yet another set of discrepancy vars
            varDeclarationNew.add(String.format(Encoding.varDeclarationTemplate, "dds" + i));
        }

        List<String> newConstraints = new ArrayList<String>();
        int constraintCounter = 0;
        for (String clause : instance.getBaseClauses()) {
            constraintCounter++;
            StringBuilder constraint = new StringBuilder("s.t. minSetConstraint")
                                        .append(constraintCounter)
                                        .append(":\n\t");
            String[] vars = clause.split(" ");
            int rightSideNum = 1;
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                if (vars[i].startsWith("-")) {
                    if (i == 0) {
                        constraint.append("-");
                    } else {
                        constraint.append(" - ");
                    }
                    constraint.append("ys")
                              .append(vars[i].split("-")[1]);
                    rightSideNum--;
                } else {
                    if (i != 0) {
                        constraint.append(" + ");
                    }
                    constraint.append("ys")
                              .append(vars[i]);
                }
            }
            constraint.append(" >= ")
                      .append(rightSideNum)
                      .append(";");
            newConstraints.add(constraint.toString());
            newConstraints.add("");
        }
        if (instance instanceof RevisionInstance) {
            for (String clause : instance.getChangeClauses()) {
                constraintCounter++;
                StringBuilder constraint = new StringBuilder("s.t. minSetConstraint")
                                            .append(constraintCounter)
                                            .append(":\n\t");
                String[] vars = clause.split(" ");
                int rightSideNum = 1;
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (vars[i].startsWith("-")) {
                        if (i == 0) {
                            constraint.append("-");
                        } else {
                            constraint.append(" - ");
                        }
                        constraint.append("zs")
                                  .append(vars[i].split("-")[1]);
                        rightSideNum--;
                    } else {
                        if (i != 0) {
                            constraint.append(" + ");
                        }
                        constraint.append("zs")
                                  .append(vars[i]);
                    }
                }
                constraint.append(" >= ")
                          .append(rightSideNum)
                          .append(";");
                newConstraints.add(constraint.toString());
                newConstraints.add("");
            }
        } else if (instance instanceof ContractionInstance) {
            int auxiliaryVar = 0;
            for (String clause : instance.getChangeClauses()) {
                auxiliaryVar++;
                constraintCounter++;
                StringBuilder constraint = new StringBuilder("s.t. minSetConstraint")
                                            .append(constraintCounter)
                                            .append(":\n\t")
                                            .append("(");
                String[] vars = clause.split(" ");
                int varNumber = vars.length - 1;
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (i != 0) {
                        constraint.append(" + ");
                    }
                    if (vars[i].startsWith("-")) {
                        constraint.append("(1 - zs");
                        constraint.append(vars[i].split("-")[1]);
                        constraint.append(")");
                    } else {
                        constraint.append("zs");
                        constraint.append(vars[i]);
                    }
                }
                constraint.append(") / ")
                          .append(varNumber)
                          .append(" <= as")
                          .append(auxiliaryVar)
                          .append(";");
                newConstraints.add(constraint.toString());
                newConstraints.add("");
            }

            // Add constraint 'sum(auxiliaryVariables) < <number of clauses>'
            constraintCounter++;
            StringBuilder constraint = new StringBuilder("s.t. minSetConstraint")
                                        .append(constraintCounter)
                                        .append(":\n\t");
            for (int x = 1; x <= instance.getChangeClauses().size(); x++) {
                if (x > 1) {
                    constraint.append(" + ");
                }
                constraint.append("as")
                          .append(x);
            }
            constraint.append(" <= ")
                      .append(instance.getChangeClauses().size() - 1)
                      .append(";");
            newConstraints.add(constraint.toString());
            newConstraints.add("");
        }

        int varNum = instance.getVarNum();
        for (int i = 1; i <= varNum; i++) {
            for (String template : Encoding.discrepancyConstraintTemplatesExact) {
                constraintCounter++;
                newConstraints.add("s.t. minSetConstraint"
                                + constraintCounter
                                + ":\n\t"
                                + template.replaceAll("d", "ds" + i)
                                          .replaceAll("k", "ys" + i)
                                          .replaceAll("v", "zs" + i));
                newConstraints.add("");
            }
        }
        for (int i = 1; i <= varNum; i++) {
            for (String template : Encoding.discrepancyConstraintTemplatesExact) {
                constraintCounter++;
                newConstraints.add("s.t. minSetConstraint"
                                + constraintCounter
                                + ":\n\t"
                                + template.replaceAll("d", "dds" + i)
                                          .replaceAll("k", "d" + i)
                                          .replaceAll("v", "ds" + i));
                newConstraints.add("");
            }
        }

        constraintCounter++;
        StringBuilder minSetConstraint = new StringBuilder();
        minSetConstraint.append("s.t. minSetConstraint")
                        .append(constraintCounter)
                        .append(":\n\t")
                        .append("dds1");
        for (int i = 2; i <= varNum; i++) {
            minSetConstraint.append(" + dds")
                            .append(i);
        }
        minSetConstraint.append(" >= 1;");
        newConstraints.add(minSetConstraint.toString());
        newConstraints.add("");

        for (int i = 1; i <= varNum; i++) {
            constraintCounter++;
            newConstraints.add("s.t. minSetConstraint"
                                + constraintCounter
                                + ":\n\td"
                                + i
                                + " - ds"
                                + i
                                + " >= 0;");
            newConstraints.add("");
        }

        StringBuilder minimizeObjective = new StringBuilder("minimize distance: ");
        for (int d = 1; d <= varNum; d++) {
            if (d != 1) {
                minimizeObjective.append(" + ");
            }
            minimizeObjective.append("ds")
                             .append(d);
        }
        minimizeObjective.append(";");

        this.ilpModelOptimum.addAll(varDeclarationNew);
        this.ilpModelOptimum.add("");
        this.ilpModelOptimum.addAll(encoding.getBaseClausesEncoding());
        this.ilpModelOptimum.addAll(encoding.getChangeClausesEncoding());
        this.ilpModelOptimum.addAll(encoding.getDiscrepancyConstraintsEncoding());
        this.ilpModelOptimum.addAll(newConstraints);
        this.ilpModelOptimum.add(minimizeObjective.toString());
        this.ilpModelOptimum.add("");
        this.ilpModelOptimum.add("end;");
        this.ilpModelOptimum.add("");
    }

    private void addMinimalSetConstraints(BeliefChangeInstance instance,
                                          List<String> varDeclarationEncoding,
                                          List<String> baseClausesEncoding,
                                          List<String> changeClausesEncoding,
                                          List<String> discrepancyConstraintsEncoding) {
        List<String> varDeclarationNew = new ArrayList<String>();
        varDeclarationNew.addAll(varDeclarationEncoding);

        if (instance instanceof ContractionInstance) {
            for (int i = 1; i <= instance.getChangeClauses().size(); i++) {
                varDeclarationNew.add(String.format(Encoding.varDeclarationTemplate, "as" + i));
            }
        }

        for (int i = 1; i <= instance.getVarNum(); i++) {
            // Create new set of belief base vars
            varDeclarationNew.add(String.format(Encoding.varDeclarationTemplate, "ys" + i));
        }
        for (int i = 1; i <= instance.getVarNum(); i++) {
            // Create new set of change formula vars
            varDeclarationNew.add(String.format(Encoding.varDeclarationTemplate, "zs" + i));
        }
        for (int i = 1; i <= instance.getVarNum(); i++) {
            // Create new set of discrepancy vars
            varDeclarationNew.add(String.format(Encoding.varDeclarationTemplate, "ds" + i));
        }
        for (int i = 1; i <= instance.getVarNum(); i++) {
            // Create yet another set of discrepancy vars
            varDeclarationNew.add(String.format(Encoding.varDeclarationTemplate, "dds" + i));
        }

        List<String> newConstraints = new ArrayList<String>();
        int constraintCounter = 0;
        for (String clause : instance.getBaseClauses()) {
            constraintCounter++;
            StringBuilder constraint = new StringBuilder("s.t. minSetConstraint")
                                        .append(constraintCounter)
                                        .append(":\n\t");
            String[] vars = clause.split(" ");
            int rightSideNum = 1;
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                if (vars[i].startsWith("-")) {
                    if (i == 0) {
                        constraint.append("-");
                    } else {
                        constraint.append(" - ");
                    }
                    constraint.append("ys")
                              .append(vars[i].split("-")[1]);
                    rightSideNum--;
                } else {
                    if (i != 0) {
                        constraint.append(" + ");
                    }
                    constraint.append("ys")
                              .append(vars[i]);
                }
            }
            constraint.append(" >= ")
                      .append(rightSideNum)
                      .append(";");
            newConstraints.add(constraint.toString());
            newConstraints.add("");
        }
        if (instance instanceof RevisionInstance) {
            for (String clause : instance.getChangeClauses()) {
                constraintCounter++;
                StringBuilder constraint = new StringBuilder("s.t. minSetConstraint")
                                            .append(constraintCounter)
                                            .append(":\n\t");
                String[] vars = clause.split(" ");
                int rightSideNum = 1;
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (vars[i].startsWith("-")) {
                        if (i == 0) {
                            constraint.append("-");
                        } else {
                            constraint.append(" - ");
                        }
                        constraint.append("zs")
                                  .append(vars[i].split("-")[1]);
                        rightSideNum--;
                    } else {
                        if (i != 0) {
                            constraint.append(" + ");
                        }
                        constraint.append("zs")
                                  .append(vars[i]);
                    }
                }
                constraint.append(" >= ")
                          .append(rightSideNum)
                          .append(";");
                newConstraints.add(constraint.toString());
                newConstraints.add("");
            }
        } else if (instance instanceof ContractionInstance) {
            int auxiliaryVar = 0;
            for (String clause : instance.getChangeClauses()) {
                auxiliaryVar++;
                constraintCounter++;
                StringBuilder constraint = new StringBuilder("s.t. minSetConstraint")
                                            .append(constraintCounter)
                                            .append(":\n\t")
                                            .append("(");
                String[] vars = clause.split(" ");
                int varNumber = vars.length - 1;
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (i != 0) {
                        constraint.append(" + ");
                    }
                    if (vars[i].startsWith("-")) {
                        constraint.append("(1 - zs");
                        constraint.append(vars[i].split("-")[1]);
                        constraint.append(")");
                    } else {
                        constraint.append("zs");
                        constraint.append(vars[i]);
                    }
                }
                constraint.append(") / ")
                            .append(varNumber)
                            .append(" <= as")
                            .append(auxiliaryVar)
                            .append(";");
                newConstraints.add(constraint.toString());
                newConstraints.add("");
            }

            // Add constraint 'sum(auxiliaryVariables) < <number of clauses>'
            constraintCounter++;
            StringBuilder constraint = new StringBuilder("s.t. minSetConstraint")
                                        .append(constraintCounter)
                                        .append(":\n\t");
            for (int x = 1; x <= instance.getChangeClauses().size(); x++) {
                if (x > 1) {
                    constraint.append(" + ");
                }
                constraint.append("as")
                          .append(x);
            }
            constraint.append(" <= ")
                      .append(instance.getChangeClauses().size() - 1)
                      .append(";");
            newConstraints.add(constraint.toString());
            newConstraints.add("");
        }

        int varNum = instance.getVarNum();
        for (int i = 1; i <= varNum; i++) {
            for (String template : Encoding.discrepancyConstraintTemplatesExact) {
                constraintCounter++;
                newConstraints.add("s.t. minSetConstraint"
                                    + constraintCounter
                                    + ":\n\t"
                                    + template.replaceAll("d", "ds" + i)
                                              .replaceAll("k", "ys" + i)
                                              .replaceAll("v", "zs" + i));
                newConstraints.add("");
            }
        }
        for (int i = 1; i <= varNum; i++) {
            for (String template : Encoding.discrepancyConstraintTemplatesExact) {
                constraintCounter++;
                newConstraints.add("s.t. minSetConstraint"
                                    + constraintCounter
                                    + ":\n\t"
                                    + template.replaceAll("d", "dds" + i)
                                              .replaceAll("k", "d" + i)
                                              .replaceAll("v", "ds" + i));
                newConstraints.add("");
            }
        }

        constraintCounter++;
        StringBuilder minSetConstraint = new StringBuilder();
        minSetConstraint.append("s.t. minSetConstraint")
                        .append(constraintCounter)
                        .append(":\n\t")
                        .append("dds1");
        for (int i = 1; i <= varNum; i++) {
            minSetConstraint.append(" + dds")
                            .append(i);
        }
        minSetConstraint.append(" >= 1;");
        newConstraints.add(minSetConstraint.toString());
        newConstraints.add("");


        for (int i = 1; i <= varNum; i++) {
            constraintCounter++;
            newConstraints.add("s.t. minSetConstraint"
                                + constraintCounter
                                + ":\n\td"
                                + i
                                + " - ds"
                                + i
                                + " >= 0;");
            newConstraints.add("");
        }

        StringBuilder minimizeObjective = new StringBuilder("minimize distance: ");
        for (int d = 1; d <= varNum; d++) {
            if (d != 1) {
                minimizeObjective.append(" + ");
            }
            minimizeObjective.append("ds")
                             .append(d);
        }
        minimizeObjective.append(";");

        this.ilpModelOptimum.addAll(varDeclarationNew);
        this.ilpModelOptimum.add("");
        this.ilpModelOptimum.addAll(baseClausesEncoding);
        this.ilpModelOptimum.addAll(changeClausesEncoding);
        this.ilpModelOptimum.addAll(discrepancyConstraintsEncoding);
        this.ilpModelOptimum.addAll(newConstraints);
        this.ilpModelOptimum.add(minimizeObjective.toString());
        this.ilpModelOptimum.add("");
        this.ilpModelOptimum.add("end;");
        this.ilpModelOptimum.add("");
    }

    private String getGlpsolOptimum() throws IOException, InterruptedException {
        Utils.writeToFile(StringUtils.join(this.ilpModelOptimum, "\n"), String.format(optimizationFileName, "dalal"));
        System.out.println("[INFO] Start solver call");
        Application.solverCallsStartTime = System.currentTimeMillis();
        Glpsol.executeSolver(String.format(optimizationFileName, "dalal"), String.format(optimizationResultFileName, "dalal"));
        Application.solverCallsEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished solver call");
        return Glpsol.readOptimumLine(String.format(optimizationResultFileName, "dalal"));
    }

    public String getOptimum(Encoding encoding) {
        try {
            createOptimumEncoding(encoding);
            return getGlpsolOptimum();
        } catch (MinDistanceException e) {
            throw e;
        } catch (Exception e) {
            throw new MinDistanceException(e);
        }
    }

    @Override
    public String getOptimum(BeliefChangeInstance instance) {
        try {
            createOptimumEncoding(instance);
            return getGlpsolOptimum();
        } catch (MinDistanceException e) {
            throw e;
        } catch (Exception e) {
            throw new MinDistanceException(e);
        }
    }

    public String getMinSetConstraints(Encoding encoding) {
        createOptimumEncoding(encoding);
        return getMinSetRestrictions();
    }

    @Override
    public String getMinSetConstraints(BeliefChangeInstance instance) {
        createOptimumEncoding(instance);
        return getMinSetRestrictions();
    }

    public String getMinSetRestrictions() {
        try {
            StringBuilder minimalDistanceSetConstraints = new StringBuilder();
            StringBuilder minimalDistanceSetConstraintsEncoding = new StringBuilder();

            int constraintCounter = 0;

            Utils.writeToFile(StringUtils.join(this.ilpModelOptimum, "\n"), String.format(optimizationFileName, "satoh"));
            System.out.println("[INFO] Start solver calls");
            Application.solverCallsStartTime = System.currentTimeMillis();
            Glpsol.executeSolver(String.format(optimizationFileName, "satoh"), String.format(optimizationResultFileName, "satoh"));
            List<Integer> model = Glpsol.readMinimalSetSatoh(String.format(optimizationResultFileName, "satoh"),
                                                             this.discrepancyVarsPositions,
                                                             this.discrepancyVars2Positions);
            boolean emptySet = false;
            while (model != null) {
                if (model.size() == 1 && model.get(0) == 0) {
                    // Empty set has been found as minimal set
                    emptySet = true;
                    break;
                }
                constraintCounter++;
                StringBuilder modelString = new StringBuilder();
                for (int var : model) {
                    modelString.append(var)
                               .append(" ");
                }
                minimalDistanceSetConstraints.append(modelString.toString().trim())
                                             .append("\n");

                List<String> ilpModelOptimumNew = new ArrayList<String>();
                ilpModelOptimumNew.addAll(this.ilpModelOptimum);
                int index = -1;
                for (int i = 0; i < ilpModelOptimumNew.size(); i++) {
                    if (ilpModelOptimumNew.get(i).startsWith("minimize")) {
                        index = i;
                        break;
                    }
                }
                StringBuilder modelConstraint = new StringBuilder("s.t. setConstraint")
                                                    .append(constraintCounter)
                                                    .append(":\n\t");
                int varNumbers = 0;
                int rightSideNum = -1;
                for (int i = 0; i < model.size(); i++) {
                    int var = model.get(i);
                    varNumbers++;
                    if (varNumbers > 1) {
                        modelConstraint.append(" + ");
                    }
                    modelConstraint.append("ds")
                                   .append(var);
                    rightSideNum++;
                }
                modelConstraint.append(" <= ")
                               .append(rightSideNum)
                               .append(";\n\n");
                minimalDistanceSetConstraintsEncoding.append(modelConstraint);

                ilpModelOptimumNew.add(index, minimalDistanceSetConstraintsEncoding.toString());

                Utils.writeToFile(StringUtils.join(ilpModelOptimumNew, "\n"), String.format(optimizationFileName, "satoh"));
                Glpsol.executeSolver(String.format(optimizationFileName, "satoh"), String.format(optimizationResultFileName, "satoh"));
                model = Glpsol.readMinimalSetSatoh(String.format(optimizationResultFileName, "satoh"),
                                                   this.discrepancyVarsPositions,
                                                   this.discrepancyVars2Positions);
            }
            Application.solverCallsEndTime = System.currentTimeMillis();
            System.out.println("[INFO] Finished solver calls");
            if (emptySet) {
                return "0";
            }
            return minimalDistanceSetConstraints.toString();
        } catch (MinimalSetConstraintsDeterminationException e) {
            throw e;
        } catch (Exception e) {
            throw new MinimalSetConstraintsDeterminationException(e);
        }
    }
}
