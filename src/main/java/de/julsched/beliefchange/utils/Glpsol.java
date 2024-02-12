package de.julsched.beliefchange.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.julsched.beliefchange.exceptions.MinDistanceException;

public class Glpsol {

    public static void executeSolver(String modelFileName, String resultFileName) throws IOException, InterruptedException {
        String[] command = new String[]{"glpsol", "-m", modelFileName, "-w", resultFileName};
        Runtime run = Runtime.getRuntime();
        Process process = run.exec(command);
        process.waitFor(); // Wait for process to write output to file and finish
    }

    public static boolean containsSolution(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        boolean solutionFound = false;
        String line = reader.readLine();
        while (line != null) {
            if (line.equals("c Status:     INTEGER OPTIMAL")) {
                solutionFound = true;
            } else if (line.equals("c Status:     INTEGER EMPTY")) {
                solutionFound = false;
                break;
            }
            line = reader.readLine();
        }
        reader.close();
        return solutionFound;
    }

    public static String readOptimumLine(String resultFileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(resultFileName));
        String line = reader.readLine();
        String optimum = "";
        boolean optimumFound = false;

        while (line != null) {
            if (line.equals("c Status:     INTEGER OPTIMAL")) {
                optimumFound = true;
            } else if (line.equals("c Status:     INTEGER EMPTY")) {
                optimumFound = false;
                break;
            }
            if (line.startsWith("c Objective:  distance = ")) {
                optimum = line.split("c Objective:  distance = ")[1].split(" ")[0];
            }
            if (optimumFound && !optimum.isEmpty()) {
                break;
            }
            line = reader.readLine();
        }
        reader.close();

        if (!optimumFound || optimum.isEmpty()) {
            throw new MinDistanceException("Belief base or belief change formula might be unsatisfiable / change formula might be a tautology");
        }
        return optimum;
    }

    public static List<Integer> readMinimalSetSatoh(String resultFileName,
                                                    List<Integer> discrepancyVarsPositions,
                                                    List<Integer> discrepancyVars2Positions) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(resultFileName));
        String line = reader.readLine();
        String optimum = "";
        boolean optimumFound = false;
        List<Integer> model = new ArrayList<>();

        int discrepancyVar = 0;
        boolean isEmptySet = true;
        while (line != null) {
            if (line.equals("c Status:     INTEGER OPTIMAL")) {
                optimumFound = true;
            } else if (line.equals("c Status:     INTEGER EMPTY")) {
                optimumFound = false;
                break;
            }
            if (line.startsWith("c Objective:  distance = ")) {
                optimum = line.split("c Objective:  distance = ")[1].split(" ")[0];
            }
            if (line.startsWith("j ")) {
                int var = Integer.parseInt(line.split(" ")[1]);
                if (discrepancyVars2Positions.contains(var)) {
                    discrepancyVar++;
                    int value = Integer.parseInt(line.split(" ")[2]);
                    if (value == 1) {
                        isEmptySet = false;
                        model.add(discrepancyVar);
                    }
                }
            }
            line = reader.readLine();
        }
        reader.close();

        if (isEmptySet && optimumFound && !optimum.isEmpty()) {
            model = new ArrayList<>();
            model.add(0);
        }
        if (!optimumFound || optimum.isEmpty() || model.size() == 0) {
            return null;
        }
        return model;
    }
}
