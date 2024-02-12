package de.julsched.beliefchange.asp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.instance.InferenceCheckInstance;
import de.julsched.beliefchange.utils.Clingo;
import de.julsched.beliefchange.utils.Utils;

public class AspInferenceCheck extends AspCheck {

    private static final String inferenceEncodingFileName = Application.dirInterimResults + "/asp-inference-check.lp";

    private InferenceCheckInstance instance;

    public AspInferenceCheck(File encodingFile, boolean properValidation, InferenceCheckInstance instance) {
        super(encodingFile, properValidation);
        this.instance = instance;
        if (this.encodingModelVarNum < this.instance.getVarNum()) {
            throw new WrongInstanceFormatException("Instance file does not contain a valid inference formula: variables not consistent with provided encoding");
        }
    }

    public void execute() throws IOException, InterruptedException {
        System.out.println("[INFO] Start inference check");
        Application.inferenceCheckStartTime = System.currentTimeMillis();

        List<String> inferenceEncodingClauses = new ArrayList<String>();
        inferenceEncodingClauses.addAll(this.encodingLines);
        inferenceEncodingClauses.add("");

        int counter = 0;
        StringBuilder aspClause = new StringBuilder(":- ");
        for (String clause : instance.getInferenceClauses()) {
            counter++;
            if (counter > 1) {
                aspClause.append(", ");
            }
            aspClause.append("1 {");
            String[] vars = clause.split(" ");
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                if (i > 0) {
                    aspClause.append("; ");
                }
                String var = vars[i];
                if (var.startsWith("-")) {
                    var = var.replace("-", "");
                    aspClause.append("not t(")
                             .append(var)
                             .append(")");
                } else {
                    aspClause.append("t(")
                             .append(var)
                             .append(")");
                }
            }
            aspClause.append("}");
        }
        aspClause.append(".");
        inferenceEncodingClauses.add(aspClause.toString());
        inferenceEncodingClauses.add("");

        Utils.writeToFile(StringUtils.join(inferenceEncodingClauses, "\n"), inferenceEncodingFileName);
        System.out.println("[INFO] Start solver call");
        Application.solverCallsStartTime = System.currentTimeMillis();
        Process process = Clingo.execute(inferenceEncodingFileName, false);
        Application.solverCallsEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished solver call");
        if (Clingo.hasSolution(process)) {
            System.out.println("[INFO] Inference result: FALSE");
        } else {
            System.out.println("[INFO] Inference result: TRUE");
        }

        Application.inferenceCheckEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished inference check");
    }
}
