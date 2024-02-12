package de.julsched.beliefchange.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CaDiCal {

    public static Process execute(String encodingFileName) throws IOException, InterruptedException {
        String[] command = new String[]{"cadical", encodingFileName};

        Runtime run  = Runtime.getRuntime();
        Process process = run.exec(command);
        process.waitFor();
        return process;
    }

    public static boolean isSatisfiable(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        boolean solutionFound = false;
        while ((line = reader.readLine()) != null) {
            if (line.equals("s SATISFIABLE")) {
                solutionFound = true;
            }
        }
        reader.close();

        if (solutionFound) {
            return true;
        } else {
            return false;
        }
    }

    public static int[] readSolutionModel(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        boolean solutionFound = false;
        StringBuilder solutionLine = new StringBuilder();
        boolean firstLineDone = false;

        while ((line = reader.readLine()) != null) {
            if (line.equals("s SATISFIABLE")) {
                solutionFound = true;
            }
            if (line.startsWith("v", 0)) {
                if (firstLineDone) {
                    solutionLine.append(" ");
                }
                String lineContent = line.split("v ")[1].split(" 0")[0];
                if (!lineContent.equals("0")) { // Happens when last result line contains 'v 0' only
                    solutionLine.append(lineContent);
                }
                firstLineDone = true;
            }
        }
        reader.close();

        if (!(solutionFound)) {
            return null;
        }

        String[] stringResult = solutionLine.toString().split(" ");
        int[] result = new int[stringResult.length];
        for (int i = 0; i < stringResult.length; i++) {
            result[i] = Integer.parseInt(stringResult[i]);
        }
        return result;
    }
}
