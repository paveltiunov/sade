package org.sade.analyzers;

public class MinimizeParameters {
    private final double omega;
    private final double delta;
    private final double phi;

    public MinimizeParameters(double omega, double delta, double phi) {
        this.omega = omega;
        this.delta = delta;
        this.phi = phi;
    }

    public double[] Wrap() {
        return new double[]{omega, delta, phi};
    }

    public double getOmega() {
        return omega;
    }

    public double getDelta() {
        return delta;
    }

    public double getPhi() {
        return phi;
    }

    @Override
    public String toString() {
        return "omega: " + omega + ", delta: " + delta + ", phi: " + phi;
    }
}
