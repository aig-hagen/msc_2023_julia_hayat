package de.julsched.beliefchange.ilp;

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
import java.util.regex.Pattern;

import de.julsched.beliefchange.exceptions.ValidationException;
import de.julsched.beliefchange.exceptions.WrongEncodingFormatException;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.utils.Glpsol;

public class IlpCheck {

    private static final Pattern commentLineRegex = Pattern.compile("^#\\sBelief\\sbase\\svariables:\\s(?<varNum>[1-9][0-9]*)$");
    private static final Pattern varDeclarationLineRegex = Pattern.compile("^var\\s(?<variable>[a-z][a-z]*[1-9][0-9]*)\\sbinary;$");
    private static final Pattern endLineRegex = Pattern.compile("^end;$");

    protected int encodingModelVarNum;
    protected List<String> varDeclarationEncoding = new ArrayList<>();
    protected List<String> nonVarDeclarationEncoding = new ArrayList<>();

    public IlpCheck(File encodingFile, boolean properValidation) {
        validateEncodingFormat(encodingFile);
        if (properValidation) {
            String resultFile = "satisfiability-test-result";
            try {
                Glpsol.executeSolver(encodingFile.getAbsolutePath(), resultFile);
                // Encoding contains syntax error
                if (!new File(resultFile).exists()) {
                    throw new ValidationException("Encoding contains syntax error");
                }
                if (!Glpsol.containsSolution(resultFile)) {
                    throw new ValidationException("Encoding is unsatisfiable");
                }
                System.out.println("[INFO] Encoding is satisfiable");
                System.out.println("-".repeat(100));
            } catch (ValidationException e) {
                throw e;
            } catch (Exception e) {
                throw new ValidationException("Failed to validate encoding", e);
            } finally {
                if (new File(resultFile).exists()) {
                    try {
                        Files.delete(Paths.get(resultFile));
                    } catch (IOException e) {
                        throw new ValidationException("Failed to validate encoding", e);
                    }
                }
            }
        }
    }

    private void validateEncodingFormat(File encodingFile) {
        System.out.println("[INFO] Validate format of encoding file '" + encodingFile.getPath() + "'");
        try (BufferedReader reader = new BufferedReader(new FileReader(encodingFile));) {
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("#")) {
                    Matcher matcher = commentLineRegex.matcher(line);
                    if (matcher.matches()) {
                        this.encodingModelVarNum = Integer.parseInt(matcher.group("varNum"));
                    }
                    line = reader.readLine();
                    continue;
                }
                if (endLineRegex.matcher(line).matches()) {
                    break;
                }
                Matcher matcher = varDeclarationLineRegex.matcher(line);
                if (matcher.matches()) {
                    this.varDeclarationEncoding.add(line);
                    line = reader.readLine();
                    continue;
                }
                this.nonVarDeclarationEncoding.add(line);
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new WrongInputException("Provided encoding file '" + encodingFile.getPath() + "' does not exist");
        } catch (IOException e) {
            throw new WrongInstanceFormatException("Provided encoding in file '"+ encodingFile.getPath() + "' cannot be read");
        }

        if (this.varDeclarationEncoding.size() == 0 && this.nonVarDeclarationEncoding.size() == 0) {
            throw new WrongEncodingFormatException("Encoding file is empty");
        }
        if (this.encodingModelVarNum == 0) {
            throw new WrongEncodingFormatException("Encoding file does not contain information on belief base variables");
        }

        System.out.println("[INFO] Encoding file format is valid");
        System.out.println("-".repeat(100));
    }
}
