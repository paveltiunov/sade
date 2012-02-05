package org.sade.analyzers;

public class TestSample {
    public static double[] prepareSample(double amplitude, double phaseAmplitude, double omega, double phi,
                                         double delta, double shift, int pointNum) {
        double[] values = new double[pointNum];
        for (int i = 0; i < pointNum; i++) {
            double t = i;
            double value = amplitude * Math.cos(phaseAmplitude * Math.cos(omega * t + phi) + delta) + shift;
            values[i] = value;
        }
        return values;
    }
}
