package de.julsched.beliefchange.naive;

import java.io.File;
import java.io.IOException;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.instance.InferenceCheckInstance;
import de.julsched.beliefchange.utils.CaDiCal;
import de.julsched.beliefchange.utils.Utils;

public class NaiveInferenceCheck extends NaiveCheck {

    private static final String inferenceCheckFileName = Application.dirInterimResults + "/naive-inference-check.cnf";

    private InferenceCheckInstance instance;

    public NaiveInferenceCheck(File encodingFile, InferenceCheckInstance instance) {
        super(encodingFile);
        this.instance = instance;
        if (this.encodingModelVarNum < this.instance.getVarNum()) {
            throw new WrongInstanceFormatException("Instance file does not contain a valid inference formula: variables not consistent with provided encoding");
        }
    }

    public void execute() throws IOException, InterruptedException {
        String paramLine = "p cnf " + this.encodingModelVarNum + " " + (this.encodingModelVarNum + this.instance.getInferenceClauses().size());
        // Check if inference formula holds for all models
        for (String model : this.models) {
            StringBuilder encoding = new StringBuilder(paramLine)
                                        .append("\n");
            String[] variables = model.split(" ");
            for (int i = 0; i < variables.length; i++) {
                encoding.append(variables[i])
                        .append(" 0\n");
            }
            for (String clause : this.instance.getInferenceClauses()) {
                encoding.append(clause)
                        .append("\n");
            }

            Utils.writeToFile(encoding.toString(), inferenceCheckFileName);
            Process process = CaDiCal.execute(inferenceCheckFileName);
            if (!CaDiCal.isSatisfiable(process)) {
                System.out.println("[INFO] Inference result: FALSE");
                return;
            }
        }
        System.out.println("[INFO] Inference result: TRUE");
    }
}
