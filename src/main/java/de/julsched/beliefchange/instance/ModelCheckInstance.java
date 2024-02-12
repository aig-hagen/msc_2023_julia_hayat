package de.julsched.beliefchange.instance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;

public class ModelCheckInstance {

    private int varNum;
    private String model;

    public ModelCheckInstance(File instanceFile) {
        validateFormat(instanceFile);
    }

    private void validateFormat(File instanceFile) {
        System.out.println("[INFO] Validate format of instance file '" + instanceFile.getPath() + "'");

        try (BufferedReader reader = new BufferedReader(new FileReader(instanceFile));) {
            int properLines = 0;
            String line = reader.readLine();
            while (line != null) {
                String prettyString = line.replaceAll("\s+", " ").trim();
                if (!prettyString.isBlank() && !prettyString.startsWith("c")) {
                    properLines++;
                    this.model = prettyString;
                }
                line = reader.readLine();
            }
            if (properLines == 0) {
                throw new WrongInstanceFormatException("Instance file is empty");
            } else if (properLines > 1) {
                throw new WrongInstanceFormatException("Instance file must contain only one model");
            }
        } catch (FileNotFoundException e) {
            throw new WrongInputException("Provided instance file '" + instanceFile.getPath() + "' does not exist");
        } catch (IOException e) {
            throw new WrongInstanceFormatException("Provided instance in file '"+ instanceFile.getPath() + "' cannot be read");
        }

        String[] vars = this.model.split(" ");
        int var = 0;
        for (int x = 0; x <= vars.length - 1; x++) {
            var++;
            int number = Math.abs(Integer.parseInt(vars[x]));
            if (number != var) {
                throw new WrongInstanceFormatException("Instance file does not contain a valid model");
            }
        }
        this.varNum = var;

        System.out.println("[INFO] Instance file format is valid");
        System.out.println("-".repeat(100));
    }

    public int getVarNum() {
        return this.varNum;
    }

    public String getModel() {
        return this.model;
    }
}
