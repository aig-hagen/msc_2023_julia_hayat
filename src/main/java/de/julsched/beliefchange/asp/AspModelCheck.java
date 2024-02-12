package de.julsched.beliefchange.asp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.instance.ModelCheckInstance;
import de.julsched.beliefchange.utils.Clingo;
import de.julsched.beliefchange.utils.Utils;

public class AspModelCheck extends AspCheck {

    private static final String modelEncodingFileName = Application.dirInterimResults + "/asp-model-check.lp";

    private ModelCheckInstance instance;

    public AspModelCheck(File encodingFile, boolean properValidation, ModelCheckInstance instance) {
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
        modelEncodingClauses.addAll(this.encodingLines);
        modelEncodingClauses.add("");

        String[] modelVars = instance.getModel().split(" ");
        StringBuilder clause;
        for (int i = 0; i <= modelVars.length - 1; i++) {
            String var = modelVars[i];
            if (var.startsWith("-")) {
                clause = new StringBuilder("not t(")
                            .append(var.split("-")[1])
                            .append(").");
            } else {
                clause = new StringBuilder("t(")
                            .append(var)
                            .append(").");
            }
            modelEncodingClauses.add(clause.toString());
        }
        modelEncodingClauses.add("");

        Utils.writeToFile(StringUtils.join(modelEncodingClauses, "\n"), modelEncodingFileName);
        System.out.println("[INFO] Start solver call");
        Application.solverCallsStartTime = System.currentTimeMillis();
        Process process = Clingo.execute(modelEncodingFileName, false);
        Application.solverCallsEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished solver call");
        if (Clingo.hasSolution(process)) {
            System.out.println("[INFO] Model result: TRUE");
        } else {
            System.out.println("[INFO] Model result: FALSE");
        }

        Application.modelCheckEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished model check");
    }
}
