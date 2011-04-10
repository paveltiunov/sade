package org.sade.analyzers;

import java.util.Arrays;

public class BisectionGradientOptimizer implements GradientOptimizer
{
    private final double precision;
    private final GradientFunction function;
    private final double[] searchRange;

    public BisectionGradientOptimizer(double precision, GradientFunction function, double[] searchRange)
    {
        this.precision = precision;
        this.function = function;
        this.searchRange = searchRange;
    }

    public double[] Optimize(double[] initial)
    {
        Division[] divisions = new Division[initial.length];
        for (int n = 0; n < 10; n++)
        {
            for (int i = 0; i < initial.length; i++)
            {
                divisions[i] = new Division(function, initial[i] - searchRange[i] / 2, initial[i] + searchRange[i] / 2, i, null, null);
            }
            for (int i = 0; i < initial.length; i++)
            {
                for (int c = 0; c < 16; c++)
                {
                    double[] centerGradient = function.Gradient(initial);
                    if (HasConverged(centerGradient)) return initial;
                    if (divisions[i].HasConverged(precision)) break;
                    Division division = divisions[i].GetNext(initial, centerGradient);
                    if (division == null) break;
                    divisions[i] = division;
                    initial[i] = divisions[i].Center();
//                        Trace.WriteLine("point " + n + ", " + i + ", " + c + ": " + initial.Aggregate("", (s, d) => s + d + "; "));
                }
            }
            for (Division division : divisions) {
                if(!division.HasConverged(precision)) continue;
            }
            break;
        }
        return initial;
    }

    private boolean HasConverged(double[] centerGradient)
    {
        double sum = 0.0;
        for (int i = 0; i < centerGradient.length; i++)
        {
            sum += centerGradient[i]*centerGradient[i];
        }
        return Math.sqrt(sum) < precision;
    }

    private class Division
    {
        private final GradientFunction function;
        private final double left;
        private final double right;
        private final int coordIndex;
        private double[] leftGradient;
        private double[] rightGradient;

        public Division(GradientFunction function, double left, double right, int coordIndex, double[] leftGradient, double[] rightGradient)
        {
            this.function = function;
            this.left = left;
            this.right = right;
            this.coordIndex = coordIndex;
            this.leftGradient = leftGradient;
            this.rightGradient = rightGradient;
        }

        public Division GetNext(double[] centerPoint, double[] centerGradient)
        {
            double[] leftPoint = Arrays.copyOf(centerPoint, centerPoint.length);
            leftPoint[coordIndex] = left;
            double[] rightPoint = Arrays.copyOf(centerPoint, centerPoint.length);
            rightPoint[coordIndex] = right;
            if (leftGradient == null)
            {
                leftGradient = function.Gradient(leftPoint);
            }
            if (rightGradient == null)
            {
                rightGradient = function.Gradient(rightPoint);
            }
            if (leftGradient[coordIndex] * centerGradient[coordIndex] < 0)
            {
                return new Division(function, left, centerPoint[coordIndex], coordIndex, leftGradient, centerGradient);
            }
            if (rightGradient[coordIndex] * centerGradient[coordIndex] < 0)
            {
                return new Division(function, centerPoint[coordIndex], right, coordIndex, centerGradient, rightGradient);
            }
            return null; //FIXME: if i can't recognize what bisection i should take is it means only others should advice?
        }

        public double Center()
        {
            return (left + right)/2;
        }

        public boolean HasConverged(double precision)
        {
            return Math.abs(left - right) < precision;
        }
    }
}
