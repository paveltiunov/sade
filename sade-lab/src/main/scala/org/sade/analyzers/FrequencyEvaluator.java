package org.sade.analyzers;

import org.apache.commons.math.complex.Complex;
import org.apache.commons.math.transform.FastFourierTransformer;

public class FrequencyEvaluator {
    public static double EvaluateFrequency(double[] truncData, double sampleRate) {
        FastFourierTransformer transformation = new FastFourierTransformer();

        Complex[] transform = transformation.transform(truncData);

        Amplitude[] amplitudes = new Amplitude[transform.length];
        for (int i = 0; i < amplitudes.length; i++) {
            amplitudes[i] = new Amplitude(transform[i].abs(), 1.0 - (i * 1.0 / amplitudes.length));
        }
        return FirstHarmonicFrequency(amplitudes);
    }

    private static double FirstHarmonicFrequency(Amplitude[] amplitudes) {
        double max = amplitudes[1].amp;
        Amplitude maxAmplitude = amplitudes[1];
        for (int i = 1; i < amplitudes.length; i++) {
            Amplitude amplitude = amplitudes[i];
            max = Math.max(amplitude.amp, max);
            if (max == amplitude.amp) maxAmplitude = amplitude;
        }
        return maxAmplitude.frequency;
    }

    private static class Amplitude {
        private final double amp;
        private final double frequency;

        public Amplitude(double amplitude, double frequency) {
            amp = amplitude;
            this.frequency = frequency;
        }
    }
}
