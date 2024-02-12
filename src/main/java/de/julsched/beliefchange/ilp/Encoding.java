package de.julsched.beliefchange.ilp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.OptimumFinder;
import de.julsched.beliefchange.exceptions.EncodingFailureException;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.instance.ContractionInstance;
import de.julsched.beliefchange.instance.RevisionInstance;
import de.julsched.beliefchange.utils.Utils;
import de.julsched.beliefchange.values.Distance;

public class Encoding {

    public static final String varDeclarationTemplate = "var %s binary;";

    public static final List<String> discrepancyConstraintTemplatesExact = Arrays.asList(new String[] {
        "d - k + v >= 0;",
        "d - v + k >= 0;",
        "d + k + v <= 2;",
        "-d + k + v >= 0;"
    });

    private static final List<String> discrepancyConstraintTemplatesVague = Arrays.asList(new String[] {
        "k - v - d <= 0;",
        "v - k - d <= 0;"
    });

    private BeliefChangeInstance instance;
    private Distance distance;
    private OptimumFinder optimumFinder;

    private int varNum;

    private List<String> varDeclarationEncoding = new ArrayList<String>();
    private List<String> baseClausesEncoding = new ArrayList<String>();
    private List<String> changeClausesEncoding = new ArrayList<String>();
    private List<String> discrepancyConstraintsEncoding = new ArrayList<String>();
    private List<String> finalConstraintsEncoding = new ArrayList<String>();

    private String minDistance;
    private String minimalDistanceSetConstraintsEncoding = "";
    private String distanceConstraint;

    private List<String> finalEncoding = new ArrayList<String>();

    public Encoding(BeliefChangeInstance instance, OptimumFinder optimumFinder, Distance distance) {
        this.distance = distance;
        this.instance = instance;
        this.optimumFinder = optimumFinder;
        this.varNum = instance.getVarNum();
    }

    public void create() {
        System.out.println("[INFO] Start compilation");
        Application.compilationStartTime = System.currentTimeMillis();
        try {
            if (this.instance instanceof ContractionInstance) {
                createModelContraction((ContractionInstance) this.instance);
            } else if (this.instance instanceof RevisionInstance) {
                createModelRevision((RevisionInstance) this.instance);
            }
            switch (this.distance) {
                case DALAL:
                    determineMinDistance();
                    createDistanceConstraint(this.minDistance);
                    break;
                case SATOH:
                    determineMinSetRestrictions();
                    break;
                default:
                    throw new WrongInputException("Unknown distance '" + this.distance + "'");
            }
            finalizeEncoding(this.instance);
            System.out.println("[INFO] Writing encoding to file '" + Application.resultFilePath + "'");
            Utils.writeToFile(StringUtils.join(this.finalEncoding, "\n"), Application.resultFilePath);
        } catch (Exception e) {
            throw new EncodingFailureException(e);
        }
        Application.compilationEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished compilation");
    }

    private void createModelContraction(ContractionInstance instance) {
        this.varDeclarationEncoding = createVarDeclarationEncodingContraction(this.varNum, instance);
        this.baseClausesEncoding = createBaseClauseEncodingContraction(instance);
        this.changeClausesEncoding = createChangeClauseEncodingContraction(instance);
        this.discrepancyConstraintsEncoding = createDiscrepancyConstraintsEncodingContraction(this.distance, this.varNum);
    }

    private void createModelRevision(RevisionInstance instance) {
        this.varDeclarationEncoding = createVarDeclarationEncodingRevision(this.varNum);
        this.baseClausesEncoding = createBaseClauseEncodingRevision(instance);
        this.changeClausesEncoding = createChangeClauseEncodingRevision(instance);
        this.discrepancyConstraintsEncoding = createDiscrepancyConstraintsEncodingRevision(this.distance, this.varNum);
    }

