package org.sade.analyzers;

public class AnalyzeResult
{
    private final MinimizeParameters parameters;
    private final int period;

    public AnalyzeResult(MinimizeParameters parameters, int period)
    {
        this.parameters = parameters;
        this.period = period;
    }

    public MinimizeParameters getParameters() {
        return parameters;
    }

    public int getPeriod() {
        return period;
    }
}
