package de.julsched.beliefchange.instance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.julsched.beliefchange.exceptions.ValidationException;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.utils.Tseitin;
import de.julsched.beliefchange.utils.MaxHS;
import de.julsched.beliefchange.utils.Utils;
import de.julsched.beliefchange.values.Operation;

public abstract class BeliefChangeInstance {

    // Regex for clauses: /^(-?[1-9][0-9]*\s)+0$/
    // Regex for parameter line: /^p\scnf\s[1-9][0-9]*\s[1-9][0-9]*$/
    static final Map<String, Pattern> regex = Map.of(
        "clause", Pattern.compile("^(-?[1-9][0-9]*\\s)+0$"),
        "paramLine", Pattern.compile("^p\\scnf\\s(?<varNum>[1-9][0-9]*)\\s(?<clauseNum>[1-9][0-9]*)$")
    );

    private static final String lineSeparatorN = "n ---";

    private final Operation operation;

    private String paramLine;
    private int varNum;
    private int clauseNum;
    private List<String> baseClauses = new ArrayList<String>();
    private List<String> changeClauses = new ArrayList<String>();

    public static String extractVarNum(String parameterLine) {
        Matcher matcher = regex.get("paramLine").matcher(parameterLine);
        if (!matcher.matches()) {
            throw new RuntimeException("'" + parameterLine + "' is not a valid dimacs parameter line");
        }
        return matcher.group("varNum");
    }

    public static String extractClauseNum(String parameterLine) {
        Matcher matcher = regex.get("paramLine").matcher(parameterLine);
        if (!matcher.matches()) {
            throw new RuntimeException("'" + parameterLine + "' is not a valid dimacs parameter line");
        }
        return matcher.group("clauseNum");
    }

    protected BeliefChangeInstance(File file, boolean validateInstance, Operation operation) {
        this.operation = operation;
        validateFormat(file);
        if (validateInstance) {
            String testFile = "satisfiability-test.cnf";
            try {
                StringBuilder satisfiabilityTest = new StringBuilder("p cnf " + this.varNum + " " + this.baseClauses.size() + "\n");
                for (String baseClause : this.baseClauses) {
                    satisfiabilityTest.append(baseClause)
                                      .append("\n");
                }
                Utils.writeToFile(satisfiabilityTest.toString(), testFile);
                Process process = MaxHS.execute(testFile, false);
                if (!MaxHS.isSatisfiable(process)) {
                    throw new ValidationException("Belief base formula is unsatisfiable");
                }
                System.out.println("[INFO] Belief base formula is satisfiable");

                if (operation == Operation.REVISION) {
                    satisfiabilityTest = new StringBuilder("p cnf " + this.varNum + " " + this.changeClauses.size() + "\n");
                    for (String changeClause : this.changeClauses) {
                        satisfiabilityTest.append(changeClause)
                                        .append("\n");
                    }
                    Utils.writeToFile(satisfiabilityTest.toString(), testFile);
                    process = MaxHS.execute(testFile, false);
                    if (!MaxHS.isSatisfiable(process)) {
                        throw new ValidationException("Revision formula is unsatisfiable");
                    }
                    System.out.println("[INFO] Revision formula is satisfiable");
                }

                String[] negatedClauses = Tseitin.negateCnfFormula(this.varNum, this.changeClauses);
                if (negatedClauses[0].equals("unsat") && operation == Operation.REVISION) {
                    // Revision formula is a tautology
                    throw new ValidationException("Revision formula is already believed (tautology)");
                }
                if (negatedClauses[0].equals("tautology") && operation == Operation.CONTRACTION) {
                    // Contraction formula is unsatisfiable
                    throw new ValidationException("Contraction formula is not believed (unsatisfiable)");
                }
                if (negatedClauses[0].equals("unsat") && operation == Operation.CONTRACTION) {
                    throw new ValidationException("Contraction formula is a tautology");
                }

                String paramLineNegation = negatedClauses[0];
                String varNumNew = BeliefChangeInstance.extractVarNum(paramLineNegation);
                if (operation == Operation.CONTRACTION) {
                    satisfiabilityTest = new StringBuilder("p cnf " + varNumNew + " " + (negatedClauses.length - 1) + "\n");
                    for (int i = 1; i < negatedClauses.length; i++) {
                        satisfiabilityTest.append(negatedClauses[i])
                                          .append("\n");
                    }
                    Utils.writeToFile(satisfiabilityTest.toString(), testFile);
                    process = MaxHS.execute(testFile, false);
                    if (!MaxHS.isSatisfiable(process)) {
                        throw new ValidationException("Contraction formula is a tautology");
                    }
                    System.out.println("[INFO] Contraction formula is not a tautology");
                }

                int clauseNumNew = this.baseClauses.size() + negatedClauses.length - 1;
                satisfiabilityTest = new StringBuilder("p cnf " + varNumNew + " " + clauseNumNew + "\n");
                for (String baseClause : this.baseClauses) {
                    satisfiabilityTest.append(baseClause)
                                      .append("\n");
                }
                for (int i = 1; i < negatedClauses.length; i++) {
                    satisfiabilityTest.append(negatedClauses[i])
                                      .append("\n");
                }
                Utils.writeToFile(satisfiabilityTest.toString(), testFile);
                process = MaxHS.execute(testFile, false);
                if (operation == Operation.REVISION) {
                    if (MaxHS.isSatisfiable(process)) {
                        System.out.println("[INFO] Revision formula is not yet believed");
                    } else {
                        throw new ValidationException("Revision formula is already believed");
                    }
                }
                if (operation == Operation.CONTRACTION) {
                    if (MaxHS.isSatisfiable(process)) {
                        throw new ValidationException("Contraction formula is not believed");
                    } else {
                        System.out.println("[INFO] Contraction formula is believed");
                    }
                }

                System.out.println("-".repeat(100));
            } catch (ValidationException e) {
                throw e;
            } catch (Exception e) {
                throw new ValidationException("Failed to validate belief change instance", e);
            } finally {
                if (new File(testFile).exists()) {
                    try {
                        Files.delete(Paths.get(testFile));
                    } catch (IOException e) {
                        throw new ValidationException("Failed to validate belief change instance", e);
                    }
                }
            }
        }
    }

