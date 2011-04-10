package org.sade.analyzers;


import cern.jet.math.Bessel;
import org.apache.commons.math.complex.Complex;

public class JacobiAngerErrorFuncDiffEvaluator implements GradientFunction
{
    private final Complex[] fourierCoefficients;

    public JacobiAngerErrorFuncDiffEvaluator(Complex[] fourierCoefficients)
    {
        this.fourierCoefficients = fourierCoefficients;
    }

    public double[] Gradient(double[] x)
    {
        double omegaSum = 0.0;
        double deltaSum = 0.0;
        double phiSum = 0.0;
        MinimizeParameters parameters = MinimizeParametersSugar.UnwrapToParams(x);
        int n = fourierCoefficients.length;
        SeriesStorage storage = new SeriesStorage(parameters, n);
        for (int i = -n + 1; i < n; i++)
        {
            Complex seriesMember = storage.SeriesMember(i);
            Complex omegaDiffSeriesMember = storage.OmegaDiffSeriesMember(i);
            Complex deltaDiffSeriesMember = storage.DeltaDiffSeriesMember(i);
            Complex phiDiffSeriesMember = storage.PhiDiffSeriesMember(i);
            Complex coeff = fourierCoefficients[Math.abs(i)];
            if (i < 0)
            {
                // We should conjugate coeff cause incoming fourier coefficients made only for positive omega
                coeff = coeff.conjugate();
            }
            omegaSum += SquareDiff(coeff, seriesMember, omegaDiffSeriesMember);
            deltaSum += SquareDiff(coeff, seriesMember, deltaDiffSeriesMember);
            phiSum += SquareDiff(coeff, seriesMember, phiDiffSeriesMember);
        }
        return new MinimizeParameters(omegaSum, deltaSum, phiSum).Wrap();
    }

    public double Value(double[] x)
    {
        double sum = 0.0;
        MinimizeParameters parameters = MinimizeParametersSugar.UnwrapToParams(x);
        int n = fourierCoefficients.length;
        SeriesStorage storage = new SeriesStorage(parameters, n);
        for (int i = -n + 1; i < n; i++)
        {
            Complex seriesMember = storage.SeriesMember(i);
            if (i < 0)
            {
                // We should conjugate seriesMember cause incoming fourier coefficients made only for positive omega
                seriesMember = seriesMember.conjugate();
            }
            double abs = (fourierCoefficients[Math.abs(i)].subtract(seriesMember)).abs();
            sum += abs*abs;
        }
        return sum;
    }

    private double SquareDiff(Complex coeff, Complex seriesMember, Complex diffSeriesMember)
    {
        return ((seriesMember.subtract(coeff)).multiply(diffSeriesMember.conjugate()).add(diffSeriesMember.multiply(seriesMember.subtract(coeff).conjugate()))).getReal();
    }


    private class SeriesStorage
    {
        private final MinimizeParameters parameters;
        private final BesselStorage storage;
        private final int n;
        private Complex deltaExp;
        private Complex mDeltaExp;
        private double[] omegaSins;
        private double[] minusOmegaSins;
        private double[] omegaCoss;
        private double[] minusOmegaCoss;

        private double[] diffOmegaCos;
        private double[] diffOmegaSin;
        private double[] diffMinusOmegaCos;
        private double[] diffMinusOmegaSin;

        private double[] diffOmegaCosByPhi;
        private double[] diffOmegaSinByPhi;
        private double[] diffMinusOmegaCosByPhi;
        private double[] diffMinusOmegaSinByPhi;

        private Complex[] iPowers;
        private Complex[] firstMultiply;
        private Complex[] secondMultiply;
        private Complex[] seriesMember;
        private Complex[] deltaDiffSeriesMember;
        private Complex[] omegaDiffSeriesMember;
        private Complex[] phiDiffSeriesMemeber;

        public SeriesStorage(MinimizeParameters parameters, int n)
        {
            this.parameters = parameters;
            this.storage = new BesselStorage(parameters.getOmega(), parameters.getPhi(), n);
            this.n = n;
            deltaExp = new Complex(0, this.parameters.getDelta()).exp().multiply(0.5);
            mDeltaExp = new Complex(0, -this.parameters.getDelta()).exp().multiply(0.5);
            InitArrays();
        }

