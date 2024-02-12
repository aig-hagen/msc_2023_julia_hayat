package de.julsched.beliefchange.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import de.julsched.beliefchange.exceptions.MinDistanceException;

public class MaxHS {

    public static Process execute(String encodingFileName, boolean printSolution) throws IOException, InterruptedException {
        String[] command;
        if (printSolution) {
            command = new String[]{"maxhs", "-printSoln", encodingFileName};
        } else {
            command = new String[]{"maxhs", encodingFileName};
        }
        Runtime run  = Runtime.getRuntime();
        Process process = run.exec(command);
        process.waitFor();
        return process;
    }

    public static String readOptimumLine(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        String optimum = "";

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("o", 0)) {
                optimum = line.split(" ")[1];
                break;
            }
        }
        reader.close();

        if (optimum.isEmpty()) {
            throw new MinDistanceException("Belief base or belief change formula might be unsatisfiable / change formula might be a tautology");
        }
        return optimum;
    }

    public static String readOptimumSolutionModel(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        String solutionLine = "";

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("v", 0)) {
                solutionLine = line.split("v ")[1];
                break;
            }
        }
        reader.close();

        if (solutionLine.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        // MaxHS returns a solution line consisting of 0 and 1 only
        String[] values = solutionLine.split("");
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals("0")) {
                result.append("-")
                      .append(i + 1)
                      .append(" ");
            } else if (values[i].equals("1")) {
                result.append(i + 1)
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    public static boolean isSatisfiable(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        boolean solutionFound = false;
        boolean noFalsifiedSofts = false;
        while ((line = reader.readLine()) != null) {
            if (line.equals("s OPTIMUM FOUND")) {
                solutionFound = true;
            }
            if (line.equals("c Solved: Number of falsified softs = 0")) {
                noFalsifiedSofts = true;
            }
        }
        reader.close();

        if (solutionFound && noFalsifiedSofts) {
            return true;
        } else {
            return false;
        }
    }

    public static int[] readSolutionModel(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        boolean solutionFound = false;
        boolean noFalsifiedSofts = false;
        String solutionLine = "";

        while ((line = reader.readLine()) != null) {
            if (line.equals("s OPTIMUM FOUND")) {
                solutionFound = true;
            }
            if (line.equals("c Solved: Number of falsified softs = 0")) {
                noFalsifiedSofts = true;
            }
            if (line.startsWith("v", 0)) {
                solutionLine = line.split("v ")[1];
            }
        }
        reader.close();

        if (!(solutionFound && noFalsifiedSofts)) {
            return null;
        }

        // MaxHS returns a solution line consisting of 0 and 1 only
        String[] values = solutionLine.split("");
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals("0")) {
                result[i] = (-1) * (i + 1);
            } else if (values[i].equals("1")) {
                result[i] = i + 1;
            }
        }
        return result;
    }
}
