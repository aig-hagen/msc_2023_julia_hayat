package de.julsched.beliefchange.sat;

import java.io.File;
import java.io.IOException;

import de.julsched.beliefchange.BeliefChangeCompiler;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.instance.ContractionInstance;
import de.julsched.beliefchange.instance.RevisionInstance;
import de.julsched.beliefchange.values.Algorithm;
import de.julsched.beliefchange.values.Distance;
import de.julsched.beliefchange.values.Operation;

public class CompilerSat extends BeliefChangeCompiler {

    private Encoding encoding;

    public CompilerSat(File instanceFile, boolean validateInstance, Operation operation, Distance distance, Algorithm preCompilationAlgorithm) {
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
    public void createEncoding() throws IOException {
        if (this.instance instanceof ContractionInstance) {
            this.encoding = new ContractionEncoding(this.instance, this.optimumFinder, this.distance);
        } else if (this.instance instanceof RevisionInstance) {
            this.encoding = new RevisionEncoding(this.instance, this.optimumFinder, this.distance);
        }
        this.encoding.create();
    }
}
