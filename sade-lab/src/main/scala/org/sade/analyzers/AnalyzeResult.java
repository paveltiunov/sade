package org.sade.analyzers;

public class AnalyzeResult
{
    private final MinimizeResult result;
    private final double realPeriod;
    private final double amplitude;
    private final double center;

    public AnalyzeResult(MinimizeResult minimizeResult, double realPeriod, double amplitude, double center)
    {
        this.result = minimizeResult;
        this.realPeriod = realPeriod;
        this.amplitude = amplitude;
        this.center = center;
    }

    public MinimizeParameters getParameters() {
        return result.getParameters();
    }

    public double getMinimizeError() {
        return result.getError();
    }

    public int getPeriod() {
        return (int) Math.round(realPeriod);
    }

    public double getRealPeriod() {
        return realPeriod;
    }

    public double getAmplitude() {
        return amplitude;
    }

    public double getCenter() {
        return center;
    }

    @Override
    public String toString() {
        return "AnalyzeResult{" +
                "result=" + result +
                ", realPeriod=" + realPeriod +
                ", amplitude=" + amplitude +
                ", center=" + center +
                '}';
    }
}
