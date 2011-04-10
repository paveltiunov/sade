package org.sade.analyzers;

class MinimizeParametersSugar
{
    public static MinimizeParameters UnwrapToParams(double[] doubleParams)
    {
        return new MinimizeParameters(doubleParams[0], doubleParams[1], doubleParams[2]);
    }
}
