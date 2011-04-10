package org.sade.analyzers;

public class MinimizeResult {
    private final double error;
    private final MinimizeParameters parameters;

    public MinimizeResult(double error, MinimizeParameters parameters) {
        this.error = error;
        this.parameters = parameters;
    }

    public double getError() {
        return error;
    }

    public MinimizeParameters getParameters() {
        return parameters;
    }
}
