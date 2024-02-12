package de.julsched.beliefchange.asp;

import java.io.File;

import de.julsched.beliefchange.BeliefChangeCompiler;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.instance.ContractionInstance;
import de.julsched.beliefchange.instance.RevisionInstance;
import de.julsched.beliefchange.values.Algorithm;
import de.julsched.beliefchange.values.Distance;
import de.julsched.beliefchange.values.Operation;

public class CompilerAsp extends BeliefChangeCompiler {

    private Encoding encoding;

    public CompilerAsp(File instanceFile, boolean validateInstance, Operation operation, Distance distance, Algorithm preCompilationAlgorithm) {
        super(preCompilationAlgorithm, distance);
        switch (operation) {
            case CONTRACTION:
                this.instance = new ContractionInstance(instanceFile, validateInstance);
                break;
            case REVISION:
                this.instance = new RevisionInstance(instanceFile, validateInstance);
                break;
            default:
                throw new WrongInputException("Invalid operation '" + operation + "'");
        }
    }

    @Override
    public void createEncoding() {
        this.encoding = new Encoding(this.instance, this.optimumFinder, this.distance);
        this.encoding.create();
    }
}
