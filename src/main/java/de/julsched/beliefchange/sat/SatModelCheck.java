package de.julsched.beliefchange.sat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.julsched.beliefchange.Application;
import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.instance.ModelCheckInstance;
import de.julsched.beliefchange.utils.CaDiCal;
import de.julsched.beliefchange.utils.Utils;

public class SatModelCheck extends SatCheck {

    private static final String modelEncodingFileName = Application.dirInterimResults + "/sat-model-check.cnf";

    private ModelCheckInstance instance;

    public SatModelCheck(File encodingFile, boolean properValidation, ModelCheckInstance instance) {
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
        int newClauseNum = this.encodingClauseNum;
        String[] modelVars = instance.getModel().split(" ");
        StringBuilder clause;
        for (int i = 0; i <= modelVars.length - 1; i++) {
            newClauseNum++;
            clause = new StringBuilder(modelVars[i])
                        .append(" 0");
            modelEncodingClauses.add(clause.toString());
        }

        modelEncodingClauses.add(0, "p cnf " + this.encodingVarNum + " " + newClauseNum);
        for (int i = 1; i < this.encodingLines.size(); i++) {
            modelEncodingClauses.add(this.encodingLines.get(i));
        }
        modelEncodingClauses.add("");

        Utils.writeToFile(StringUtils.join(modelEncodingClauses, "\n"), modelEncodingFileName);
        System.out.println("[INFO] Start solver call");
        Application.solverCallsStartTime = System.currentTimeMillis();
        Process process = CaDiCal.execute(modelEncodingFileName);
        Application.solverCallsEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished solver call");

        if (CaDiCal.isSatisfiable(process)) {
            System.out.println("[INFO] Model result: TRUE");
        } else {
            System.out.println("[INFO] Model result: FALSE");
        }

        Application.modelCheckEndTime = System.currentTimeMillis();
        System.out.println("[INFO] Finished model check");
    }
}
