package de.julsched.beliefchange;

import de.julsched.beliefchange.instance.BeliefChangeInstance;
import de.julsched.beliefchange.values.Distance;

public abstract class OptimumFinder {

    protected Distance distance;

    public OptimumFinder(Distance distance) {
        this.distance = distance;
    }

    public abstract String getOptimum(BeliefChangeInstance instance);

    public abstract String getMinSetConstraints(BeliefChangeInstance instance);
}
