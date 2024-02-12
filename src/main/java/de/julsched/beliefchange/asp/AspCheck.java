package de.julsched.beliefchange.asp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.julsched.beliefchange.exceptions.ValidationException;
import de.julsched.beliefchange.exceptions.WrongEncodingFormatException;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.utils.Clingo;

public class AspCheck {

    private static final Pattern commentLineRegex = Pattern.compile("^%\\sBelief\\sbase\\svariables:\\s(?<varNum>[1-9][0-9]*)$");
    private static final Pattern varDeclarationLineRegex = Pattern.compile("^\\{t\\(1\\.\\.(?<varNum>[1-9][0-9]*)\\)\\}\\.$");

    protected List<String> encodingLines = new ArrayList<>();
    protected int encodingVarNum;
    protected int encodingModelVarNum;

    public AspCheck(File encodingFile, boolean properValidation) {
        validateEncodingFormat(encodingFile);
        if (properValidation) {
            try {
                Process process = Clingo.execute(encodingFile.getAbsolutePath(), false);
                if (!Clingo.hasSolution(process)) {
                    throw new ValidationException("Encoding is unsatisfiable or contains syntax error");
                }
                System.out.println("[INFO] Encoding is satisfiable");
                System.out.println("-".repeat(100));
            } catch (ValidationException e) {
                throw e;
            } catch (Exception e) {
                throw new ValidationException("Failed to validate encoding", e);
            }
        }
    }

    private void validateEncodingFormat(File encodingFile) {
        System.out.println("[INFO] Validate format of encoding file '" + encodingFile.getPath() + "'");
        try (BufferedReader reader = new BufferedReader(new FileReader(encodingFile));) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.isBlank()) {
                    String prettyLine = line.replaceAll("\s+", " ").trim();
                    if (prettyLine.startsWith("%")) {
                        Matcher matcher = commentLineRegex.matcher(prettyLine);
                        if (matcher.matches()) {
                            this.encodingModelVarNum = Integer.parseInt(matcher.group("varNum"));
                        }
                        line = reader.readLine();
                        continue;
                    }
                    this.encodingLines.add(prettyLine);
                    Matcher matcher = varDeclarationLineRegex.matcher(prettyLine);
                    if (matcher.matches()) {
                        this.encodingVarNum = Integer.parseInt(matcher.group("varNum"));
                    }
                }
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new WrongInputException("Provided encoding file '" + encodingFile.getPath() + "' does not exist");
        } catch (IOException e) {
            throw new WrongInstanceFormatException("Provided encoding in file '"+ encodingFile.getPath() + "' cannot be read");
        }

        if (this.encodingLines.size() == 0) {
            throw new WrongEncodingFormatException("Encoding file is empty");
        }
        if (this.encodingModelVarNum == 0) {
            throw new WrongEncodingFormatException("Encoding file does not contain information on belief base variables");
        }
        if (this.encodingVarNum == 0) {
            throw new WrongEncodingFormatException("Encoding file is not valid");
        }

        System.out.println("[INFO] Encoding file format is valid");
        System.out.println("-".repeat(100));
    }
}
