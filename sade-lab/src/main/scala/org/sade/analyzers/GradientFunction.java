package org.sade.analyzers;

public interface GradientFunction
{
    double[] Gradient(double[] at);

    double Value(double[] at);
}
