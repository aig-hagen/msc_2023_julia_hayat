package de.julsched.beliefchange.instance;

import java.io.File;

import de.julsched.beliefchange.values.Operation;

public class ContractionInstance extends BeliefChangeInstance {

    public ContractionInstance(File file, boolean validateInstance) {
        super(file, validateInstance, Operation.CONTRACTION);
    }
}
