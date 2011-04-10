package org.sade.analyzers;


import org.apache.commons.math.complex.Complex;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class JacobiAngerErrorFuncDiffEvaluatorTest {
    private JacobiAngerErrorFuncDiffEvaluator diffEvaluator = new JacobiAngerErrorFuncDiffEvaluator(new Complex[]{new Complex(1, 0)});

    @Test
    public void Gutter() {
        AssertGradient(new MinimizeParameters(1, 1, 1), new double[]{0.2630532730, 0.7570630777, 0.006855919472});
        AssertGradient(new MinimizeParameters(4, 3, 5), new double[]{0.07981104558, -0.5751954182e-1, 1.056511658});
    }

    @Test
    public void TwoMembers() {
        diffEvaluator = new JacobiAngerErrorFuncDiffEvaluator(new Complex[]{new Complex(0, 0), new Complex(0, 0)});
        AssertGradient(new MinimizeParameters(1, 1, 1), new double[]{0.1236440491, -0.2238161204, 0.04551849113});
    }

    @Test
    public void Zero() {
        diffEvaluator = new JacobiAngerErrorFuncDiffEvaluator(new Complex[]
                {
                        new Complex(0.4134377767, 0),
                        new Complex(-0.2000718412, -0.3115902879),
                        new Complex(0.02584987433, -0.5645182659e-1),
                        new Complex(-0.1602031601e-1, 0.002249148620)
                });
        AssertGradient(new MinimizeParameters(1, 1, 1), new double[]{0.0, 0.0, 0.0});
    }

    private void AssertGradient(MinimizeParameters minimizeParameters, double[] expected) {
        double[] diff = diffEvaluator.Gradient(minimizeParameters.Wrap());
        Assert.assertThat(diff[0], Matchers.closeTo(expected[0], 1E-5));
        Assert.assertThat(diff[1], Matchers.closeTo(expected[1], 1E-5));
        Assert.assertThat(diff[2], Matchers.closeTo(expected[2], 1E-5));
    }
}