    private void validateFormat(File file) {
        System.out.println("[INFO] Validate format of instance file '" + file.getPath() + "'");

        List<String> lines = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file));) {
            boolean hasProperContent = false;
            String line = reader.readLine();
            while (line != null) {
                String prettyLine = line.replaceAll("\s+", " ").trim();
                lines.add(prettyLine);
                if (!prettyLine.isBlank() && !prettyLine.startsWith("c")) {
                    hasProperContent = true;
                }
                line = reader.readLine();
            }
            if (!hasProperContent) {
                throw new WrongInstanceFormatException("Instance file is empty");
            }
        } catch (FileNotFoundException e) {
            throw new WrongInputException("Provided instance file '" + file.getPath() + "' does not exist");
        } catch (IOException e) {
            throw new WrongInstanceFormatException("Provided instance in file '"+ file.getPath() + "' cannot be read");
        }

        int paramLineIndex = validateParamLine(lines);
        validateClauses(lines, paramLineIndex);

        System.out.println("[INFO] Instance file format is valid");
        System.out.println("-".repeat(100));
    }

    private int validateParamLine(List<String> lines) {
        int paramLineIndex = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isBlank() && !line.startsWith("c")) {
                Matcher matcher = regex.get("paramLine").matcher(line);
                if (!matcher.matches()) {
                    throw new WrongInstanceFormatException("Line " + (i + 1) + ": invalid parameter line");
                } else {
                    this.paramLine = line;
                    paramLineIndex = i;
                    break;
                }
            }
        }
        this.varNum = Integer.parseInt(BeliefChangeInstance.extractVarNum(this.paramLine));
        this.clauseNum = Integer.parseInt(BeliefChangeInstance.extractClauseNum(this.paramLine));
        return paramLineIndex;
    }

    private void validateClauses(List<String> lines, int paramLineIndex) {
        int separatorsN = 0;
        int separatorNIndex = -1;
        int separatorNLine = 0;

        int properLinesTotal = 1;
        for (int i = paramLineIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isBlank() && !line.startsWith("c")) {
                properLinesTotal++;
                if (line.equals(lineSeparatorN)) {
                    separatorsN++;
                    separatorNIndex = i;
                    separatorNLine = properLinesTotal;
                }
            }
        }

        if (separatorsN == 0) {
            throw new WrongInstanceFormatException(
                "Base clauses and " + this.operation + " clauses must be separated by a line of the form '" + lineSeparatorN + "'"
            );
        } else if (separatorsN > 1) {
            throw new WrongInstanceFormatException("More than one set of " + this.operation + " clauses: Only one set should be specified");
        }
        if (separatorNLine == 2) {
            throw new WrongInstanceFormatException("At least one base clause needs to be provided");
        } else if (separatorNLine == properLinesTotal) {
            throw new WrongInstanceFormatException("At least one " + this.operation + " clause needs to be provided");
        }

        int maxNum = 0;

        for (int i = paramLineIndex + 1; i < separatorNIndex; i++) {
            String clause = lines.get(i);
            if (!clause.isBlank() && !clause.startsWith("c")) {
                Matcher matcher = regex.get("clause").matcher(clause);
                if (!matcher.matches()) {
                    throw new WrongInstanceFormatException("Line " + (i + 1) + ": invalid base clause");
                }
                this.baseClauses.add(clause);
                String[] vars = clause.split(" ");
                for (int x = 0; x < vars.length - 1; x++) { // Skip last index since that is '0'
                    int number = Math.abs(Integer.parseInt(vars[x]));
                    if (number > maxNum) {
                        maxNum = number;
                    }
                }
            }
        }

        for (int i = separatorNIndex + 1; i < lines.size(); i++) {
            String clause = lines.get(i);
            if (!clause.isBlank() && !clause.startsWith("c")) {
                Matcher matcher = regex.get("clause").matcher(clause);
                if (!matcher.matches()) {
                    throw new WrongInstanceFormatException("Line " + (i + 1) + ": invalid " + this.operation + " clause");
                }
                this.changeClauses.add(clause);
                String[] vars = clause.split(" ");
                for (int x = 0; x < vars.length - 1; x++) { // Skip last index since that is '0'
                    int number = Math.abs(Integer.parseInt(vars[x]));
                    if (number > maxNum) {
                        maxNum = number;
                    }
                }
            }
        }

        if (maxNum > this.varNum) {
            throw new WrongInstanceFormatException("Found variable '" + maxNum + "', but expected a total of " + this.varNum + " variables");
        }

        int clauseNum = this.baseClauses.size() + this.changeClauses.size();
        if (clauseNum != this.clauseNum) {
            throw new WrongInstanceFormatException("Found " + clauseNum + " clauses, but expected " + this.clauseNum);
        }
    }

    public List<String> getBaseClauses() {
        return this.baseClauses;
    }

    public List<String> getChangeClauses() {
        return this.changeClauses;
    }

    public int getClauseNum() {
        return this.clauseNum;
    }

    public Operation getOperation() {
        return this.operation;
    }

    public int getVarNum() {
        return this.varNum;
    }
}
