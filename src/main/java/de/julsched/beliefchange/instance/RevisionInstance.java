package de.julsched.beliefchange.instance;

import java.io.File;

import de.julsched.beliefchange.values.Operation;

public class RevisionInstance extends BeliefChangeInstance {

    public RevisionInstance(File file, boolean validateInstance) {
        super(file, validateInstance, Operation.REVISION);
    }
}
