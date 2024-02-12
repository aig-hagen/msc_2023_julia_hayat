package de.julsched.beliefchange.naive;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.exceptions.ModelDeterminationException;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.instance.ContractionInstance;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.instance.RevisionInstance;
import de.julsched.beliefchange.utils.Tseitin;
import de.julsched.beliefchange.utils.CaDiCal;
import de.julsched.beliefchange.utils.Utils;
import de.julsched.beliefchange.values.Distance;
import de.julsched.beliefchange.values.Operation;

public class NaiveModelDetermination {

    private static final String modelDeterminationFileName = Application.dirInterimResults + "/naive-model-determination.cnf";

    private BeliefChangeInstance instance;
    private Distance distance;

    public NaiveModelDetermination(File instanceFile, boolean validateInstance, Operation operation, Distance distance) {
        switch (operation) {
            case CONTRACTION:
                this.instance = new ContractionInstance(instanceFile, validateInstance);
                break;
            case REVISION:
                this.instance = new RevisionInstance(instanceFile, validateInstance);
                break;
            default:
                throw new WrongInputException("Invalid operation '" + operation + "'");
        }
        this.distance = distance;
    }

    public void execute() throws IOException, InterruptedException {
        // Determine models of base
        List<String> baseClauses = this.instance.getBaseClauses();
        StringBuilder dimacs = new StringBuilder("p cnf ")
                                .append(this.instance.getVarNum())
                                .append(" ")
                                .append(baseClauses.size())
                                .append("\n");
        for (String clause : baseClauses) {
            dimacs.append(clause)
                  .append("\n");
        }
        List<int[]> baseModels = getModels(dimacs.toString());
        if (baseModels.size() == 0) {
            throw new ModelDeterminationException("Failed to determine models of belief base. "
                                                    + "Belief base is unsatisfiable");
        }

        // Determine models of change formula
        if (this.instance.getOperation() == Operation.CONTRACTION) {
            List<String> changeClauses = negateChangeClauses(this.instance);
            if (changeClauses.get(0).equals("tautology")) {
                throw new ModelDeterminationException("Failed to determine models. "
                                                        + "Contraction formula is unsatisfiable");
            }
            if (changeClauses.get(0).equals("unsat")) {
                throw new ModelDeterminationException("Contraction formula is a tautology");
            }
            String paramLine = changeClauses.get(0);
            String varNumNew = BeliefChangeInstance.extractVarNum(paramLine);
            dimacs = new StringBuilder("p cnf ")
                        .append(varNumNew)
                        .append(" ")
                        .append(changeClauses.size() - 1)
                        .append("\n");
            for (int i = 1; i < changeClauses.size(); i++) {
                dimacs.append(changeClauses.get(i))
                      .append("\n");
            }
        } else {
            List<String> changeClauses = this.instance.getChangeClauses();
            dimacs = new StringBuilder("p cnf ")
                        .append(this.instance.getVarNum())
                        .append(" ")
                        .append(changeClauses.size())
                        .append("\n");
            for (String clause : changeClauses) {
                dimacs.append(clause)
                      .append("\n");
            }
        }
        List<int[]> changeFormulaModels = getModels(dimacs.toString());
        if (changeFormulaModels.size() == 0) {
            throw new ModelDeterminationException("Failed to determine models of change formula. "
                                                    + "Change formula is unsatisfiable");
        }

        List<int[]> resultModels = null;
        switch(this.distance) {
            case DALAL:
                resultModels = runDalal(baseModels, changeFormulaModels);
                break;
            case SATOH:
                resultModels = runSatoh(baseModels, changeFormulaModels);
                break;
            default:
                throw new WrongInputException("Invalid distance '" + this.distance + "'");
        }
        if (this.instance.getOperation() == Operation.CONTRACTION) {
            resultModels.addAll(baseModels);
        }

        // Set ensures that duplicate models are eliminated
        HashSet<String> modelsSet = new HashSet<String>();
        for (int[] resultModel : resultModels) {
            StringBuilder finalModel = new StringBuilder();
            for (int i = 0; i < resultModel.length; i++) {
                finalModel.append(resultModel[i])
                          .append(" ");
            }
            modelsSet.add(finalModel.toString().trim());
        }

        StringBuilder models = new StringBuilder();
        for (String finalModel : modelsSet) {
            models.append(finalModel)
                  .append("\n");
        }

        StringBuilder output = new StringBuilder("# Belief base variables: ")
                                .append(this.instance.getVarNum())
                                .append("\n")
                                .append(models.toString());
        Utils.writeToFile(output.toString(), Application.resultFilePath);
    }

