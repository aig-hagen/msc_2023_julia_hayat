package de.julsched.beliefchange.ilp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.instance.InferenceCheckInstance;
import de.julsched.beliefchange.utils.Glpsol;
import de.julsched.beliefchange.utils.Utils;

public class IlpInferenceCheck extends IlpCheck {

    private static final String inferenceEncodingFileName = Application.dirInterimResults + "/ilp-inference-check.mod";
    private static final String inferenceEncodingResultFileName = Application.dirInterimResults + "/ilp-inference-check-result";

    private InferenceCheckInstance instance;

    public IlpInferenceCheck(File encodingFile, boolean properValidation, InferenceCheckInstance instance) {
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
        inferenceEncodingClauses.addAll(this.varDeclarationEncoding);
        for (int a = 1; a <= this.instance.getInferenceClauses().size(); a++) {
            inferenceEncodingClauses.add(String.format(Encoding.varDeclarationTemplate, "ai" + a));
        }

        inferenceEncodingClauses.addAll(this.nonVarDeclarationEncoding);
inferenceEncodingClauses.add("");

        int constraintCounter = 0;
        for (String clause : instance.getInferenceClauses()) {
            constraintCounter++;
            StringBuilder constraint = new StringBuilder("s.t. inferenceConstraint")
                                        .append(constraintCounter)
                                        .append(":\n\t")
                                        .append("(");
            String[] vars = clause.split(" ");
            for (int i = 0; i < vars.length - 1; i++) { // Skip last index (since that is '0')
                if (i != 0) {
                    constraint.append(" + ");
                }
                if (vars[i].startsWith("-")) {
                    constraint.append("(1 - x");
                    constraint.append(vars[i].split("-")[1]);
                    constraint.append(")");
                } else {
                    constraint.append("x");
                    constraint.append(vars[i]);
                }
            }
            constraint.append(") / ")
                      .append(vars.length - 1)
                      .append(" <= ai")
                      .append(constraintCounter)
                      .append(";");
            inferenceEncodingClauses.add(constraint.toString());
            inferenceEncodingClauses.add("");
        }
        // Add constraint 'sum(auxiliaryVariables) < <number of clauses>'
        constraintCounter++;
        StringBuilder constraint = new StringBuilder("s.t. inferenceConstraint")
                                    .append(constraintCounter)
                                    .append(":\n\t");
        for (int a = 1; a <= this.instance.getInferenceClauses().size(); a++) {
            if (a > 1) {
                constraint.append(" + ");
            }
            constraint.append("ai")
                      .append(a);
        }
        constraint.append(" <= ")
                  .append(this.instance.getInferenceClauses().size() - 1)
                  .append(";");
        inferenceEncodingClauses.add(constraint.toString());
        inferenceEncodingClauses.add("");
        inferenceEncodingClauses.add("end;");
        inferenceEncodingClauses.add("");

        Utils.writeToFile(StringUtils.join(inferenceEncodingClauses, "\n"), inferenceEncodingFileName);
        System.out.println("[INFO] Start solver call");
        Application.solverCallsStartTime = System.currentTimeMillis();
        Glpsol.executeSolver(inferenceEncodingFileName, inferenceEncodingResultFileName);
        Application.solverCallsEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished solver call");

        if (Glpsol.containsSolution(inferenceEncodingResultFileName)) {
            System.out.println("[INFO] Inference result: FALSE");
        } else {
            System.out.println("[INFO] Inference result: TRUE");
        }

        Application.inferenceCheckEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished inference check");
    }
}
