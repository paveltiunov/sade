package org.sade.analyzers;

import org.apache.commons.math.complex.Complex;
import org.sade.analyzers.math.FFT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JacobiAngerAnalyzer
{
    private AnalyzeResult lastAnalyzeResult;
    private double[] previousSamples;
    private final double Precision = 1E-4;
    private final int MaxIterations = 2000;
    private final int FourierCoeffCount = 8;
    private final int omegaSlices = 3;
    private final int phaseSlices = 6;

    public MinimizeResult AnalyzeSample(double[] sample, double omega, double delta, double phi)
    {
        return AnalyzeSample(sample, new MinimizeParameters(omega, delta, phi));
    }

    private MinimizeResult AnalyzeSample(double[] sample, MinimizeParameters minimizeParameters)
    {
        Complex[] firstCoeff = GetFirstCoefficients(ReScale(sample));
        MinimizeResult result = new MinimizeResult(0, minimizeParameters);
        for (int i = 4; i <= firstCoeff.length; i*=2)
        {
            result = MinimizeErrorFunction(result.getParameters(), Arrays.copyOf(firstCoeff, i));
        }
        return result;
    }

    private double[] ReScale(double[] sample)
    {
        double min = sample[0];
        for (double v : sample) {
            min = Math.min(v,min);
        }
        double max = sample[0];
        for (double v : sample) {
            max = Math.max(v,max);
        }
        double amplitude = (max - min)/2;
        double center = min + amplitude;
        double[] result = new double[sample.length];
        for (int i = 0; i < sample.length; i++) {
            double v = sample[i];
            result[i] = (v - center)/amplitude;
        }
        return result;
    }

    private Complex[] GetFirstCoefficients(double[] sample)
    {
        return FFT.transform(sample, FourierCoeffCount);
    }

    private MinimizeResult MinimizeErrorFunction(MinimizeParameters initialParams, Complex[] fourierCoefficients)
    {
        JacobiAngerErrorFuncDiffEvaluator gradientFunction = new JacobiAngerErrorFuncDiffEvaluator(fourierCoefficients);
        BisectionGradientOptimizer descentOptimizer = new BisectionGradientOptimizer(Precision, gradientFunction, new MinimizeParameters(1.5,Math.PI/2,Math.PI/2).Wrap());

        double[] result = descentOptimizer.Optimize(initialParams.Wrap());
        double error = gradientFunction.Value(result);
        return new MinimizeResult(error, MinimizeParametersSugar.UnwrapToParams(result));
    }

    public boolean IsFirstAnalyze()
    {
        return lastAnalyzeResult == null;
    }

    public void ResetAnalyze()
    {
        lastAnalyzeResult = null;
    }

    public List<AnalyzeResult> DivideAndAnalyze(double[] sample)
    {
        List<AnalyzeResult> analyzeResults = new ArrayList<AnalyzeResult>();

        int searchPeriod = IsFirstAnalyze() ? ScanPeriod(sample) : lastAnalyzeResult.getPeriod();
        if (!IsFirstAnalyze())
        {
            double[] newSample = Arrays.copyOf(previousSamples, previousSamples.length + sample.length);
            System.arraycopy(sample, 0, newSample, previousSamples.length, sample.length);
            sample = newSample;
        }

        final double searchPeriodCoeff = 0.01;

        for (; sample.length > Math.round(searchPeriod * (1 + searchPeriodCoeff))*2+1; sample = skip(sample, searchPeriod))
        {
            double[] sampleToAnalyze = take(sample, searchPeriod);
            if (IsFirstAnalyze())
            {
                lastAnalyzeResult = new AnalyzeResult(ScanForEntryParameters(sampleToAnalyze).getParameters(), searchPeriod);
            }
            else
            {
                searchPeriod = SearchPeriod(sample, (int)Math.round(lastAnalyzeResult.getPeriod() * (1 - searchPeriodCoeff)), (int)Math.round(lastAnalyzeResult.getPeriod() * (1 + searchPeriodCoeff)));
                lastAnalyzeResult = new AnalyzeResult(AnalyzeSample(sampleToAnalyze, lastAnalyzeResult.getParameters()).getParameters(), searchPeriod);
            }
            analyzeResults.add(lastAnalyzeResult);
        }
        previousSamples = sample;
        return analyzeResults;
    }

    public static double[] take(double[] sample, int searchPeriod) {
        return Arrays.copyOf(sample, Math.min(searchPeriod, sample.length));
    }

    public static double[] skip(double[] sample, int searchPeriod) {
        int destLength = sample.length - searchPeriod;
        if (destLength < 0) {
            return new double[0];
        }
        double[] dest = new double[destLength];
        System.arraycopy(sample, searchPeriod, dest, 0, destLength);
        return dest;
    }

    private int ScanPeriod(double[] sample)
    {
        int log = (int)Math.pow(2, Math.floor(Math.log(sample.length) / Math.log(2)));
        double frequency = FrequencyEvaluator.evaluateFrequency(take(sample, log));
        double period = 1/frequency;
        int searchPeriod = (int) period;
        searchPeriod = SearchPeriod(sample, (int)Math.round(searchPeriod * 0.95), (int)Math.round(searchPeriod * 1.05));
        return searchPeriod;
    }

    private MinimizeResult ScanForEntryParameters(double[] sampleToAnalyze)
    {
        MinimizeResult result;
        List<MinimizeResult> results = new ArrayList<MinimizeResult>();
        for (int i = 0; i < phaseSlices; i++)
        {
            for (int j = 0; j < phaseSlices; j++)
            {
                for (int k = 0; k < omegaSlices; k++)
                {
                    MinimizeResult minimizeResult = AnalyzeSample(sampleToAnalyze,
                                                       new MinimizeParameters(2 + k*1.0/omegaSlices, Math.PI/phaseSlices*i, 2*Math.PI/phaseSlices*j));
                    double delta = minimizeResult.getParameters().getDelta();
                    if (delta > Math.PI || delta < 0.0) continue;
                    results.add(minimizeResult);
                }
            }
        }
        double min = results.get(0).getError();
        MinimizeResult minResult = null;
        for (MinimizeResult minimizeResult : results) {
            min = Math.min(min, minimizeResult.getError());
            if (min == minimizeResult.getError()) minResult = minimizeResult;
        }
        return minResult;
    }

    private int SearchPeriod(double[] sample, int from, int to)
    {
        List<Point> dotProducts = new ArrayList<Point>();
        double[] twoPeriods = take(sample, to * 2);
        for (int i = from; i < to+1; i++)
        {
            double xx = 0.0;
            double yy = 0.0;
            double xy = 0.0;
            for (int j = 0; j < i; j++)
            {
                double x = twoPeriods[j];
                double y = twoPeriods[i+j];
                xx += x*x;
                yy += y*y;
                xy += x*y;
            }
            dotProducts.add(new Point(i, xy / (Math.sqrt(xx) * Math.sqrt(yy))));
        }
        double max = dotProducts.get(0).value;
        int maxI = dotProducts.get(0).i;
        for (Point point : dotProducts) {
            max = Math.max(max, point.value);
            if (max == point.value) maxI = point.i;
        }
        return maxI;
    }

}