        private void InitArrays()
        {
            // +1 for derivatives
            int evaluateCount = n*2 + 2;
            omegaSins = new double[evaluateCount];
            minusOmegaSins = new double[evaluateCount];
            omegaCoss = new double[evaluateCount];
            minusOmegaCoss = new double[evaluateCount];
            iPowers = new Complex[evaluateCount];
            firstMultiply = new Complex[evaluateCount];
            secondMultiply = new Complex[evaluateCount];

            int memberCount = n*2+1;
            diffOmegaCos = new double[memberCount];
            diffOmegaSin = new double[memberCount];
            diffMinusOmegaCos = new double[memberCount];
            diffMinusOmegaSin = new double[memberCount];

            diffOmegaCosByPhi = new double[memberCount];
            diffOmegaSinByPhi = new double[memberCount];
            diffMinusOmegaCosByPhi = new double[memberCount];
            diffMinusOmegaSinByPhi = new double[memberCount];

            seriesMember = new Complex[memberCount];
            deltaDiffSeriesMember = new Complex[memberCount];
            omegaDiffSeriesMember = new Complex[memberCount];
            phiDiffSeriesMemeber = new Complex[memberCount];

            for (int k = -n + 1; k < n + 1; k++)
            {
                omegaCoss[k + n + 1] = storage.BesselOmegaCos(k, 1);
                minusOmegaCoss[k + n + 1] = storage.BesselOmegaCos(k, -1);
                omegaSins[k + n + 1] = storage.BesselOmegaSin(k, 1);
                minusOmegaSins[k + n + 1] = storage.BesselOmegaSin(k, -1);
                iPowers[k + n + 1] = IPower(k);
                firstMultiply[k + n + 1] = deltaExp.multiply(iPowers[k + n + 1]).multiply(omegaCoss[k + n + 1]);
                secondMultiply[k + n + 1] = mDeltaExp.multiply(iPowers[k + n + 1]).multiply(minusOmegaCoss[k + n + 1]);
            }

            for (int k = -n + 1; k < n; k++)
            {
                double diffBesselOmegaCos = (-omegaCoss[k + n + 2] + k * omegaCoss[k + n + 1] / storage.omegaCosPhi);
                double diffBesselOmegaSin = (-omegaSins[k + n + 2] + k * omegaSins[k + n + 1] / storage.omegaSinPhi);
                double diffBesselMinusOmegaCos = (minusOmegaCoss[k + n + 2] + k * minusOmegaCoss[k + n + 1] / storage.omegaCosPhi);
                double diffBesselMinusOmegaSin = (minusOmegaSins[k + n + 2] + k * minusOmegaSins[k + n + 1] / storage.omegaSinPhi);

                diffOmegaCos[k + n + 1] = diffBesselOmegaCos * storage.cosPhi;
                diffOmegaSin[k + n + 1] = diffBesselOmegaSin * storage.sinPhi;
                diffMinusOmegaCos[k + n + 1] = diffBesselMinusOmegaCos * storage.cosPhi;
                diffMinusOmegaSin[k + n + 1] = diffBesselMinusOmegaSin * storage.sinPhi;

                diffOmegaCosByPhi[k + n + 1] = diffBesselOmegaCos * (-storage.omegaSinPhi);
                diffOmegaSinByPhi[k + n + 1] = diffBesselOmegaSin * storage.omegaCosPhi;
                diffMinusOmegaCosByPhi[k + n + 1] = diffBesselMinusOmegaCos * (-storage.omegaSinPhi);
                diffMinusOmegaSinByPhi[k + n + 1] = diffBesselMinusOmegaSin * storage.omegaCosPhi;
            }

            Complex plusI = new Complex(0, 1);
            Complex minusI = new Complex(0, -1);

            for (int j = -n + 1; j < n; j++)
            {
                for (int k = -n + 1; k < n; k++)
                {
                    int i = j + k;
                    if (i < -n + 1 || i > n - 1)
                    {
                        continue;
                    }
                    Complex firstMultiplyByMinusOmegaSins = firstMultiply[j + n + 1].multiply(minusOmegaSins[k + n + 1]);
                    Complex secondMultiplyByOmegaSins = secondMultiply[j + n + 1].multiply(omegaSins[k + n + 1]);

                    if (seriesMember[i + n + 1] == null) seriesMember[i + n + 1] = new Complex(0,0);
                    seriesMember[i + n + 1] = seriesMember[i + n + 1].add(firstMultiplyByMinusOmegaSins.add(secondMultiplyByOmegaSins));

                    if (deltaDiffSeriesMember[i + n + 1] == null) deltaDiffSeriesMember[i + n + 1] = new Complex(0,0);
                    deltaDiffSeriesMember[i + n + 1] = deltaDiffSeriesMember[i + n + 1].add(plusI.multiply(firstMultiplyByMinusOmegaSins).add(minusI.multiply(secondMultiplyByOmegaSins)));

                    Complex deltaExpByIPowers = deltaExp.multiply(iPowers[j + n + 1]);
                    Complex mDeltaExpByIPowers = mDeltaExp.multiply(iPowers[j + n + 1]);

                    if (omegaDiffSeriesMember[i + n + 1] == null) omegaDiffSeriesMember[i + n + 1] = new Complex(0,0);
                    omegaDiffSeriesMember[i + n + 1] = omegaDiffSeriesMember[i + n + 1].add(deltaExpByIPowers.multiply((diffOmegaCos[j + n + 1] * minusOmegaSins[k + n + 1] + omegaCoss[j + n + 1] * diffMinusOmegaSin[k + n + 1])).add(mDeltaExpByIPowers.multiply((diffMinusOmegaCos[j + n + 1] * omegaSins[k + n + 1] + minusOmegaCoss[j + n + 1] * diffOmegaSin[k + n + 1]))));

                    if (phiDiffSeriesMemeber[i + n + 1] == null) phiDiffSeriesMemeber[i + n + 1] = new Complex(0,0);
                    phiDiffSeriesMemeber[i + n + 1] = phiDiffSeriesMemeber[i + n + 1].add(deltaExpByIPowers.multiply((diffOmegaCosByPhi[j + n + 1] * minusOmegaSins[k + n + 1] + omegaCoss[j + n + 1] * diffMinusOmegaSinByPhi[k + n + 1])).add(mDeltaExpByIPowers.multiply((diffMinusOmegaCosByPhi[j + n + 1] * omegaSins[k + n + 1] + minusOmegaCoss[j + n + 1] * diffOmegaSinByPhi[k + n + 1]))));
                }
            }
        }