    private List<int[]> getModels(String instance) throws IOException, InterruptedException {
        List<int[]> models = new ArrayList<int[]>();
        String dimacsInstance = instance;
        Utils.writeToFile(dimacsInstance, modelDeterminationFileName);
        Process process = CaDiCal.execute(modelDeterminationFileName);
        int[] model;
        while ((model = CaDiCal.readSolutionModel(process)) != null) {
            if (this.instance.getOperation() == Operation.CONTRACTION) {
                model = Arrays.copyOfRange(model, 0, this.instance.getVarNum());
            }
            models.add(model);
            StringBuilder newClause = new StringBuilder();
            for (int i = 0; i < model.length; i++) {
                int var = model[i];
                newClause.append((-1) * var)
                         .append(" ");
            }
            newClause.append("0\n");
            dimacsInstance = dimacsInstance + newClause;
            String[] dimacsInstanceClauses = dimacsInstance.split("\n");
            String paramLineOld = dimacsInstanceClauses[0];
            String varNum = BeliefChangeInstance.extractVarNum(paramLineOld);
            int clauseNumOld = Integer.parseInt(BeliefChangeInstance.extractClauseNum(paramLineOld));
            int clauseNumNew = clauseNumOld + 1;
            String paramLineNew = "p cnf " + varNum + " " + clauseNumNew;
            dimacsInstanceClauses[0] = paramLineNew;

            StringBuilder dimacsInstanceBuilder = new StringBuilder();
            for (String line : dimacsInstanceClauses) {
                dimacsInstanceBuilder.append(line)
                                     .append("\n");
            }
            dimacsInstance = dimacsInstanceBuilder.toString();

            Utils.writeToFile(dimacsInstance, modelDeterminationFileName);
            process = CaDiCal.execute(modelDeterminationFileName);
        }
        return models;
    }

    private List<int[]> runDalal(List<int[]> baseModels, List<int[]> changeFormulaModels) throws IOException {
        // Compare all models to find the minimum distance
        System.out.println("[INFO] Start minimum distance determination");
        int minDistance = this.instance.getVarNum();
        List<int[]> resultModels = new ArrayList<int[]>();
        for (int[] baseModel : baseModels) {
            for (int[] changeFormulaModel : changeFormulaModels) {
                int differences = 0;
                for (int i = 0; i < baseModel.length; i++) {
                    if (differences > minDistance) {
                        break;
                    }

                    if (baseModel[i] != changeFormulaModel[i]) {
                        differences++;
                    }
                }
                if (differences == minDistance) {
                    resultModels.add(changeFormulaModel);
                } else if (differences < minDistance) {
                    minDistance = differences;
                    resultModels = new ArrayList<int[]>();
                    resultModels.add(changeFormulaModel);
                }
            }
        }
        System.out.println("[INFO] Minimum distance: " + minDistance);
        System.out.println("[INFO] Finished minimum distance determination");
        return resultModels;
    }

    private List<int[]> runSatoh(List<int[]> baseModels, List<int[]> changeFormulaModels) throws IOException {
        // Compare all models to get the difference sets
        List<int[]> differenceSets = new ArrayList<int[]>();
        for (int[] baseModel : baseModels) {
            for (int[] changeFormulaModel : changeFormulaModels) {
                int[] differenceSet = new int[this.instance.getVarNum()];
                for (int i = 0; i < baseModel.length; i++) {
                    if (baseModel[i] == changeFormulaModel[i]) {
                        differenceSet[i] = 0;
                    } else {
                        differenceSet[i] = 1;
                    }
                }
                differenceSets.add(differenceSet);
            }
        }

        // Determine minimal sets
        System.out.println("[INFO] Start minimal distance sets determination");
        List<int[]> minimalSets = new ArrayList<int[]>();
        for (int[] differenceSet : differenceSets) {
            boolean hasProperSubset = false;
            for (int[] differenceSet2 : differenceSets) {
                boolean areIdentical = true;
                boolean set2isSubset = true;
                for (int i = 0; i < differenceSet.length; i++) {
                    if (differenceSet[i] != differenceSet2[i]) {
                        areIdentical = false;
                    }
                    if (differenceSet[i] == 0 && differenceSet2[i] == 1) {
                        set2isSubset = false;
                        break;
                    }
                }
                if (!areIdentical && set2isSubset) { // = proper subset
                    hasProperSubset = true;
                    break;
                }
            }
            if (!hasProperSubset) {
                minimalSets.add(differenceSet);
            }
        }
        System.out.println("[INFO] Finished minimal distance sets determination");

        // Determine the result models
        List<int[]> resultModels = new ArrayList<int[]>();
        for (int[] baseModel : baseModels) {
            for (int[] changeFormulaModel : changeFormulaModels) {
                int[] differenceSet = new int[this.instance.getVarNum()];
                for (int i = 0; i < baseModel.length; i++) {
                    if (baseModel[i] == changeFormulaModel[i]) {
                        differenceSet[i] = 0;
                    } else {
                        differenceSet[i] = 1;
                    }
                }

                for (int[] minimalSet : minimalSets) {
                    boolean isIdentical = true;
                    for (int i = 0; i < minimalSet.length; i++) {
                        if (minimalSet[i] != differenceSet[i]) {
                            isIdentical = false;
                            break;
                        }
                    }
                    if (isIdentical) {
                        resultModels.add(changeFormulaModel);
                        break;
                    }
                }
            }
        }
        return resultModels;
    }

    private List<String> negateChangeClauses(BeliefChangeInstance instance) {
        String[] dimacsNegation = Tseitin.negateCnfFormula(instance.getVarNum(), instance.getChangeClauses());
        return Arrays.asList(dimacsNegation);
    }
}
