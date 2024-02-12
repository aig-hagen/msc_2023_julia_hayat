package de.julsched.beliefchange;

import java.io.IOException;

import de.julsched.beliefchange.asp.Asp;
import de.julsched.beliefchange.exceptions.WrongInputException;
import de.julsched.beliefchange.ilp.Ilp;
import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.sat.MaxSat;
import de.julsched.beliefchange.values.Algorithm;
import de.julsched.beliefchange.values.Distance;

public abstract class BeliefChangeCompiler {

    protected Distance distance;
    protected BeliefChangeInstance instance;
    protected OptimumFinder optimumFinder;

    public BeliefChangeCompiler(Algorithm preCompilationAlgorithm, Distance distance) {
        switch (preCompilationAlgorithm) {
            case ASP:
                this.optimumFinder = new Asp(distance);
                break;
            case ILP:
                this.optimumFinder = new Ilp(distance);
                break;
            case MAXSAT:
                this.optimumFinder = new MaxSat(distance);
                break;
            default:
                throw new WrongInputException("Unsupported pre-compilation algorithm: '" + preCompilationAlgorithm + "'");
        }
        this.distance = distance;
    }

    public abstract void createEncoding() throws IOException;
}