        public Complex SeriesMember(int i)
        {
            return seriesMember[i + n + 1];
        }

        public Complex DeltaDiffSeriesMember(int i)
        {
            return deltaDiffSeriesMember[i + n + 1];
        }

        public Complex OmegaDiffSeriesMember(int i)
        {
            return omegaDiffSeriesMember[i + n + 1];
        }

        public Complex PhiDiffSeriesMember(int i)
        {
            return phiDiffSeriesMemeber[i + n + 1];
        }

        private Complex IPower(int j)
        {
            switch (Math.abs(j)%4)
            {
                case 0:
                    return new Complex(1, 0);
                case 1:
                    return new Complex(0, 1*Math.signum(j));
                case 2:
                    return new Complex(-1, 0);
                case 3:
                    return new Complex(0, -1*Math.signum(j));
            }
            throw new RuntimeException("Never get here");
        }
    }

    private class BesselStorage
    {
        private final double omega;
        private final double phi;
        private final int n;
        private double[] besselOmegaCosPhi;
        private double[] besselOmegaSinPhi;
        public double omegaCosPhi;
        public double omegaSinPhi;
        public double cosPhi;
        public double sinPhi;

        public BesselStorage(double omega, double phi, int n)
        {
            this.omega = omega;
            this.phi = phi;
            this.n = n;
            Evaluate();
        }

        private void Evaluate()
        {
            // n + 1 for derivatives
            int evaluateCount = n + 1;
            besselOmegaSinPhi = new double[evaluateCount];
            besselOmegaCosPhi = new double[evaluateCount];

            sinPhi = Math.sin(phi);
            cosPhi = Math.cos(phi);
            omegaSinPhi = omega*sinPhi;
            omegaCosPhi = omega*cosPhi;
            for (int i = 0; i < evaluateCount; i++)
            {
                besselOmegaSinPhi[i] = Bessel.jn(i, omegaSinPhi);
                besselOmegaCosPhi[i] = Bessel.jn(i, omegaCosPhi);
            }
        }

        public double BesselOmegaCos(int order, int omegaSign)
        {
            return besselOmegaCosPhi[Math.abs(order)]*GetSign(order, omegaSign);
        }

        public double BesselOmegaSin(int order, int omegaSign)
        {
            return besselOmegaSinPhi[Math.abs(order)]*GetSign(order, omegaSign);
        }

        private int GetSign(int order, int omegaSign)
        {
            int sign = order%2 == 0 ? 1 : omegaSign;
            if (order < 0)
            {
                sign *= order%2 == 0 ? 1 : -1;
            }
            return sign;
        }
    }
}