    public static List<String> createVarDeclarationEncodingRevision(int varNum) {
        List<String> varDeclaration = new ArrayList<String>();
        for (int x = 1; x <= varNum; x++) {
            varDeclaration.add(String.format(varDeclarationTemplate, "x" + x));
        }
        for (int x = 1; x <= varNum; x++) {
            varDeclaration.add(String.format(varDeclarationTemplate, "y" + x));
        }
        for (int x = 1; x <= varNum; x++) {
            varDeclaration.add(String.format(varDeclarationTemplate, "d" + x));
        }
        return varDeclaration;
    }

    public static List<String> createBaseClauseEncodingRevision(BeliefChangeInstance instance) {
        List<String> baseClausesEncoding = new ArrayList<String>();
        int constraintCounter = 0;
        for (String clause : instance.getBaseClauses()) {
            constraintCounter++;
            StringBuilder constraint = new StringBuilder("s.t. baseConstraint")
                                        .append(constraintCounter)
                                        .append(":\n\t");
            String[] vars = clause.split(" ");
            if (vars.length == 2) {
                constraint.append("y");
                if (vars[0].startsWith("-")) {
                    constraint.append(vars[0].split("-")[1])
                              .append(" = 0;");
                } else {
                    constraint.append(vars[0])
                              .append(" = 1;");
                }
            } else {
                int rightSideNum = 1;
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (vars[i].startsWith("-")) {
                        if (i != 0) {
                            constraint.append(" - ");
                        } else {
                            constraint.append("-");
                        }
                        constraint.append("y")
                                  .append(vars[i].split("-")[1]);
                        rightSideNum--;
                    } else {
                        if (i != 0) {
                            constraint.append(" + ");
                        }
                        constraint.append("y")
                                  .append(vars[i]);
                    }
                }
                constraint.append(" >= ")
                          .append(rightSideNum)
                          .append(";");
            }
            baseClausesEncoding.add(constraint.toString());
            baseClausesEncoding.add("");
        }
        return baseClausesEncoding;
    }

    public static List<String> createChangeClauseEncodingRevision(BeliefChangeInstance instance) {
        List<String> changeClausesEncoding = new ArrayList<String>();
        int constraintCounter = 0;
        for (String clause : instance.getChangeClauses()) {
            constraintCounter++;
            StringBuilder constraint = new StringBuilder("s.t. changeConstraint")
                                        .append(constraintCounter)
                                        .append(":\n\t");
            String[] vars = clause.split(" ");
            if (vars.length == 2) {
                constraint.append("x");
                if (vars[0].startsWith("-")) {
                    constraint.append(vars[0].split("-")[1])
                              .append(" = 0;");
                } else {
                    constraint.append(vars[0])
                              .append(" = 1;");
                }
            } else {
                int rightSideNum = 1;
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (vars[i].startsWith("-")) {
                        if (i != 0) {
                            constraint.append(" - ");
                        } else {
                            constraint.append("-");
                        }
                        constraint.append("x")
                                  .append(vars[i].split("-")[1]);
                        rightSideNum--;
                    } else {
                        if (i != 0) {
                            constraint.append(" + ");
                        }
                        constraint.append("x")
                                  .append(vars[i]);
                    }
                }
                constraint.append(" >= ")
                          .append(rightSideNum)
                          .append(";");
            }
            changeClausesEncoding.add(constraint.toString());
            changeClausesEncoding.add("");
        }
        return changeClausesEncoding;
    }

    public static List<String> createDiscrepancyConstraintsEncodingRevision(Distance distance, int varNum) {
        List<String> discrepancyConstraintTemplates;
        if (distance == Distance.SATOH) {
            discrepancyConstraintTemplates = discrepancyConstraintTemplatesExact;
        } else {
            discrepancyConstraintTemplates = discrepancyConstraintTemplatesVague;
        }
        List<String> discrepancyConstraints = new ArrayList<String>();
        int constraintCounter = 0;
        for (int x = 1; x <= varNum; x++) {
            for (String template : discrepancyConstraintTemplates) {
                constraintCounter++;
                discrepancyConstraints.add("s.t. discrepancyVarsConstraint"
                                        + constraintCounter
                                        + ":\n\t"
                                        + template.replaceAll("d", "d" + x)
                                                  .replaceAll("k", "y" + x)
                                                  .replaceAll("v", "x" + x));
                discrepancyConstraints.add("");
            }
        }
        return discrepancyConstraints;
    }

    public static List<String> createVarDeclarationEncodingContraction(int varNum, BeliefChangeInstance instance) {
        List<String> varDeclaration = new ArrayList<String>();
        // Create var declaration for auxiliary vars
        for (int a = 1; a <= instance.getChangeClauses().size(); a++) {
            varDeclaration.add(String.format(varDeclarationTemplate, "a" + a));
        }
        for (int i = 1; i <= varNum; i++) {
            // Create var declaration for base clause vars
            varDeclaration.add(String.format(varDeclarationTemplate, "y" + i));
        }
        for (int i = 1; i <= varNum; i++) {
            // Create var declaration for change clause vars
            varDeclaration.add(String.format(varDeclarationTemplate, "z" + i));
        }
        for (int i = 1; i <= varNum; i++) {
            // Create var declaration for discrepancy vars
            varDeclaration.add(String.format(varDeclarationTemplate, "d" + i));
        }
        return varDeclaration;
    }

    public static List<String> createBaseClauseEncodingContraction(BeliefChangeInstance instance) {
        List<String> baseClausesEncoding = new ArrayList<String>();
        int constraintCounter = 0;
        for (String clause : instance.getBaseClauses()) {
            constraintCounter++;
            StringBuilder constraint = new StringBuilder("s.t. baseConstraint")
                                        .append(constraintCounter)
                                        .append(":\n\t");
            String[] vars = clause.split(" ");
            if (vars.length == 2) {
                constraint.append("y");
                if (vars[0].startsWith("-")) {
                    constraint.append(vars[0].split("-")[1])
                              .append(" = 0;");
                } else {
                    constraint.append(vars[0])
                              .append(" = 1;");
                }
            } else {
                int rightSideNum = 1;
                for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                    if (vars[i].startsWith("-")) {
                        if (i != 0) {
                            constraint.append(" - ");
                        } else {
                            constraint.append("-");
                        }
                        constraint.append("y")
                                  .append(vars[i].split("-")[1]);
                        rightSideNum--;
                    } else {
                        if (i != 0) {
                            constraint.append(" + ");
                        }
                        constraint.append("y")
                                  .append(vars[i]);
                    }
                }
                constraint.append(" >= ")
                          .append(rightSideNum)
                          .append(";");
            }
            baseClausesEncoding.add(constraint.toString());
            baseClausesEncoding.add("");
        }
        return baseClausesEncoding;
    }

    public static List<String> createChangeClauseEncodingContraction(BeliefChangeInstance instance) {
        List<String> changeClausesEncoding = new ArrayList<String>();
        int constraintCounter = 0;
        for (String clause : instance.getChangeClauses()) {
            constraintCounter++;
            StringBuilder constraint = new StringBuilder("s.t. changeConstraint")
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
                    constraint.append("(1 - z");
                    constraint.append(vars[i].split("-")[1]);
                    constraint.append(")");
                } else {
                    constraint.append("z");
                    constraint.append(vars[i]);
                }
            }
            constraint.append(") / ")
                      .append(varNumber)
                      .append(" <= a")
                      .append(constraintCounter)
                      .append(";");
            changeClausesEncoding.add(constraint.toString());
            changeClausesEncoding.add("");
        }

        // Add constraint 'sum(auxiliaryVariables) < <number of clauses>'
        constraintCounter++;
        StringBuilder constraint = new StringBuilder("s.t. changeConstraint")
                                    .append(constraintCounter)
                                    .append(":\n\t");
        for (int a = 1; a <= instance.getChangeClauses().size(); a++) {
            if (a > 1) {
                constraint.append(" + ");
            }
            constraint.append("a")
                      .append(a);
        }
        constraint.append(" <= ")
                  .append(instance.getChangeClauses().size() - 1)
                  .append(";");
        changeClausesEncoding.add(constraint.toString());
        changeClausesEncoding.add("");
        return changeClausesEncoding;
    }

    public static List<String> createDiscrepancyConstraintsEncodingContraction(Distance distance, int varNum) {
        List<String> discrepancyConstraintTemplates;
        if (distance == Distance.SATOH) {
            discrepancyConstraintTemplates = discrepancyConstraintTemplatesExact;
        } else {
            discrepancyConstraintTemplates = discrepancyConstraintTemplatesVague;
        }
        List<String> discrepancyConstraints = new ArrayList<String>();
        int constraintCounter = 0;
        for (int x = 1; x <= varNum; x++) {
            for (String template : discrepancyConstraintTemplates) {
                constraintCounter++;
                discrepancyConstraints.add("s.t. discrepancyVarsConstraint"
                                    + constraintCounter
                                    + ":\n\t"
                                    + template.replaceAll("d", "d" + x)
                                              .replaceAll("k", "y" + x)
                                              .replaceAll("v", "z" + x));
                discrepancyConstraints.add("");
            }
        }
        return discrepancyConstraints;
    }

    private void determineMinDistance() {
        System.out.println("[INFO] Start minimum distance determination");
        Application.optimumFinderStartTime = System.currentTimeMillis();
        if (this.optimumFinder instanceof Ilp) {
            this.minDistance = ((Ilp) this.optimumFinder).getOptimum(this);
        } else {
            this.minDistance = this.optimumFinder.getOptimum(this.instance);
        }
        Application.optimumFinderEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Minimum distance: " + this.minDistance);
        System.out.println("[INFO] Finished minimum distance determination");
    }

    private void determineMinSetRestrictions() {
        System.out.println("[INFO] Start minimal distance sets determination");
        Application.optimumFinderStartTime = System.currentTimeMillis();
        String result;
        if (this.optimumFinder instanceof Ilp) {
            result = ((Ilp) this.optimumFinder).getMinSetConstraints(this);
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
            StringBuilder builder = new StringBuilder();
            int constraintCounter = 0;
            int auxVarCounter = 0;
            for (String constraint : result.split("\n")) {
                this.varDeclarationEncoding.add(String.format(varDeclarationTemplate, "s" + (auxVarCounter + 1)));
                this.varDeclarationEncoding.add(String.format(varDeclarationTemplate, "s" + (auxVarCounter + 2)));
                auxVarCounter += 2;
                constraintCounter++;
                builder.append("s.t. setConstraint")
                       .append(constraintCounter)
                       .append(":\n\t");
                String[] modelVars = constraint.split(" ");
                for (int i = 0; i < modelVars.length; i++) {
                    String modelVar = modelVars[i];
                    builder.append("d")
                           .append(modelVar);
                    if (i != modelVars.length - 1) {
                        builder.append(" + ");
                    }
                }
                if (modelVars.length > 1) {
                    builder.append(" - ")
                           .append(modelVars.length - 1);
                }
                builder.append(" <= s")
                       .append(auxVarCounter - 1)
                       .append(";\n\n");

                constraintCounter++;
                builder.append("s.t. setConstraint")
                       .append(constraintCounter)
                       .append(":\n\t(");
                for (int d = 1; d <= this.varNum; d++) {
                    if (d != 1) {
                        builder.append(" + ");
                    }
                    builder.append("d")
                           .append(d);
                }
                builder.append(" - ")
                       .append(modelVars.length)
                       .append(")");
                int difference = this.varNum - modelVars.length;
                if (difference != 1) {
                    builder.append(" / ")
                           .append(difference);
                }
                builder.append(" <= s")
                       .append(auxVarCounter)
                       .append(";\n\n");

                constraintCounter++;
                builder.append("s.t. setConstraint")
                       .append(constraintCounter)
                       .append(":\n\t")
                       .append("s")
                       .append(auxVarCounter - 1)
                       .append(" + s")
                       .append(auxVarCounter)
                       .append(" <= 1;\n\n");
            }
            this.minimalDistanceSetConstraintsEncoding = builder.toString();
        }
        if (result.equals("0")) {
            this.minimalDistanceSetConstraintsEncoding = "0";
        }
        Application.optimumFinderEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished minimal distance sets determination");
    }

    private void createDistanceConstraint(String distance) {
        StringBuilder distanceConstraintTemp = new StringBuilder();
        for (int d = 1; d <= this.varNum; d++) {
            if (d != 1) {
                distanceConstraintTemp.append(" + ");
            }
            distanceConstraintTemp.append("d")
                                  .append(d);
        }
        this.distanceConstraint = "s.t. distanceConstraint:\n\t"
                            + distanceConstraintTemp
                            + " = "
                            + distance
                            + ";";
    }

    private void finalizeEncoding(BeliefChangeInstance instance) {
        this.optimumFinder = null;
        this.finalEncoding.add("# Belief base variables: " + this.varNum);
        // If empty, then all difference sets are minimal sets
        if (this.distance == Distance.SATOH && this.minimalDistanceSetConstraintsEncoding.isEmpty()) {
            createSimpleFinalEncoding();
            return;
        }
        if (instance instanceof ContractionInstance) {
            for (int v = 1; v <= this.varNum; v++) {
                // Create original vars declarations
                this.varDeclarationEncoding.add(String.format(varDeclarationTemplate, "x" + v));
            }
            for (int v = 1; v <= this.varNum; v++) {
                // Create new base vars
                this.varDeclarationEncoding.add(String.format(varDeclarationTemplate, "b" + v));
            }
            for (int v = 1; v <= this.varNum; v++) {
                // Create new discrepancy vars set #1
                this.varDeclarationEncoding.add(String.format(varDeclarationTemplate, "e" + v));
            }
            for (int v = 1; v <= this.varNum; v++) {
                // Create new discrepancy vars set #2
                this.varDeclarationEncoding.add(String.format(varDeclarationTemplate, "f" + v));
            }

            // Create two auxiliary vars
            this.varDeclarationEncoding.add(String.format(varDeclarationTemplate, "g1"));
            this.varDeclarationEncoding.add(String.format(varDeclarationTemplate, "g2"));

            int constraintCounter = 0;
            for (String clause : instance.getBaseClauses()) {
                constraintCounter++;
                StringBuilder constraint = new StringBuilder("s.t. finalBaseClause")
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
                        constraint.append("b")
                                  .append(vars[i].split("-")[1]);
                        rightSideNum--;
                    } else {
                        if (i != 0) {
                            constraint.append(" + ");
                        }
                        constraint.append("b")
                                  .append(vars[i]);
                    }
                }
                constraint.append(" >= ")
                          .append(rightSideNum)
                          .append(";");
                this.finalConstraintsEncoding.add(constraint.toString());
                this.finalConstraintsEncoding.add("");
            }

            constraintCounter = 0;
            for (int i = 1; i <= this.varNum; i++) {
                for (String template : discrepancyConstraintTemplatesVague) {
                    constraintCounter++;
                    this.finalConstraintsEncoding.add("s.t. finalDiscrepancyConstraint"
                                                    + constraintCounter
                                                    + ":\n\t"
                                                    + template.replaceAll("d", "e" + i)
                                                              .replaceAll("k", "x" + i)
                                                              .replaceAll("v", "b" + i));
                    this.finalConstraintsEncoding.add("");
                }
            }

            for (int i = 1; i <= this.varNum; i++) {
                for (String template : discrepancyConstraintTemplatesVague) {
                    constraintCounter++;
                    this.finalConstraintsEncoding.add("s.t. finalDiscrepancyConstraint"
                                                        + constraintCounter
                                                        + ":\n\t"
                                                        + template.replaceAll("d", "f" + i)
                                                                  .replaceAll("k", "x" + i)
                                                                  .replaceAll("v", "z" + i));
                    this.finalConstraintsEncoding.add("");
                }
            }

            StringBuilder finalConstraint = new StringBuilder();
            finalConstraint.append("s.t. finalConstraint1:\n\t(e1");
            for (int i = 2; i <= this.varNum; i++) {
                finalConstraint.append(" + e")
                               .append(i);
            }
            finalConstraint.append(") / ")
                           .append(this.varNum)
                           .append(" <= g1;");
            this.finalConstraintsEncoding.add(finalConstraint.toString());
            this.finalConstraintsEncoding.add("");

            finalConstraint = new StringBuilder();
            finalConstraint.append("s.t. finalConstraint2:\n\t(f1");
            for (int i = 2; i <= this.varNum; i++) {
                finalConstraint.append(" + f")
                               .append(i);
            }
            finalConstraint.append(") / ")
                           .append(this.varNum)
                           .append(" <= g2;");
            this.finalConstraintsEncoding.add(finalConstraint.toString());
            this.finalConstraintsEncoding.add("");

            finalConstraint = new StringBuilder();
            finalConstraint.append("s.t. finalConstraint3:\n\t")
                           .append("g1 + g2 <= 1;");
            this.finalConstraintsEncoding.add(finalConstraint.toString());
            this.finalConstraintsEncoding.add("");
        }
        this.finalEncoding.addAll(this.varDeclarationEncoding);
        this.finalEncoding.add("");
        this.finalEncoding.addAll(this.baseClausesEncoding);
        this.finalEncoding.addAll(this.changeClausesEncoding);
        if (this.distance == Distance.DALAL) {
            this.finalEncoding.addAll(this.discrepancyConstraintsEncoding);
            this.finalEncoding.add(this.distanceConstraint);
        } else if (this.distance == Distance.SATOH) {
            if (this.minimalDistanceSetConstraintsEncoding.equals("0")) {
                if (this.instance instanceof RevisionInstance) {
                    this.discrepancyConstraintsEncoding = createDiscrepancyConstraintsEncodingRevision(Distance.DALAL, this.varNum);
                } else {
                    this.discrepancyConstraintsEncoding = createDiscrepancyConstraintsEncodingContraction(Distance.DALAL, this.varNum);
                }
                this.finalEncoding.addAll(this.discrepancyConstraintsEncoding);
                createDistanceConstraint("0");
                this.finalEncoding.add(this.distanceConstraint);
            } else {
                this.finalEncoding.addAll(this.discrepancyConstraintsEncoding);
                this.finalEncoding.add(this.minimalDistanceSetConstraintsEncoding);
            }
        }
        if (this.finalConstraintsEncoding.size() > 0) {
            this.finalEncoding.add("");
            this.finalEncoding.addAll(this.finalConstraintsEncoding);
        }
        this.finalEncoding.add("end;");
        this.finalEncoding.add("");
    }

    private void createSimpleFinalEncoding() {
        if (instance instanceof ContractionInstance) {
            for (int v = 1; v <= this.varNum; v++) {
                this.finalEncoding.add(String.format(varDeclarationTemplate, "z" + v));
            }
            for (int a = 1; a <= instance.getChangeClauses().size(); a++) {
                // Create auxiliary vars declarations
                this.finalEncoding.add(String.format(varDeclarationTemplate, "a" + a));
            }
            for (int v = 1; v <= this.varNum; v++) {
                // Create original vars declarations
                this.finalEncoding.add(String.format(varDeclarationTemplate, "x" + v));
            }
            for (int v = 1; v <= this.varNum; v++) {
                // Create base vars
                this.finalEncoding.add(String.format(varDeclarationTemplate, "b" + v));
            }
            for (int v = 1; v <= this.varNum; v++) {
                // Create new discrepancy vars set #1
                this.finalEncoding.add(String.format(varDeclarationTemplate, "e" + v));
            }
            for (int v = 1; v <= this.varNum; v++) {
                // Create new discrepancy vars set #2
                this.finalEncoding.add(String.format(varDeclarationTemplate, "f" + v));
            }

            // Create two auxiliary vars
            this.finalEncoding.add(String.format(varDeclarationTemplate, "g1"));
            this.finalEncoding.add(String.format(varDeclarationTemplate, "g2"));

            this.finalEncoding.add("");
            this.finalEncoding.addAll(this.changeClausesEncoding);
            this.finalEncoding.add("");

            int constraintCounter = 0;
            for (String clause : instance.getBaseClauses()) {
                constraintCounter++;
                StringBuilder constraint = new StringBuilder("s.t. baseClause")
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
                        constraint.append("b")
                                  .append(vars[i].split("-")[1]);
                        rightSideNum--;
                    } else {
                        if (i != 0) {
                            constraint.append(" + ");
                        }
                        constraint.append("b")
                                  .append(vars[i]);
                    }
                }
                constraint.append(" >= ")
                          .append(rightSideNum)
                          .append(";");
                this.finalEncoding.add(constraint.toString());
                this.finalEncoding.add("");
            }

            constraintCounter = 0;
            for (int i = 1; i <= this.varNum; i++) {
                for (String template : discrepancyConstraintTemplatesVague) {
                    constraintCounter++;
                    this.finalEncoding.add("s.t. finalDiscrepancyConstraint"
                                            + constraintCounter
                                            + ":\n\t"
                                            + template.replaceAll("d", "e" + i)
                                                      .replaceAll("k", "x" + i)
                                                      .replaceAll("v", "b" + i));
                    this.finalEncoding.add("");
                }
            }

            for (int i = 1; i <= this.varNum; i++) {
                for (String template : discrepancyConstraintTemplatesVague) {
                    constraintCounter++;
                    this.finalEncoding.add("s.t. finalDiscrepancyConstraint"
                                                + constraintCounter
                                                + ":\n\t"
                                                + template.replaceAll("d", "f" + i)
                                                          .replaceAll("k", "x" + i)
                                                          .replaceAll("v", "z" + i));
                    this.finalEncoding.add("");
                }
            }

            StringBuilder finalConstraint = new StringBuilder();
            finalConstraint.append("s.t. finalConstraint1:\n\t(e1");
            for (int i = 2; i <= this.varNum; i++) {
                finalConstraint.append(" + e")
                               .append(i);
            }
            finalConstraint.append(") / ")
                           .append(this.varNum)
                           .append(" <= g1;");
            this.finalEncoding.add(finalConstraint.toString());
            this.finalEncoding.add("");

            finalConstraint = new StringBuilder();
            finalConstraint.append("s.t. finalConstraint2:\n\t(f1");
            for (int i = 2; i <= this.varNum; i++) {
                finalConstraint.append(" + f")
                               .append(i);
            }
            finalConstraint.append(") / ")
                           .append(this.varNum)
                           .append(" <= g2;");
            this.finalEncoding.add(finalConstraint.toString());
            this.finalEncoding.add("");

            finalConstraint = new StringBuilder();
            finalConstraint.append("s.t. finalConstraint3:\n\t")
                           .append("g1 + g2 <= 1;");
            this.finalEncoding.add(finalConstraint.toString());
            this.finalEncoding.add("");

        } else {
            for (int v = 1; v <= this.varNum; v++) {
                this.finalEncoding.add(String.format(varDeclarationTemplate, "x" + v));
            }
            this.finalEncoding.add("");
            this.finalEncoding.addAll(this.changeClausesEncoding);
        }
        this.finalEncoding.add("end;");
        this.finalEncoding.add("");
    }

    public List<String> getVarDeclarationEncoding() {
        return this.varDeclarationEncoding;
    }

    public List<String> getBaseClausesEncoding() {
        return this.baseClausesEncoding;
    }

    public List<String> getChangeClausesEncoding() {
        return this.changeClausesEncoding;
    }

    public List<String> getDiscrepancyConstraintsEncoding() {
        return this.discrepancyConstraintsEncoding;
    }

    public BeliefChangeInstance getInstance() {
        return this.instance;
    }

    public int getVarNum() {
        return this.varNum;
    }
}
