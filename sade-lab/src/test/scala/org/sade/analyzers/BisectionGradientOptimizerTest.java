package org.sade.analyzers;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class BisectionGradientOptimizerTest {
    @Test
    public void Bisection() {
        AssertOptimizeIsCorrect(new BisectionGradientOptimizer(1E-5, new StubSinGradient(), new double[]{Math.PI * 1.5}));
    }

    private void AssertOptimizeIsCorrect(GradientOptimizer optimizer) {
        double[] result = optimizer.Optimize(new double[]{0});
        Assert.assertThat(result[0], Matchers.closeTo(-Math.PI / 2, 1E-4));
    }

    public class StubSinGradient implements GradientFunction {
        public double[] Gradient(double[] at) {
            return new double[]{Math.cos(at[0])};
        }

        public double Value(double[] at) {
            return Math.sin(at[0]);
        }
    }
}