package de.julsched.beliefchange.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.julsched.beliefchange.exceptions.MinDistanceException;

public class Clingo {

    public static Process execute(String encodingFileName, boolean returnAllModels) throws IOException, InterruptedException {
        String[] command;
        if (returnAllModels) {
            command = new String[]{"clingo", "--models", "0", encodingFileName};
        } else {
            command = new String[]{"clingo", encodingFileName};
        }
        Runtime run = Runtime.getRuntime();
        Process process = run.exec(command);
        process.waitFor();
        return process;
    }

    public static boolean hasSolution(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals("SATISFIABLE")) {
                reader.close();
                return true;
            }
        }
        reader.close();
        return false;
    }

    public static String readOptimumLine(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        String optimum = "";
        boolean optimumFound = false;
        while ((line = reader.readLine()) != null) {
            if (line.equals("  Optimum    : yes")) {
                optimumFound = true;
            }
            if (line.startsWith("Optimization :", 0)) {
                optimum = line.split(" ")[2];
                break;
            }
        }
        reader.close();

        if (!optimumFound || optimum.isEmpty()) {
            throw new MinDistanceException(
                "Belief base or belief change formula might be unsatisfiable  / change formula might be a tautology"
            );
        }
        return optimum;
    }

    public static String readOptimumResult(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        List<String> lines = new ArrayList<>();
        boolean optimumFound = false;
        int index = -1;
        int optimizationLineIndex = -1;
        while ((line = reader.readLine()) != null) {
            index++;
            if (line.equals("  Optimum    : yes")) {
                optimumFound = true;
            }
            if (line.startsWith("Optimization:", 0)) {
                optimizationLineIndex = index;
            }
            lines.add(line);
        }
        reader.close();

        if (!optimumFound) {
            return null;
        }
        String resultLine = lines.get(optimizationLineIndex - 1);
        return resultLine;
    }
}
