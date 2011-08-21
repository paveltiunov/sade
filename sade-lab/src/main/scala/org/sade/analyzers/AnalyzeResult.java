package org.sade.analyzers;

public class AnalyzeResult
{
    private final MinimizeResult result;
    private final float realPeriod;

    public AnalyzeResult(MinimizeResult minimizeResult, float realPeriod)
    {
        this.result = minimizeResult;
        this.realPeriod = realPeriod;
    }

    public MinimizeParameters getParameters() {
        return result.getParameters();
    }

    public double getMinimizeError() {
        return result.getError();
    }

    public int getPeriod() {
        return Math.round(realPeriod);
    }

    public float getRealPeriod() {
        return realPeriod;
    }
}
