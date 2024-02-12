package de.julsched.beliefchange.naive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.julsched.beliefchange.exceptions.WrongEncodingFormatException;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;

public class NaiveCheck {

    private static final Pattern commentLineRegex = Pattern.compile("^#\\sBelief\\sbase\\svariables:\\s(?<varNum>[1-9][0-9]*)$");

    protected int encodingModelVarNum;
    protected List<String> models = new ArrayList<>();

    public NaiveCheck(File encodingFile) {
        validateEncodingFormat(encodingFile);
    }

    private void validateEncodingFormat(File encodingFile) {
        System.out.println("[INFO] Validate format of encoding file '" + encodingFile.getPath() + "'");
        int varNumMax = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(encodingFile));) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.isBlank()) {
                    String prettyLine = line.replaceAll("\s+", " ").trim();
                    if (!prettyLine.startsWith("#")) {
                        String[] vars = prettyLine.split(" ");
                        for (String var : vars) {
                            int varInt = Math.abs(Integer.parseInt(var));
                            if (varInt > varNumMax) {
                                varNumMax = varInt;
                            }
                        }
                        this.models.add(prettyLine);
                    }
                    Matcher matcher = commentLineRegex.matcher(prettyLine);
                    if (matcher.matches()) {
                        this.encodingModelVarNum = Integer.parseInt(matcher.group("varNum"));
                    }
                }
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new WrongInputException("Provided encoding file '" + encodingFile.getPath() + "' does not exist");
        } catch (IOException e) {
            throw new WrongInstanceFormatException("Provided encoding in file '"+ encodingFile.getPath() + "' cannot be read");
        }

        if (this.models.size() == 0) {
            throw new WrongEncodingFormatException("Encoding file is empty");
        }
        if (this.encodingModelVarNum == 0) {
            throw new WrongEncodingFormatException("Encoding file does not contain information on belief base variables");
        }
        if (varNumMax > this.encodingModelVarNum) {
            throw new WrongEncodingFormatException("Encoding file contains models with unknown variables");
        }
        System.out.println("[INFO] Encoding file format is valid");
        System.out.println("-".repeat(100));
    }
}
