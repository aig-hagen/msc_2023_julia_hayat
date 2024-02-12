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
import java.util.regex.Matcher;

import de.julsched.beliefchange.exceptions.ValidationException;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.utils.CaDiCal;
import de.julsched.beliefchange.utils.Tseitin;
import de.julsched.beliefchange.utils.Utils;

public class InferenceCheckInstance {

    private List<String> inferenceClauses = new ArrayList<String>();
    private int varNum;

    public InferenceCheckInstance(File instanceFile, boolean validateInstance) {
        validateFormat(instanceFile);
        if (validateInstance) {
            String testFile = "satisfiability-test.cnf";
            try {
                String[] negatedClauses = Tseitin.negateCnfFormula(this.varNum, this.inferenceClauses);
                if (negatedClauses[0].equals("tautology")) {
                    // Inference formula is unsatisfiable
                    throw new ValidationException("Inference formula is unsatisfiable");
                }
                if (negatedClauses[0].equals("unsat")) {
                    // Inference formula is a tautology
                    throw new ValidationException("Inference formula is a tautology");
                }

                StringBuilder satisfiabilityTest = new StringBuilder("p cnf " + this.varNum + " " + this.inferenceClauses.size() + "\n");
                for (String clause : this.inferenceClauses) {
                    satisfiabilityTest.append(clause)
                                      .append("\n");
                }
                Utils.writeToFile(satisfiabilityTest.toString(), testFile);
                Process process = CaDiCal.execute(testFile);
                if (!CaDiCal.isSatisfiable(process)) {
                    throw new ValidationException("Inference formula is unsatisfiable");
                }
                System.out.println("[INFO] Inference formula is satisfiable");

                String paramLineNegation = negatedClauses[0];
                String varNumNew = BeliefChangeInstance.extractVarNum(paramLineNegation);
                satisfiabilityTest = new StringBuilder("p cnf " + varNumNew + " " + (negatedClauses.length - 1) + "\n");
                for (int i = 1; i < negatedClauses.length; i++) {
                    satisfiabilityTest.append(negatedClauses[i])
                                      .append("\n");
                }
                Utils.writeToFile(satisfiabilityTest.toString(), testFile);
                process = CaDiCal.execute(testFile);
                if (!CaDiCal.isSatisfiable(process)) {
                    throw new ValidationException("Inference formula is a tautology");
                }
                System.out.println("[INFO] Inference formula is not a tautology");
                System.out.println("-".repeat(100));
            } catch (ValidationException e) {
                throw e;
            } catch (Exception e) {
                throw new ValidationException("Failed to validate inference instance", e);
            } finally {
                if (new File(testFile).exists()) {
                    try {
                        Files.delete(Paths.get(testFile));
                    } catch (IOException e) {
                        throw new ValidationException("Failed to validate inference instance", e);
                    }
                }
            }
        }
    }

    private void validateFormat(File instanceFile) {
        System.out.println("[INFO] Validate format of instance file '" + instanceFile.getPath() + "'");

        try (BufferedReader reader = new BufferedReader(new FileReader(instanceFile));) {
            String line = reader.readLine();
            while (line != null) {
                String prettyLine = line.replaceAll("\s+", " ").trim();
                if (!prettyLine.isBlank() && !prettyLine.startsWith("c")) {
                    Matcher matcher = BeliefChangeInstance.regex.get("clause").matcher(prettyLine);
                    if (!matcher.matches()) {
                        throw new WrongInstanceFormatException("Instance file contains invalid clause: " + line);
                    }
                    this.inferenceClauses.add(prettyLine);
                    String[] vars = prettyLine.split(" ");
                    for (int x = 0; x < vars.length - 1; x++) { // Skip last index since that is '0'
                        int number = Math.abs(Integer.parseInt(vars[x]));
                        if (number > this.varNum) {
                            this.varNum = number;
                        }
                    }
                }
                line = reader.readLine();
            }
            if (inferenceClauses.size() == 0) {
                throw new WrongInstanceFormatException("Instance file is empty");
            }
        } catch (FileNotFoundException e) {
            throw new WrongInputException("Provided instance file '" + instanceFile.getPath() + "' does not exist");
        } catch (IOException e) {
            throw new WrongInstanceFormatException("Provided instance in file '"+ instanceFile.getPath() + "' cannot be read");
        }

        System.out.println("[INFO] Instance file format is valid");
        System.out.println("-".repeat(100));
    }

    public int getVarNum() {
        return this.varNum;
    }

    public List<String> getInferenceClauses() {
        return this.inferenceClauses;
    }
}
