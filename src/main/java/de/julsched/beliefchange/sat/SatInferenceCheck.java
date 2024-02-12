package de.julsched.beliefchange.sat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.instance.InferenceCheckInstance;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.utils.Tseitin;
import de.julsched.beliefchange.utils.CaDiCal;
import de.julsched.beliefchange.utils.Utils;

public class SatInferenceCheck extends SatCheck {

    private static final String inferenceEncodingFileName = Application.dirInterimResults + "/sat-inference-check.cnf";

    private InferenceCheckInstance instance;

    public SatInferenceCheck(File encodingFile, boolean properValidation, InferenceCheckInstance instance) {
        super(encodingFile, properValidation);
        this.instance = instance;
        if (this.encodingModelVarNum < this.instance.getVarNum()) {
            throw new WrongInstanceFormatException("Instance file does not contain a valid inference formula: variables not consistent with provided encoding");
        }
    }

    public void execute() throws IOException, InterruptedException {
        System.out.println("[INFO] Start inference check");
        Application.inferenceCheckStartTime = System.currentTimeMillis();

        String[] negatedClauses = Tseitin.negateCnfFormula(this.encodingVarNum, this.instance.getInferenceClauses());
        if (negatedClauses[0].equals("tautology")) {
            // Negated formula is a tautology -> inference formula is unsatisfiable -> cannot be inferred from belief base
            System.out.println("[INFO] Inference result: FALSE");
            Application.inferenceCheckEndTime = System.currentTimeMillis();
            System.out.println("[INFO] Finished inference check");
            return;
        }
        if (negatedClauses[0].equals("unsat")) {
            // Negated formula is unsatisfiable -> inference formula is a tautology -> can always be inferred from belief base
            System.out.println("[INFO] Inference result: TRUE");
            Application.inferenceCheckEndTime = System.currentTimeMillis();
            System.out.println("[INFO] Finished inference check");
            return;
        }
        String paramLineNegation = negatedClauses[0];
        String varNumNew = BeliefChangeInstance.extractVarNum(paramLineNegation);

        String paramLineEncoding = this.encodingLines.get(0);
        this.encodingLines.remove(0);
        String[] paramsEncoding = paramLineEncoding.split(" ");
        paramsEncoding[2] = varNumNew;
        paramsEncoding[3] = Integer.toString(Integer.parseInt(paramsEncoding[3]) + (negatedClauses.length - 1));
        StringBuilder paramLineNew = new StringBuilder();
        for (String param : paramsEncoding) {
            paramLineNew.append(param)
                        .append(" ");
        }
        this.encodingLines.add(0, paramLineNew.toString().trim());

        List<String> inferenceEncodingClauses = new ArrayList<String>();
        inferenceEncodingClauses.addAll(this.encodingLines);
        for (int i = 1; i < negatedClauses.length; i++) {
            inferenceEncodingClauses.add(negatedClauses[i]);
        }
        inferenceEncodingClauses.add("");

        Utils.writeToFile(StringUtils.join(inferenceEncodingClauses, "\n"), inferenceEncodingFileName);
        System.out.println("[INFO] Start solver call");
        Application.solverCallsStartTime = System.currentTimeMillis();
        Process process = CaDiCal.execute(inferenceEncodingFileName);
        Application.solverCallsEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished solver call");

        if (CaDiCal.isSatisfiable(process)) {
            System.out.println("[INFO] Inference result: FALSE");
        } else {
            System.out.println("[INFO] Inference result: TRUE");
        }
        Application.inferenceCheckEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished inference check");
    }
}
