package de.julsched.beliefchange.sat;

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
import de.julsched.beliefchange.utils.CaDiCal;

public class SatCheck {

    private static final Pattern commentLineRegex = Pattern.compile("^c\\sBelief\\sbase\\svariables:\\s(?<varNum>[1-9][0-9]*)$");
    private static final Pattern paramLineRegex = Pattern.compile("^p\\scnf\\s(?<varNum>[1-9][0-9]*)\\s(?<clauseNum>[1-9][0-9]*)$");

    protected List<String> encodingLines = new ArrayList<>();
    protected int encodingVarNum;
    protected int encodingClauseNum;
    protected int encodingModelVarNum;

    public SatCheck(File encodingFile, boolean properValidation) {
        validateEncodingFormat(encodingFile);
        if (properValidation) {
            try {
                Process process = CaDiCal.execute(encodingFile.getAbsolutePath());
                if (!CaDiCal.isSatisfiable(process)) {
                    throw new ValidationException("Encoding is unsatisfiable");
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

    protected void validateEncodingFormat(File encodingFile) {
        System.out.println("[INFO] Validate format of encoding file '" + encodingFile.getPath() + "'");
        try (BufferedReader reader = new BufferedReader(new FileReader(encodingFile));) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.isBlank()) {
                    String prettyLine = line.replaceAll("\s+", " ").trim();
                    if (!prettyLine.startsWith("c")) {
                        this.encodingLines.add(prettyLine);
                    }
                    Matcher matcher = commentLineRegex.matcher(prettyLine);
                    if (matcher.matches()) {
                        this.encodingModelVarNum = Integer.parseInt(matcher.group("varNum"));
                    }
                    matcher = paramLineRegex.matcher(prettyLine);
                    if (matcher.matches()) {
                        this.encodingVarNum = Integer.parseInt(matcher.group("varNum"));
                        this.encodingClauseNum = Integer.parseInt(matcher.group("clauseNum"));
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
            throw new WrongEncodingFormatException("Encoding file does not contain parameter line");
        }

        System.out.println("[INFO] Encoding file format is valid");
        System.out.println("-".repeat(100));
    }
}
