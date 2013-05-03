package org.sade.analyzers;

public class ReScaleResult {
    public final double[] values;
    public final double amplitude;
    public final double center;

    ReScaleResult(double[] values, double amplitude, double center) {
        this.values = values;
        this.amplitude = amplitude;
        this.center = center;
    }
}
