package de.julsched.beliefchange.sat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.OptimumFinder;
import de.julsched.beliefchange.exceptions.EncodingFailureException;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.sat.utils.BinaryCounter;
import de.julsched.beliefchange.utils.Utils;
import de.julsched.beliefchange.values.Distance;


public abstract class Encoding {

    public static final List<String> discrepancyClauseTemplatesExact = Arrays.asList(new String[] {
        "d -x y 0",
        "d x -y 0",
        "-d -x -y 0",
        "-d x y 0"
    });

    public static final List<String> discrepancyClauseTemplatesVague = Arrays.asList(new String[] {
        "d -x y 0",
        "d x -y 0"
    });

    protected BeliefChangeInstance instance;
    protected Distance distance;
    protected OptimumFinder optimumFinder;

    protected int clauseNum;
    protected int varNum;
    protected int clauseNumBasicEncoding;

    protected int varNumMinimalSetEncoding = 0;

    protected String minDistance;
    protected List<String> minimalDistanceSetConstraints = new ArrayList<String>();

    protected List<Integer> discrepancyVars = new ArrayList<Integer>();
    protected List<Integer> counterBitsReversed = new ArrayList<Integer>();

    protected List<String> result = new ArrayList<String>();

    protected Encoding(BeliefChangeInstance instance, OptimumFinder optimumFinder, Distance distance) throws IOException {
        this.instance = instance;
        this.distance = distance;
        this.optimumFinder = optimumFinder;
        this.clauseNum = instance.getClauseNum();
        this.varNum = instance.getVarNum();
    }

    public BeliefChangeInstance getInstance() {
        return this.instance;
    }

    public int getVarNum() {
        return this.varNum;
    }

    public List<Integer> getDiscrepancyVars() {
        return this.discrepancyVars;
    }

    public void create() throws IOException {
        System.out.println("[INFO] Start compilation");
        Application.compilationStartTime = System.currentTimeMillis();
        try {
            createVarMap();
            createBasicEncoding();
            createDiscrepancyClauses();
            switch (this.distance) {
                case DALAL:
                    findMinDistance();
                    if (this.minDistance.equals("0")) {
                        createZeroDistanceConstraintClauses();
                    } else {
                        createBinaryCounterClauses();
                        createDistanceConstraintClauses();
                    }
                    break;
                case SATOH:
                    findMinimalDistanceSetConstraints();
                    if (!this.minimalDistanceSetConstraints.isEmpty()) {
                        createMinimalSetConstraints();
                    }
                    break;
                default:
                    throw new WrongInputException("Unknown distance '" + this.distance + "'");
            }
            finalizeEncoding();
            System.out.println("[INFO] Writing encoding to file '" + Application.resultFilePath + "'");
            Utils.writeToFile(StringUtils.join(this.result, "\n"), Application.resultFilePath);
        } catch (EncodingFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new EncodingFailureException(e);
        }
        Application.compilationEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished compilation");
    }

    protected abstract void createVarMap();

    protected abstract void createBasicEncoding() throws IOException;

    protected abstract void createDiscrepancyClauses();

    protected void determineDiscrepancyVars(int vars) {
        for (int i = 1; i <= vars; i++) {
            this.varNum++;
            this.discrepancyVars.add(this.varNum);
        }
    }

    private void findMinDistance() {
        System.out.println("[INFO] Start minimum distance determination");
        Application.optimumFinderStartTime = System.currentTimeMillis();
        determineMinDistance();
        Application.optimumFinderEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Minimum distance: " + this.minDistance);
        System.out.println("[INFO] Finished minimum distance determination");
    }

    protected abstract void determineMinDistance();

    protected void createZeroDistanceConstraintClauses() {
        for (int i = 0; i < this.discrepancyVars.size(); i++) {
            int discrepancyVar = this.discrepancyVars.get(i);
            StringBuilder finalConstraint = new StringBuilder();
            finalConstraint.append("-")
                           .append(discrepancyVar)
                           .append(" 0");
            this.result.add(finalConstraint.toString());
            this.clauseNum++;
        }
    }

    protected void createBinaryCounterClauses() {
        BinaryCounter binaryCounter = new BinaryCounter();
        List<String> encoding = binaryCounter.createEncoding(this.discrepancyVars);
        this.counterBitsReversed = binaryCounter.getCounterBitsReversed();
        this.varNum = binaryCounter.getNumMax();
        this.result.addAll(encoding);
        this.clauseNum += encoding.size();
    }

    private void createDistanceConstraintClauses() {
        String minDistanceBinary = Integer.toBinaryString(Integer.parseInt(this.minDistance));
        int index = -1;
        StringBuilder clause;
        for (int i = minDistanceBinary.length() - 1; i >= 0; i--) {
            char bit = minDistanceBinary.charAt(i);
            index++;
            if (bit == '0') {
                clause = new StringBuilder("-")
                            .append(this.counterBitsReversed.get(index))
                            .append(" 0");
                this.result.add(clause.toString());
            } else if (bit == '1') {
                clause = new StringBuilder(Integer.toString(this.counterBitsReversed.get(index)))
                            .append(" 0");
                this.result.add(clause.toString());
            }
            this.clauseNum++;
        }
        for (int i = index + 1; i < this.counterBitsReversed.size(); i++) {
            clause = new StringBuilder("-")
                        .append(this.counterBitsReversed.get(i))
                        .append(" 0");
            this.result.add(clause.toString());
            this.clauseNum++;
        }
    }

    protected abstract void finalizeEncoding();

    protected void addParamsLine() {
        this.result.add(0, "p cnf " + this.varNum + " " + this.clauseNum);
        this.result.add(0, "c Belief base variables: " + this.instance.getVarNum());
        this.result.add("");
    }

    private void findMinimalDistanceSetConstraints() {
        System.out.println("[INFO] Start minimal distance sets determination");
        Application.optimumFinderStartTime = System.currentTimeMillis();
        this.minimalDistanceSetConstraints = getMinimalDistanceSetConstraints();
        Application.optimumFinderEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished minimal distance sets determination");
    }

    protected List<String> getMinimalDistanceSetConstraints() {
        List<String> resultLines = new ArrayList<String>();
        String result;
        if (this.optimumFinder instanceof MaxSat) {
            result = ((MaxSat) this.optimumFinder).getMinSetConstraints(this);
        } else {
            result = this.optimumFinder.getMinSetConstraints(instance);
        }
        if (result.isEmpty()) {
            System.out.println("[INFO] Determined minimal distance sets: " + 0);
        } else if (result.equals("0")) {
            System.out.println("[INFO] Determined minimal distance sets: " + 1);
        } else {
            System.out.println("[INFO] Determined minimal distance sets: " + result.split("\n").length);
        }
        if (!result.isEmpty() && !result.equals("0")) {
            for (String line : result.split("\n")) {
                StringBuilder newLine = new StringBuilder();
                String[] modelVars = line.split(" ");
                for (int i = 0; i < modelVars.length; i++) {
                    String modelVar = modelVars[i];
                    newLine.append(this.discrepancyVars.get(Integer.parseInt(modelVar) - 1));
                    if (i != modelVars.length - 1) {
                        newLine.append(" ");
                    }
                }
                resultLines.add(newLine.toString());
            }
        }
        if (result.equals("0")) {
            resultLines.add("0");
        }
        return resultLines;
    }

    protected abstract void createMinimalSetConstraints();
}
