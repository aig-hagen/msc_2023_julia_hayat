package de.julsched.beliefchange.ilp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.instance.ModelCheckInstance;
import de.julsched.beliefchange.utils.Glpsol;
import de.julsched.beliefchange.utils.Utils;

public class IlpModelCheck extends IlpCheck {

    private static final String modelFileNameModel = Application.dirInterimResults + "/ilp-model-check.mod";
    private static final String resultFileNameModel = Application.dirInterimResults + "/ilp-model-check-result";

    private ModelCheckInstance instance;

    public IlpModelCheck(File encodingFile, boolean properValidation, ModelCheckInstance instance) {
        super(encodingFile, properValidation);
        this.instance = instance;
        if (this.encodingModelVarNum != this.instance.getVarNum()) {
            throw new WrongInstanceFormatException("Instance file does not contain a valid model: variables not consistent with provided encoding");
        }
    }

    public void execute() throws IOException, InterruptedException {
        System.out.println("[INFO] Start model check");
        Application.modelCheckStartTime = System.currentTimeMillis();

        List<String> modelEncodingClauses = new ArrayList<String>();
        modelEncodingClauses.addAll(this.varDeclarationEncoding);
        modelEncodingClauses.addAll(this.nonVarDeclarationEncoding);
        modelEncodingClauses.add("");

        String[] modelVars = instance.getModel().split(" ");
        StringBuilder clause;
        for (int i = 0; i <= modelVars.length - 1; i++) {
            String var = modelVars[i];
            clause = new StringBuilder("s.t. modelConstraint")
                        .append(i + 1)
                        .append(":\n\tx");
            if (var.startsWith("-")) {
                clause.append(var.split("-")[1])
                      .append(" = 0;");
            } else {
                clause.append(var)
                      .append(" = 1;");
            }
            modelEncodingClauses.add(clause.toString());
            modelEncodingClauses.add("");
        }
        modelEncodingClauses.add("end;");
        modelEncodingClauses.add("");

        Utils.writeToFile(StringUtils.join(modelEncodingClauses, "\n"), modelFileNameModel);
        System.out.println("[INFO] Start solver call");
        Application.solverCallsStartTime = System.currentTimeMillis();
        Glpsol.executeSolver(modelFileNameModel, resultFileNameModel);
        Application.solverCallsEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished solver call");

        if (Glpsol.containsSolution(resultFileNameModel)) {
            System.out.println("[INFO] Model result: TRUE");
        } else {
            System.out.println("[INFO] Model result: FALSE");
        }

        Application.modelCheckEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished model check");
    }
}
