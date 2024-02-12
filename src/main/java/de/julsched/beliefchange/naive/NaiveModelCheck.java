package de.julsched.beliefchange.naive;

import java.io.File;

import de.julsched.beliefchange.exceptions.WrongInstanceFormatException;
import de.julsched.beliefchange.instance.ModelCheckInstance;

public class NaiveModelCheck extends NaiveCheck {

    private ModelCheckInstance instance;

    public NaiveModelCheck(File encodingFile, ModelCheckInstance instance) {
        super(encodingFile);
        this.instance = instance;
        if (this.encodingModelVarNum != this.instance.getVarNum()) {
            throw new WrongInstanceFormatException("Instance file does not contain a valid model: variables not consistent with provided encoding");
        }
    }

    public void execute() {
        String modelToMatch = this.instance.getModel();
        for (String model : this.models) {
            if (model.equals(modelToMatch)) {
                System.out.println("[INFO] Model result: TRUE");
                return;
            }
        }
        System.out.println("[INFO] Model result: FALSE");
    }
}
